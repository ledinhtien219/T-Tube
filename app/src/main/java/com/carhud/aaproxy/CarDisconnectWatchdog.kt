package com.carhud.aaproxy

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

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
    private const val PROJECTION_EXIT_CONFIRM_MS = 6_000L
    const val PREF_AUTO_EXIT_ON_AA_DISCONNECT = "pref_auto_exit_on_aa_disconnect"

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
    private var usbPowerSeen = false

    @Volatile
    private var powerLossStartedAt = 0L

    @Volatile
    private var shutdownTriggered = false

    @Volatile
    internal var shutdownOverrideForTests: ((Context, String) -> Unit)? = null

    // CarConnection is transport-agnostic; do not infer USB vs Wi-Fi from power.
    // Never close on the initial NOT_CONNECTED provider value.
    private val connectionExitGate = ProjectionDisconnectGate(PROJECTION_EXIT_CONFIRM_MS)
    private var connectionLiveData: LiveData<Int>? = null

    private val projectionExitRunnable = Runnable {
        val context = appContext ?: return@Runnable
        if (!projectionSessionSeen || shutdownTriggered) return@Runnable
        if (!context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
                .getBoolean(PREF_AUTO_EXIT_ON_AA_DISCONNECT, true)) return@Runnable
        if (connectionExitGate.shouldExit(SystemClock.uptimeMillis())) {
            triggerShutdown(context, "Android Auto projection disconnected (confirmed 6 seconds)")
        }
    }

    private val connectionObserver = Observer<Int> { type ->
        if (type == null) return@Observer
        connectionExitGate.onConnectionType(type, SystemClock.uptimeMillis())
        handler.removeCallbacks(projectionExitRunnable)
        if (connectionExitGate.isPending()) {
            handler.postDelayed(projectionExitRunnable, PROJECTION_EXIT_CONFIRM_MS)
        }
    }

    private fun beginCarConnectionObservation(context: Context) {
        if (connectionLiveData != null) return
        try {
            val live = CarConnection(context.applicationContext).type
            connectionLiveData = live
            live.observeForever(connectionObserver)
        } catch (e: Exception) {
            connectionLiveData = null
            Log.w(TAG, "CarConnection monitor unavailable; retaining USB watchdog", e)
        }
    }

    private fun stopCarConnectionObservation() {
        handler.removeCallbacks(projectionExitRunnable)
        connectionLiveData?.removeObserver(connectionObserver)
        connectionLiveData = null
        connectionExitGate.reset()
    }

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
        connectionExitGate.cancelPending()
        handler.removeCallbacks(projectionExitRunnable)
        beginCarConnectionObservation(context.applicationContext)

        val plugged = getPluggedType(context)
        if (plugged != 0) {
            externalPowerSeen = true
        }
        if ((plugged and BatteryManager.BATTERY_PLUGGED_USB) != 0) {
            usbPowerSeen = true
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

        val powered = isProjectionPowerConnected(app)
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
        val plugged = getPluggedType(context)
        val usbPowered = (plugged and BatteryManager.BATTERY_PLUGGED_USB) != 0
        if (usbPowered) {
            usbPowerSeen = true
        }
        val powered = if (usbPowerSeen) usbPowered else plugged != 0
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

    private fun getPluggedType(context: Context): Int {
        return try {
            val battery = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        } catch (_: Throwable) {
            0
        }
    }

    private fun isExternalPowerConnected(context: Context): Boolean =
        getPluggedType(context) != 0

    private fun isProjectionPowerConnected(context: Context): Boolean {
        val plugged = getPluggedType(context)
        val usbPowered = (plugged and BatteryManager.BATTERY_PLUGGED_USB) != 0
        if (usbPowered) usbPowerSeen = true
        return if (usbPowerSeen) usbPowered else plugged != 0
    }

    internal fun isExternalPowerConnectedForTests(context: Context): Boolean =
        isExternalPowerConnected(context)

    internal fun isUsbPowerConnectedForTests(context: Context): Boolean =
        (getPluggedType(context) and BatteryManager.BATTERY_PLUGGED_USB) != 0

    fun resetAfterShutdown() {
        handler.removeCallbacks(pollRunnable)
        stopCarConnectionObservation()
        resetSession()
        shutdownTriggered = true
    }

    internal fun resetForTests() {
        handler.removeCallbacks(pollRunnable)
        stopCarConnectionObservation()
        projectionSessionSeen = false
        projectionServiceAlive = false
        externalPowerSeen = false
        usbPowerSeen = false
        powerLossStartedAt = 0L
        shutdownTriggered = false
        appContext = null
        shutdownOverrideForTests = null
    }

    private fun resetSession() {
        handler.removeCallbacks(pollRunnable)
        projectionSessionSeen = false
        externalPowerSeen = false
        usbPowerSeen = false
        powerLossStartedAt = 0L
        shutdownTriggered = false
    }
}
