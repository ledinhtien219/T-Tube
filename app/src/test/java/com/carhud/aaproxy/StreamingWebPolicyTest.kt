package com.carhud.aaproxy

import android.app.Application
import android.webkit.WebView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowWebSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE, application = Application::class)
class StreamingWebPolicyTest {
    @Test fun domainFamiliesIncludeServiceSubdomains() {
        assertTrue(StreamingWebPolicy.isTv360("https://tv360.vn/login"))
        assertTrue(StreamingWebPolicy.isTv360("https://hotro.tv360.vn/page"))
        assertTrue(StreamingWebPolicy.isTv360("https://static-zlr.tv360.vn/file"))
        assertFalse(StreamingWebPolicy.isTv360("https://tv360.vn.example.org"))

        assertTrue(StreamingWebPolicy.isVtvGo("https://vtvgo.vn/"))
        assertTrue(StreamingWebPolicy.isVtvGo("https://www.vtvgo.vn/"))
        assertTrue(StreamingWebPolicy.isVtvGo("https://openapp.vtvgo.vn/policy"))
        assertTrue(StreamingWebPolicy.isVtvGo("https://next.vtvgo.vn/"))
        assertFalse(StreamingWebPolicy.isVtvGo("https://vtvgo.vn.example.org"))
    }

    @Test fun configurationUsesBrowserLikeMobileIdentity() {
        ShadowWebSettings.setDefaultUserAgent(
            "Mozilla/5.0 (Linux; Android 16; Pixel 9; wv) AppleWebKit/537.36 " +
                "Version/4.0 Chrome/153.0.8010.36 Mobile Safari/537.36"
        )
        val web = WebView(RuntimeEnvironment.getApplication())
        try {
            StreamingWebPolicy.configure(web, "tv360", "https://tv360.vn/login")
            val ua = web.settings.userAgentString
            assertTrue(ua.contains("Android 16"))
            assertTrue(ua.contains("Chrome/153.0.8010.36"))
            assertTrue(ua.contains("Mobile"))
            assertFalse(ua.contains("; wv"))
            assertFalse(ua.contains("Version/4.0"))
            assertTrue(web.settings.javaScriptEnabled)
            assertTrue(web.settings.domStorageEnabled)
            assertFalse(web.settings.mediaPlaybackRequiresUserGesture)
        } finally {
            web.destroy()
            ShadowWebSettings.reset()
        }
    }
}
