package com.carhud.aaproxy

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/**
 * Clears any component override left by the removed ultrawide host-toggle build.
 * PackageManager component state can survive an APK update, so users who once
 * enabled ultrawide must be migrated back to the manifest-default projection
 * host without clearing app data.
 */
object StableProjectionHost {
    private const val TAG = "StableProjectionHost"

    fun ensureEnabled(context: Context) {
        try {
            val app = context.applicationContext
            val component = ComponentName(app, CarHudProjectionService::class.java)
            val pm = app.packageManager
            val current = pm.getComponentEnabledSetting(component)
            if (current != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP
                )
                Log.i(TAG, "Restored CarHudProjectionService to manifest default")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to restore projection component state", t)
        }
    }
}

class StableProjectionUpgradeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            StableProjectionHost.ensureEnabled(context)
        }
    }
}
