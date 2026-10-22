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
    @Test fun searchVideoAndBackKeepStableDesktopIdentityWithoutAffectingOtherSites() {
        ShadowWebSettings.setDefaultUserAgent("Mozilla/5.0 (Linux; Android 16; Pixel; wv) AppleWebKit/537.36 Version/4.0 Chrome/153.0.8010.36 Mobile Safari/537.36")
        val web = WebView(RuntimeEnvironment.getApplication())
        try {
            TikTokWebPolicy.configure(web, "https://www.tiktok.com/search?q=nhac")
            val searchUa = web.settings.userAgentString
            TikTokWebPolicy.configure(web, "https://www.tiktok.com/@user/video/123")
            val videoUa = web.settings.userAgentString
            assertEquals(searchUa, videoUa)
            assertEquals(TikTokWebPolicy.userAgent("ignored"), videoUa)
            assertTrue(videoUa.contains("X11; Linux x86_64"))
            assertTrue(videoUa.contains("Chrome/128.0.0.0"))
            assertFalse(videoUa.contains("Android"))
            assertFalse(videoUa.contains("Mobile"))
            assertFalse(videoUa.contains("; wv"))

            TikTokWebPolicy.configure(web, "https://www.tiktok.com/login/phone-or-email/email")
            val loginUa = web.settings.userAgentString
            assertTrue(loginUa.contains("Android 16"))
            assertTrue(loginUa.contains("Chrome/153.0.8010.36"))
            assertTrue(loginUa.contains("Mobile"))
            assertFalse(loginUa.contains("; wv"))
            assertFalse(loginUa.contains("Version/4.0"))
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

    @Test fun loginAndExternalAuthArePartOfTikTokSession() {
        assertTrue(TikTokWebPolicy.isLoginRoute("https://www.tiktok.com/login"))
        assertTrue(TikTokWebPolicy.isLoginRoute("https://www.tiktok.com/signup"))
        assertTrue(TikTokWebPolicy.isSessionUrl("https://accounts.google.com/o/oauth2/v2/auth"))
        assertTrue(TikTokWebPolicy.isSessionUrl("https://appleid.apple.com/auth/authorize"))
        assertTrue(TikTokWebPolicy.isSessionUrl("https://m.facebook.com/login"))
        assertFalse(TikTokWebPolicy.isSessionUrl("https://example.org/login"))
    }

    @Test fun webViewIdentityUsesStableDesktopChromeUa() {
        val ua = "Mozilla/5.0 (Linux; Android 16; Pixel; wv) AppleWebKit/537.36 Version/4.0 Chrome/153.0.8010.36 Mobile Safari/537.36"
        val normalized = TikTokWebPolicy.userAgent(ua)
        assertEquals(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
            normalized
        )
    }
}
