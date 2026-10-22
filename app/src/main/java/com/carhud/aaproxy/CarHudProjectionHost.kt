package com.carhud.aaproxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
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
    private var projectionForeground = false

    // surfaceCreated + surfaceChanged both fire on start, and some head units repeat
    // surfaceChanged with identical geometry. Rebuilding the VirtualDisplay/Presentation
    // each time flashes the background and can leave video black or restarted.
    private var lastSetupKey = ""
    private var hostConfiguration: Configuration? = null
    private val displayHandler = Handler(Looper.getMainLooper())
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (::surfaceView.isInitialized && surfaceView.display?.displayId == displayId) {
                val frame = surfaceView.holder.surfaceFrame
                setupPresentation(surfaceView.holder.surface, frame.width(), frame.height())
            }
        }
    }

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
        (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
            .registerDisplayListener(displayListener, displayHandler)

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
            // Never consume touches once Android Auto has switched to Maps or
            // another app. Some head units keep the old projection surface
            // attached briefly during the handoff.
            if (!projectionForeground) return@setOnTouchListener false

            // Keep complete pointer streams for fast typing and WebView gestures.
            if (presentation?.dispatchProjectedTouch(event) != true) {
                gestureDetector.onTouchEvent(event)
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        projectionForeground = true
        presentation?.onProjectionHostForegrounded()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val frame = holder.surfaceFrame
        setupPresentation(holder.surface, frame.width(), frame.height())
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        setupPresentation(
            holder.surface,
            width,
            height
        )
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // The same Surface object can come back after destroy; force a full rebuild then.
        lastSetupKey = ""
        CarDisconnectWatchdog.onProjectionLifecycleEnded(this, "projection surface destroyed")
        try {
            virtualDisplay?.setSurface(null)

            // Surface loss also happens while Android Auto hands the screen to
            // Google Maps. Do not force Chromium/timers/audio focus back to the
            // foreground here: that used to make Maps janky and could steal
            // navigation audio focus. Preserve only real background playback.
            if (!projectionForeground) {
                val web = CarMediaManager.getPersistentWebView()
                val keepAudio = CarMediaManager.isPlaying && CarMediaManager.userWantsPlayback
                if (web is BackgroundAudioWebView) web.enableBackgroundAudio = keepAudio
                if (keepAudio) {
                    CarMediaManager.registerService(this)
                    CarMediaManager.acquireWakeLock(this)
                } else {
                    web?.clearFocus()
                    web?.onPause()
                    CarMediaManager.releaseWakeLock()
                }
            }
        } catch (_: Exception) {}
    }

    private fun resolveDensity(width: Int, height: Int): ProjectionMetrics.Density {
        val target = surfaceView.display
        // Never use DEFAULT_DISPLAY: it commonly identifies the phone, not the car.
        if (target != null && target.displayId != Display.DEFAULT_DISPLAY) {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            target.getRealMetrics(metrics)
            if (metrics.densityDpi in 72..960) {
                return ProjectionMetrics.resolve(width, height, metrics.densityDpi, "host_display")
            }
        }
        val config = hostConfiguration ?: resources.configuration
        val phone = applicationContext.resources.configuration
        val differsFromPhone = config.densityDpi != phone.densityDpi ||
            config.screenWidthDp != phone.screenWidthDp || config.screenHeightDp != phone.screenHeightDp
        if (differsFromPhone && ProjectionMetrics.matchesViewport(
                width, height, config.screenWidthDp, config.screenHeightDp, config.densityDpi
            )) {
            return ProjectionMetrics.resolve(width, height, config.densityDpi, "host_configuration")
        }
        return ProjectionMetrics.resolve(width, height)
    }

    override fun onConfigurationChanged(newConfiguration: Configuration) {
        super.onConfigurationChanged(newConfiguration)
        hostConfiguration = Configuration(newConfiguration)
        if (::surfaceView.isInitialized) {
            surfaceView.post {
                val frame = surfaceView.holder.surfaceFrame
                setupPresentation(surfaceView.holder.surface, frame.width(), frame.height())
                presentation?.onCarConfigurationChanged(newConfiguration)
            }
        }
    }

    private fun setupPresentation(surface: Surface?, width: Int, height: Int) {
        if (surface == null || !surface.isValid || width <= 0 || height <= 0) return

        val density = resolveDensity(width, height)
        val dpi = density.dpi
        val setupKey = "${System.identityHashCode(surface)}|$width|$height|$dpi"
        if (setupKey == lastSetupKey && virtualDisplay != null && presentation != null) return

        try {
            presentation?.dismiss()
            presentation = null
            virtualDisplay?.release()
            virtualDisplay = null

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

            val persistentWeb = CarMediaManager.getPersistentCarWebView(createDisplayContext(vd.display))
            val p = CarPresentation(this, vd.display, persistentWeb)
            p.show()
            if (projectionForeground) p.onProjectionHostForegrounded()
            else p.onProjectionHostBackgrounded()
            presentation = p
            lastSetupKey = setupKey
            Log.i("CarHudProjection", "Surface=${width}x$height dpi=$dpi source=${density.source}")

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
                    .putString("car_screen_dpi_source", density.source)
                    .putBoolean("car_screen_connected", true)
                    .apply()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            try { AppCrashHandler.logError(e, "CarHudProjectionSetupPresentation") } catch (_: Exception) {}
        }
    }

    override fun onStop() {
        projectionForeground = false
        presentation?.onProjectionHostBackgrounded()
        CarDisconnectWatchdog.onProjectionLifecycleEnded(this, "projection activity stopped")
        super.onStop()
    }

    override fun onDestroy() {
        (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).unregisterDisplayListener(displayListener)
        CarDisconnectWatchdog.onProjectionLifecycleEnded(this, "projection activity destroyed")
        try {
            presentation?.dismiss()
            presentation = null
            virtualDisplay?.release()
            virtualDisplay = null
            lastSetupKey = ""
            CarMediaManager.setCarConnectionState(false)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
