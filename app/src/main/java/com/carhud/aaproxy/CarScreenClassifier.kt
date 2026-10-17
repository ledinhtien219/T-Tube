package com.carhud.aaproxy

/**
 * Classifies the ACTUAL Android Auto content viewport.
 *
 * Do not use the phone/VirtualDisplay density for form-factor detection:
 * projection mode can inherit the phone DPI, causing very different car
 * surfaces (for example 770x460 and 1196x460) to collapse into the same
 * "compact" bucket.
 *
 * Width is normalized to a canonical 480px viewport height, making the
 * classifier stable across equivalent 800x480 / 1280x720 / 1920x1080 style
 * render targets while preserving aspect-ratio differences.
 */
object CarScreenClassifier {
    data class Profile(
        val widthPx: Int,
        val heightPx: Int,
        val aspectRatio: Float,
        val normalizedWidth: Float,
        val portrait: Boolean,
        val ultrawide: Boolean,
        val screenClass: String,
        val veryCompactWidth: Boolean,
        val compactWidth: Boolean,
        val compactHeight: Boolean
    )

    fun classify(widthPx: Int, heightPx: Int): Profile {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val aspect = w.toFloat() / h.toFloat()
        val normalizedWidth = w.toFloat() * 480f / h.toFloat()
        val portrait = aspect < 0.95f
        val ultrawide = aspect >= 1.85f

        val screenClass = when {
            portrait -> "PORTRAIT"
            aspect >= 2.40f -> "SUPER_ULTRAWIDE"
            ultrawide -> "ULTRAWIDE"
            aspect >= 1.55f -> "WIDE"
            else -> "COMPACT"
        }

        return Profile(
            widthPx = w,
            heightPx = h,
            aspectRatio = aspect,
            normalizedWidth = normalizedWidth,
            portrait = portrait,
            ultrawide = ultrawide,
            screenClass = screenClass,
            veryCompactWidth = normalizedWidth < 700f,
            compactWidth = normalizedWidth < 820f,
            compactHeight = h < 430
        )
    }
}
