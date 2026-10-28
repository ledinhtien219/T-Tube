package com.tcar.tmirror

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Optional, user-enabled touch forwarding. Never enabled automatically.
 * No accessibility content is read; only user-initiated car touch gestures.
 */
class MirrorTouchService : AccessibilityService() {
    companion object {
        @Volatile private var active: MirrorTouchService? = null
        val enabled: Boolean get() = active != null

        fun sendGesture(from: Pair<Float, Float>, to: Pair<Float, Float>, duration: Long) {
            active?.inject(from, to, duration)
        }
    }

    override fun onServiceConnected() { super.onServiceConnected(); active = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    @Suppress("DEPRECATION")
    private fun inject(from: Pair<Float, Float>, to: Pair<Float, Float>, duration: Long) {
        val screen = Point()
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(screen)
        if (screen.x <= 0 || screen.y <= 0) return
        val path = Path().apply {
            moveTo(from.first * screen.x, from.second * screen.y)
            if (duration > 150L) lineTo(to.first * screen.x, to.second * screen.y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration.coerceIn(55L, 900L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onDestroy() {
        if (active === this) active = null
        super.onDestroy()
    }
}
