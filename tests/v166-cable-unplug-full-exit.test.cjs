const fs = require('node:fs');
const assert = require('node:assert/strict');

const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml','utf8');
const projection = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt','utf8');
const shutdown = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarAppShutdownManager.kt','utf8');
const media = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');

// The projection service that is actually declared in the manifest owns cable detach handling.
assert.match(manifest, /android:name="com\.carhud\.aaproxy\.CarHudProjectionService"/);
assert.match(projection, /Intent\.ACTION_POWER_DISCONNECTED/);
assert.match(projection, /UsbManager\.ACTION_USB_DEVICE_DETACHED/);
assert.match(projection, /UsbManager\.ACTION_USB_ACCESSORY_DETACHED/);
assert.match(projection, /CarAppShutdownManager\.shutdownCompletely/);
assert.match(projection, /if \(wasConnected\)[\s\S]*?Android Auto projection service destroyed/);

// A real finishing surface must not deliberately restart background playback.
assert.match(projection, /if \(isFinishing\)[\s\S]*?web\?\.onPause\(\)[\s\S]*?CarMediaManager\.releaseWakeLock\(\)/);

// Full shutdown disables playback, does not wake the phone WebView, and stops services.
assert.match(shutdown, /CarMediaManager\.userWantsPlayback = false/);
assert.match(shutdown, /setCarConnectionState\(false, resumePhoneOnDisconnect = false\)/);
assert.match(shutdown, /stopService\(Intent\(context, CarHudProjectionService::class\.java\)\)/);
assert.match(shutdown, /stopService\(mediaIntent\)/);
assert.match(media, /resumePhoneOnDisconnect: Boolean = true/);
assert.match(media, /if \(!resumePhoneOnDisconnect\)[\s\S]*?phone\?\.pauseTimers\(\)/);

// The phone task is removed immediately on the exit broadcast.
assert.match(main, /CarAppShutdownManager\.ACTION_FULL_EXIT/);
assert.match(main, /finishAndRemoveTask\(\)/);
assert.match(main, /unregisterReceiver\(it\)/);

console.log('PASS wired Android Auto disconnect fully exits phone app and background services');
