package com.carhud.aaproxy

/**
 * Strictly observes a real PROJECTION (2) -> NOT_CONNECTED (0) transition.
 * Initial disconnected/unknown values never close a standalone phone app.
 * USB and wireless share the same Android Auto connection type.
 */
internal class ProjectionDisconnectGate(private val confirmMs: Long = 6_000L) {
    private var hasBeenProjected = false
    private var disconnectStartedAt = -1L
    private var connectionType = -1

    fun onConnectionType(type: Int, uptimeMs: Long) {
        connectionType = type
        when (type) {
            2 -> {
                hasBeenProjected = true
                disconnectStartedAt = -1L
            }
            0 -> if (hasBeenProjected && disconnectStartedAt < 0L) {
                disconnectStartedAt = uptimeMs
            }
            else -> disconnectStartedAt = -1L
        }
    }

    fun isProjectionActive(): Boolean = hasBeenProjected && connectionType == 2

    fun isPending() =
        hasBeenProjected && connectionType == 0 && disconnectStartedAt >= 0L

    fun shouldExit(uptimeMs: Long) =
        isPending() && uptimeMs - disconnectStartedAt >= confirmMs

    fun cancelPending() {
        disconnectStartedAt = -1L
    }

    fun reset() {
        hasBeenProjected = false
        disconnectStartedAt = -1L
        connectionType = -1
    }
}
