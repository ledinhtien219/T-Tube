package com.carhud.aaproxy

import android.app.Presentation
import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ProjectionDensityRuntimeTest {
    @Test fun cachedWebViewUsesCurrentDisplayDensityAndViewport() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        var web: WebView? = null
        try {
            // Reuse one Chromium view while moving between densities and shapes.
            for ((width, height, dpi) in listOf(Triple(800, 480, 160), Triple(1280, 480, 160), Triple(1920, 720, 240),
                    Triple(1920, 480, 160), Triple(720, 1280, 240))) {
                val loaded = CountDownLatch(1)
                val measured = CountDownLatch(1)
                var result = ""
                var reader: ImageReader? = null
                var presentation: Presentation? = null
                var virtualDisplay: android.hardware.display.VirtualDisplay? = null
                try {
                    instrumentation.runOnMainSync {
                        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
                        reader!!.setOnImageAvailableListener({ it.acquireLatestImage()?.close() }, Handler(Looper.getMainLooper()))
                        virtualDisplay = (app.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                            .createVirtualDisplay("DpiRegression", width, height, dpi, reader!!.surface,
                                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION)
                        presentation = Presentation(app, virtualDisplay!!.display)
                        web?.takeIf { ProjectionWebViewFactory.needsRecreation(it, presentation!!.context) }?.let {
                            it.destroy()
                            web = null
                        }
                        val current = web ?: ProjectionWebViewFactory.create(presentation!!.context).also { web = it }
                        (current.context as MutableContextWrapper).baseContext = presentation!!.context
                        current.settings.javaScriptEnabled = true
                        current.settings.useWideViewPort = true
                        current.settings.loadWithOverviewMode = false
                        current.setInitialScale(100)
                        current.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) { loaded.countDown() }
                        }
                        presentation!!.setContentView(current)
                        presentation!!.show()
                        current.dispatchConfigurationChanged(presentation!!.context.resources.configuration)
                        assertEquals(dpi, current.resources.displayMetrics.densityDpi)
                        current.loadDataWithBaseURL("https://density.test/", """
                            <meta name="viewport" content="width=device-width,initial-scale=1">
                            <style>html,body{margin:0}video{width:100%;height:100%;object-fit:contain}</style>
                            <video></video>
                        """.trimIndent(), "text/html", "UTF-8", null)
                    }
                    assertTrue("Local viewport did not load", loaded.await(10, TimeUnit.SECONDS))
                    instrumentation.runOnMainSync {
                        web!!.evaluateJavascript("JSON.stringify({dpr:devicePixelRatio,w:innerWidth,css:document.querySelector('video').getBoundingClientRect().width})") {
                            result = it
                            measured.countDown()
                        }
                    }
                    assertTrue(measured.await(5, TimeUnit.SECONDS))
                    val json = JSONObject(org.json.JSONTokener(result).nextValue() as String)
                    assertEquals("DPR after move to ${width}x$height", dpi / 160.0, json.getDouble("dpr"), .05)
                    assertEquals("Video must fill CSS viewport", json.getDouble("w"), json.getDouble("css"), 2.0)
                    assertEquals("Viewport width must follow car density", width * 160.0 / dpi, json.getDouble("w"), 3.0)
                } finally {
                    instrumentation.runOnMainSync {
                        (web?.parent as? android.view.ViewGroup)?.removeView(web)
                        presentation?.dismiss()
                        virtualDisplay?.release()
                        reader?.close()
                    }
                }
            }
        } finally {
            instrumentation.runOnMainSync { web?.destroy() }
        }
    }
}
