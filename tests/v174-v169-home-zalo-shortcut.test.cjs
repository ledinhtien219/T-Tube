const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const layout = fs.readFileSync('app/src/main/res/layout/activity_main.xml','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt','utf8');
const licenseConfig = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/LicenseConfig.kt','utf8');

assert.match(gradle, /versionCode = (?:261|262)/);
assert.match(gradle, /versionName = "1\.0\.70"/);

// The temporary v1.0.69 Zalo-group shortcut has been fully removed.
assert.doesNotMatch(layout, /btnHomeZalo|Nhóm Zalo/);
assert.doesNotMatch(main, /btnHomeZalo|Không mở được nhóm Zalo/);
assert.doesNotMatch(settings, /title = "ZALO T TUBE"|Nhóm Zalo/);

// Activation/support keeps only the original personal Zalo contact.
assert.match(licenseConfig, /SUPPORT_ZALO_URL = "https:\/\/zalo\.me\/0366717255"/);
assert.doesNotMatch(licenseConfig, /zalo\.me\/g\//);

console.log('PASS v1.0.70 build 261: Zalo group UI removed; personal activation support retained');
