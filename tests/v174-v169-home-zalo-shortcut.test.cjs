const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const layout = fs.readFileSync('app/src/main/res/layout/activity_main.xml','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const licenseConfig = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/LicenseConfig.kt','utf8');

assert.match(gradle, /versionCode = 260/);
assert.match(gradle, /versionName = "1\.0\.69"/);

// Home header has a compact community shortcut exactly beside the T Tube wordmark.
assert.match(layout, /android:id="@\+id\/btnHomeZalo"/);
assert.match(layout, /android:text="Z"/);
assert.match(layout, /android:text="Nhóm Zalo"/);
assert.match(layout, /android:layout_height="34dp"/);

// One tap opens the canonical Zalo group link shared across activation/settings/home.
assert.match(licenseConfig, /SUPPORT_ZALO_URL = "https:\/\/zalo\.me\/g\/v8zlxq1055wjovfvgcsn"/);
assert.match(main, /R\.id\.btnHomeZalo[\s\S]*?LicenseConfig\.SUPPORT_ZALO_URL/);

// The shortcut belongs to Home only and does not occupy YouTube/IPTV/Settings headers.
assert.match(
  main,
  /R\.id\.btnHomeZalo\)\?\.visibility =[\s\S]*?if \(tab == TAB_HOME\) View\.VISIBLE else View\.GONE/
);

console.log('PASS v1.0.69 build 260: home header Zalo icon+label shortcut');
