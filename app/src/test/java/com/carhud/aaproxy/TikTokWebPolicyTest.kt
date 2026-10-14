package com.carhud.aaproxy

import android.app.Application
import android.webkit.WebView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowWebSettings
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE, application = Application::class)
class TikTokWebPolicyTest {
    @Test fun searchVideoAndBackKeepDesktopIdentityWithoutAffectingOtherSites() {
        ShadowWebSettings.setDefaultUserAgent("Mozilla/5.0 (Linux; Android 16; Pixel; wv) AppleWebKit/537.36 Version/4.0 Chrome/153.0.8010.36 Mobile Safari/537.36")
        val web = WebView(RuntimeEnvironment.getApplication())
        try {
            TikTokWebPolicy.configure(web, "https://www.tiktok.com/search?q=nhac")
            val searchUa = web.settings.userAgentString
            TikTokWebPolicy.configure(web, "https://www.tiktok.com/@user/video/123")
            val videoUa = web.settings.userAgentString
            assertEquals(searchUa, videoUa)
            assertFalse(videoUa.contains("Mobile"))
            TikTokWebPolicy.configure(web, "https://www.tiktok.com/search?q=nhac")
            assertEquals(searchUa, web.settings.userAgentString)
            TikTokWebPolicy.configure(web, "https://www.tiktok.com/search?q=khac")
            assertEquals(searchUa, web.settings.userAgentString)
            TikTokWebPolicy.configure(web, "https://example.org/search")
            assertEquals(searchUa, web.settings.userAgentString)
        } finally {
            web.destroy()
            ShadowWebSettings.reset()
        }
    }

    @Test fun searchClassificationRejectsForeignDomains() {
        assertTrue(TikTokWebPolicy.isSearch("https://www.tiktok.com/search?q=nhac"))
        assertTrue(TikTokWebPolicy.isSearch("https://www.tiktok.com/search/video?q=nhac"))
        assertFalse(TikTokWebPolicy.isSearch("https://www.tiktok.com/@user/video/123"))
        assertFalse(TikTokWebPolicy.isSearch("https://tiktok.com.example.org/search"))
        assertFalse(TikTokWebPolicy.isSearch("https://example.org/?url=tiktok.com/search"))
    }

    @Test fun desktopIdentityPreservesInstalledChromiumVersion() {
        val ua = "Mozilla/5.0 (Linux; Android 16; Pixel; wv) AppleWebKit/537.36 Version/4.0 Chrome/153.0.8010.36 Mobile Safari/537.36"
        val desktop = TikTokWebPolicy.userAgent(ua)
        assertTrue(desktop.contains("Chrome/153.0.8010.36"))
        assertFalse(desktop.contains("Android"))
        assertFalse(desktop.contains("Mobile"))
        assertFalse(desktop.contains("Version/4.0"))
        assertFalse(desktop.contains("; wv"))
    }
}
