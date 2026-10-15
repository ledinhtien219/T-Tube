const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const watchdog = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDisconnectWatchdog.kt','utf8');
const mediaService = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarMediaBrowserService.kt','utf8');
const shutdown = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarAppShutdownManager.kt','utf8');
const runtime = fs.readFileSync('app/src/androidTest/java/com/carhud/aaproxy/CarDisconnectWatchdogRuntimeTest.kt','utf8');

assert.match(gradle, /versionCode = 259/);
assert.match(gradle, /versionName = "1\.0\.68"/);

// A real projection session must stay armed even if AA tears down its service
// while USB power is still present.
assert.match(watchdog, /Projection service destroyed while cable is still powered; keeping session armed/);
assert.doesNotMatch(
  watchdog.match(/fun onProjectionServiceDestroyed\([\s\S]*?\n    \}/)?.[0] || '',
  /resetSession\(\)/
);
assert.match(runtime, /serviceTeardownWhilePoweredThenLaterUnplugStillShutsDown/);
assert.match(runtime, /onProjectionServiceDestroyed\(context\)[\s\S]*?dumpsys battery set usb 0/);

// Full shutdown must synchronously release SystemUI media controls before kill.
assert.match(mediaService, /fun forceShutdownForDisconnect\(context: Context\)/);
assert.match(mediaService, /PlaybackStateCompat\.STATE_STOPPED/);
assert.match(mediaService, /mediaSession\?\.isActive = false/);
assert.match(mediaService, /mediaSession\?\.release\(\)/);
assert.match(mediaService, /STOP_FOREGROUND_REMOVE/);
assert.match(mediaService, /nm\.cancel\(NOTIFICATION_ID\)/);
assert.match(shutdown, /CarMediaBrowserService\.forceShutdownForDisconnect\(context\)/);

console.log('PASS v1.0.68 build 259: sticky cable watchdog + synchronous media-session teardown');
