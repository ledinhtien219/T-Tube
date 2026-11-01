package com.carhud.aaproxy

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.mlm.adblock.AdblockEngine
import org.mlm.adblock.RequestTypeMapper
import java.io.File
import java.lang.ref.WeakReference
import java.net.URL
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

/**
 * Experimental Brave adblock-rust engine for the YouTube WebView ONLY.
 *
 * Filter compilation and downloads run off the UI/WebView request threads.
 * Requests fail OPEN on initialization errors. Media, YouTube player APIs,
 * non-GET requests and navigations are never intercepted.
 *
 * The JNI library is licensed MPL-2.0; see docs/brave-shields-experiment.md.
 */
object BraveYouTubeShields {
    private const val CACHE_FILE = "brave-youtube-filters.txt"
    private const val CACHE_AGE_MS = 72L * 60 * 60 * 1000
    private const val MAX_LIST_BYTES = 3_500_000
    private const val MAX_RULE_BYTES = 7_000_000
    private const val MAX_START_JS = 60_000

    // Official upstream list maintainers. Never execute remote JavaScript files;
    // only feed rule text to the audited rule parser and its bundled resources.
    private val lists = listOf(
        "https://easylist.to/easylist/easylist.txt",
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/quick-fixes.txt",
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/unbreak.txt"
    )

    private val initialRules = """
        ! Safe offline baseline: ad hosts only, no first-party media blocking.
        ||doubleclick.net^
        ||googlesyndication.com^
        ||googleadservices.com^
    """.trimIndent()

