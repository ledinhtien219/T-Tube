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
  assert.match(source, /if \(v\.playbackRate !== 1\.0\)/); // safeguard below
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
