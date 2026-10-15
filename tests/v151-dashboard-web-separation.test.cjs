const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const dash = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt', 'utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const media = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt', 'utf8');

const versionCode = Number(gradle.match(/versionCode = (\d+)/)?.[1] || 0);
const versionName = gradle.match(/versionName = "([^"]+)"/)?.[1] || '';
assert.ok(versionCode >= 238);
assert.ok(/^1\.0\./.test(versionName));

assert.match(dash, /PREF_SHOW_CARD_M3U, false/);
assert.match(dash, /PREF_SHOW_FAV_CHANNELS, false/);
assert.match(main, /PREF_SHOW_CARD_M3U, false/);
assert.match(main, /PREF_SHOW_FAV_CHANNELS, false/);

assert.match(media, /private fun isUrlCompatibleWithApp/);
assert.match(media, /"youtube" -> value\.contains\("youtube\.com"/);
assert.match(media, /"web" -> !\(value\.contains\("youtube\.com"/);
assert.match(media, /if \(!isUrlCompatibleWithApp\(appId, session\.url\)\)/);

assert.match(presentation, /private fun ensureCorrectFullscreenAppPage\(\)/);
assert.match(presentation, /currentActiveAppId == "tiktok"/);
assert.match(presentation, /Gravity\.START or Gravity\.CENTER_VERTICAL/);
assert.match(presentation, /pendingSessionRestoreAppId/);

assert.match(tiktok, /function isAppOnlyWall/);
assert.match(tiktok, /data-ttube-app-promo-wall/);
assert.doesNotMatch(tiktok, /data-ttube-login-wall/);

console.log('PASS v1.0.51: dashboard defaults + TikTok web-only mode + YouTube/web session separation');
