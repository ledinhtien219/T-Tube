const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const host = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt', 'utf8');
const screen = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');

assert.match(gradle, /versionCode = 262/);
assert.match(gradle, /versionName = "1\.0\.70-mazda1"/);

assert.match(host, /MotionEvent\.AXIS_SCROLL/);
assert.match(host, /MotionEvent\.AXIS_VSCROLL/);
assert.match(host, /MotionEvent\.AXIS_HSCROLL/);
assert.match(host, /handleMazdaRotaryMotion/);
assert.match(host, /dispatchMazdaRotary\(direction\)/);
assert.match(host, /dispatchMazdaKey\(event\)/);

assert.match(screen, /addAction\(Action\.PAN\)/);
assert.match(screen, /setPanModeListener/);
assert.match(screen, /dispatchMazdaSurfaceScroll/);

assert.match(presentation, /fun dispatchMazdaRotary\(direction: Int\)/);
assert.match(presentation, /fun dispatchMazdaKey\(event: KeyEvent\)/);
assert.match(presentation, /KEYCODE_DPAD_CENTER/);
assert.match(presentation, /KEYCODE_ENTER/);
assert.match(presentation, /KEYCODE_BACK/);
assert.match(presentation, /ttube-mazda-focus/);
assert.match(presentation, /href\*="\/watch"/);

// Mazda build keeps the agreed YouTube results swipe tuning.
assert.match(presentation, /scrollScale = if \(isYouTubeResults\) 1\.5f else 2\.0f/);
assert.match(presentation, /flingScale = if \(isYouTubeResults\) 0\.28f else 0\.45f/);

console.log('PASS v1.0.70-mazda1: commander rotary, click, dpad, back and YouTube tuning are present');
