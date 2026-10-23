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
        val showAlerts = intent.getBooleanExtra("alerts", false)
        WazeHudManager.setActiveStyleId(this, 7)
        WazeHudManager.setScale(this, requestedScale)
        WazeHudManager.setLocked(this, true)

        val root = FrameLayout(this).apply {
            // Slightly lighter backing makes the translucent HUD background visible
            // in CI screenshots without pretending this is an Android Auto map.
            setBackgroundColor(Color.rgb(45, 52, 61))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val overlay = VietmapHudOverlay(this).apply {
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

        // Stop live state from racing the deterministic screenshot payload.
        overlay.post {
            overlay.stopObservingState()
            overlay.setPreviewMode(false)
            overlay.applyProjectionViewport(1280, 720)

            if (showAlerts) {
                val now = System.currentTimeMillis()
                overlay.updateUi(
                    VietmapAlertData(
                        isConnected = true,
                        currentSpeed = 57,
                        speedLimit = 60,
                        turnCode = 3,
                        turnAction = "right",
                        turnDescription = "Rẽ phải",
                        distanceToTurnMeters = 250,
                        roadName = "Đ. Nguyễn Văn Linh",
                        nextRoadName = "Đ. Nguyễn Hữu Thọ",
                        laneGuidance = "↑  ↑  ↗",
                        warningType = VietmapWarningType.SPEED_CAMERA,
                        alertTitle = "Camera tốc độ",
                        distanceMeters = 400,
                        upcomingAlerts = listOf(
                            WazeAlertItem(
                                code = 2,
                                warningType = VietmapWarningType.SPEED_CAMERA,
                                title = "Camera tốc độ",
                                distanceMeters = 400,
                                sourceIcon = "speed_camera",
                                sourceSubtype = "camera_speed"
                            ),
                            WazeAlertItem(
                                code = 4,
                                warningType = VietmapWarningType.HAZARD,
                                title = "Cảnh sát",
                                distanceMeters = 900,
                                sourceIcon = "police",
                                sourceSubtype = "police_hiding"
                            ),
                            WazeAlertItem(
                                code = 8,
                                warningType = VietmapWarningType.SPEED_LIMIT_ZONE,
                                title = "Giới hạn tốc độ",
                                distanceMeters = 1200,
                                value = 80,
                                sourceIcon = "speed_limit",
                                sourceSubtype = "speed_limit_zone"
                            )
                        ),
                        alertSource = "WAZE_HLP",
                        alertTimestamp = now,
                        source = "WAZE_HLP",
                        sourceUpdatedAt = now,
                        lastUpdated = now
                    )
                )
            } else {
                overlay.updateUi(
                    VietmapAlertData(
                        isConnected = true,
                        currentSpeed = 57,
                        speedLimit = 60,
                        roadName = "Đ. Nguyễn Văn Linh",
                        source = "WAZE_HLP",
                        sourceUpdatedAt = System.currentTimeMillis(),
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
