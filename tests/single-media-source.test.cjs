const fs = require('node:fs');
const assert = require('node:assert/strict');

const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const descriptor = fs.readFileSync(
  'app/src/main/res/xml/automotive_app_desc.xml',
  'utf8'
);
const receiver = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/MediaSourceUpgradeReceiver.kt',
  'utf8'
);

// The packaged app must expose exactly one Android Auto media browser source.
const serviceBlocks = [...manifest.matchAll(/<service\b[\s\S]*?<\/service>/g)]
  .map((match) => match[0])
  .filter((block) => block.includes('android.media.browse.MediaBrowserService'));

assert.equal(serviceBlocks.length, 1);
assert.match(
  serviceBlocks[0],
  /android:name="com\.carhud\.aaproxy\.CarMediaBrowserService"/
);
assert.match(serviceBlocks[0], /android:label="T Tube Media"/);

const mediaUses = [...descriptor.matchAll(/<uses\s+name="media"\s*\/>/g)];
assert.equal(mediaUses.length, 1);

// On APK replacement, refresh only the media component so Android Auto can
// invalidate a stale launcher row left by older installs.
assert.match(manifest, /android:name="com\.carhud\.aaproxy\.MediaSourceUpgradeReceiver"/);
assert.match(
  manifest,
  /MediaSourceUpgradeReceiver"[\s\S]*?android\.intent\.action\.MY_PACKAGE_REPLACED/
);
assert.match(receiver, /CarMediaBrowserService::class\.java/);
assert.match(receiver, /COMPONENT_ENABLED_STATE_DISABLED/);
assert.match(receiver, /COMPONENT_ENABLED_STATE_DEFAULT/);
assert.match(receiver, /PackageManager\.DONT_KILL_APP/);
assert.match(receiver, /postDelayed/);
assert.doesNotMatch(receiver, /stopService\(/);

// The main T Tube projection entry remains untouched and available.
assert.match(
  manifest,
  /android:name="com\.carhud\.aaproxy\.CarHudProjectionService"/
);
assert.match(manifest, /CATEGORY_PROJECTION/);

console.log('PASS single AA media source: one service + upgrade cache refresh');
