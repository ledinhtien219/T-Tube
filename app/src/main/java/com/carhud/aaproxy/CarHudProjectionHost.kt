package com.carhud.aaproxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.util.Log
import com.google.android.apps.auto.sdk.CarActivity
import com.google.android.apps.auto.sdk.CarActivityService

/**
 * Projection host matched to the v1.0.39 APK that was verified on Android Auto.
 *
 * Important: do not give SurfaceView or its parent an opaque background here.
 * On DHU/AA that can cover the VirtualDisplay surface and produce a black screen.
 */
class CarHudProjectionService : CarActivityService() {

    private var cableDisconnectReceiver: BroadcastReceiver? = null

    override fun getCarActivity(): Class<out CarActivity> =
        CarHudProjectionActivity::class.java

    override fun onCreate() {
        super.onCreate()
        try {
            AppCrashHandler.init(applicationContext)
            CarTtsManager.init(applicationContext)
            CarTtsManager.startListeningToRepository()
            GpsSpeedManager.start(applicationContext)
            WazeHlpWebSocketManager.start()

            // This is the projection service actually declared in AndroidManifest.
            // Listen here (not only in the legacy AndroidX CarAppService) so a
            // physical cable/accessory detach always shuts the phone app down too.
            val disconnectFilter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            }
            cableDisconnectReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action ?: return
                    Log.i("CarHudProjectionService", "Projection cable disconnected: $action")
                    CarAppShutdownManager.shutdownCompletely(
                        (context ?: applicationContext).applicationContext,
                        "Android Auto cable disconnected ($action)"
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(cableDisconnectReceiver, disconnectFilter, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(cableDisconnectReceiver, disconnectFilter)
            }
        } catch (e: Exception) {
            try { AppCrashHandler.logError(e, "CarHudProjectionServiceOnCreate") } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        val wasConnected = CarMediaManager.isCarConnected
        try {
            cableDisconnectReceiver?.let { unregisterReceiver(it) }
            cableDisconnectReceiver = null
        } catch (_: Exception) {}

        super.onDestroy()

        // Fallback for hosts/devices that do not deliver a USB detach broadcast.
        // If an active projection service is destroyed, treat it as a real car
        // disconnect and terminate the phone/background process as requested.
        if (wasConnected) {
            CarAppShutdownManager.shutdownCompletely(
                applicationContext,
                "Android Auto projection service destroyed"
            )
        }
    }
}

class CarHudProjectionActivity : CarActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var gestureDetector: GestureDetector

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: CarPresentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setIgnoreConfigChanges(-1)
            val ui = getCarUiController()
            ui.getStatusBarController().hideAppHeader()
            ui.getMenuController().hideMenuButton()
        } catch (_: Throwable) {}

        val root = FrameLayout(this)

        surfaceView = SurfaceView(this).apply {
            holder.addCallback(this@CarHudProjectionActivity)
            isClickable = true
            isFocusable = true
        }

        root.addView(
            surfaceView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    presentation?.dispatchTouch(e.x, e.y)
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    presentation?.dispatchScroll(distanceX, distanceY)
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    presentation?.dispatchFling(velocityX, velocityY)
                    return true
                }
            }
        )

        surfaceView.setOnTouchListener { _, event ->
            // Keep complete pointer streams for fast typing and WebView gestures.
            if (presentation?.dispatchProjectedTouch(event) != true) {
                gestureDetector.onTouchEvent(event)
            }
            true
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val frame = holder.surfaceFrame
        val width = frame.width().takeIf { it > 0 } ?: 800
        val height = frame.height().takeIf { it > 0 } ?: 480
        setupPresentation(holder.surface, width, height)
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        setupPresentation(
            holder.surface,
            width.takeIf { it > 0 } ?: 800,
            height.takeIf { it > 0 } ?: 480
        )
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        try {
            virtualDisplay?.setSurface(null)

            // Surface loss can be a resize/recreation while projection is still
            // connected, so do not treat this callback alone as a cable detach.
            // The service-level USB/power receiver owns full shutdown.
            val web = CarMediaManager.getPersistentWebView()
            web?.onResume()
            web?.resumeTimers()

            if (CarMediaManager.isPlaying || CarMediaManager.userWantsPlayback) {
                CarMediaManager.registerService(this)
                CarMediaManager.acquireWakeLock(this)
                CarMediaManager.ensureAudioFocus()
            }
        } catch (_: Exception) {}
    }

    private fun setupPresentation(surface: Surface?, width: Int, height: Int) {
        if (surface == null || !surface.isValid) return

        try {
            presentation?.dismiss()
            presentation = null
            virtualDisplay?.release()
            virtualDisplay = null

            val dpi = resources.displayMetrics.densityDpi.takeIf { it > 0 } ?: 160
            val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val vd = dm.createVirtualDisplay(
                "TTubeProjectionDisplay",
                width,
                height,
                dpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )
            virtualDisplay = vd

            val persistentWeb = CarMediaManager.getPersistentCarWebView(this)
            val p = CarPresentation(this, vd.display, persistentWeb)
            p.show()
            presentation = p

            CarMediaManager.setCarConnectionState(true)
            CarMediaManager.carScreenWidth = width
            CarMediaManager.carScreenHeight = height
            CarMediaManager.carScreenDpi = dpi

            try {
                getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putInt("car_screen_width", width)
                    .putInt("car_screen_height", height)
                    .putInt("car_screen_dpi", dpi)
                    .putBoolean("car_screen_connected", true)
                    .apply()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            try { AppCrashHandler.logError(e, "CarHudProjectionSetupPresentation") } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        try {
            presentation?.dismiss()
            presentation = null
            virtualDisplay?.release()
            virtualDisplay = null
            CarMediaManager.setCarConnectionState(false)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
