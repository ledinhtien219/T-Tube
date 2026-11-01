package com.carhud.aaproxy

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.util.Collections
import java.util.WeakHashMap

/**
 * YouTube-only cosmetic feed ad filtering and native 4-column PiP browsing.
 *
 * Preserves original YouTube cards/video surfaces. No extra WebView, media
 * interception, seek, playback-rate overrides or continuous whole-page scan.
 */
object YouTubeFeedPipEnhancer {
    private val registered = Collections.newSetFromMap(WeakHashMap<WebView, Boolean>())

    private fun script(view: WebView): String? = try {
        view.context.assets.open("youtube_feed_pip_enhancer.js")
            .bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    }

    /** Register before first navigation, where supported. */
    fun install(view: WebView) {
        val js = script(view) ?: return
        synchronized(registered) {
            if (!registered.add(view)) return
        }
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(
                    view, js,
                    setOf("https://www.youtube.com", "https://m.youtube.com", "https://music.youtube.com")
                )
            }
        } catch (_: Throwable) {
            // Some Android WebView versions lack document-start injection.
            // onPageFinished still installs the same self-contained CSS/JS.
        }
    }

    /** Refresh CSS and native card grid on YouTube SPA and PiP transitions. */
    fun inject(view: WebView) {
        val host = try {
            android.net.Uri.parse(view.url ?: "").host?.lowercase()
        } catch (_: Throwable) {
            null
        }
        if (host != "youtube.com" && host?.endsWith(".youtube.com") != true) return
        val js = script(view) ?: return
        view.post {
            try { view.evaluateJavascript(js, null) } catch (_: Throwable) {}
        }
    }
}
