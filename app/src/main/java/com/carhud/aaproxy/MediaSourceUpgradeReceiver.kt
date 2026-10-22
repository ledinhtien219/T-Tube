package com.carhud.aaproxy

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Refreshes the single Android Auto media source after an in-place APK update.
 *
 * Some Android Auto launcher builds can retain a stale media-source row across
 * package upgrades even though the merged APK contains only one
 * MediaBrowserService. Briefly cycling only that service component forces the
 * host to invalidate the old component cache without changing the projection
 * entry or the application's normal launcher activity.
 */
class MediaSourceUpgradeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MediaSourceUpgrade"
        private const val REENABLE_DELAY_MS = 750L
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        val handler = Handler(Looper.getMainLooper())

        try {
            val packageManager = appContext.packageManager
            val mediaComponent = ComponentName(
                appContext,
                CarMediaBrowserService::class.java
            )

            // Package replacement has already restarted app components, so this
            // short DONT_KILL_APP cycle is safe and limited to the media source.
            packageManager.setComponentEnabledSetting(
                mediaComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )

            handler.postDelayed({
                try {
                    packageManager.setComponentEnabledSetting(
                        mediaComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.i(TAG, "Refreshed Android Auto media source after upgrade")
                } catch (error: Throwable) {
                    Log.w(TAG, "Unable to re-enable Android Auto media source", error)
                } finally {
                    pendingResult.finish()
                }
            }, REENABLE_DELAY_MS)
        } catch (error: Throwable) {
            Log.w(TAG, "Unable to refresh Android Auto media source", error)
            pendingResult.finish()
        }
    }
}
