const fs = require('node:fs');
const assert = require('node:assert/strict');

const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const descriptor = fs.readFileSync('app/src/main/res/xml/automotive_app_desc.xml', 'utf8');
const manager = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHostModeManager.kt',
  'utf8'
);
const settings = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt',
  'utf8'
);
const service = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudAutoService.kt',
  'utf8'
);
const factory = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarUltrawideTemplateFactory.kt',
  'utf8'
);
const iptv = fs.readFileSync('app/src/main/assets/iptv_player.html', 'utf8');
const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);

assert.match(manifest, /CarHudProjectionService"[\s\S]*?android:enabled="true"/);
assert.match(manifest, /CarHudAutoService"[\s\S]*?android:enabled="false"/);
assert.match(descriptor, /name="template"/);
assert.match(descriptor, /name="projection"/);

assert.match(manager, /fun requestMode\(context: Context, preferUltrawide: Boolean\): State/);
assert.match(manager, /if \(!isAndroidAutoSessionActive\(\)\)/);
assert.match(manager, /CarDisconnectWatchdog\.hasSeenProjectionSession\(\)/);
assert.match(manager, /Enable the next host first, then disable the old one/);
assert.match(manager, /PackageManager\.DONT_KILL_APP/);
assert.match(manager, /fun applyPendingAfterDisconnect\(context: Context\)/);

assert.match(settings, /KEY_ULTRAWIDE_FULL_SURFACE = "ultrawide_full_surface"/);
assert.match(settings, /Ưu tiên toàn màn hình siêu rộng/);
assert.match(settings, /CarHostModeManager\.requestMode\(this@SettingsActivity, checked\)/);
assert.match(settings, /Chờ áp dụng lựa chọn mới/);

assert.match(factory, /MapTemplate\.Builder\(\)[\s\S]*?setPane\(pane\)[\s\S]*?setHeader\(header\)[\s\S]*?build\(\)/);
assert.match(factory, /Header\.Builder\(\)[\s\S]*?setTitle\("\\u200B"\)/);
const modernBlock = factory.match(/if \(carApiLevel >= 5\)[\s\S]*?return MapTemplate\.Builder\(\)[\s\S]*?\.build\(\)/)?.[0] || '';
assert.doesNotMatch(modernBlock, /setActionStrip/);

assert.match(service, /CarDisconnectWatchdog\.onProjectionServiceCreated/);
assert.match(service, /CarDisconnectWatchdog\.onProjectionServiceDestroyed/);
const destroyBlock = service.match(/override fun onDestroy\(\)[\s\S]*?override fun createHostValidator/)?.[0] || '';
assert.doesNotMatch(destroyBlock, /shutdownCompletely/);

assert.match(iptv, /function normalizeProjectedViewport\(\)/);
assert.match(iptv, /window\.ThtvIptvNormalizeViewport = normalizeProjectedViewport/);
assert.match(presentation, /ThtvIptvNormalizeViewport/);

console.log('PASS ultrawide toggle v2: reconnect-only state machine + valid action-free MapTemplate + IPTV reflow');
