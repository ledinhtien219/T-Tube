const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const projection = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt','utf8');
const shutdown = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarAppShutdownManager.kt','utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt','utf8');

assert.match(gradle, /versionCode = 261/);
assert.match(gradle, /versionName = "1\.0\.70"/);

// Phone defaults to dark only when the user has never chosen a mode.
assert.match(main, /if \(!prefs\.contains\(PREF_THEME_MODE\)\)[\s\S]*?SettingsActivity\.THEME_NIGHT/);
assert.match(main, /initIptvWebView[\s\S]*?YouTubePlayerHelper\.applyUltraPerformance\(w\)/);

// Wired Android Auto detach fully closes the phone task/process, but unrelated
// power changes are ignored when no projection is active.
assert.match(projection, /CarDisconnectWatchdog\.shouldHandleCableDetach\(\)/);
assert.match(projection, /CarDisconnectWatchdog\.onCableDetachBroadcast/);
assert.match(shutdown, /ACTION_FULL_EXIT/);
assert.match(shutdown, /resumePhoneOnDisconnect = false/);
assert.match(shutdown, /pauseAllPlaybackForShutdown\(\)/);

// TikTok uses the real desktop website: web login/cookies remain enabled,
// app-only redirects are blocked, and only native-app promotion walls are hidden.
assert.match(tiktok, /javaScriptCanOpenWindowsAutomatically = true/);
assert.match(tiktok, /DESKTOP_UA/);
assert.match(tiktok, /function isAppOnlyWall/);
assert.match(tiktok, /data-ttube-app-promo-wall/);
assert.doesNotMatch(tiktok, /data-ttube-login-wall/);
assert.match(tiktok, /"https:\/\/www\.tiktok\.com\/search\?q=" \+ Uri\.encode/);
assert.doesNotMatch(tiktok, /search\/video\?q=/);

// DOM work is mutation-driven and frame-coalesced; no polling loop.
assert.match(tiktok, /MutationObserver/);
assert.match(tiktok, /requestAnimationFrame/);
assert.doesNotMatch(tiktok, /setInterval\s*\(/);

console.log('PASS v1.0.70 build 261: cable exit + dark phone default + TikTok login/search + smoother WebViews');
