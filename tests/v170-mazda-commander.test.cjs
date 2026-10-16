const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const host = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt',
  'utf8'
);
const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);

assert.match(gradle, /versionName = "1\.0\.70-mazda"/);
assert.match(gradle, /versionCode = 262/);

assert.match(host, /InputDevice\.SOURCE_ROTARY_ENCODER/);
assert.match(host, /MotionEvent\.AXIS_SCROLL/);
assert.match(host, /override fun dispatchGenericMotionEvent/);
assert.match(host, /override fun dispatchKeyEvent/);
assert.match(host, /KEYCODE_DPAD_UP/);
assert.match(host, /KEYCODE_DPAD_DOWN/);
assert.match(host, /KEYCODE_DPAD_CENTER/);
assert.match(host, /KEYCODE_ENTER/);

assert.match(presentation, /fun dispatchProjectedKeyEvent\(event: KeyEvent\)/);
assert.match(presentation, /fun dispatchMazdaRotary\(axis: Float\)/);
assert.match(presentation, /KEYCODE_DPAD_DOWN else KeyEvent\.KEYCODE_DPAD_UP/);
assert.match(presentation, /coerceIn\(1, 6\)/);

console.log('PASS Mazda Commander: rotary, DPAD and press input are forwarded to projection UI');
