package com.carhud.aaproxy

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.MapTemplate
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class CarHudAutoScreen(carContext: CarContext) : Screen(carContext), SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: CarPresentation? = null
    private var carSurface: Surface? = null
    private val handler = Handler(Looper.getMainLooper())

    private var nativeInput: CarInputSession? = null
    private val nativeSearchListener: (CarInputSession) -> Unit = { input ->
        handler.post {
            try {
                // Replace a stale native keyboard session instead of cancelling the
                // new request. This matters on DHU/AA hosts that briefly keep the old
                // SearchTemplate alive while the projection surface is recreated.
                nativeInput?.takeIf { it !== input && !it.closed }?.cancel()
                nativeInput = input
                screenManager.push(
                    CarSearchScreen(carContext, input) {
                        if (nativeInput === input) nativeInput = null
                    }
                )
            } catch (e: Exception) {
                if (nativeInput === input) nativeInput = null
                input.cancel()
                e.printStackTrace()
            }
        }
    }

    init {
        try {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
            val navManager = carContext.getCarService(androidx.car.app.navigation.NavigationManager::class.java)
            navManager.setNavigationManagerCallback(object : androidx.car.app.navigation.NavigationManagerCallback {
                override fun onStopNavigation() {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        CarMediaManager.registerCarNativeSearchListener(nativeSearchListener)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                nativeInput?.cancel()
                CarMediaManager.unregisterCarNativeSearchListener(nativeSearchListener)
                fullCleanup()
            }
        })
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        carSurface = surfaceContainer.surface
        val width = if (surfaceContainer.width > 0) surfaceContainer.width else 800
        val height = if (surfaceContainer.height > 0) surfaceContainer.height else 480
        val dpi = if (surfaceContainer.dpi > 0) surfaceContainer.dpi else 160

        handler.post {
            setupPresentation(carSurface, width, height, dpi)
            try { invalidate() } catch (e: Exception) {}
        }
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {}
    override fun onStableAreaChanged(stableArea: Rect) {}

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        CarDisconnectWatchdog.onProjectionLifecycleEnded(
            carContext,
            "full-surface SurfaceContainer destroyed"
        )
        handler.post {
            try {
                // User switched to Maps/Waze/Android Auto home. Keep the Presentation and
                // VirtualDisplay alive (headless) so Chromium still considers the WebView
                // attached to a visible Window. Detaching/dismissing here makes YouTube/HLS
                // get background-throttled or suspended on a number of Android Auto phones.
                // The display is recreated in setupPresentation() when our surface returns.
                try { virtualDisplay?.setSurface(null) } catch (_: Exception) {}

                val web = CarMediaManager.getPersistentWebView()
                web?.onResume()
                web?.resumeTimers()
                if (CarMediaManager.isPlaying || CarMediaManager.userWantsPlayback) {
                    CarMediaManager.registerService(carContext)
                    CarMediaManager.acquireWakeLock(carContext)
                    CarMediaManager.ensureAudioFocus()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try { invalidate() } catch (e: Exception) {}
        }
    }

    override fun onClick(x: Float, y: Float) {
        if (Looper.myLooper() == handler.looper) presentation?.dispatchTouch(x, y)
        else handler.post { presentation?.dispatchTouch(x, y) }
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        handler.post {
            presentation?.dispatchScroll(distanceX, distanceY)
        }
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        handler.post {
            presentation?.dispatchFling(velocityX, velocityY)
        }
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {}

    fun onCarConfigurationChanged(newConfiguration: android.content.res.Configuration) {
        handler.post {
            presentation?.onCarConfigurationChanged(newConfiguration)
        }
    }

    private fun setupPresentation(surface: Surface?, width: Int, height: Int, dpi: Int) {
        if (surface == null || !surface.isValid) return

        try {
            // CRITICAL FIX: NEVER reuse VirtualDisplay if it was headless (surface=null). 
            // Android has a bug where setting surface back causes a permanent black screen (sound but no picture)!
            // Always recreate the VirtualDisplay and Presentation, and just re-attach the persistent WebView.
            presentation?.dismiss()
            virtualDisplay?.release()

            val dm = carContext.getSystemService(CarContext.DISPLAY_SERVICE) as DisplayManager
            val vd = dm.createVirtualDisplay(
                "CarHudYouTubeDisplay",
                width,
                height,
                dpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )
            virtualDisplay = vd

            val persistentWeb = CarMediaManager.getPersistentCarWebView(carContext)
            val pres = CarPresentation(carContext, vd.display, persistentWeb)
            pres.show()
            presentation = pres

            CarMediaManager.setCarConnectionState(true)
            CarDisconnectWatchdog.onProjectionConnected(carContext)
            CarMediaManager.carScreenWidth = width
            CarMediaManager.carScreenHeight = height
            CarMediaManager.carScreenDpi = dpi
            // Respect an explicit user pause. Only keep the media pipeline hot when playback
            // is already active/requested; reopening our screen must not unexpectedly resume audio.
            if (CarMediaManager.isPlaying || CarMediaManager.userWantsPlayback) {
                CarMediaManager.ensureAudioFocus()
                CarMediaManager.acquireWakeLock(carContext)
            }

            try {
                val prefs = carContext.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt("car_screen_width", width)
                    .putInt("car_screen_height", height)
                    .putInt("car_screen_dpi", dpi)
                    .putBoolean("car_screen_connected", true)
                    .putString("car_host_runtime", CarHostModeManager.MODE_ULTRAWIDE)
                    .putInt("car_app_api_level", carContext.carAppApiLevel)
                    .apply()
            } catch (e: Exception) {}


        } catch (e: Exception) {
            AppCrashHandler.logError(e, "AndroidAutoSetupPresentation")
            e.printStackTrace()
        }
    }


    private fun fullCleanup() {
        try {
            presentation?.dismiss()
            presentation = null
            virtualDisplay?.release()
            virtualDisplay = null
            CarMediaManager.setCarConnectionState(false)
        } catch (e: Exception) {}
    }

    override fun onGetTemplate(): Template {
        // API 5+ supports MapTemplate without an ActionStrip. The previous
        // experiment crashed because MapTemplate requires both Pane and Header;
        // provide valid empty map content plus a zero-width header here.
        if (carContext.carAppApiLevel >= 5) {
            // Pane.Builder() cannot be empty unless setLoading(true). An empty,
            // non-loading Pane crashes the host with:
            // "The pane is set to loading but is not empty, or vice versa".
            // Use one zero-width informational row instead: valid content, no
            // action strip, and no loading spinner over the projected surface.
            val invisiblePane = Pane.Builder()
                .addRow(
                    Row.Builder()
                        .setTitle("\u200B")
                        .build()
                )
                .build()

            return MapTemplate.Builder()
                .setPane(invisiblePane)
                .setHeader(
                    Header.Builder()
                        .setTitle("\u200B")
                        .build()
                )
                .build()
        }

        // Legacy Car API hosts require NavigationTemplate and a non-empty
        // ActionStrip. Keep the smallest possible transparent placeholder.
        val transparent = CarColor.createCustom(0x00000000, 0x00000000)
        val hiddenActionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("\u200B")
                    .setFlags(Action.FLAG_PRIMARY)
                    .setBackgroundColor(transparent)
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(hiddenActionStrip)
            .build()
    }
}
