package com.carhud.aaproxy

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

object CarHostModeManager {
    private const val TAG = "CarHostModeManager"
    const val MODE_STABLE = "stable"
    const val MODE_ULTRAWIDE = "ultrawide"

    data class State(
        val requestedUltrawide: Boolean,
        val appliedMode: String,
        val pending: Boolean,
        val sessionActive: Boolean
    )

    fun getState(context: Context): State {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
        val requested = prefs.getBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false)
        val applied = currentAppliedMode(app)
        val active = isAndroidAutoSessionActive()
        return State(
            requestedUltrawide = requested,
            appliedMode = applied,
            pending = (requested && applied != MODE_ULTRAWIDE) ||
                (!requested && applied != MODE_STABLE),
            sessionActive = active
        )
    }

    /**
     * Persisted by SettingsActivity before this call. Never mutate PackageManager
     * while Android Auto is attached: several OEM hosts treat disabling the bound
     * service as an application crash.
     */
    fun requestMode(context: Context, preferUltrawide: Boolean): State {
        val app = context.applicationContext
        if (!isAndroidAutoSessionActive()) {
            applyModeNow(app, preferUltrawide)
        } else {
            Log.i(TAG, "Host switch deferred until AA disconnect; ultrawide=$preferUltrawide")
        }
        return getState(app)
    }

    /**
     * Reconcile stale component overrides after app start/update, but only when
     * no AA session is alive.
     */
    fun reconcileFromPreferences(context: Context): State {
        val app = context.applicationContext
        if (!isAndroidAutoSessionActive()) {
            val prefs = app.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
            applyModeNow(
                app,
                prefs.getBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false)
            )
        }
        return getState(app)
    }

    /**
     * Called only after watchdog state has been reset by a confirmed disconnect.
     */
    fun applyPendingAfterDisconnect(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
        applyModeNow(
            app,
            prefs.getBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false)
        )
    }

    private fun isAndroidAutoSessionActive(): Boolean =
        CarMediaManager.isCarConnected || CarDisconnectWatchdog.hasSeenProjectionSession()

    private fun currentAppliedMode(context: Context): String {
        val pm = context.packageManager
        val ultra = isEnabled(
            pm,
            ComponentName(context, CarHudAutoService::class.java),
            manifestDefault = false
        )
        val stable = isEnabled(
            pm,
            ComponentName(context, CarHudProjectionService::class.java),
            manifestDefault = true
        )
        return when {
            ultra && !stable -> MODE_ULTRAWIDE
            stable && !ultra -> MODE_STABLE
            else -> MODE_STABLE
        }
    }

    private fun isEnabled(
        pm: PackageManager,
        component: ComponentName,
        manifestDefault: Boolean
    ): Boolean {
        return when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
            else -> manifestDefault
        }
    }

    private fun applyModeNow(context: Context, preferUltrawide: Boolean) {
        // Enable the next host first, then disable the old one. This guarantees
        // that package discovery never observes a moment with no AA entry.
        setEnabled(
            context,
            if (preferUltrawide) CarHudAutoService::class.java
            else CarHudProjectionService::class.java,
            true
        )
        setEnabled(
            context,
            if (preferUltrawide) CarHudProjectionService::class.java
            else CarHudAutoService::class.java,
            false
        )
        Log.i(
            TAG,
            "Applied AA host mode=" +
                if (preferUltrawide) MODE_ULTRAWIDE else MODE_STABLE
        )
    }

    private fun setEnabled(context: Context, cls: Class<*>, enabled: Boolean) {
        val pm = context.packageManager
        val component = ComponentName(context, cls)
        val desired = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (pm.getComponentEnabledSetting(component) == desired) return
        pm.setComponentEnabledSetting(component, desired, PackageManager.DONT_KILL_APP)
    }
}

class CarHostModeUpgradeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CarHostModeManager.reconcileFromPreferences(context)
        }
    }
}
