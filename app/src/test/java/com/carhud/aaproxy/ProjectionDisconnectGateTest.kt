package com.carhud.aaproxy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionDisconnectGateTest {
    @Test fun disconnectedInitiallyDoesNotExit() {
        val gate = ProjectionDisconnectGate()
        gate.onConnectionType(0, 100L)
        assertFalse(gate.shouldExit(100_000L))
    }

    @Test fun projectedThenDisconnectedExitsAfterSixSeconds() {
        val gate = ProjectionDisconnectGate()
        gate.onConnectionType(2, 1_000L)
        gate.onConnectionType(0, 4_000L)
        assertFalse(gate.shouldExit(9_999L))
        assertTrue(gate.shouldExit(10_000L))
    }

    @Test fun reconnectBeforeGraceCancelsExit() {
        val gate = ProjectionDisconnectGate()
        gate.onConnectionType(2, 1_000L)
        gate.onConnectionType(0, 3_000L)
        gate.onConnectionType(2, 5_000L)
        assertFalse(gate.shouldExit(100_000L))
    }

    @Test fun appSwitchWithoutDisconnectIsSafe() {
        val gate = ProjectionDisconnectGate()
        gate.onConnectionType(2, 0L)
        gate.onConnectionType(2, 20_000L)
        assertFalse(gate.shouldExit(100_000L))
    }

    @Test fun resetAndNativeDoNotExit() {
        val gate = ProjectionDisconnectGate()
        gate.onConnectionType(2, 0L)
        gate.onConnectionType(0, 3_000L)
        gate.reset()
        gate.onConnectionType(1, 4_000L)
        gate.onConnectionType(0, 5_000L)
        assertFalse(gate.shouldExit(100_000L))
    }
}
