package com.tcar.tmirror

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import java.util.concurrent.CopyOnWriteArraySet

/** Single-frame buffer. All bitmap ownership is protected by this lock. */
object MirrorFrames {
    private var bitmap: Bitmap? = null
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    fun addListener(listener: () -> Unit) { listeners.add(listener) }
    fun removeListener(listener: () -> Unit) { listeners.remove(listener) }

    fun submit(next: Bitmap) {
        synchronized(this) {
            val old = bitmap
            bitmap = next
            if (old !== next) old?.recycle()
        }
        listeners.forEach { listener -> runCatching { listener() } }
    }

    fun draw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        synchronized(this) {
            val frame = bitmap
            if (frame != null && !frame.isRecycled) {
                val scale = minOf(canvas.width.toFloat() / frame.width, canvas.height.toFloat() / frame.height)
                val destW = frame.width * scale
                val destH = frame.height * scale
                val left = (canvas.width - destW) / 2f
                val top = (canvas.height - destH) / 2f
                canvas.drawBitmap(
                    frame,
                    Rect(0, 0, frame.width, frame.height),
                    RectF(left, top, left + destW, top + destH),
                    paint
                )
            } else {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = maxOf(18f, canvas.width / 28f)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("T-Mirror", canvas.width / 2f, canvas.height * 0.43f, textPaint)
                textPaint.textSize *= 0.55f
                canvas.drawText("Mở ứng dụng trên điện thoại để bắt đầu", canvas.width / 2f,
                    canvas.height * 0.56f, textPaint)
            }
        }
    }

    /** Convert a touch in the letterboxed car pane to phone-normalized coordinates. */
    fun normalizedPoint(x: Float, y: Float, paneWidth: Int, paneHeight: Int): Pair<Float, Float>? =
        synchronized(this) {
            val b = bitmap ?: return@synchronized null
            if (paneWidth <= 0 || paneHeight <= 0) return@synchronized null
            val scale = minOf(paneWidth.toFloat() / b.width, paneHeight.toFloat() / b.height)
            val displayedW = b.width * scale
            val displayedH = b.height * scale
            val left = (paneWidth - displayedW) / 2f
            val top = (paneHeight - displayedH) / 2f
            val nx = (x - left) / displayedW
            val ny = (y - top) / displayedH
            if (nx !in 0f..1f || ny !in 0f..1f) null else nx to ny
        }

    fun clear() {
        synchronized(this) { bitmap?.recycle(); bitmap = null }
        listeners.forEach { listener -> runCatching { listener() } }
    }
}
