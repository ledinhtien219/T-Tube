package com.tcar.tmirror

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import java.nio.ByteBuffer

/**
 * Clean-room MediaProjection capture; nothing in this class originates from
 * ScreenOnAuto's proprietary APK. Captures with user consent, never hidden.
 */
class CaptureService : Service() {
    companion object {
        const val ACTION_START = "com.tcar.tmirror.START"
        const val ACTION_STOP = "com.tcar.tmirror.STOP"
        const val EXTRA_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        const val CHANNEL = "tmirror_capture"
        const val NOTIFICATION_ID = 42
        @Volatile var running = false
            private set
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var callback: MediaProjection.Callback? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var lastFrameAt = 0L
    private var stopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "T-Mirror screen sharing",
            NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        if (projection != null) return START_NOT_STICKY

        val mainIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, CaptureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("T-Mirror đang chia sẻ màn hình")
            .setContentText("Nhấn để mở ứng dụng và dừng chia sẻ.")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pi)
            .addAction(Notification.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, "Dừng", stopIntent).build())
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        val code = intent.getIntExtra(EXTRA_CODE, 0)
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else intent.getParcelableExtra(EXTRA_DATA)
        if (data == null || code == 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        worker = HandlerThread("TMirrorCapture").also { it.start() }
        handler = Handler(worker!!.looper)
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val instance = manager.getMediaProjection(code, data)
            if (instance == null) {
                stopSelf()
                return START_NOT_STICKY
            }
            projection = instance
            callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture()
                    stopSelf()
                }

                override fun onCapturedContentResize(width: Int, height: Int) {
                    if (width > 0 && height > 0) {
                        handler?.post { resize(width, height) }
                    }
                }
            }.also { instance.registerCallback(it, handler) }

            // Match actual phone screen, but limit scale to reduce heat and RAM.
            val size = getSystemService(WindowManager::class.java).defaultDisplay
            val point = android.graphics.Point().also { size.getRealSize(it) }
            val width = point.x.coerceAtLeast(1)
            val height = point.y.coerceAtLeast(1)
            val maxEdge = 1280f
            val ratio = minOf(1f, maxEdge / maxOf(width, height))
            val w = (width * ratio).toInt().coerceAtLeast(2)
            val h = (height * ratio).toInt().coerceAtLeast(2)
            val dpi = resources.displayMetrics.densityDpi

            setReader(w, h)
            virtualDisplay = instance.createVirtualDisplay(
                "TMirrorPhone",
                w, h, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader!!.surface, null, handler
            )
            running = true
            Log.i("TMirror", "Capture started: ${w}x${h}")
        } catch (e: Exception) {
            Log.e("TMirror", "Cannot start MediaProjection", e)
            stopCapture()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun setReader(width: Int, height: Int) {
        reader?.setOnImageAvailableListener(null, null)
        reader?.close()
        val next = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        next.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val now = SystemClock.uptimeMillis()
                // Basic 10 fps throttle; drawing remains within this service.
                if (now - lastFrameAt < 100) return@setOnImageAvailableListener
                lastFrameAt = now
                val plane = image.planes[0]
                val pixelStride = plane.pixelStride
                if (pixelStride != 4) return@setOnImageAvailableListener
                val rowStride = plane.rowStride
                val paddedWidth = rowStride / pixelStride
                val raw = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
                val buffer: ByteBuffer = plane.buffer
                buffer.rewind()
                raw.copyPixelsFromBuffer(buffer)
                val cropped = if (paddedWidth > image.width) {
                    Bitmap.createBitmap(raw, 0, 0, image.width, image.height)
                        .also { raw.recycle() }
                } else raw
                MirrorFrames.submit(cropped)
            } catch (e: Exception) {
                Log.w("TMirror", "Frame skipped", e)
            } finally {
                image.close()
            }
        }, handler)
        reader = next
    }

    private fun resize(width: Int, height: Int) {
        val vd = virtualDisplay ?: return
        val maxEdge = 1280f
        val ratio = minOf(1f, maxEdge / maxOf(width, height))
        val w = (width * ratio).toInt().coerceAtLeast(2)
        val h = (height * ratio).toInt().coerceAtLeast(2)
        if (reader?.width == w && reader?.height == h) return
        // Android 14+: resize and replace the Surface on the SAME display.
        // Never call MediaProjection.createVirtualDisplay a second time.
        vd.setSurface(null)
        setReader(w, h)
        vd.resize(w, h, resources.displayMetrics.densityDpi)
        vd.setSurface(reader!!.surface)
    }

    private fun stopCapture() {
        if (stopping) return
        stopping = true
        running = false
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { reader?.setOnImageAvailableListener(null, null) } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        reader = null
        val active = projection
        projection = null
        try { active?.stop() } catch (_: Exception) {}
        MirrorFrames.clear()
        worker?.quitSafely()
        worker = null
        handler = null
        stopping = false
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
