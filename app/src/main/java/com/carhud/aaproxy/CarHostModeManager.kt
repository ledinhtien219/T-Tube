package com.carhud.aaproxy

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * Switches between the normal projection host and the full-surface navigation host.
 *
 * Rules:
 *  - Never touch component state while an AA session is alive.
 *  - Apply both component states atomically on Android 13+.
 *  - Verify the final state after every switch.
 *  - Roll back to STABLE if anything is inconsistent.
 */
object CarHostModeManager {
    private const val TAG = "CarHostModeManager"
    private const val KEY_PENDING = "car_host_mode_pending"
    private const val KEY_APPLIED = "car_host_mode_applied"
    private const val KEY_LAST_ERROR = "car_host_mode_last_error"

    const val MODE_STABLE = "stable"
    const val MODE_ULTRAWIDE = "ultrawide"

    enum class RequestResult {
        APPLIED,
        PENDING_DISCONNECT,
        ROLLED_BACK
    }

    data class State(
        val desired: String,
        val applied: String,
        val pending: Boolean,
        val stableEnabled: Boolean,
        val ultrawideEnabled: Boolean,
        val lastError: String
    )

    fun state(context: Context): State {
        val prefs = prefs(context)
        return State(
            desired = if (prefs.getBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false)) {
                MODE_ULTRAWIDE
            } else {
                MODE_STABLE
            },
            applied = prefs.getString(KEY_APPLIED, MODE_STABLE) ?: MODE_STABLE,
            pending = prefs.getBoolean(KEY_PENDING, false),
            stableEnabled = isComponentEnabled(context, CarHudProjectionService::class.java, true),
            ultrawideEnabled = isComponentEnabled(context, CarHudAutoService::class.java, false),
            lastError = prefs.getString(KEY_LAST_ERROR, "").orEmpty()
        )
    }

    fun requestMode(context: Context, preferUltrawide: Boolean): RequestResult {
        prefs(context).edit()
            .putBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, preferUltrawide)
            .apply()

        if (sessionActive()) {
            prefs(context).edit().putBoolean(KEY_PENDING, true).apply()
            Log.i(TAG, "Mode change deferred until Android Auto disconnect")
            return RequestResult.PENDING_DISCONNECT
        }
        return applyDesiredNow(context)
    }

    fun reconcileIfSafe(context: Context): RequestResult {
        if (sessionActive()) {
            val s = state(context)
            if ((s.desired == MODE_ULTRAWIDE) != s.ultrawideEnabled) {
                prefs(context).edit().putBoolean(KEY_PENDING, true).apply()
                return RequestResult.PENDING_DISCONNECT
            }
            return RequestResult.APPLIED
        }
        return applyDesiredNow(context)
    }

    fun applyPendingAfterDisconnect(context: Context): RequestResult {
        return applyDesiredNow(context)
    }

    private fun applyDesiredNow(context: Context): RequestResult {
        val app = context.applicationContext
        val wantWide = prefs(app).getBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false)
        return try {
            applyComponentPair(app, wantWide)
            if (!verifyPair(app, wantWide)) {
                throw IllegalStateException("Android Auto host component verification failed")
            }
            prefs(app).edit()
                .putString(KEY_APPLIED, if (wantWide) MODE_ULTRAWIDE else MODE_STABLE)
                .putBoolean(KEY_PENDING, false)
                .remove(KEY_LAST_ERROR)
                .apply()
            Log.i(TAG, "Applied host mode: " + if (wantWide) MODE_ULTRAWIDE else MODE_STABLE)
            RequestResult.APPLIED
        } catch (t: Throwable) {
            Log.e(TAG, "Host mode switch failed; rolling back to stable", t)
            rollbackStable(app, t)
            RequestResult.ROLLED_BACK
        }
    }

    private fun applyComponentPair(context: Context, wide: Boolean) {
        val pm = context.packageManager
        val stable = ComponentName(context, CarHudProjectionService::class.java)
        val ultrawide = ComponentName(context, CarHudAutoService::class.java)
        val stableState = if (wide) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        val ultrawideState = if (wide) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.setComponentEnabledSettings(
                listOf(
                    PackageManager.ComponentEnabledSetting(
                        stable,
                        stableState,
                        PackageManager.DONT_KILL_APP
                    ),
                    PackageManager.ComponentEnabledSetting(
                        ultrawide,
                        ultrawideState,
                        PackageManager.DONT_KILL_APP
                    )
                )
            )
        } else {
            // Only executed after disconnect, so there is no live host to kill.
            // Enable the destination before disabling the source to avoid a
            // transient state with no Android Auto entry.
            if (wide) {
                pm.setComponentEnabledSetting(
                    ultrawide,
                    ultrawideState,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    stable,
                    stableState,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    stable,
                    stableState,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    ultrawide,
                    ultrawideState,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    private fun verifyPair(context: Context, wide: Boolean): Boolean {
        val stable = isComponentEnabled(context, CarHudProjectionService::class.java, true)
        val ultrawide = isComponentEnabled(context, CarHudAutoService::class.java, false)
        return if (wide) !stable && ultrawide else stable && !ultrawide
    }

    private fun rollbackStable(context: Context, cause: Throwable) {
        try {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                ComponentName(context, CarHudProjectionService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                ComponentName(context, CarHudAutoService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Throwable) {}

        prefs(context).edit()
            .putBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false)
            .putString(KEY_APPLIED, MODE_STABLE)
            .putBoolean(KEY_PENDING, false)
            .putString(KEY_LAST_ERROR, cause.javaClass.simpleName + ": " + (cause.message ?: "unknown"))
            .apply()
    }

    private fun isComponentEnabled(
        context: Context,
        cls: Class<*>,
        manifestDefault: Boolean
    ): Boolean {
        return when (
            context.packageManager.getComponentEnabledSetting(ComponentName(context, cls))
        ) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
            else -> manifestDefault
        }
    }

    private fun sessionActive(): Boolean {
        return CarMediaManager.isCarConnected ||
            CarDisconnectWatchdog.hasSeenProjectionSession()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
}

class CarHostModeUpgradeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CarHostModeManager.reconcileIfSafe(context.applicationContext)
        }
    }
}
