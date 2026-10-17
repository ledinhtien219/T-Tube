package com.carhud.aaproxy

import kotlin.math.roundToInt

/** Resolves logical rendering DPI from the host, with a labelled geometry fallback.
 * Physical panel PPI cannot be inferred from a projection resolution alone. */
internal object ProjectionMetrics {
    data class Density(val dpi: Int, val source: String)

    fun resolve(widthPx: Int, heightPx: Int, hostDpi: Int = 0, source: String = "host"): Density {
        require(widthPx > 0 && heightPx > 0) { "Projection surface must have a measured size" }
        // Logical host density is authoritative, even when equal to the phone DPI.
        if (hostDpi in 72..960) return Density(hostDpi, source)
        // Fit both axes: extra ultrawide pixels must not enlarge controls vertically.
        val fit = minOf(minOf(widthPx, heightPx) / 480f, maxOf(widthPx, heightPx) / 800f)
        return Density((160f * fit).roundToInt().coerceIn(80, 640), "estimated_surface")
    }

    // Only use this entry point with density supplied by the host SurfaceContainer.
    @Suppress("UNUSED_PARAMETER")
    fun resolveDpi(widthPx: Int, heightPx: Int, reportedDpi: Int, phoneDpi: Int): Int =
        resolve(widthPx, heightPx, reportedDpi).dpi

    fun matchesViewport(widthPx: Int, heightPx: Int, widthDp: Int, heightDp: Int, dpi: Int): Boolean {
        if (widthDp <= 0 || heightDp <= 0 || dpi !in 72..960) return false
        val density = dpi / 160f
        fun close(actual: Int, expected: Float) = kotlin.math.abs(actual - expected) <= maxOf(32f, actual * .08f)
        return close(widthPx, widthDp * density) && close(heightPx, heightDp * density)
    }
}
