package com.tcar.tmirror

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.apps.auto.sdk.CarActivity
import com.google.android.apps.auto.sdk.CarActivityService

class LegacyCarService : CarActivityService() {
    override fun getCarActivity(): Class<out CarActivity> = MirrorCarActivity::class.java
}

class FullCarService : CarActivityService() {
    override fun getCarActivity(): Class<out CarActivity> = MirrorCarActivity::class.java
}

/**
 * Android Auto projection canvas. Google Maps never enters this app's process;
 * the host decides full/Legacy layout and retains the genuine Maps experience.
 */
class MirrorCarActivity : CarActivity(), SurfaceHolder.Callback {
    private lateinit var surface: SurfaceView
    private var isReady = false
    private val frameListener: () -> Unit = { surface.post { drawFrame() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setIgnoreConfigChanges(-1)
            getCarUiController().getStatusBarController().hideAppHeader()
            getCarUiController().getMenuController().hideMenuButton()
        } catch (_: Throwable) {}
        surface = SurfaceView(this).apply {
            holder.addCallback(this@MirrorCarActivity)
            setZOrderOnTop(false)
        }
        val root = FrameLayout(this)
        root.addView(surface, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)
        MirrorFrames.addListener(frameListener)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isReady = true
        drawFrame()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        isReady = true
        drawFrame()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) { isReady = false }

    private fun drawFrame() {
        if (!isReady || !surface.holder.surface.isValid) return
        var canvas: android.graphics.Canvas? = null
        try {
            canvas = surface.holder.lockCanvas()
            if (canvas != null) MirrorFrames.draw(canvas)
        } catch (_: Exception) {
            // Surface is asynchronously torn down when AA switches panes.
        } finally {
            if (canvas != null) {
                try { surface.holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        MirrorFrames.removeListener(frameListener)
        super.onDestroy()
    }
}
