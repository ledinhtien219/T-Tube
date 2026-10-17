package com.carhud.aaproxy

import org.junit.Assert.*
import org.junit.Test

class ProjectionMetricsTest {
    @Test fun preservesExplicitHostDensityAcrossScreenShapes() {
        for ((w, h) in listOf(800 to 480, 1280 to 720, 1920 to 480, 720 to 1280, 640 to 720)) {
            for (dpi in listOf(120, 160, 213, 240, 320, 480, 640)) {
                val resolved = ProjectionMetrics.resolve(w, h, dpi, "host_display")
                assertEquals(dpi, resolved.dpi)
                assertEquals("host_display", resolved.source)
            }
        }
    }

    @Test fun fallbackUsesBothAxesInsteadOfEnlargingUltrawideControls() {
        val cases = listOf(Triple(800, 480, 160), Triple(1280, 720, 240),
            Triple(1920, 720, 240), Triple(1920, 480, 160), Triple(2560, 720, 240),
            Triple(1920, 1080, 360), Triple(480, 272, 91), Triple(640, 480, 128))
        for ((w, h, expected) in cases) {
            assertEquals("${w}x$h", expected, ProjectionMetrics.resolve(w, h).dpi)
            assertEquals("rotation ${w}x$h", expected, ProjectionMetrics.resolve(h, w).dpi)
            assertEquals("estimated_surface", ProjectionMetrics.resolve(w, h).source)
        }
    }

    @Test fun invalidHostValuesFallBackWithoutUsingPhoneDensity() {
        for (dpi in listOf(-1, 0, 1, 71, 961, Int.MAX_VALUE)) {
            assertEquals(160, ProjectionMetrics.resolve(1920, 480, dpi).dpi)
        }
    }

    @Test fun configurationMustDescribeTheActualViewport() {
        assertTrue(ProjectionMetrics.matchesViewport(1280, 720, 853, 480, 240))
        assertFalse(ProjectionMetrics.matchesViewport(1280, 720, 411, 891, 420))
        assertFalse(ProjectionMetrics.matchesViewport(720, 1280, 853, 480, 240))
        assertFalse(ProjectionMetrics.matchesViewport(1280, 720, 0, 0, 240))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unmeasuredSurfaceIsNotReplacedWithAnInventedScreen() {
        ProjectionMetrics.resolve(0, 480)
    }
}
