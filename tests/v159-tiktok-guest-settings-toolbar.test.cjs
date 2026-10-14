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

// Guest fallback is fed only by public TikTok video anchors already present in the page.
assert.match(tiktok, /function collectPublicVideos/);
assert.match(tiktok, /ttube_tiktok_guest_queue/);
assert.match(tiktok, /function guestNavigate/);
assert.match(tiktok, /state\.guestFallback = true/);
assert.match(tiktok, /bottom-login-entry/);
assert.match(tiktok, /browse-login/);
assert.match(tiktok, /unlockDocumentAfterModal/);
assert.doesNotMatch(tiktok, /setInterval\s*\(/);
assert.doesNotMatch(tiktok, /\[id\*="login"\]/);

// TikTok controls are bigger and moved into the requested #2 slot.
assert.match(presentation, /tiktokIconSizeDp = \(\((?:62|64) \* scale\) \/ 100\)\.coerceIn\((?:58|60), (?:78|82)\)/);
assert.match(presentation, /leftMargin = dp\((?:54|82)\)/);
assert.match(presentation, /topMargin = dp\((?:22|48)\)/);

// Simplified phone settings.
assert.doesNotMatch(settings, /title = "MIC HỆ THỐNG ANDROID AUTO"/);
assert.doesNotMatch(settings, /Nút Giao diện Sáng \/ Tối/);
assert.doesNotMatch(settings, /Nút Bàn phím Web/);

console.log('PASS TikTok guest fallback + toolbar #2 + simplified settings');
