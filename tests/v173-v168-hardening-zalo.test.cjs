const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const activation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/ActivationActivity.kt','utf8');
const license = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/LicenseManager.kt','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const watchdog = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDisconnectWatchdog.kt','utf8');
const runtime = fs.readFileSync('app/src/androidTest/java/com/carhud/aaproxy/CarDisconnectWatchdogRuntimeTest.kt','utf8');
const waze = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WazeHlpWebSocketManager.kt','utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt','utf8');
const licenseConfig = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/LicenseConfig.kt','utf8');
const workflow = fs.readFileSync('.github/workflows/build-apk.yml','utf8');

assert.ok(Number(gradle.match(/versionCode = (\d+)/)?.[1]) >= 261);
assert.match(gradle, /versionName = "1\.0\.\d+"/);

// No public/client-side developer unlock shortcut.
assert.doesNotMatch(activation, /219171|setDevBypass|handleDevBypassClick|showDevPasswordDialog/);
assert.doesNotMatch(license, /fun setDevBypass/);

// Background audio only holds the CPU wake lock while playback is active/desired.
const pauseStop = main.match(/override fun onPause\(\)[\s\S]*?override fun onDestroy\(\)/)?.[0] || '';
assert.match(pauseStop, /CarMediaManager\.isPlaying \|\| CarMediaManager\.userWantsPlayback/);
assert.match(pauseStop, /CarMediaManager\.releaseWakeLock\(\)/);

// Remaining wireless/AC charge must not hide a wired Android Auto USB loss.
assert.match(watchdog, /usbPowerSeen/);
assert.match(watchdog, /BatteryManager\.BATTERY_PLUGGED_USB/);
assert.match(runtime, /usbUnplugStillShutsDownWhileWirelessChargingRemains/);

// Waze HLP is local IPC only.
assert.match(waze, /InetSocketAddress\("127\.0\.0\.1", port\)/);
assert.doesNotMatch(waze, /InetSocketAddress\(port\)/);

// Group/community Zalo UI was intentionally removed; activation support keeps
// the original personal Zalo contact only.
assert.match(licenseConfig, /SUPPORT_ZALO_URL = "https:\/\/zalo\.me\/0366717255"/);
assert.doesNotMatch(settings, /title = "ZALO T TUBE"/);
assert.doesNotMatch(main, /btnHomeZalo|Nhóm Zalo/);

// Normal CI now runs the Kotlin/JVM unit suite before distributing the APK.
assert.match(workflow, /\.\/gradlew testDebugUnitTest --stacktrace/);
assert.match(workflow, /TTube-v1\\.0\\.(?:70|75)-build(?:261|270)-APK/);

console.log('PASS v1.0.75 build 270: no Zalo group UI + license/wakelock/cable/Waze/CI hardening');
