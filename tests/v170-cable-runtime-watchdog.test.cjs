const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const projection = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt','utf8');
const watchdog = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDisconnectWatchdog.kt','utf8');
const shutdown = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarAppShutdownManager.kt','utf8');
const media = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt','utf8');
const runtime = fs.readFileSync('app/src/androidTest/java/com/carhud/aaproxy/CarDisconnectWatchdogRuntimeTest.kt','utf8');
const workflow = fs.readFileSync('.github/workflows/build-apk.yml','utf8');

assert.match(gradle, /versionCode = 260/);

// Do not trust the mutable CarMediaManager flag as the only disconnect signal.
assert.match(projection, /CarDisconnectWatchdog\.onProjectionConnected\(this\)/);
assert.match(projection, /CarDisconnectWatchdog\.onProjectionLifecycleEnded/);
assert.match(projection, /CarDisconnectWatchdog\.onProjectionServiceDestroyed/);
assert.match(projection, /CarDisconnectWatchdog\.shouldHandleCableDetach\(\)/);
assert.doesNotMatch(projection, /val wasConnected = CarMediaManager\.isCarConnected/);

// Wired watchdog remembers a real projection session and independently checks power.
assert.match(watchdog, /projectionSessionSeen/);
assert.match(watchdog, /ACTION_BATTERY_CHANGED/);
assert.match(watchdog, /BatteryManager\.EXTRA_PLUGGED/);
assert.match(watchdog, /POWER_LOSS_CONFIRM_MS/);
assert.match(watchdog, /externalPowerSeen/);
assert.match(watchdog, /usbPowerSeen/);
assert.match(watchdog, /BatteryManager\.BATTERY_PLUGGED_USB/);
assert.match(watchdog, /CarAppShutdownManager\.shutdownCompletely/);

// Full exit pauses both persistent WebViews before flipping connection state.
assert.match(media, /fun pauseAllPlaybackForShutdown\(\)/);
assert.match(media, /listOfNotNull\(carWebView, phoneWebView\)\.distinct\(\)/);
assert.match(shutdown, /pauseAllPlaybackForShutdown\(\)[\s\S]*?setCarConnectionState\(false, resumePhoneOnDisconnect = false\)/);

// Android runtime test simulates USB power connected -> disconnected with no detach broadcast.
assert.match(runtime, /dumpsys battery set usb 1/);
assert.match(runtime, /dumpsys battery set usb 0/);
assert.match(runtime, /Watchdog did not detect cable power loss without USB detach broadcast/);
assert.match(runtime, /projectionLifecycleEndWhileStillPoweredDoesNotShutdown/);
assert.match(runtime, /serviceTeardownWhilePoweredThenLaterUnplugStillShutsDown/);
assert.match(runtime, /Projection session was incorrectly forgotten after service teardown/);
assert.match(runtime, /usbUnplugStillShutsDownWhileWirelessChargingRemains/);
assert.match(runtime, /USB unplug was masked by remaining wireless charging/);

// APK distribution is gated on both cable-loss runtime test and TikTok runtime test.
assert.match(workflow, /CarDisconnectWatchdogRuntimeTest/);
assert.match(workflow, /TikTokRuntimeSmokeTest/);
assert.match(workflow, /cable_status/);
assert.match(workflow, /tiktok_status/);
assert.match(workflow, /TTube-v1\.0\.69-build260-APK/);

console.log('PASS v1.0.69 build 260: wired cable loss is independently detected, hard-stopped, runtime-tested, and gates APK');
