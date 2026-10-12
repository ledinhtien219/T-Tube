package com.carhud.aaproxy

import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView

/** The mobile TikTok search shell sends searches to the app; desktop serves web results. */
internal object TikTokWebPolicy {
    fun isTikTok(url: String?): Boolean {
        val host = Uri.parse(url.orEmpty()).host.orEmpty().lowercase()
        return host == "tiktok.com" || host.endsWith(".tiktok.com")
    }

    fun isSearch(url: String?): Boolean = isTikTok(url) &&
        Uri.parse(url.orEmpty()).path.orEmpty().let { it == "/search" || it.startsWith("/search/") }

    fun userAgent(defaultUa: String, desktop: Boolean): String {
        val mobile = defaultUa.replace("; wv", "").replace(Regex("Version/[\\d.]+\\s?"), "")
        return if (desktop) mobile.replace(Regex("\\([^)]*\\)"), "(X11; Linux x86_64)")
            .replace("Mobile ", "") else mobile
    }

    fun configure(view: WebView, url: String?) {
        if (!isTikTok(url)) return
        val ua = userAgent(WebSettings.getDefaultUserAgent(view.context), isSearch(url))
        if (view.settings.userAgentString != ua) view.settings.userAgentString = ua
    }

    fun fitSearchResults(view: WebView, url: String?) {
        if (!isSearch(url)) return
        view.evaluateJavascript("""
            (function() {
                if (!/^\/search(?:\/|$)/.test(location.pathname)) return;
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) { meta = document.createElement('meta'); meta.name = 'viewport'; document.head.appendChild(meta); }
                meta.content = 'width=980';
            })();
        """.trimIndent(), null)
    }
}
