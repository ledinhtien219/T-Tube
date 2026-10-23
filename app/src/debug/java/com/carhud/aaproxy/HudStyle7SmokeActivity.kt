package com.carhud.aaproxy

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * Debug-only screen used by CI to render the real HUD view and capture it with adb.
 * This is intentionally excluded from release builds.
 */
class HudStyle7SmokeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val requestedScale = intent.getIntExtra("scale", 100).coerceIn(60, 150)
        WazeHudManager.setActiveStyleId(this, 7)
        WazeHudManager.setScale(this, requestedScale)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(18, 18, 18))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val overlay = VietmapHudOverlay(this).apply {
            // Fill the real view with deterministic sample Waze data first.
            setSamplePreview(7)
            // Then re-apply production scaling using a 1280x720 AA viewport.
            setPreviewMode(false)
            applyProjectionViewport(1280, 720)
        }

        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = 24
                topMargin = 24
            }
        )

        setContentView(root)

        // Re-apply after measurement/attachment so the screenshot reflects
        // the same scale transform used by the production projection overlay.
        overlay.post {
            overlay.setPreviewMode(false)
            overlay.applyProjectionViewport(1280, 720)
        }
    }
}
