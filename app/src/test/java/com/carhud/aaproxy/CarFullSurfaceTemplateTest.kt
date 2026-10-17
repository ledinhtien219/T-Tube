package com.carhud.aaproxy

import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.MapTemplate
import org.junit.Assert.assertNotNull
import org.junit.Test

class CarFullSurfaceTemplateTest {
    @Test
    fun zeroWidthPaneRowBuildsWithoutPaneStateException() {
        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("\u200B")
                    .build()
            )
            .build()

        val template = MapTemplate.Builder()
            .setPane(pane)
            .setHeader(
                Header.Builder()
                    .setTitle("\u200B")
                    .build()
            )
            .build()

        assertNotNull(template)
    }
}
