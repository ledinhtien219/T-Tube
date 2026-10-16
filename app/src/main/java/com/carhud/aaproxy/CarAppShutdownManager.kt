package com.carhud.aaproxy

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * Handles complete application shutdown when the cable is unplugged or Android Auto disconnects.
 * Stops all services, audio playback, background tasks, and kills the app process cleanly.
 */
object CarAppShutdownManager {

    private const val TAG = "CarAppShutdownManager"
    const val ACTION_FULL_EXIT = "com.carhud.aaproxy.ACTION_FULL_EXIT"

    @Volatile
    private var isShuttingDown = false

    fun shutdownCompletely(context: Context, reason: String) {
        if (isShuttingDown) return
        isShuttingDown = true
        CarDisconnectWatchdog.resetAfterShutdown()
        // The real cable-disconnect path is the safe moment to swap Android
        // Auto entry components for the next connection.
        try {
            CarHostModeManager.applyPendingAfterDisconnect(context.applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying pending Android Auto host mode", e)
        }
        Log.i(TAG, "Shutting down application completely. Reason: $reason")

        try {
            // 1. Stop Audio Playback and Release WakeLocks
            try {
                CarMediaManager.userWantsPlayback = false
                CarMediaManager.currentLoadingUrl = null
                // Stop both persistent WebViews before connection state changes;
                // otherwise the async pause path can target the wrong WebView.
                CarMediaManager.pauseAllPlaybackForShutdown()
                CarMediaManager.releaseWakeLock()
                CarMediaManager.setCarConnectionState(false, resumePhoneOnDisconnect = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping CarMediaManager", e)
            }

            // 2. Stop Background Services & WebSockets
            try {
                WazeHlpWebSocketManager.stop()
                GpsSpeedManager.stop()
                CarTtsManager.shutdown()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping background tasks", e)
            }

            // 3. Stop whichever Android Auto host is active + media services
            // so no component can restart playback while the phone task closes.
            try {
                context.stopService(Intent(context, CarHudProjectionService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping CarHudProjectionService", e)
            }
            try {
                context.stopService(Intent(context, CarHudAutoService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping CarHudAutoService", e)
            }
            try {
                // Release MediaSession + foreground notification synchronously
                // before process death so OEM SystemUI cannot keep stale media
                // controls alive after the cable is unplugged.
                CarMediaBrowserService.forceShutdownForDisconnect(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error force-stopping CarMediaBrowserService", e)
            }

            // Persist the disconnected state before the process is terminated.
            try {
                context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("car_screen_connected", false)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting disconnected state", e)
            }

            // 4. Send Broadcast to finish/remove the phone task immediately
            try {
                val exitIntent = Intent(ACTION_FULL_EXIT).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(exitIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending full exit broadcast", e)
            }

            // 5. Force process exit after short delay to allow broadcast processing
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.i(TAG, "Killing process PID: ${Process.myPid()}")
                    Process.killProcess(Process.myPid())
                    exitProcess(0)
                } catch (e: Exception) {
                    exitProcess(0)
                }
            }, 300L)
        } catch (e: Exception) {
            Log.e(TAG, "Uncaught error during shutdown", e)
            try {
                Process.killProcess(Process.myPid())
                exitProcess(0)
            } catch (ex: Exception) {}
        }
    }
}
