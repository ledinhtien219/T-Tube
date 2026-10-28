const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const host = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt', 'utf8');
const adblock = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubeAdBlocker.kt', 'utf8');

assert.match(gradle, /applicationId = "com\.ledinhtien\.ttube"/);
assert.match(gradle, /compileSdk = 36/);
assert.match(gradle, /targetSdk = 36/);
assert.match(gradle, /versionCode = 302/);
assert.match(gradle, /versionName = "1\.1\.4"/);
assert.match(gradle, /PLAY_DISTRIBUTION/);
assert.match(gradle, /isDebuggable = false/);

assert.doesNotMatch(manifest, /ACCESS_SURFACE/);
assert.doesNotMatch(manifest, /NAVIGATION_TEMPLATES/);
assert.match(manifest, /CATEGORY_PROJECTION/);
assert.match(manifest, /CarHudProjectionService/);

assert.match(main, /if \(!BuildConfig\.PLAY_DISTRIBUTION\)/);
assert.match(main, /if \(!BuildConfig\.PLAY_DISTRIBUTION && !LicenseManager\.isLicensed\(this\)\)/);

assert.match(presentation, /handleMazdaPadKeyDown/);
assert.match(presentation, /handleMazdaPadKeyUp/);
assert.match(presentation, /tryMazdaNativeFocus/);
assert.match(presentation, /mazdaPadDetentAccel/);
assert.match(host, /handleMazdaPadKeyDown/);
assert.match(host, /handleMazdaPadKeyUp/);

assert.match(adblock, /existing\.version === 5/);
assert.match(adblock, /version: 5/);
assert.match(adblock, /suppressAdChrome/);
assert.match(adblock, /adUiObserver/);
assert.match(adblock, /carhud-ad-active/);
assert.match(adblock, /v\.currentTime = d;/);
assert.doesNotMatch(adblock, /v\.playbackRate = 16\.0/);
assert.doesNotMatch(adblock, /v\.muted = true/);

const intercept = adblock.slice(
  adblock.indexOf('fun shouldIntercept'),
  adblock.indexOf('/**', adblock.indexOf('fun shouldIntercept') + 10)
);
assert.match(intercept, /return null/);
assert.doesNotMatch(intercept, /createEmptyResponse/);

const watchdog = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDisconnectWatchdog.kt', 'utf8');
const gate = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/ProjectionDisconnectGate.kt', 'utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt', 'utf8');
const dashboard = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt', 'utf8');

assert.match(watchdog, /CarConnection\(context.applicationContext\)\.type/);
assert.match(watchdog, /PROJECTION_EXIT_CONFIRM_MS = 6_000L/);
assert.match(watchdog, /connectionExitGate\.shouldExit/);
assert.match(watchdog, /beginCarConnectionObservation/);
assert.match(gate, /hasBeenProjected && connectionType == 0/);
assert.match(settings, /PREF_AUTO_EXIT_ON_AA_DISCONNECT/);
assert.match(dashboard, /PREF_CUSTOM_WALLPAPER_REVISION/);
assert.match(dashboard, /wallpaperGeneration == generation/);
assert.match(dashboard, /activeVideoWallpaperRevision == revision/);
assert.match(main, /PREF_CUSTOM_WALLPAPER_REVISION/);
assert.match(presentation, /keepMazdaPadCursorAlive\(force: Boolean = false\)/);
assert.match(adblock, /adUiFramePending/);
assert.match(adblock, /requestAnimationFrame/);
console.log('PASS: Play 1.1.0 stable074 + Mazda + Fermata2 + runtime performance + wireless auto-exit');

const disconnect = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDisconnectWatchdog.kt','utf8');
const picker = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const adCss = adblock.slice(adblock.indexOf('var adUiSelectors'),adblock.indexOf('function getPlayer'));
assert.match(disconnect, /if \(action == Intent\.ACTION_POWER_DISCONNECTED\) return/);
assert.match(disconnect, /connectionLiveData != null \|\| connectionExitGate\.isProjectionActive\(\)/);
assert.match(disconnect, /autoExitEnabled\(context\)/);
assert.doesNotMatch(disconnect, /onProjectionLifecycleEnded[\s\S]{0,250}checkPowerState/);
assert.match(picker, /withContext\(Dispatchers\.IO\) \{ preparePickedWallpaper\(uri\) \}/);
assert.match(picker, /pending\.renameTo\(dest\)/);
assert.match(picker, /copied > maxBytes/);
assert.match(dashboard, /coroutineScope\.launch\(Dispatchers\.IO\)/);
assert.match(dashboard, /inJustDecodeBounds = true/);
assert.match(dashboard, /inSampleSize = sample/);
assert.match(dashboard, /wallpaperImageJob\?\.cancel\(\)/);
assert.match(dashboard, /60_000L - \(System\.currentTimeMillis\(\) % 60_000L\)/);
assert.doesNotMatch(adCss, /^\s*'\.ytp-ad-player-overlay',/m);
assert.doesNotMatch(adCss, /^\s*'\.ytp-ad-player-overlay-layout',/m);
assert.doesNotMatch(adCss, /^\s*'\.ytp-ad-player-overlay-skip-or-preview-container',/m);
assert.match(adblock, /\.ytp-ad-skip-button-modern/);
console.log('PASS build 300 stability gates: wireless power, wallpaper IO, sampled bitmap, skip control');
