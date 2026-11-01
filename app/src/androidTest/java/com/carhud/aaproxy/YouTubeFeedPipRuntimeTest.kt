package com.carhud.aaproxy

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android 15 WebView smoke: execute the REAL shipped JS against a native
 * watch-page card DOM. No network or YouTube login required.
 */
@RunWith(AndroidJUnit4::class)
class YouTubeFeedPipRuntimeTest {
    @Test
    @SuppressLint("SetJavaScriptEnabled")
    fun pipGridHasFourColumnsAndRemovesFeedAdWithoutDeletingNativeCards() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val js = inst.targetContext.assets.open("youtube_feed_pip_enhancer.js")
            .bufferedReader().use { it.readText() }
        val latch = CountDownLatch(1)
        val passed = AtomicBoolean(false)
        var view: WebView? = null
        inst.runOnMainSync {
            view = WebView(inst.targetContext)
            val web = view!!
            web.settings.javaScriptEnabled = true
            web.webViewClient = object : WebViewClient() {
                override fun onPageFinished(v: WebView, url: String?) {
                    v.evaluateJavascript(js) {
                        // The enhancement intentionally debounces DOM work by
                        // 180ms to avoid scroll jank; test after that settle.
                        v.postDelayed({
                            v.evaluateJavascript(
                            """
                            (function(){
                                var grid=document.querySelector('[data-ttube-four-col="1"]');
                                var ad=document.querySelector('ytd-ad-slot-renderer');
                                var ordinary=document.querySelector('a[href*="/watch?v="]');
                                var style=document.getElementById('ttube-feed-pip-v2');
                                return !!grid && !!ad && !!ordinary && !!style &&
                                  getComputedStyle(grid).display==='grid' &&
                                  getComputedStyle(ad).display==='none' &&
                                  style.textContent.includes('grid-template-columns:repeat(4,minmax(0,1fr))');
                            })();
                            """.trimIndent()
                            ) { value ->
                                passed.set(value == "true")
                                latch.countDown()
                            }
                        }, 650L)
                    }
                }
            }
            val markup = """
                <!doctype html><html class="carhud-youtube-pip"><head></head><body>
                <ytd-watch-flexy>
                  <div id="secondary" data-carhud-rec-scroll="1">
                    <ytd-watch-next-secondary-results-renderer>
                      <div id="items">
                        <ytd-compact-video-renderer><a href="/watch?v=123456789ab">Video A</a></ytd-compact-video-renderer>
                        <ytd-compact-video-renderer><a href="/watch?v=123456789ac">Video B</a></ytd-compact-video-renderer>
                        <ytd-compact-video-renderer><a href="/watch?v=123456789ad">Video C</a></ytd-compact-video-renderer>
                        <ytd-compact-video-renderer><a href="/watch?v=123456789ae">Video D</a></ytd-compact-video-renderer>
                        <ytd-rich-item-renderer><ytd-ad-slot-renderer>Quảng cáo</ytd-ad-slot-renderer></ytd-rich-item-renderer>
                      </div>
                    </ytd-watch-next-secondary-results-renderer>
                  </div>
                </ytd-watch-flexy>
                </body></html>
            """.trimIndent()
            web.loadDataWithBaseURL(
                "https://www.youtube.com/watch?v=123456789ab",
                markup, "text/html", "UTF-8", null
            )
        }
        try {
            assertTrue("WebView did not execute the enhancer", latch.await(20, TimeUnit.SECONDS))
            assertTrue("Four-column PiP or feed ad CSS not active", passed.get())
        } finally {
            inst.runOnMainSync { view?.destroy() }
        }
    }
}
