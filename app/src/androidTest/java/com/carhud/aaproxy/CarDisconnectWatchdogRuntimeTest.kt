package com.carhud.aaproxy

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class CarDisconnectWatchdogRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext.applicationContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun usbPowerLossDuringProjectionTriggersFullShutdownPath() {
        val shutdown = CountDownLatch(1)
        val reason = AtomicReference("")

        try {
            CarDisconnectWatchdog.resetForTests()

            // Force a deterministic "wired power present" state in the emulator.
            device.executeShellCommand("dumpsys battery unplug")
            device.executeShellCommand("dumpsys battery set ac 0")
            device.executeShellCommand("dumpsys battery set wireless 0")
            device.executeShellCommand("dumpsys battery set usb 1")

            assertTrue(
                "Emulator did not report USB power connected",
                waitUntil(4_000) {
                    CarDisconnectWatchdog.isExternalPowerConnectedForTests(context)
                }
            )

            CarDisconnectWatchdog.shutdownOverrideForTests = { _, why ->
                reason.set(why)
                shutdown.countDown()
            }

            CarDisconnectWatchdog.onProjectionServiceCreated(context)
            CarDisconnectWatchdog.onProjectionConnected(context)
            assertTrue(
                "Projection session was not latched",
                CarDisconnectWatchdog.hasSeenProjectionSession()
            )

            // Simulate the exact failure mode we care about: power disappears
            // without sending USB_DEVICE_DETACHED to the app.
            device.executeShellCommand("dumpsys battery set usb 0")

            assertTrue(
                "Watchdog did not detect cable power loss without USB detach broadcast",
                shutdown.await(6, TimeUnit.SECONDS)
            )
            assertTrue(
                "Unexpected disconnect reason: ${reason.get()}",
                reason.get().contains("power", ignoreCase = true)
            )
        } finally {
            CarDisconnectWatchdog.resetForTests()
            runCatching { device.executeShellCommand("dumpsys battery reset") }
        }
    }

    @Test
    fun serviceTeardownWhilePoweredThenLaterUnplugStillShutsDown() {
        val shutdown = CountDownLatch(1)
        val reason = AtomicReference("")

        try {
            CarDisconnectWatchdog.resetForTests()
            device.executeShellCommand("dumpsys battery unplug")
            device.executeShellCommand("dumpsys battery set ac 0")
            device.executeShellCommand("dumpsys battery set wireless 0")
            device.executeShellCommand("dumpsys battery set usb 1")

            assertTrue(
                "Emulator did not report USB power connected",
                waitUntil(4_000) {
                    CarDisconnectWatchdog.isExternalPowerConnectedForTests(context)
                }
            )

            CarDisconnectWatchdog.shutdownOverrideForTests = { _, why ->
                reason.set(why)
                shutdown.countDown()
            }

            CarDisconnectWatchdog.onProjectionServiceCreated(context)
            CarDisconnectWatchdog.onProjectionConnected(context)
            assertTrue(
                "Projection session was not latched before service teardown",
                CarDisconnectWatchdog.hasSeenProjectionSession()
            )

            // OEM/AA may tear down the projection service while USB power remains.
            CarDisconnectWatchdog.onProjectionServiceDestroyed(context)

            assertTrue(
                "Projection session was incorrectly forgotten after service teardown",
                CarDisconnectWatchdog.hasSeenProjectionSession()
            )
            assertTrue(
                "Service teardown while still powered incorrectly shut the app down",
                shutdown.count == 1L
            )

            // Physical cable unplug happens later, without USB detach broadcast.
            device.executeShellCommand("dumpsys battery set usb 0")

            assertTrue(
                "Watchdog missed unplug after projection service teardown",
                shutdown.await(6, TimeUnit.SECONDS)
            )
            assertTrue(
                "Unexpected disconnect reason after service teardown: " + reason.get(),
                reason.get().contains("power", ignoreCase = true)
            )
        } finally {
            CarDisconnectWatchdog.resetForTests()
            runCatching { device.executeShellCommand("dumpsys battery reset") }
        }
    }
    @Test
    fun projectionLifecycleEndWhileStillPoweredDoesNotShutdown() {
        val shutdown = CountDownLatch(1)

        try {
            CarDisconnectWatchdog.resetForTests()
            device.executeShellCommand("dumpsys battery unplug")
            device.executeShellCommand("dumpsys battery set ac 0")
            device.executeShellCommand("dumpsys battery set wireless 0")
            device.executeShellCommand("dumpsys battery set usb 1")

            assertTrue(
                "Emulator did not report USB power connected",
                waitUntil(4_000) {
                    CarDisconnectWatchdog.isExternalPowerConnectedForTests(context)
                }
            )

            CarDisconnectWatchdog.shutdownOverrideForTests = { _, _ ->
                shutdown.countDown()
            }
            CarDisconnectWatchdog.onProjectionServiceCreated(context)
            CarDisconnectWatchdog.onProjectionConnected(context)

            // Android Auto can stop/destroy the projection UI when switching to
            // Maps/Waze. As long as cable power is still present this must not be
            // mistaken for a physical disconnect.
            CarDisconnectWatchdog.onProjectionLifecycleEnded(
                context,
                "runtime-test normal app switch"
            )
            SystemClock.sleep(1_800)

            assertTrue(
                "Lifecycle-only stop incorrectly triggered full shutdown",
                shutdown.count == 1L
            )
        } finally {
            CarDisconnectWatchdog.resetForTests()
            runCatching { device.executeShellCommand("dumpsys battery reset") }
        }
    }

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (runCatching { condition() }.getOrDefault(false)) return true
            SystemClock.sleep(200)
        }
        return false
    }
}
