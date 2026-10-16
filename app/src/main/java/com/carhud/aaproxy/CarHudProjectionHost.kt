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
import android.view.KeyEvent
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
            CarDisconnectWatchdog.onProjectionServiceCreated(applicationContext)

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
                    if (!CarDisconnectWatchdog.shouldHandleCableDetach()) {
                        Log.d("CarHudProjectionService", "Ignoring detach without projection session: $action")
                        return
                    }
                    Log.i("CarHudProjectionService", "Projection cable disconnected: $action")
                    CarDisconnectWatchdog.onCableDetachBroadcast(
                        (context ?: applicationContext).applicationContext,
                        action
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
        try {
            cableDisconnectReceiver?.let { unregisterReceiver(it) }
            cableDisconnectReceiver = null
        } catch (_: Exception) {}

        // Do this before super.onDestroy() while application state is still
        // available. The watchdog remembers that projection was active even if
        // Activity.onDestroy() already cleared CarMediaManager.isCarConnected.
        CarDisconnectWatchdog.onProjectionServiceDestroyed(applicationContext)

        super.onDestroy()
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
            isFocusableInTouchMode = true
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

        // Mazda Commander / other rotary head units can deliver the knob as either
        // generic ACTION_SCROLL motion or DPAD/ENTER key events.
        surfaceView.setOnGenericMotionListener { _, event ->
            handleMazdaRotaryMotion(event)
        }
        surfaceView.setOnKeyListener { _, _, event ->
            presentation?.dispatchMazdaKey(event) == true
        }
        surfaceView.requestFocus()
    }

    private fun handleMazdaRotaryMotion(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_SCROLL) return false

        val generic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            event.getAxisValue(MotionEvent.AXIS_SCROLL)
        } else 0f
        val vertical = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
        val horizontal = event.getAxisValue(MotionEvent.AXIS_HSCROLL)

        val value = when {
            kotlin.math.abs(generic) > 0.001f -> generic
            kotlin.math.abs(vertical) > 0.001f -> vertical
            kotlin.math.abs(horizontal) > 0.001f -> horizontal
            else -> 0f
        }
        if (value == 0f) return false

        // Scroll-wheel convention: negative means forward/down, positive back/up.
        // Clamp bursts so one physical detent cannot skip a large part of the UI.
        val steps = kotlin.math.ceil(kotlin.math.abs(value).toDouble()).toInt().coerceIn(1, 4)
        val direction = if (value < 0f) 1 else -1
        Log.d(
            "CarHudMazda",
            "rotary source=${event.source} generic=$generic v=$vertical h=$horizontal steps=$steps dir=$direction"
        )
        repeat(steps) {
            presentation?.dispatchMazdaRotary(direction)
        }
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (presentation?.dispatchMazdaKey(event) == true) return true
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (handleMazdaRotaryMotion(event)) return true
        return super.dispatchGenericMotionEvent(event)
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
        CarDisconnectWatchdog.onProjectionLifecycleEnded(this, "projection surface destroyed")
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
            CarDisconnectWatchdog.onProjectionConnected(this)
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

    override fun onStop() {
        CarDisconnectWatchdog.onProjectionLifecycleEnded(this, "projection activity stopped")
        super.onStop()
    }

    override fun onDestroy() {
        CarDisconnectWatchdog.onProjectionLifecycleEnded(this, "projection activity destroyed")
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
