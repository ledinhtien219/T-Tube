package com.carhud.aaproxy

import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView

/** Use one desktop identity for feed, search and history, including guest browsing. */
internal object TikTokWebPolicy {
    fun isTikTok(url: String?): Boolean {
        val host = Uri.parse(url.orEmpty()).host.orEmpty().lowercase()
        return host == "tiktok.com" || host.endsWith(".tiktok.com")
    }

    fun isSearch(url: String?): Boolean = isTikTok(url) &&
        Uri.parse(url.orEmpty()).path.orEmpty().let { it == "/search" || it.startsWith("/search/") }

    fun userAgent(defaultUa: String): String {
        val mobile = defaultUa.replace("; wv", "").replace(Regex("Version/[\\d.]+\\s?"), "")
        return mobile.replace(Regex("\\([^)]*\\)"), "(X11; Linux x86_64)")
            .replace("Mobile ", "")
    }

    fun configure(view: WebView, url: String?) {
        if (!isTikTok(url)) return
        val ua = userAgent(WebSettings.getDefaultUserAgent(view.context))
        if (view.settings.userAgentString != ua) view.settings.userAgentString = ua
    }

    fun fitLayout(view: WebView, url: String?) {
        if (!isTikTok(url)) return
        view.evaluateJavascript("""
            (function() {
                if (!(location.hostname === 'tiktok.com' || location.hostname.endsWith('.tiktok.com'))) return;
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) { meta = document.createElement('meta'); meta.name = 'viewport'; document.head.appendChild(meta); }
                meta.content = 'width=device-width, initial-scale=1';
                var style = document.getElementById('thtv-tiktok-layout');
                if (!style) { style = document.createElement('style'); style.id = 'thtv-tiktok-layout'; document.head.appendChild(style); }
                // The site's minimum 348px video width otherwise clips portrait
                // videos on a landscape display shorter than 650px.
                style.textContent = '[data-e2e="feed-video"] { min-width: 0 !important; min-height: 0 !important; }';
            })();
        """.trimIndent(), null)
    }
}
