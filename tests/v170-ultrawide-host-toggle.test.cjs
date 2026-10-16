const fs = require('node:fs');
const assert = require('node:assert/strict');

const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const descriptor = fs.readFileSync('app/src/main/res/xml/automotive_app_desc.xml', 'utf8');
const settings = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt',
  'utf8'
);
const manager = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHostModeManager.kt',
  'utf8'
);
const service = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudAutoService.kt',
  'utf8'
);
const screen = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt',
  'utf8'
);
const shutdown = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarAppShutdownManager.kt',
  'utf8'
);

assert.match(manifest, /CarHudProjectionService"[\s\S]*?android:enabled="true"/);
assert.match(manifest, /CarHudAutoService"[\s\S]*?android:enabled="false"/);
assert.match(descriptor, /name="template"/);
assert.match(descriptor, /name="projection"/);

assert.match(settings, /KEY_ULTRAWIDE_FULL_SURFACE = "ultrawide_full_surface"/);
assert.match(settings, /Ưu tiên toàn màn hình siêu rộng/);
assert.match(settings, /CarHostModeManager\.requestMode\(this@SettingsActivity, enabled\)/);
assert.doesNotMatch(settings, /CarHostModeManager\.applyMode\(/);

assert.match(manager, /fun requestMode\(context: Context, preferUltrawide: Boolean\): Boolean/);
assert.match(manager, /if \(isAndroidAutoSessionActive\(\)\)/);
assert.match(manager, /CarDisconnectWatchdog\.hasSeenProjectionSession\(\)/);
assert.match(manager, /fun applyPendingAfterDisconnect\(context: Context\)/);
assert.match(manager, /CarHudAutoService::class\.java, preferUltrawide/);
assert.match(manager, /CarHudProjectionService::class\.java, !preferUltrawide/);
assert.match(manager, /PackageManager\.DONT_KILL_APP/);

assert.match(service, /CarDisconnectWatchdog\.onProjectionServiceCreated/);
assert.match(service, /CarDisconnectWatchdog\.onProjectionServiceDestroyed/);
assert.match(service, /CarDisconnectWatchdog\.onCableDetachBroadcast/);
const destroyBlock = service.match(/override fun onDestroy\(\)[\s\S]*?override fun createHostValidator/)?.[0] || '';
assert.doesNotMatch(destroyBlock, /shutdownCompletely/);

assert.match(screen, /surfaceContainer\.width/);
assert.match(screen, /surfaceContainer\.height/);
assert.match(screen, /CarDisconnectWatchdog\.onProjectionConnected\(carContext\)/);
assert.match(screen, /carContext\.carAppApiLevel >= 5/);
assert.match(screen, /MapTemplate\.Builder\(\)[\s\S]*?Pane\.Builder\(\)\.build\(\)/);
assert.match(screen, /NavigationTemplate\.Builder\(\)/);
assert.match(screen, /setTitle\("\\u200B"\)/);

assert.match(shutdown, /CarHostModeManager\.applyPendingAfterDisconnect\(context\.applicationContext\)/);

console.log('PASS AA host toggle: deferred switching + action-free modern ultrawide mode');
