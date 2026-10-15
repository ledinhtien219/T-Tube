const fs = require('node:fs');
const assert = require('node:assert/strict');
const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const media = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt', 'utf8');
const theme = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');

const versionCode = Number(gradle.match(/versionCode = (\d+)/)?.[1] || 0);
const versionName = gradle.match(/versionName = "([^"]+)"/)?.[1] || '';
assert.ok(versionCode >= 244);
assert.ok(/^1\.0\./.test(versionName));

// TikTok uses the real desktop website with a light-touch WebView policy.
assert.match(tiktok, /loadWithOverviewMode = false/);
assert.match(tiktok, /RENDERER_PRIORITY_IMPORTANT/);
assert.match(tiktok, /DESKTOP_UA/);
assert.match(tiktok, /View\.LAYER_TYPE_HARDWARE/);

// No high-frequency polling or feedback loops.
assert.doesNotMatch(tiktok, /setInterval\s*\(/);
assert.match(tiktok, /MutationObserver/);
assert.match(tiktok, /childList: true/);
assert.match(tiktok, /subtree: true/);
assert.match(tiktok, /requestAnimationFrame/);

// Do not rewrite feed/video layout or synthesize guest navigation.
assert.doesNotMatch(tiktok, /ttube-tiktok-video-only/);
assert.doesNotMatch(tiktok, /guestQueue/);
assert.doesNotMatch(tiktok, /collectPublicVideos/);

// Preserve cookies, but do not restore stale TikTok WebView state.
assert.match(media, /"tiktok" -> value\.contains\("tiktok\.com", true\)/);
assert.match(media, /setOf\("web", "youtube", "tiktok"(?:, "iptv")?\)/);
assert.match(presentation, /app\.id != "tiktok"/);

// Generic universal theme inversion observer must stand down on TikTok.
assert.match(theme, /var tiktok = host === 'tiktok\.com'/);
assert.match(theme, /if \(youtube \|\| tiktok \|\| iptv\)/);
assert.match(theme, /window\.__carhudThemeObserver\.disconnect\(\)/);

console.log('PASS TikTok desktop web policy + cookie retention + clean state');
