package com.carhud.aaproxy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AppSwitchRuntimeTest {
    @Test fun repeatedAndRapidSwitchesDoNotLoadYoutubeHomeOrRestartTheSameApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        val requests = CopyOnWriteArrayList<String>()
        var reader: ImageReader? = null
        var vd: android.hardware.display.VirtualDisplay? = null
        var web: WebView? = null
        var presentation: CarPresentation? = null
        val html = "<meta name='viewport' content='width=device-width,initial-scale=1'><title>Switch fixture</title><body>Local navigation fixture</body>"
        val loaded = CountDownLatch(1)
        val webUrl = "https://switch-fixture.test/browser"
        val youtubeUrl = "https://m.youtube.com/results?search_query=switch-fixture"
        val browser = WebAppItem("web", "Browser", webUrl)
        val youtube = WebAppItem("youtube", "YouTube", youtubeUrl)
        try {
            instrumentation.runOnMainSync {
                CarMediaManager.clearWebAppSession("web")
                CarMediaManager.clearWebAppSession("youtube")
                CarMediaManager.activeAppId = "youtube"
                CarMediaManager.userWantsPlayback = false
                reader = ImageReader.newInstance(800, 480, PixelFormat.RGBA_8888, 2)
                reader!!.setOnImageAvailableListener({ it.acquireLatestImage()?.close() }, Handler(Looper.getMainLooper()))
                vd = (app.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                    .createVirtualDisplay("SwitchRegression", 800, 480, 160, reader!!.surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION)
                web = ProjectionWebViewFactory.create(app.createDisplayContext(vd!!.display))
                web!!.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) { loaded.countDown() }
                }
                web!!.loadDataWithBaseURL("https://m.youtube.com/fixture-seed", html, "text/html", "UTF-8", null)
            }
            assertTrue(loaded.await(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                presentation = CarPresentation(app, vd!!.display, web)
                presentation!!.show()
                val delegate = web!!.webViewClient
                web!!.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                        if (request.isForMainFrame) requests.add(request.url.toString())
                        return WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(html.toByteArray()))
                    }
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) { delegate.onPageStarted(view, url, favicon) }
                    override fun onPageCommitVisible(view: WebView, url: String) { delegate.onPageCommitVisible(view, url) }
                    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) { delegate.doUpdateVisitedHistory(view, url, isReload) }
                    override fun onPageFinished(view: WebView, url: String) { delegate.onPageFinished(view, url) }
                }
                presentation!!.switchWebApp(browser)
            }
            fun waitFor(url: String) {
                val deadline = SystemClock.uptimeMillis() + 10_000
                while (SystemClock.uptimeMillis() < deadline) {
                    var ready = false
                    instrumentation.runOnMainSync { ready = web!!.url == url && web!!.progress == 100 }
                    if (ready) return
                    SystemClock.sleep(50)
                }
                fail("Navigation did not settle on $url; requests=$requests")
            }
            waitFor(webUrl)
            val browserRequests = requests.count { it == webUrl }
            instrumentation.runOnMainSync { repeat(12) { presentation!!.switchWebApp(browser) } }
            SystemClock.sleep(500)
            assertEquals("Repeated app taps must not load it again", browserRequests, requests.count { it == webUrl })
            instrumentation.runOnMainSync { presentation!!.switchWebApp(youtube, startUrl = youtubeUrl) }
            waitFor(youtubeUrl)
            assertEquals("A fresh YouTube target must load once", 1, requests.count { it == youtubeUrl })
            instrumentation.runOnMainSync {
                repeat(4) {
                    presentation!!.switchWebApp(browser, startUrl = webUrl)
                    presentation!!.switchWebApp(youtube, startUrl = youtubeUrl)
                }
            }
            waitFor(youtubeUrl)
            SystemClock.sleep(1000)
            instrumentation.runOnMainSync {
                assertEquals(youtubeUrl, web!!.url)
                assertFalse("Switching apps must not force playback", CarMediaManager.userWantsPlayback)
                repeat(8) { presentation!!.showDashboard(); presentation!!.switchWebApp(youtube) }
            }
            assertFalse("A stale page must not trigger an extra home load", requests.any { it == "https://m.youtube.com" || it == "https://m.youtube.com/" })
        } finally {
            instrumentation.runOnMainSync {
                presentation?.dismiss()
                web?.let { CarMediaManager.unregisterCarWebView(it); it.destroy() }
                vd?.release()
                reader?.close()
            }
        }
    }
}
