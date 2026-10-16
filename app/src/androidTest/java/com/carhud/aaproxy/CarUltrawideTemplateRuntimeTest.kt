package com.carhud.aaproxy

import androidx.car.app.navigation.model.MapTemplate
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CarUltrawideTemplateRuntimeTest {
    @Test
    fun modernTemplateBuildsWithoutActionStrip() {
        val template = CarUltrawideTemplateFactory.build(5)
        assertTrue(template is MapTemplate)
        assertNull((template as MapTemplate).actionStrip)
    }

    @Test
    fun legacyTemplateStillBuilds() {
        val template = CarUltrawideTemplateFactory.build(1)
        assertTrue(template is NavigationTemplate)
    }
}
