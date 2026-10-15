const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt', 'utf8');

const versionCode = Number(gradle.match(/versionCode = (\d+)/)?.[1] || 0);
const versionName = gradle.match(/versionName = "([^"]+)"/)?.[1] || '';
assert.ok(versionCode >= 246);
assert.ok(/^1\.0\./.test(versionName));

// Native web mode only suppresses prompts that force the native app.
assert.match(tiktok, /function isAppOnlyWall/);
assert.match(tiktok, /continue with tiktok app/);
assert.match(tiktok, /open tiktok lite/);
assert.match(tiktok, /data-ttube-app-promo-wall/);
assert.match(tiktok, /unlockPage/);
assert.doesNotMatch(tiktok, /guestQueue/);
assert.doesNotMatch(tiktok, /setInterval\s*\(/);

// TikTok controls use the compact vertical rail requested at the far-left edge.
assert.match(presentation, /tiktokIconSizeDp = \(\(58 \* scale\) \/ 100\)\.coerceIn\(52, 72\)/);
assert.match(presentation, /orientation = LinearLayout\.VERTICAL/);
assert.match(presentation, /leftMargin = dp\(3\)/);
assert.match(presentation, /Gravity\.START or Gravity\.CENTER_VERTICAL/);

// Simplified phone settings.
assert.doesNotMatch(settings, /title = "MIC HỆ THỐNG ANDROID AUTO"/);
assert.doesNotMatch(settings, /Nút Giao diện Sáng \/ Tối/);
assert.doesNotMatch(settings, /Nút Bàn phím Web/);

console.log('PASS TikTok native web + toolbar #2 + simplified settings');
