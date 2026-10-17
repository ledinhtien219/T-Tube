package com.carhud.aaproxy

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebBackForwardList
import android.webkit.WebView
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE, application = Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class WebSessionRestoreTest {
    private class TestWeb(context: Context) : WebView(context) {
        var page = "https://m.youtube.com/watch?v=abcdefghijk"
        var playerReady = true
        val scripts = mutableListOf<String>()
        var scrollRestores = 0
        private val handler = Handler(Looper.getMainLooper())
        override fun getUrl() = page
        override fun getProgress() = 100
        override fun saveState(outState: Bundle): WebBackForwardList? = null
        override fun restoreState(inState: Bundle): WebBackForwardList? = null
        override fun loadUrl(url: String) { page = url }
        override fun stopLoading() {}
        override fun scrollTo(x: Int, y: Int) { scrollRestores++ }
        override fun postDelayed(action: Runnable, delayMillis: Long) = handler.postDelayed(action, delayMillis)
        override fun evaluateJavascript(script: String, callback: ValueCallback<String>?) {
            scripts.add(script)
            callback?.onReceiveValue(if (playerReady) "true" else "false")
        }
        fun seeks() = scripts.count { it.contains("player.seekTo(") }
    }
    private lateinit var web: TestWeb
    private var previousWeb: Any? = null
    private val webField = CarMediaManager::class.java.getDeclaredField("carWebView").apply { isAccessible = true }

    @Before fun prepare() {
        web = TestWeb(RuntimeEnvironment.getApplication())
        previousWeb = webField.get(CarMediaManager)
        webField.set(CarMediaManager, web)
        CarMediaManager.activeAppId = "youtube"
        CarMediaManager.userWantsPlayback = false
        CarMediaManager.updatePlaybackProgress(42, 120)
        CarMediaManager.saveWebAppSession("youtube", web)
    }
    @After fun cleanUp() {
        CarMediaManager.saveWebAppSession("", null) // Invalidate queued callbacks.
        webField.set(CarMediaManager, previousWeb)
        web.destroy()
    }
    @Test fun restoringSeeksOnceAndKeepsAnExplicitPause() {
        assertTrue(CarMediaManager.restoreWebAppSession("youtube", web))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8))
        assertEquals(1, web.seeks())
        assertEquals(1, web.scrollRestores)
        assertFalse(CarMediaManager.userWantsPlayback)
    }
    @Test fun rapidReturnToSameAppInvalidatesPreviousRestore() {
        CarMediaManager.restoreWebAppSession("youtube", web)
        web.page = "https://example.com/"
        CarMediaManager.saveWebAppSession("web", web)
        CarMediaManager.activeAppId = "youtube"
        CarMediaManager.restoreWebAppSession("youtube", web)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8))
        assertEquals("Old A callback must not run after A-B-A", 1, web.seeks())
    }
    @Test fun waitsForMediaMetadataWithoutRepeatedlySeeking() {
        web.playerReady = false
        CarMediaManager.restoreWebAppSession("youtube", web)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        assertEquals(0, web.seeks())
        web.playerReady = true
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(7))
        assertEquals(1, web.seeks())
    }
    @Test fun navigatingToAnotherVideoCancelsTheOldPositionRestore() {
        web.playerReady = false
        CarMediaManager.restoreWebAppSession("youtube", web)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        web.page = "https://m.youtube.com/watch?v=01234567890"
        web.playerReady = true
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8))
        assertEquals(0, web.seeks())
    }
    @Test fun homepageDoesNotInheritPositionFromTheLastPlayedVideo() {
        web.page = "https://m.youtube.com/"
        CarMediaManager.saveWebAppSession("youtube", web)
        CarMediaManager.restoreWebAppSession("youtube", web)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8))
        assertEquals(0, web.seeks())
    }
}
