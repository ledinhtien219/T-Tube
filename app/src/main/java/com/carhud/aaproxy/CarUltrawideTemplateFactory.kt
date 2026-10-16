package com.carhud.aaproxy

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapTemplate
import androidx.car.app.navigation.model.NavigationTemplate

object CarUltrawideTemplateFactory {
    fun build(carApiLevel: Int): Template {
        if (carApiLevel >= 5) {
            // MapTemplate requires BOTH Pane and Header. ActionStrip is optional.
            // The previous implementation omitted Header and could throw at runtime.
            val pane = Pane.Builder().build()
            val header = Header.Builder()
                .setTitle("\u200B")
                .build()

            return MapTemplate.Builder()
                .setPane(pane)
                .setHeader(header)
                .build()
        }

        // Legacy host fallback: NavigationTemplate requires an ActionStrip.
        val transparent = CarColor.createCustom(0x00000000, 0x00000000)
        val hiddenActionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("\u200B")
                    .setFlags(Action.FLAG_PRIMARY)
                    .setBackgroundColor(transparent)
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(hiddenActionStrip)
            .build()
    }
}
