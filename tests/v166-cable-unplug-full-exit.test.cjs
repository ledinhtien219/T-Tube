const fs = require('node:fs');
const assert = require('node:assert/strict');

const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml','utf8');
const projection = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt','utf8');
const autoService = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudAutoService.kt','utf8');
const shutdown = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarAppShutdownManager.kt','utf8');
const media = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');

// Both selectable hosts preserve the same confirmed cable-detach shutdown path.
assert.match(manifest, /android:name="com\.carhud\.aaproxy\.CarHudProjectionService"/);
assert.match(manifest, /android:name="com\.carhud\.aaproxy\.CarHudAutoService"/);
assert.match(projection, /Intent\.ACTION_POWER_DISCONNECTED/);
assert.match(projection, /UsbManager\.ACTION_USB_DEVICE_DETACHED/);
assert.match(projection, /UsbManager\.ACTION_USB_ACCESSORY_DETACHED/);
assert.match(projection, /CarDisconnectWatchdog\.onCableDetachBroadcast/);
assert.match(projection, /CarDisconnectWatchdog\.onProjectionServiceDestroyed/);
assert.doesNotMatch(projection, /if \(wasConnected\)/);
assert.match(autoService, /UsbManager\.ACTION_USB_ACCESSORY_DETACHED/);
assert.match(autoService, /CarDisconnectWatchdog\.onCableDetachBroadcast/);
assert.match(autoService, /CarDisconnectWatchdog\.onProjectionServiceDestroyed/);
const autoDestroy = autoService.match(/override fun onDestroy\(\)[\s\S]*?override fun createHostValidator/)?.[0] || '';
assert.doesNotMatch(autoDestroy, /shutdownCompletely/);

// Surface recreation is not itself treated as a disconnect; cable/service lifecycle
// owns the full exit so resize/config changes do not terminate the app.
assert.match(projection, /Surface loss can be a resize\/recreation[\s\S]*?service-level USB\/power receiver owns full shutdown/);

// Full shutdown disables playback, does not wake the phone WebView, and stops services.
assert.match(shutdown, /CarMediaManager\.userWantsPlayback = false/);
assert.match(shutdown, /pauseAllPlaybackForShutdown\(\)/);
assert.match(shutdown, /setCarConnectionState\(false, resumePhoneOnDisconnect = false\)/);
assert.match(shutdown, /stopService\(Intent\(context, CarHudProjectionService::class\.java\)\)/);
assert.match(shutdown, /stopService\(Intent\(context, CarHudAutoService::class\.java\)\)/);
assert.match(shutdown, /CarHostModeManager\.applyPendingAfterDisconnect\(context\.applicationContext\)/);
assert.match(shutdown, /CarMediaBrowserService\.forceShutdownForDisconnect\(context\)/);
assert.match(media, /resumePhoneOnDisconnect: Boolean = true/);
assert.match(media, /if \(!resumePhoneOnDisconnect\)[\s\S]*?phone\?\.pauseTimers\(\)/);

// The phone task is removed immediately on the exit broadcast.
assert.match(main, /CarAppShutdownManager\.ACTION_FULL_EXIT/);
assert.match(main, /finishAndRemoveTask\(\)/);
assert.match(main, /unregisterReceiver\(it\)/);

console.log('PASS wired Android Auto disconnect fully exits phone app and background services');
