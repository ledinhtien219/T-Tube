const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const workflow = fs.readFileSync('.github/workflows/build-apk.yml', 'utf8');
const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const host = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt', 'utf8');
const source = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');

assert.match(gradle, /versionCode = 272/);
assert.match(gradle, /versionName = "1\.0\.75"/);
assert.match(workflow, /TTube-v1\.0\.75-build272-APK/);

assert.match(manifest, /CarHudProjectionService/);
assert.match(manifest, /CATEGORY_PROJECTION/);

assert.match(host, /import android\.view\.KeyEvent/);
assert.match(host, /override fun onKeyDown\(keyCode: Int, event: KeyEvent\): Boolean/);
assert.match(host, /presentation\?\.handleMazdaPadKeyDown\(keyCode, event\)/);
assert.match(host, /override fun onKeyUp\(keyCode: Int, event: KeyEvent\): Boolean/);
assert.match(host, /presentation\?\.handleMazdaPadKeyUp\(keyCode, event\)/);

assert.match(source, /private var mazdaPadCursor: TextView\?/);
assert.match(source, /private val mazdaPadRepeatRunnable = object : Runnable/);
assert.match(source, /root\.postOnAnimation\(this\)/);
assert.match(source, /private fun tryMazdaNativeFocus\(direction: Int\): Boolean/);
assert.match(source, /private fun moveMazdaPadCursorByPixels/);
assert.match(source, /fun handleMazdaPadKeyDown\(keyCode: Int, event: KeyEvent\): Boolean/);
assert.match(source, /fun handleMazdaPadKeyUp\(keyCode: Int, event: KeyEvent\): Boolean/);
assert.match(source, /KEYCODE_DPAD_CENTER -> clickMazdaPadCursor\(\)/);
assert.match(source, /handleMazdaPadKeyDown\(event\.keyCode, event\)/);
assert.match(source, /handleMazdaPadKeyUp\(event\.keyCode, event\)/);
assert.match(source, /root\.removeCallbacks\(mazdaPadRepeatRunnable\)/);

console.log('PASS v1.0.75 build 272: smooth Mazda PAD + direct CarActivity routing');
