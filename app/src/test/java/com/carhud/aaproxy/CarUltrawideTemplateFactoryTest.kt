package com.carhud.aaproxy

import androidx.car.app.navigation.model.MapTemplate
import androidx.car.app.navigation.model.NavigationTemplate
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarUltrawideTemplateFactoryTest {
    @Test
    fun modernTemplateBuildsWithoutActionStrip() {
        // Calling build() is the regression: the old code omitted Header and
        // threw IllegalStateException only when Android Auto asked for template.
        val template = CarUltrawideTemplateFactory.build(5)
        assertTrue(template is MapTemplate)

        val getter = template.javaClass.getMethod("getActionStrip")
        assertNull(getter.invoke(template))
    }

    @Test
    fun legacyTemplateStillBuilds() {
        val template = CarUltrawideTemplateFactory.build(1)
        assertTrue(template is NavigationTemplate)
    }
}