    private val started = AtomicBoolean(false)
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "ttube-brave-shields").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())
    private val gate = Any()
    private val views = Collections.newSetFromMap(WeakHashMap<WebView, Boolean>())
    private val injected = Collections.newSetFromMap(WeakHashMap<WebView, Boolean>())
    @Volatile private var engine: AdblockEngine? = null
    @Volatile private var youtubeActive = false

    fun start(context: Context, view: WebView) {
        synchronized(views) { views.add(view) }
        if (!started.compareAndSet(false, true)) {
            if (engine != null) installScriptlets(view)
            return
        }
        val app = context.applicationContext
        worker.execute {
            try {
                val cache = File(app.cacheDir, CACHE_FILE)
                val cached = try {
                    cache.takeIf { it.exists() && it.length() in 1..MAX_RULE_BYTES.toLong() }
                        ?.readText()
                } catch (_: Exception) { null }
                val rules = cached ?: initialRules
                replaceEngine(app, rules)
                if (cache.lastModified() + CACHE_AGE_MS < System.currentTimeMillis()) {
                    val updated = downloadLists() ?: return@execute
                    // Validate all lists and engine before replacing the working instance.
                    if (replaceEngine(app, updated)) {
                        try { cache.writeText(updated) } catch (_: Exception) {}
                    }
                }
            } catch (_: Throwable) {
                // Fail open: the existing native Skip and SponsorBlock still work.
            }
        }
    }

    fun setYouTubePage(url: String?) {
        val host = try { Uri.parse(url ?: "").host?.lowercase() } catch (_: Exception) { null }
        youtubeActive = isYouTubeHost(host)
    }

    fun intercept(request: WebResourceRequest?): WebResourceResponse? {
        if (!youtubeActive || request == null || request.isForMainFrame ||
            !request.method.equals("GET", ignoreCase = true)) return null
        val uri = request.url ?: return null
        if (uri.scheme != "https" || isProtectedYouTubeTraffic(uri)) return null
        val source = request.requestHeaders["Referer"] ?: request.requestHeaders["referer"]
        // Restrict to a known YouTube document, not arbitrary app/browser pages.
        val sourceHost = try { Uri.parse(source ?: "").host } catch (_: Exception) { null }
        if (sourceHost != null && !isYouTubeHost(sourceHost.lowercase())) return null
        val type = RequestTypeMapper.from(uri, request.requestHeaders["Accept"])
        if (type == "media" || type == "document" || type == "subdocument") return null
        val blocked = synchronized(gate) {
            try {
                engine?.checkNetworkRequest(uri.toString(), source ?: "https://www.youtube.com/", type)
                    ?.let { it.matched && !it.exception && it.redirect.isNullOrEmpty() } ?: false
            } catch (_: Throwable) { false }
        }
        if (!blocked) return null
        return YouTubeAdBlocker.createEmptyResponse()
    }

    private fun isYouTubeHost(host: String?): Boolean =
        host == "youtube.com" || host?.endsWith(".youtube.com") == true ||
            host == "youtu.be"

    private fun isProtectedYouTubeTraffic(uri: Uri): Boolean {
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path?.lowercase().orEmpty()
        return host == "googlevideo.com" || host.endsWith(".googlevideo.com") ||
            host == "youtube.com" && path == "/watch" ||
            path.contains("/videoplayback") || path.endsWith(".m3u8") ||
            path.endsWith(".mpd") || path.contains("/youtubei/v1/player") ||
            path.contains("/youtubei/v1/next") ||
            path.contains("/youtubei/v1/browse") ||
            path.contains("/youtubei/v1/search") ||
            host == "youtube.googleapis.com" || host == "youtubei.googleapis.com"
    }

    private fun replaceEngine(context: Context, rules: String): Boolean {
        var candidate: AdblockEngine? = null
        return try {
            candidate = AdblockEngine.create()
            if (!candidate.loadFilterList(rules)) return false
            // Packaged by the JNI library; enables trusted Brave scriptlet definitions.
            val resources = context.assets.open("adblock/resources.json")
                .bufferedReader().use { it.readText() }
            if (!candidate.loadResources(resources)) return false
            synchronized(gate) {
                val previous = engine
                engine = candidate
                candidate = null
                previous?.close()
            }
            synchronized(views) {
                views.toList().forEach { view -> main.post { installScriptlets(view) } }
            }
            true
        } catch (_: Throwable) {
            false
        } finally {
            try { candidate?.close() } catch (_: Throwable) {}
        }
    }

    private fun installScriptlets(view: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        try {
            // Scriptlets only on YouTube origins, and only once rules are
            // available; do not mark the WebView injected on an empty cache.
            val cosmetic = synchronized(gate) {
                engine?.cosmeticResources("https://www.youtube.com/watch?v=ttube")
            } ?: return
            val js = cosmetic.js
            if (js.isBlank() || js.length > MAX_START_JS) return
            synchronized(injected) { if (!injected.add(view)) return }
            WebViewCompat.addDocumentStartJavaScript(
                view,
                "(function(){try{\n" + js + "\n}catch(e){}})();",
                setOf("https://www.youtube.com", "https://m.youtube.com", "https://music.youtube.com")
            )
        } catch (_: Throwable) {
            synchronized(injected) { injected.remove(view) }
        }
    }

    private fun downloadLists(): String? {
        val combined = StringBuilder(initialRules).append('\n')
        for (url in lists) {
            try {
                val conn = URL(url).openConnection() as HttpsURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 7000
                conn.instanceFollowRedirects = false
                try {
                    if (conn.responseCode != 200 || conn.contentLengthLong > MAX_LIST_BYTES) return null
                    val bytes = conn.inputStream.use { stream ->
                        // Android 10 InputStream compatibility: no Java 9
                        // readNBytes() call that may be absent on older ART.
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(16 * 1024)
                        var count = 0
                        while (true) {
                            val read = stream.read(buf)
                            if (read < 0) break
                            count += read
                            if (count > MAX_LIST_BYTES) return null
                            out.write(buf, 0, read)
                        }
                        out.toByteArray()
                    }
                    combined.append(String(bytes, Charsets.UTF_8)).append('\n')
                    if (combined.length > MAX_RULE_BYTES) return null
                } finally { conn.disconnect() }
            } catch (_: Exception) {
                // Keep the previous compiled rules; do not replace with partial lists.
                return null
            }
        }
        return combined.toString()
    }
}
