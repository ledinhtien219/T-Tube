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
import com.google.android.apps.auto.sdk.CarActivity
import com.google.android.apps.auto.sdk.CarActivityService

/**
 * v1.0.45 + Fermata-style Android Auto projection host.
 *
 * THTV keeps its existing CarPresentation, WebView/session, IPTV, YouTube,
 * Browser and HUD logic from v1.0.45. Only the Android Auto transport changes:
 * instead of AndroidX CarAppService/NavigationTemplate/ActionStrip, Android Auto
 * hosts a CarActivity directly. This removes the host template action container.
 */
class CarHudProjectionService : CarActivityService() {

    private var powerDisconnectReceiver: BroadcastReceiver? = null

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

            // Preserve the stable v1.0.45 disconnect cleanup behavior.
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            powerDisconnectReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    context?.let {
                        CarAppShutdownManager.shutdownCompletely(
                            it.applicationContext,
                            "Projection cable unplugged (${intent?.action})"
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(powerDisconnectReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(powerDisconnectReceiver, filter)
            }
        } catch (e: Exception) {
            try { AppCrashHandler.logError(e, "CarHudProjectionServiceOnCreate") } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        try {
            powerDisconnectReceiver?.let { unregisterReceiver(it) }
            powerDisconnectReceiver = null
        } catch (_: Exception) {}

        super.onDestroy()

        try {
            CarAppShutdownManager.shutdownCompletely(
                applicationContext,
                "CarHudProjectionService onDestroy"
            )
        } catch (_: Exception) {}
    }
}

class CarHudProjectionActivity : CarActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var gestureDetector: GestureDetector

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: CarPresentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Match Fermata's CarActivity initialization exactly.
        try {
            setIgnoreConfigChanges(-1)
        } catch (_: Throwable) {}
        hideHostChrome()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            clipToPadding = false
            clipChildren = false
        }

        surfaceView = SurfaceView(this).apply {
            holder.addCallback(this@CarHudProjectionActivity)
            setBackgroundColor(Color.BLACK)
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

        // Some hosts recreate their chrome after activity creation. Re-apply the
        // same Fermata UI-controller flags after the view is attached.
        root.postDelayed({ hideHostChrome() }, 250L)
        root.postDelayed({ hideHostChrome() }, 1000L)

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
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        hideHostChrome()
        if (::surfaceView.isInitialized) {
            surfaceView.postDelayed({ hideHostChrome() }, 250L)
        }
    }

    private fun hideHostChrome() {
        try {
            val ui = getCarUiController()
            ui.getStatusBarController().hideAppHeader()
            ui.getMenuController().hideMenuButton()
        } catch (_: Throwable) {}
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val frame = holder.surfaceFrame
        setupPresentation(
            holder.surface,
            frame.width().takeIf { it > 0 } ?: surfaceView.width.takeIf { it > 0 } ?: 800,
            frame.height().takeIf { it > 0 } ?: surfaceView.height.takeIf { it > 0 } ?: 480
        )
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        setupPresentation(
            holder.surface,
            width.takeIf { it > 0 } ?: surfaceView.width.takeIf { it > 0 } ?: 800,
            height.takeIf { it > 0 } ?: surfaceView.height.takeIf { it > 0 } ?: 480
        )
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        try {
            // Same v1.0.45 behavior: keep Chromium/media alive while THTV is
            // temporarily hidden by Maps, Waze or the Android Auto launcher.
            virtualDisplay?.setSurface(null)

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
        if (surface == null || !surface.isValid || width <= 0 || height <= 0) return

        try {
            // Recreate instead of reattaching a previously headless
            // VirtualDisplay; this preserves the black-screen fix from v1.0.45.
            presentation?.dismiss()
            presentation = null
            virtualDisplay?.release()
            virtualDisplay = null

            val dpi = resources.displayMetrics.densityDpi.takeIf { it > 0 } ?: 160
            val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val vd = dm.createVirtualDisplay(
                "THTVProjectionDisplay",
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

            if (CarMediaManager.isPlaying || CarMediaManager.userWantsPlayback) {
                CarMediaManager.ensureAudioFocus()
                CarMediaManager.acquireWakeLock(this)
            }

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
            e.printStackTrace()
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
