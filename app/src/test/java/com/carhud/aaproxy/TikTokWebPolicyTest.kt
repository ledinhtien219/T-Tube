package com.carhud.aaproxy

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE, application = Application::class)
class TikTokWebPolicyTest {
    @Test fun onlyTikTokSearchUsesDesktop() {
        assertTrue(TikTokWebPolicy.isSearch("https://www.tiktok.com/search?q=nhac"))
        assertTrue(TikTokWebPolicy.isSearch("https://www.tiktok.com/search/video?q=nhac"))
        assertFalse(TikTokWebPolicy.isSearch("https://www.tiktok.com/@user/video/123"))
        assertFalse(TikTokWebPolicy.isSearch("https://tiktok.com.example.org/search"))
        assertFalse(TikTokWebPolicy.isSearch("https://example.org/?url=tiktok.com/search"))
    }

    @Test fun preserveInstalledChromiumVersionAndRestoreMobileForFeed() {
        val ua = "Mozilla/5.0 (Linux; Android 16; Pixel; wv) AppleWebKit/537.36 Version/4.0 Chrome/153.0.8010.36 Mobile Safari/537.36"
        val desktop = TikTokWebPolicy.userAgent(ua, true)
        assertTrue(desktop.contains("Chrome/153.0.8010.36"))
        assertFalse(desktop.contains("Android"))
        assertFalse(desktop.contains("Mobile"))
        assertFalse(desktop.contains("Version/4.0"))
        val mobile = TikTokWebPolicy.userAgent(ua, false)
        assertTrue(mobile.contains("Android 16"))
        assertTrue(mobile.contains("Mobile"))
        assertFalse(mobile.contains("; wv"))
    }
}
