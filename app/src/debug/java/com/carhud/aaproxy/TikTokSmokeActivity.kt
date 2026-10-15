package com.carhud.aaproxy

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * Debug-only host used by CI to exercise the exact TikTok WebView policy against
 * the live tiktok.com site. It is not present in release builds.
 */
class TikTokSmokeActivity : Activity() {
    lateinit var webView: WebView
        private set

    @Volatile
    var lastFinishedUrl: String = ""
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WebView.setWebContentsDebuggingEnabled(true)
        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(FrameLayout(this).apply { addView(webView) })

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val target = request.url.toString()
                if (TikTokWebPolicy.isAppRedirect(target)) return true
                if (request.isForMainFrame && TikTokWebPolicy.isTikTok(target)) {
                    TikTokWebPolicy.configure(view, target)
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (TikTokWebPolicy.isTikTok(url)) {
                    TikTokWebPolicy.configure(view, url)
                    TikTokWebPolicy.fitLayout(view, url)
                }
            }

            override fun onPageCommitVisible(view: WebView, url: String) {
                super.onPageCommitVisible(view, url)
                TikTokWebPolicy.fitLayout(view, url)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (view != null) TikTokWebPolicy.fitLayout(view, url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                lastFinishedUrl = url
                TikTokWebPolicy.fitLayout(view, url)
            }
        }

        loadHome()
    }

    fun loadHome() {
        val target = "https://www.tiktok.com/"
        TikTokWebPolicy.configure(webView, target)
        webView.loadUrl(target)
    }

    fun loadSearch(query: String) {
        val target = TikTokWebPolicy.searchUrl(query)
        TikTokWebPolicy.configure(webView, target)
        webView.loadUrl(target)
    }

    override fun onDestroy() {
        runCatching {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
