const fs = require('node:fs');
const assert = require('node:assert/strict');

const phone = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const host = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');

function section(source, startNeedle, endNeedle) {
  const start = source.indexOf(startNeedle);
  assert.notEqual(start, -1, 'Missing section: ' + startNeedle);
  const end = source.indexOf(endNeedle, start + startNeedle.length);
  assert.notEqual(end, -1, 'Missing end section: ' + endNeedle);
  return source.slice(start, end);
}

// Phone Activity must not wake every WebView while Maps owns the foreground.
const phonePause = section(phone, 'override fun onPause()', 'override fun onStop()');
const phoneStop = section(phone, 'override fun onStop()', 'private fun setPhoneWebViewsForeground');
assert.doesNotMatch(phonePause, /onResume\(|resumeTimers\(/);
assert.doesNotMatch(phoneStop, /onResume\(|resumeTimers\(/);
assert.match(phone, /!CarMediaManager\.isCarConnected/);
assert.match(phone, /web\.enableBackgroundAudio = foreground \|\| keepThisWeb/);
assert.match(phone, /web\.clearFocus\(\)[\s\S]*?web\.onPause\(\)/);

// Projection host must stop consuming touch immediately after handoff.
assert.match(host, /if \(!projectionForeground\) return@setOnTouchListener false/);
assert.match(host, /override fun onStop\(\)[\s\S]*?projectionForeground = false[\s\S]*?presentation\?\.onProjectionHostBackgrounded\(\)/);

// Surface teardown must not forcibly resume Chromium or reclaim audio focus.
const surfaceDestroyed = section(host, 'override fun surfaceDestroyed', 'private fun resolveDensity');
assert.doesNotMatch(surfaceDestroyed, /resumeTimers\(|ensureAudioFocus\(/);
assert.match(surfaceDestroyed, /if \(!projectionForeground\)/);
assert.match(surfaceDestroyed, /web\?\.onPause\(\)/);

// Presentation itself has a second input/render guard for head units that keep
// the old surface attached briefly during the switch to Google Maps.
assert.match(presentation, /private var projectionHostForeground = true/);
assert.match(presentation, /fun dispatchProjectedTouch\(event: MotionEvent\): Boolean \{[\s\S]*?if \(!projectionHostForeground\) return false/);
assert.match(presentation, /fun dispatchTouch\(x: Float, y: Float\) \{[\s\S]*?if \(!projectionHostForeground\) return/);
assert.match(presentation, /fun onProjectionHostBackgrounded\(\)/);
assert.match(presentation, /projectedTouchActive = false/);
assert.match(presentation, /root\.visibility = View\.INVISIBLE/);
assert.match(presentation, /fun onProjectionHostForegrounded\(\)[\s\S]*?root\.visibility = View\.VISIBLE/);

console.log('PASS Google Maps handoff: background WebViews suspended, projection touch released, no forced audio-focus reacquire');
