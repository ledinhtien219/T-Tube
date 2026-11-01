package com.carhud.aaproxy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mlm.adblock.AdblockEngine

/** Real Android/emulator JNI smoke, not a fake/mock of Rust. */
@RunWith(AndroidJUnit4::class)
class BraveShieldsNativeTest {
    @Test
    fun rustEngineLoadsAndFiltersWithoutBlockingOrdinaryVideo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = AdblockEngine.create()
        try {
            assertTrue(engine.loadFilterList("||ads.ttube-example.invalid^"))
            val resources = context.assets.open("adblock/resources.json")
                .bufferedReader().use { it.readText() }
            assertTrue(engine.loadResources(resources))
            assertTrue(
                engine.shouldBlock(
                    "https://ads.ttube-example.invalid/banner.js",
                    "https://www.youtube.com/watch?v=abc",
                    "script"
                )
            )
            assertFalse(
                engine.shouldBlock(
                    "https://www.youtube.com/watch?v=abc",
                    "https://www.youtube.com/",
                    "document"
                )
            )
        } finally {
            engine.close()
        }
    }
}
