package com.carhud.aaproxy

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
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
 * Android Auto host path modeled after Fermata:
 * CarActivityService + CarActivity instead of AndroidX CarAppService templates.
 *
 * The existing THTV UI is still rendered by CarPresentation on a VirtualDisplay,
 * but the target surface is a full-screen SurfaceView owned by CarActivity.
 * That removes the template ActionStrip layer that Android Auto was rendering
 * as the unwanted circular action button.
 */
class CarHudProjectionService : CarActivityService() {

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
        } catch (e: Exception) {
            try { AppCrashHandler.logError(e, "CarHudProjectionServiceOnCreate") } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
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

        // Fermata explicitly removes Android Auto's own header/menu chrome.
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
            gestureDetector.onTouchEvent(event)
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
            // Keep Chromium/media alive while Android Auto temporarily hides the app.
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
        if (surface == null || !surface.isValid) return

        try {
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
            throw e
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
