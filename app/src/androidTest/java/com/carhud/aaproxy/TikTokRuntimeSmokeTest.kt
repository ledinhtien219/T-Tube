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
    private val device = UiDevice.getInstance(instrumentation)
    private val targetContext = instrumentation.targetContext
    private val evidenceDir = File(
        targetContext.getExternalFilesDir(null),
        "tiktok-smoke"
    )

    @Test
    fun liveTikTokHomeScrollAndSearchStayRendered() {
        evidenceDir.deleteRecursively()
        evidenceDir.mkdirs()
        shellAppend("T Tube TikTok runtime smoke")

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
                val homeFits = waitUntil(6_000) { fitsViewport(activity) }
                if (!homeFits) appendDiagnostic("home-fit-failure", activity)
                assertTrue(
                    "TikTok home overflows the WebView horizontally",
                    homeFits
                )
                appendStage("home", activity, mediaCount(activity))
                screenshot("01-home.png")

                repeat(12) { index ->
                    val w = device.displayWidth
                    val h = device.displayHeight
                    device.swipe(w / 2, (h * 0.78).toInt(), w / 2, (h * 0.25).toInt(), 18)
                    SystemClock.sleep(700)
                    assertTrue("TikTok became blank after swipe ${index + 1}", isHealthy(activity))
                }
                assertTrue("TikTok feed lost all video/media nodes after scrolling", mediaCount(activity) >= 1)
                appendStage("after-12-swipes", activity, mediaCount(activity))
                screenshot("02-after-scroll.png")

                instrumentation.runOnMainSync { activity.loadSearch("cat") }
                val searchReady = waitUntil(55_000) {
                    currentUrl(activity).contains("/search") &&
                        isHealthy(activity) &&
                        (searchUiCount(activity) >= 1 || videoLinkCount(activity) >= 1)
                }
                if (!searchReady) {
                    appendDiagnostic("search-timeout", activity)
                    screenshot("98-search-timeout.png")
                }
                assertTrue("TikTok search route did not render a usable web UI", searchReady)
                val searchFits = waitUntil(6_000) { fitsViewport(activity) }
                if (!searchFits) appendDiagnostic("search-fit-failure", activity)
                assertTrue(
                    "TikTok search overflows the WebView horizontally",
                    searchFits
                )

                appendStage("search-cat", activity, mediaCount(activity))
                screenshot("03-search.png")

                repeat(4) { index ->
                    val w = device.displayWidth
                    val h = device.displayHeight
                    device.swipe(w / 2, (h * 0.78).toInt(), w / 2, (h * 0.25).toInt(), 18)
                    SystemClock.sleep(650)

                    // TikTok's live search SPA can briefly virtualize/unmount the
                    // visible result tree immediately after a swipe. Require it
                    // to recover quickly instead of sampling a single transient frame.
                    val recovered = waitUntil(5_000) {
                        currentUrl(activity).contains("/search") && isHealthy(activity)
                    }
                    if (!recovered) {
                        appendDiagnostic("search-swipe-${index + 1}-blank", activity)
                        screenshot("97-search-swipe-${index + 1}-blank.png")
                    }
                    assertTrue(
                        "TikTok search stayed blank after swipe ${index + 1}",
                        recovered
                    )
                }
                assertTrue(
                    "TikTok search UI disappeared after scrolling",
                    searchUiCount(activity) >= 1 || videoLinkCount(activity) >= 1
                )
                appendStage("search-after-4-swipes", activity, mediaCount(activity))
                screenshot("04-search-after-scroll.png")
            } catch (t: Throwable) {
                appendStage("FAIL: ${t.message}", activity, runCatching { mediaCount(activity) }.getOrDefault(-1))
                appendDiagnostic("failure", activity)
                screenshot("99-failure.png")
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

    private fun fitsViewport(activity: TikTokSmokeActivity): Boolean = evalBoolean(
        activity,
        """
        (function() {
          try {
            var state = window.__ttubeTikTokWebOnly || {};
            var viewport = Number(state.fitViewport || 0);
            var rawWidth = Number(state.fitRawWidth || 0);
            var scale = Number(state.fitScale || 0);
            if (!(viewport > 0 && rawWidth > 0 && scale > 0)) return false;
            return rawWidth * scale <= viewport * 1.035;
          } catch (_) { return false; }
        })();
        """.trimIndent()
    )

    private fun mediaCount(activity: TikTokSmokeActivity): Int = evalInt(
        activity,
        """document.querySelectorAll('video, a[href*="/video/"]').length"""
    )

    private fun searchUiCount(activity: TikTokSmokeActivity): Int = evalInt(
        activity,
        """document.querySelectorAll('[data-e2e*="search"], [class*="Search"], [class*="search"], input[type="search"]').length"""
    )

    private fun videoLinkCount(activity: TikTokSmokeActivity): Int = evalInt(
        activity,
        """document.querySelectorAll('a[href*="/video/"]').length"""
    )

    private fun evalBoolean(activity: TikTokSmokeActivity, script: String): Boolean =
        eval(activity, script).trim() == "true"

    private fun evalInt(activity: TikTokSmokeActivity, script: String): Int =
        eval(activity, script).trim().trim('"').toDoubleOrNull()?.toInt() ?: 0

    private fun evalString(activity: TikTokSmokeActivity, script: String): String {
        val raw = eval(activity, script)
        return runCatching { org.json.JSONArray("[$raw]").getString(0) }
            .getOrDefault(raw.trim('"'))
    }

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

    private fun screenshot(name: String) {
        runCatching {
            device.takeScreenshot(File(evidenceDir, name))
        }
    }

    private fun appendStage(stage: String, activity: TikTokSmokeActivity, media: Int) {
        val url = runCatching { currentUrl(activity) }.getOrDefault("<unavailable>")
        val links = runCatching { videoLinkCount(activity) }.getOrDefault(-1)
        val searchUi = runCatching { searchUiCount(activity) }.getOrDefault(-1)
        shellAppend("$stage | url=$url | media=$media | videoLinks=$links | searchUi=$searchUi")
    }

    private fun appendDiagnostic(stage: String, activity: TikTokSmokeActivity) {
        val url = runCatching { currentUrl(activity) }.getOrDefault("<unavailable>")
        val text = runCatching {
            evalString(
                activity,
                """((document.body && (document.body.innerText || document.body.textContent)) || '').replace(/\s+/g, ' ').trim().slice(0, 1800)"""
            )
        }.getOrDefault("<body unavailable>")
        val searchNodes = runCatching {
            evalInt(
                activity,
                """document.querySelectorAll('[data-e2e*="search"], [class*="Search"], [class*="search"]').length"""
            )
        }.getOrDefault(-1)
        val links = runCatching { videoLinkCount(activity) }.getOrDefault(-1)
        val fit = runCatching {
            evalString(
                activity,
                """
                JSON.stringify((function() {
                  var s = window.__ttubeTikTokWebOnly || {};
                  return {
                    viewport: s.fitViewport || 0,
                    rawWidth: s.fitRawWidth || 0,
                    scale: s.fitScale || 0,
                    passes: s.fitPasses || 0,
                    innerWidth: window.innerWidth || 0,
                    scrollWidth: Math.max(
                      document.documentElement ? document.documentElement.scrollWidth : 0,
                      document.body ? document.body.scrollWidth : 0
                    )
                  };
                })())
                """.trimIndent()
            )
        }.getOrDefault("<fit unavailable>")
        shellAppend("$stage | url=$url | videoLinks=$links | searchNodes=$searchNodes | fit=$fit")
        shellAppend("BODY: $text")
    }

    private fun shellAppend(line: String) {
        val clean = line.replace("\n", " ")
        android.util.Log.i("TTUBE_SMOKE", clean)
        runCatching {
            evidenceDir.mkdirs()
            File(evidenceDir, "report.txt").appendText(clean + "\n")
        }
    }

    private fun <T> android.app.Instrumentation.runOnMainSyncResult(block: () -> T): T {
        val result = AtomicReference<T>()
        runOnMainSync { result.set(block()) }
        return result.get()
    }
}
