package com.carhud.aaproxy

import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class TikTokRuntimeSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun liveTikTokHomeScrollAndSearchStayRendered() {
        val evidenceDir = File(targetContext.getExternalFilesDir(null), "tiktok-smoke").apply {
            mkdirs()
        }
        val report = File(evidenceDir, "report.txt").apply {
            writeText("T Tube TikTok runtime smoke\n")
        }

        ActivityScenario.launch(TikTokSmokeActivity::class.java).use { scenario ->
            val holder = AtomicReference<TikTokSmokeActivity>()
            scenario.onActivity { holder.set(it) }
            val activity = holder.get()
            device.waitForIdle()

            try {
                assertTrue(
                    "TikTok home did not render usable web content",
                    waitUntil(55_000) { isHealthy(activity) && mediaCount(activity) >= 1 }
                )
                appendStage(report, "home", activity, mediaCount(activity))
                screenshot(evidenceDir, "01-home.png")

                // Real swipe gestures, not JS scrollBy: this exercises the same
                // touch path the user uses in Android WebView.
                repeat(12) { index ->
                    val w = device.displayWidth
                    val h = device.displayHeight
                    device.swipe(w / 2, (h * 0.78).toInt(), w / 2, (h * 0.25).toInt(), 18)
                    SystemClock.sleep(700)
                    assertTrue("TikTok became blank after swipe ${index + 1}", isHealthy(activity))
                }
                assertTrue("TikTok feed lost all video/media nodes after scrolling", mediaCount(activity) >= 1)
                appendStage(report, "after-12-swipes", activity, mediaCount(activity))
                screenshot(evidenceDir, "02-after-scroll.png")

                instrumentation.runOnMainSync { activity.loadSearch("cat") }
                assertTrue(
                    "TikTok search route did not render video results",
                    waitUntil(55_000) {
                        currentUrl(activity).contains("/search") &&
                            isHealthy(activity) &&
                            videoLinkCount(activity) >= 1
                    }
                )
                appendStage(report, "search-cat", activity, mediaCount(activity))
                screenshot(evidenceDir, "03-search.png")

                repeat(4) { index ->
                    val w = device.displayWidth
                    val h = device.displayHeight
                    device.swipe(w / 2, (h * 0.78).toInt(), w / 2, (h * 0.25).toInt(), 18)
                    SystemClock.sleep(650)
                    assertTrue("TikTok search became blank after swipe ${index + 1}", isHealthy(activity))
                }
                assertTrue("TikTok search lost its video result links", videoLinkCount(activity) >= 1)
                appendStage(report, "search-after-4-swipes", activity, mediaCount(activity))
                screenshot(evidenceDir, "04-search-after-scroll.png")
            } catch (t: Throwable) {
                appendStage(report, "FAIL: ${t.message}", activity, mediaCount(activity))
                screenshot(evidenceDir, "99-failure.png")
                throw t
            }
        }
    }

    private fun currentUrl(activity: TikTokSmokeActivity): String =
        instrumentation.runOnMainSyncResult { activity.webView.url.orEmpty() }

    private fun isHealthy(activity: TikTokSmokeActivity): Boolean = evalBoolean(
        activity,
        """
        (function() {
          try {
            var body = document.body;
            if (!body) return false;
            var text = ((body.innerText || body.textContent || '') + '').trim();
            var root = document.querySelector('main, #app, #root, [data-e2e="app-root"]');
            var r = root && root.getBoundingClientRect ? root.getBoundingClientRect() : null;
            var sized = !!(r && r.width > 120 && r.height > 120);
            var blocked = /access denied|unusual traffic|captcha|verify to continue|network error|something went wrong/i.test(text);
            return location.hostname.indexOf('tiktok.com') >= 0 &&
                   sized && text.length > 80 && !blocked;
          } catch (_) { return false; }
        })();
        """.trimIndent()
    )

    private fun mediaCount(activity: TikTokSmokeActivity): Int = evalInt(
        activity,
        "document.querySelectorAll('video, a[href*="/video/"]').length"
    )

    private fun videoLinkCount(activity: TikTokSmokeActivity): Int = evalInt(
        activity,
        "document.querySelectorAll('a[href*="/video/"]').length"
    )

    private fun evalBoolean(activity: TikTokSmokeActivity, script: String): Boolean =
        eval(activity, script).trim() == "true"

    private fun evalInt(activity: TikTokSmokeActivity, script: String): Int =
        eval(activity, script).trim().trim('"').toDoubleOrNull()?.toInt() ?: 0

    private fun eval(activity: TikTokSmokeActivity, script: String): String {
        val result = AtomicReference("null")
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            val web: WebView = activity.webView
            web.evaluateJavascript(script) {
                result.set(it ?: "null")
                latch.countDown()
            }
        }
        assertTrue("Timed out evaluating TikTok WebView JavaScript", latch.await(8, TimeUnit.SECONDS))
        return result.get()
    }

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (runCatching { condition() }.getOrDefault(false)) return true
            SystemClock.sleep(750)
        }
        return false
    }

    private fun screenshot(dir: File, name: String) {
        runCatching { device.takeScreenshot(File(dir, name)) }
    }

    private fun appendStage(report: File, stage: String, activity: TikTokSmokeActivity, media: Int) {
        val url = runCatching { currentUrl(activity) }.getOrDefault("<unavailable>")
        val links = runCatching { videoLinkCount(activity) }.getOrDefault(-1)
        report.appendText("$stage | url=$url | media=$media | videoLinks=$links\n")
    }

    private fun <T> android.app.Instrumentation.runOnMainSyncResult(block: () -> T): T {
        val result = AtomicReference<T>()
        runOnMainSync { result.set(block()) }
        return result.get()
    }
}
