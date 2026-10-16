package com.carhud.aaproxy

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Selects exactly one Android Auto entry path.
 *
 * Stable mode uses the Fermata-style CarActivity projection host.
 * Ultrawide mode uses AndroidX CarAppService + SurfaceContainer so compatible
 * Coolwalk/widescreen hosts can provide the full navigation canvas.
 */
object CarHostModeManager {
    private const val TAG = "CarHostModeManager"

    fun syncFromPreferences(context: Context) {
        val prefs = context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
        applyMode(
            context,
            prefs.getBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false)
        )
    }

    fun applyMode(context: Context, preferUltrawide: Boolean) {
        val app = context.applicationContext
        setComponentEnabled(app, CarHudAutoService::class.java, preferUltrawide)
        setComponentEnabled(app, CarHudProjectionService::class.java, !preferUltrawide)
        Log.i(TAG, "Android Auto host mode = " + if (preferUltrawide) "ULTRAWIDE_SURFACE" else "STABLE_PROJECTION")
    }

    private fun setComponentEnabled(context: Context, cls: Class<*>, enabled: Boolean) {
        val pm = context.packageManager
        val component = ComponentName(context, cls)
        val desired = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (pm.getComponentEnabledSetting(component) == desired) return
        pm.setComponentEnabledSetting(
            component,
            desired,
            PackageManager.DONT_KILL_APP
        )
    }
}
