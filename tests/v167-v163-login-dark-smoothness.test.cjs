const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const projection = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt','utf8');
const shutdown = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarAppShutdownManager.kt','utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt','utf8');

assert.match(gradle, /versionCode = 252/);
assert.match(gradle, /versionName = "1\.0\.63"/);

// Phone defaults to dark only when the user has never chosen a mode.
assert.match(main, /if \(!prefs\.contains\(PREF_THEME_MODE\)\)[\s\S]*?SettingsActivity\.THEME_NIGHT/);
assert.match(main, /initIptvWebView[\s\S]*?YouTubePlayerHelper\.applyUltraPerformance\(w\)/);

// Wired Android Auto detach fully closes the phone task/process, but unrelated
// power changes are ignored when no projection is active.
assert.match(projection, /if \(!CarMediaManager\.isCarConnected\)[\s\S]*?Ignoring detach without active projection/);
assert.match(projection, /CarAppShutdownManager\.shutdownCompletely/);
assert.match(shutdown, /ACTION_FULL_EXIT/);
assert.match(shutdown, /resumePhoneOnDisconnect = false/);

// TikTok login is explicitly allowed and search remains on the public WebView route.
assert.match(tiktok, /javaScriptCanOpenWindowsAutomatically = true/);
assert.match(tiktok, /function isLoginRoute\(\)/);
assert.match(tiktok, /function installLoginIntentTracking\(\)/);
assert.match(tiktok, /state\.loginRequestedAt = Date\.now\(\)/);
assert.match(tiktok, /if \(loginWall && allowLogin\)/);
assert.match(tiktok, /removeAttribute\('data-ttube-login-wall'\)/);
assert.doesNotMatch(tiktok, /html:not\(\.ttube-tiktok-search\) \[data-e2e="top-login-button"\]/);
assert.match(tiktok, /"https:\/\/www\.tiktok\.com\/search\/video\?q=" \+ Uri\.encode/);
assert.match(tiktok, /"https:\/\/www\.tiktok\.com\/search\?q=" \+ Uri\.encode/);

// DOM work is frame-coalesced and rate-limited instead of rescanning on every mutation.
assert.match(tiktok, /state\.lastApplyAt = Date\.now\(\)/);
assert.match(tiktok, /var collectGap = isSearchRoute\(\) \? 420 : 900/);
assert.match(tiktok, /var delay = Math\.max\(72, 160 - elapsed\)/);
assert.match(tiktok, /requestAnimationFrame/);
assert.doesNotMatch(tiktok, /setInterval\s*\(/);

console.log('PASS v1.0.63 build 252: cable exit + dark phone default + TikTok login/search + smoother WebViews');
