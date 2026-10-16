package com.carhud.aaproxy

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Selects exactly one Android Auto entry path.
 *
 * IMPORTANT: component state is never changed while an Android Auto session is
 * active. Disabling the currently bound service mid-session makes some OEM
 * hosts terminate the app with an "unexpected error" screen.
 */
object CarHostModeManager {
    private const val TAG = "CarHostModeManager"

    /**
     * Reconcile the manifest components with the saved preference only when AA
     * is not currently attached. Returns true when the requested mode is active,
     * false when it remains pending until disconnect.
     */
    fun syncFromPreferences(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
        val preferUltrawide =
            prefs.getBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false)
        return requestMode(context, preferUltrawide)
    }

    /**
     * The preference is already persisted by SettingsActivity. Apply it now only
     * if there is no live/sticky AA session; otherwise leave component state
     * untouched and let shutdown apply it safely after cable disconnect.
     */
    fun requestMode(context: Context, preferUltrawide: Boolean): Boolean {
        if (isAndroidAutoSessionActive()) {
            Log.i(
                TAG,
                "Deferring Android Auto host switch until disconnect; requested=" +
                    if (preferUltrawide) "ULTRAWIDE_SURFACE" else "STABLE_PROJECTION"
            )
            return false
        }
        applyModeNow(context.applicationContext, preferUltrawide)
        return true
    }

    /**
     * Called from the real disconnect/shutdown path after the sticky session has
     * been reset. This is the safe point to swap Android Auto entry components.
     */
    fun applyPendingAfterDisconnect(context: Context) {
        val prefs = context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
        applyModeNow(
            context.applicationContext,
            prefs.getBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false)
        )
    }

    private fun isAndroidAutoSessionActive(): Boolean {
        return CarMediaManager.isCarConnected ||
            CarDisconnectWatchdog.hasSeenProjectionSession()
    }

    private fun applyModeNow(context: Context, preferUltrawide: Boolean) {
        setComponentEnabled(context, CarHudAutoService::class.java, preferUltrawide)
        setComponentEnabled(context, CarHudProjectionService::class.java, !preferUltrawide)
        Log.i(
            TAG,
            "Android Auto host mode applied = " +
                if (preferUltrawide) "ULTRAWIDE_SURFACE" else "STABLE_PROJECTION"
        )
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
