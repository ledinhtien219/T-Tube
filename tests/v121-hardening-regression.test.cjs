// Regression guards for source assembled by the complete Hey Mimi v17 workflow.
// Run after ci/hey-mimi/v18-youtube-hardening.py, not on the pre-patch source.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');

function read(file) { return fs.readFileSync('app/src/main/java/com/carhud/aaproxy/' + file, 'utf8'); }

test('YouTube never forces 16x speed or rewrites playback rate every second', () => {
  const source = read('YouTubePlayerHelper.kt');
  assert.doesNotMatch(source, /video\.playbackRate\s*=\s*16/);
  assert.doesNotMatch(source, /v\.playbackRate\s*=\s*1\.0/);
  assert.match(source, /function killYouTubeAds\(/);
  const adblock = read('YouTubeAdBlocker.kt');
  assert.doesNotMatch(adblock, /v\.currentTime\s*=\s*d\s*;/);
  assert.doesNotMatch(adblock, /v\.playbackRate\s*=/);
});

test('hidden PiP uses original WebView player with viewport-sized four-row scroll grid', () => {
  const source = read('YouTubePlayerHelper.kt');
  const presentation = read('CarPresentation.kt');
  assert.match(source, /window\.__carhudHidePip = function\(\)/);
  assert.match(source, /window\.__carhudPipHidden = true/);
  assert.match(source, /grid-auto-rows:minmax\(102px,calc\(\(100dvh - 54px\)\/4\)\)/);
  assert.match(source, /syncRecommendationPane\(pip \|\| hiddenPip\)/);
  assert.match(source, /if \(!pip && !hiddenPip && window\.__carhudMiniPlayerHost\)/);
  assert.match(source, /window\.__carhudPipHidden = false/);
  assert.match(source, /fun hideMiniPlayer\(view: WebView\?\)/);
  assert.match(presentation, /YouTubePlayerHelper\.hideMiniPlayer\(web\)/);
});

test('reverse-camera wallpaper recovery retries an interrupted custom image', () => {
  const source = read('CarDashboardView.kt');
  assert.match(source, /val customImageVisible =/);
  assert.match(source, /val customImageLoading =/);
  assert.match(source, /\(!customImage \|\| customImageVisible \|\| customImageLoading\)/);
  assert.match(source, /staticWallpaperSignature = null/);
});

test('Maps ignores unrelated notification removals and does not falsely claim route launch', () => {
  const source = read('VietmapNotificationListenerService.kt');
  assert.match(source, /activeGoogleMapsNavigationKeys\.remove\(sbn\.key\)/);
  assert.match(source, /if \(activeGoogleMapsNavigationKeys\.isNotEmpty\(\)\)/);
  assert.match(source, /notification\.category == Notification\.CATEGORY_NAVIGATION/);
  assert.match(source, /processGoogleMapsNotification\(candidate\)/);
});

test('YouTube skip buttons are preserved rather than hidden by their ad parents', () => {
  const adblock = read('YouTubeAdBlocker.kt');
  assert.doesNotMatch(adblock, /'\.video-ads',/);
  assert.doesNotMatch(adblock, /'\.ytp-ad-module',/);
  assert.match(adblock, /ytp-skip-ad-button__button/);
  assert.match(adblock, /ytp-ad-skip-button-container button/);
  const postpatch = fs.readFileSync('ci/hey-mimi/v18-youtube-hardening.py', 'utf8');
  assert.match(postpatch, /preserve-native-skip-parent-containers/);
  assert.match(postpatch, /modern-skip-button-variants/);
});

test('Fermata-style YouTube lifecycle reattaches per navigation without busy polling', () => {
  const adblock = read('YouTubeAdBlocker.kt');
  assert.match(adblock, /existing\.version === 6/);
  assert.match(adblock, /window\.addEventListener\('yt-navigate-start'/);
  assert.match(adblock, /window\.addEventListener\('yt-navigate-finish', tick\)/);
  assert.match(adblock, /state\.timer = setInterval\(tick, 2500\)/);
  assert.match(adblock, /clickable = btn\.matches/);
  assert.doesNotMatch(adblock, /v\.playbackRate\s*=/);
});

test('Only known third-party ad hosts are intercepted; media and first-party player stay untouched', () => {
  const src = read('YouTubeAdBlocker.kt');
  assert.match(src, /if \(request\.isForMainFrame\) return null/);
  assert.match(src, /if \(!request\.method\.equals\("GET"/);
  assert.match(src, /if \(!isDedicatedAdHost\(host\)\) return null/);
  assert.match(src, /host\.endsWith\("\.doubleclick\.net"\)/);
  assert.match(src, /host\.endsWith\("\.googlesyndication\.com"\)/);
  assert.match(src, /host\.endsWith\("\.googleadservices\.com"\)/);
  assert.match(src, /return createEmptyResponse\(\)/);
  assert.doesNotMatch(src, /fun shouldIntercept[\s\S]*?return isAdUrl\(uri\)/);
});
