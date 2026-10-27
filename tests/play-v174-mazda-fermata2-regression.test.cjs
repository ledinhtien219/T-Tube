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
assert.match(gradle, /versionCode = 297/);
assert.match(gradle, /versionName = "1\.0\.99"/);
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

console.log('PASS: Play 1.0.99 = stable 0.74 UI + Mazda DPAD + Fermata2 + Play policy gates');
