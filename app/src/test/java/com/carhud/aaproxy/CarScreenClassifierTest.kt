package com.carhud.aaproxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarScreenClassifierTest {
    @Test
    fun split770x460StaysCompactButNotVeryCompact() {
        val p = CarScreenClassifier.classify(770, 460)
        assertEquals("WIDE", p.screenClass)
        assertTrue(p.compactWidth)
        assertFalse(p.veryCompactWidth)
        assertFalse(p.ultrawide)
        assertFalse(p.portrait)
    }

    @Test
    fun long1196x460IsSuperUltrawideAndSpacious() {
        val p = CarScreenClassifier.classify(1196, 460)
        assertEquals("SUPER_ULTRAWIDE", p.screenClass)
        assertTrue(p.ultrawide)
        assertFalse(p.compactWidth)
        assertFalse(p.veryCompactWidth)
    }

    @Test
    fun hd1280x720UsesSameAspectDrivenRules() {
        val p = CarScreenClassifier.classify(1280, 720)
        assertEquals("WIDE", p.screenClass)
        assertFalse(p.ultrawide)
        assertFalse(p.portrait)
    }

    @Test
    fun portrait480x800IsPortrait() {
        val p = CarScreenClassifier.classify(480, 800)
        assertEquals("PORTRAIT", p.screenClass)
        assertTrue(p.portrait)
        assertFalse(p.ultrawide)
    }
}
