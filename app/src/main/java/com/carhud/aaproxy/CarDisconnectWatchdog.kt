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
    private const val USB_DETACH_CONFIRM_MS = 1_500L
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
        if (!autoExitEnabled(context)) return@Runnable
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

    // Charger power is NOT evidence of Android Auto disconnect. When AndroidX
    // cannot provide state, only an explicit USB accessory/device detach is a
    // fallback. Never poll battery power to terminate a wireless session.
    private val pollRunnable = object : Runnable {
        override fun run() = Unit
    }

    private val usbDetachFallback = Runnable {
        val context = appContext ?: return@Runnable
        if (!projectionSessionSeen || shutdownTriggered || !autoExitEnabled(context)) return@Runnable
        if (connectionLiveData != null || connectionExitGate.isProjectionActive()) return@Runnable
        triggerShutdown(context, "explicit USB accessory/device detached (no state provider)")
    }

    private fun autoExitEnabled(context: Context): Boolean =
        context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
            .getBoolean(PREF_AUTO_EXIT_ON_AA_DISCONNECT, true)

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
        handler.removeCallbacks(usbDetachFallback)
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
        Log.i(TAG, "Projection session active; awaiting Android Auto connection state")
    }

    /**
     * Called from Activity/Surface lifecycle callbacks. A normal Android Auto app
     * switch may produce these callbacks while the cable is still powered, so we
     * only shut down here if the independent power check confirms cable loss.
     */
    fun onProjectionLifecycleEnded(context: Context, reason: String) {
        appContext = context.applicationContext
        // Surface loss can be a normal app switch or projection resize.
        Log.d(TAG, "Projection surface ended; waiting for connection state: $reason")
    }

    /**
     * Service destruction is the final lifecycle fallback. Do not consult
     * CarMediaManager.isCarConnected here because Activity.onDestroy may already
     * have cleared it. Use the sticky projectionSessionSeen state instead.
     */
    fun onProjectionServiceDestroyed(context: Context) {
        appContext = context.applicationContext
        projectionServiceAlive = false
        // Android Auto may recreate this service even while connected.
        // Keep the CarConnection observer until confirmed disconnect.
        Log.d(TAG, "Projection service destroyed; waiting for connection state")
    }

    fun shouldHandleCableDetach(): Boolean =
        projectionSessionSeen || CarMediaManager.isCarConnected

    fun onCableDetachBroadcast(context: Context, action: String) {
        if (!shouldHandleCableDetach() || shutdownTriggered || !autoExitEnabled(context)) return
        // A phone can be charging over USB while Android Auto runs wirelessly.
        if (action == Intent.ACTION_POWER_DISCONNECTED) return
        if (action != android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED &&
            action != android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_DETACHED) return
        if (connectionLiveData != null || connectionExitGate.isProjectionActive()) return
        appContext = context.applicationContext
        handler.removeCallbacks(usbDetachFallback)
        handler.postDelayed(usbDetachFallback, USB_DETACH_CONFIRM_MS)
    }

    fun hasSeenProjectionSession(): Boolean = projectionSessionSeen

    private fun triggerShutdown(context: Context, reason: String) {
        if (shutdownTriggered) return
        shutdownTriggered = true
        handler.removeCallbacks(pollRunnable)
        handler.removeCallbacks(usbDetachFallback)
        handler.removeCallbacks(projectionExitRunnable)
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
        handler.removeCallbacks(usbDetachFallback)
        stopCarConnectionObservation()
        resetSession()
        shutdownTriggered = true
    }

    internal fun resetForTests() {
        handler.removeCallbacks(pollRunnable)
        handler.removeCallbacks(usbDetachFallback)
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
