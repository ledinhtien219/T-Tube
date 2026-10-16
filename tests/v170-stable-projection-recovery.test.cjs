const fs = require('node:fs');
const assert = require('node:assert/strict');

const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const descriptor = fs.readFileSync('app/src/main/res/xml/automotive_app_desc.xml', 'utf8');
const main = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/MainActivity.kt',
  'utf8'
);
const recovery = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/StableProjectionHost.kt',
  'utf8'
);

assert.match(manifest, /android:name="com\.carhud\.aaproxy\.CarHudProjectionService"/);
assert.doesNotMatch(manifest, /android:name="com\.carhud\.aaproxy\.CarHudAutoService"/);
assert.match(descriptor, /name="projection"/);
assert.doesNotMatch(descriptor, /name="template"/);

assert.match(manifest, /StableProjectionUpgradeReceiver[\s\S]*MY_PACKAGE_REPLACED/);
assert.match(recovery, /COMPONENT_ENABLED_STATE_DEFAULT/);
assert.match(recovery, /CarHudProjectionService::class\.java/);
assert.match(recovery, /PackageManager\.DONT_KILL_APP/);
assert.match(main, /StableProjectionHost\.ensureEnabled\(this\)/);

console.log('PASS stable AA host: projection-only package + upgrade recovery');
