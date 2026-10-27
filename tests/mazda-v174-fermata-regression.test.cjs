const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const host = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt', 'utf8');
const adblock = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubeAdBlocker.kt', 'utf8');

assert.match(gradle, /applicationId = "com\.tcar\.auto"/);
assert.match(gradle, /versionCode = 272/);
assert.match(gradle, /versionName = "1\.0\.74-mazda-fermata2"/);

assert.match(presentation, /handleMazdaPadKeyDown/);
assert.match(presentation, /handleMazdaPadKeyUp/);
assert.match(presentation, /tryMazdaNativeFocus/);
assert.match(presentation, /mazdaPadDetentAccel/);
assert.match(host, /handleMazdaPadKeyDown/);
assert.match(host, /handleMazdaPadKeyUp/);

assert.match(adblock, /existing\.version === 5/);
assert.match(adblock, /version: 5/);
assert.match(adblock, /v\.currentTime = d;/);
assert.doesNotMatch(adblock, /v\.playbackRate = 16\.0/);
assert.doesNotMatch(adblock, /v\.muted = true/);

assert.match(adblock, /adUiObserver/);
assert.match(adblock, /suppressAdChrome/);
assert.match(adblock, /carhud-ad-active/);
assert.match(adblock, /ytp-ad-player-overlay-instream-info/);
assert.match(adblock, /ytp-ad-player-overlay-skip-or-preview-container/);
assert.match(adblock, /ytp-ad-preview-container/);
assert.match(adblock, /childList: true/);
assert.match(adblock, /subtree: true/);

const intercept = adblock.slice(
  adblock.indexOf('fun shouldIntercept'),
  adblock.indexOf('/**', adblock.indexOf('fun shouldIntercept') + 10)
);
assert.match(intercept, /return null/);
assert.doesNotMatch(intercept, /createEmptyResponse/);

console.log('PASS: stable 0.74 base + Mazda DPAD + Fermata-style YouTube ad handling');
