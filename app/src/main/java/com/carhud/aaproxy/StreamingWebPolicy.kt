package com.carhud.aaproxy

import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Browser compatibility policy for built-in Vietnamese streaming services.
 *
 * Keep these sites on their real web origins. T Tube only supplies a normal
 * browser-like WebView identity, persistent cookies and same-window login
 * navigation so auth/subdomain redirects do not get lost on Android Auto.
 */
internal object StreamingWebPolicy {
    private fun host(url: String?): String =
        runCatching { Uri.parse(url.orEmpty()).host.orEmpty().lowercase() }.getOrDefault("")

    private fun inDomainFamily(host: String, root: String): Boolean =
        host == root || host.endsWith(".$root")

    fun isTv360(url: String?): Boolean = inDomainFamily(host(url), "tv360.vn")

    fun isVtvGo(url: String?): Boolean = inDomainFamily(host(url), "vtvgo.vn")

    fun ownsUrl(appId: String, url: String?): Boolean = when (appId) {
        "tv360" -> isTv360(url)
        "vtvgo" -> isVtvGo(url)
        else -> false
    }

    fun browserUserAgent(view: WebView): String =
        WebSettings.getDefaultUserAgent(view.context)
            .replace("; wv", "")
            .replace(Regex("Version/\\d+\\.\\d+\\s?"), "")

    fun configure(view: WebView, appId: String, url: String?) {
        if (appId !in setOf("tv360", "vtvgo") && !isTv360(url) && !isVtvGo(url)) return

        val browserUa = browserUserAgent(view)
        view.settings.apply {
            if (userAgentString != browserUa) userAgentString = browserUa
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = false
            cacheMode = WebSettings.LOAD_DEFAULT
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }

        runCatching {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAcceptThirdPartyCookies(view, true)
                }
            }
        }
    }

    fun onPageFinished(view: WebView, appId: String, url: String?) {
        if (!ownsUrl(appId, url)) return
        installLoginNavigationBridge(view, url)
        runCatching { CookieManager.getInstance().flush() }
    }

    private fun installLoginNavigationBridge(view: WebView, url: String?) {
        val path = runCatching { Uri.parse(url.orEmpty()).path.orEmpty().lowercase() }.getOrDefault("")
        val loginLike = path.contains("login") ||
            path.contains("signin") ||
            path.contains("sign-in") ||
            path.contains("account") ||
            path.contains("auth")
        if (!loginLike) return

        view.evaluateJavascript(
            """
            (function() {
              try {
                if (window.__ttubeLoginBridgeInstalled) return;
                window.__ttubeLoginBridgeInstalled = true;

                document.addEventListener('click', function(e) {
                  var target = e.target;
                  var a = target && target.closest ? target.closest('a[target="_blank"]') : null;
                  if (a) a.setAttribute('target', '_self');
                }, true);

                var nativeOpen = window.open;
                window.open = function(url, target, features) {
                  try {
                    if (url && typeof url === 'string') {
                      location.assign(url);
                      return window;
                    }
                  } catch (_) {}
                  return nativeOpen ? nativeOpen.call(window, url, '_self', features) : window;
                };
              } catch (_) {}
            })();
            """.trimIndent(),
            null
        )
    }
}
