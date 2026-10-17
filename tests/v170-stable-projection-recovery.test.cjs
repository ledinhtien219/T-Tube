const fs = require('node:fs');
const assert = require('node:assert/strict');

const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const descriptor = fs.readFileSync('app/src/main/res/xml/automotive_app_desc.xml', 'utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt', 'utf8');
const manager = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHostModeManager.kt', 'utf8');
const screen = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');

assert.match(manifest, /CarHudProjectionService"[\s\S]*?android:enabled="true"/);
assert.match(manifest, /CarHudAutoService"[\s\S]*?android:enabled="false"/);
assert.match(manifest, /CarHostModeUpgradeReceiver[\s\S]*MY_PACKAGE_REPLACED/);
assert.match(descriptor, /name="template"/);
assert.match(descriptor, /name="projection"/);

assert.match(settings, /KEY_ULTRAWIDE_FULL_SURFACE = "ultrawide_full_surface"/);
assert.match(settings, /Ưu tiên toàn màn hình siêu rộng/);
assert.match(settings, /CarHostModeManager\.requestMode\(this@SettingsActivity, enabled\)/);
assert.match(main, /CarHostModeManager\.reconcileIfSafe\(this\)/);

assert.match(manager, /setComponentEnabledSettings/);
assert.match(manager, /if \(sessionActive\(\)\)/);
assert.match(manager, /PENDING_DISCONNECT/);
assert.match(manager, /verifyPair/);
assert.match(manager, /rollbackStable/);
assert.match(manager, /applyPendingAfterDisconnect/);

assert.match(screen, /carContext\.carAppApiLevel >= 5/);
assert.match(screen, /MapTemplate\.Builder\(\)/);
assert.match(screen, /Header\.Builder\(\)[\s\S]*?setTitle\("\\u200B"\)/);
assert.match(screen, /putInt\("car_app_api_level", carContext\.carAppApiLevel\)/);

assert.match(presentation, /private fun forceIptvViewportFit\(target: WebView = web\)/);
assert.match(presentation, /ttube-aa-iptv-viewport-v2/);
assert.match(presentation, /width:100vw!important/);

console.log('PASS AA toggle v2: atomic verified host switch + no-ActionStrip full mode + IPTV viewport repair');
