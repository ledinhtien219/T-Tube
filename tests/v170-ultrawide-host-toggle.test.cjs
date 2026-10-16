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
const screen = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt',
  'utf8'
);

assert.match(manifest, /CarHudProjectionService"[\s\S]*?android:enabled="true"/);
assert.match(manifest, /CarHudAutoService"[\s\S]*?android:enabled="false"/);
assert.match(descriptor, /name="template"/);
assert.match(descriptor, /name="projection"/);

assert.match(settings, /KEY_ULTRAWIDE_FULL_SURFACE = "ultrawide_full_surface"/);
assert.match(settings, /Ưu tiên toàn màn hình siêu rộng/);
assert.match(settings, /CarHostModeManager\.applyMode\(this@SettingsActivity, enabled\)/);
assert.match(settings, /ngắt và kết nối lại Android Auto/);
assert.match(settings, /CarHostModeManager\.syncFromPreferences\(this\)/);

assert.match(manager, /CarHudAutoService::class\.java, preferUltrawide/);
assert.match(manager, /CarHudProjectionService::class\.java, !preferUltrawide/);
assert.match(manager, /PackageManager\.DONT_KILL_APP/);

assert.match(screen, /surfaceContainer\.width/);
assert.match(screen, /surfaceContainer\.height/);
assert.match(screen, /carContext\.carAppApiLevel >= 5/);
assert.match(screen, /MapTemplate\.Builder\(\)/);

console.log('PASS AA host toggle: stable default + user-selectable ultrawide SurfaceContainer mode');
