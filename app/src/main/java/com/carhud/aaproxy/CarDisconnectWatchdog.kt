package com.carhud.aaproxy

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * Watches a real wired Android Auto session for cable/power loss.
 *
 * Some phones/hosts do not deliver USB_DEVICE_DETACHED to the projection app,
 * and the projection Activity can be torn down before the Service. Therefore
 * the disconnect decision cannot rely on CarMediaManager.isCarConnected alone.
 *
 * Once a projection surface has really been created, we remember that fact for
 * the lifetime of the projection service and independently watch the battery
 * plugged state. A confirmed external-power transition ON -> OFF is treated as
 * a cable disconnect and triggers a complete application shutdown.
 */
object CarDisconnectWatchdog {
    private const val TAG = "CarDisconnectWatchdog"
    private const val POLL_MS = 500L
    private const val POWER_LOSS_CONFIRM_MS = 900L

    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var projectionSessionSeen = false

    @Volatile
    private var projectionServiceAlive = false

    @Volatile
    private var externalPowerSeen = false

    @Volatile
    private var powerLossStartedAt = 0L

    @Volatile
    private var shutdownTriggered = false

    @Volatile
    internal var shutdownOverrideForTests: ((Context, String) -> Unit)? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            val context = appContext ?: return
            if (!projectionSessionSeen || shutdownTriggered) return

            checkPowerState(context, "watchdog poll")

            if (projectionSessionSeen && !shutdownTriggered) {
                handler.postDelayed(this, POLL_MS)
            }
        }
    }

    fun onProjectionServiceCreated(context: Context) {
        appContext = context.applicationContext
        projectionServiceAlive = true
    }

    fun onProjectionConnected(context: Context) {
        appContext = context.applicationContext
        projectionSessionSeen = true
        projectionServiceAlive = true
        shutdownTriggered = false
        powerLossStartedAt = 0L

        if (isExternalPowerConnected(context)) {
            externalPowerSeen = true
        }

        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
        Log.i(TAG, "Projection session active; externalPowerSeen=$externalPowerSeen")
    }

    /**
     * Called from Activity/Surface lifecycle callbacks. A normal Android Auto app
     * switch may produce these callbacks while the cable is still powered, so we
     * only shut down here if the independent power check confirms cable loss.
     */
    fun onProjectionLifecycleEnded(context: Context, reason: String) {
        appContext = context.applicationContext
        if (!projectionSessionSeen || shutdownTriggered) return
        checkPowerState(context.applicationContext, reason)
    }

    /**
     * Service destruction is the final lifecycle fallback. Do not consult
     * CarMediaManager.isCarConnected here because Activity.onDestroy may already
     * have cleared it. Use the sticky projectionSessionSeen state instead.
     */
    fun onProjectionServiceDestroyed(context: Context) {
        val app = context.applicationContext
        appContext = app
        projectionServiceAlive = false

        if (!projectionSessionSeen || shutdownTriggered) return

        val powered = isExternalPowerConnected(app)
        if (externalPowerSeen && !powered) {
            triggerShutdown(app, "projection service destroyed after cable power loss")
            return
        }

        // IMPORTANT: do NOT forget a real projection session just because the
        // CarActivityService is destroyed/recreated while the cable is still
        // plugged in. Some Android Auto hosts tear this service down when the
        // projection app loses foreground. The media service can remain alive,
        // then a later physical unplug would otherwise be missed completely.
        //
        // Keep the sticky session armed and continue the independent power poll
        // until cable loss is confirmed or the process itself is shut down.
        if (powered) {
            externalPowerSeen = true
            powerLossStartedAt = 0L
            handler.removeCallbacks(pollRunnable)
            handler.post(pollRunnable)
            Log.i(TAG, "Projection service destroyed while cable is still powered; keeping session armed")
        } else if (!externalPowerSeen) {
            // No reliable power sample was ever observed. Keep polling rather
            // than clearing the session so a later detach/power signal can win.
            handler.removeCallbacks(pollRunnable)
            handler.post(pollRunnable)
        }
    }

    fun shouldHandleCableDetach(): Boolean =
        projectionSessionSeen || CarMediaManager.isCarConnected

    fun onCableDetachBroadcast(context: Context, action: String) {
        if (!shouldHandleCableDetach() || shutdownTriggered) return
        triggerShutdown(
            context.applicationContext,
            "Android Auto cable disconnected ($action)"
        )
    }

    fun hasSeenProjectionSession(): Boolean = projectionSessionSeen

    private fun checkPowerState(context: Context, source: String) {
        val powered = isExternalPowerConnected(context)
        if (powered) {
            externalPowerSeen = true
            powerLossStartedAt = 0L
            return
        }

        if (!externalPowerSeen) return

        val now = SystemClock.uptimeMillis()
        if (powerLossStartedAt == 0L) {
            powerLossStartedAt = now
            Log.i(TAG, "External power lost; confirming disconnect. source=$source")
            handler.removeCallbacks(pollRunnable)
            handler.postDelayed(pollRunnable, POWER_LOSS_CONFIRM_MS)
            return
        }

        if (now - powerLossStartedAt >= POWER_LOSS_CONFIRM_MS) {
            triggerShutdown(
                context,
                "Android Auto external power lost while projection active ($source)"
            )
        }
    }

    private fun triggerShutdown(context: Context, reason: String) {
        if (shutdownTriggered) return
        shutdownTriggered = true
        handler.removeCallbacks(pollRunnable)
        Log.i(TAG, "Confirmed projection disconnect: $reason")

        val override = shutdownOverrideForTests
        if (override != null) {
            override(context.applicationContext, reason)
        } else {
            CarAppShutdownManager.shutdownCompletely(context.applicationContext, reason)
        }
    }

    private fun isExternalPowerConnected(context: Context): Boolean {
        return try {
            val battery = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            plugged != 0
        } catch (_: Throwable) {
            false
        }
    }

    internal fun isExternalPowerConnectedForTests(context: Context): Boolean =
        isExternalPowerConnected(context)

    fun resetAfterShutdown() {
        handler.removeCallbacks(pollRunnable)
        resetSession()
        shutdownTriggered = true
    }

    internal fun resetForTests() {
        handler.removeCallbacks(pollRunnable)
        projectionSessionSeen = false
        projectionServiceAlive = false
        externalPowerSeen = false
        powerLossStartedAt = 0L
        shutdownTriggered = false
        appContext = null
        shutdownOverrideForTests = null
    }

    private fun resetSession() {
        handler.removeCallbacks(pollRunnable)
        projectionSessionSeen = false
        externalPowerSeen = false
        powerLossStartedAt = 0L
        shutdownTriggered = false
    }
}
