const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');

assert.match(gradle, /versionCode = 258/);
assert.match(gradle, /versionName = "1\.0\.67"/);

// Non-TikTok projected pages must render at the VirtualDisplay's native scale.
const scaleBlock = presentation.match(/fun applyWebScaleForUrl\(url: String\?\) \{[\s\S]*?\n    \}/)?.[0] || '';
assert.notEqual(scaleBlock, '');
assert.match(scaleBlock, /web\.setInitialScale\(100\)/);
assert.match(scaleBlock, /web\.settings\.loadWithOverviewMode = false/);
assert.match(scaleBlock, /web\.settings\.useWideViewPort = true/);
assert.doesNotMatch(scaleBlock, /effectiveCarDpi/);
assert.doesNotMatch(scaleBlock, /effectivePhoneDpi/);
assert.doesNotMatch(scaleBlock, /\(\(effectiveCarDpi \/ effectivePhoneDpi\)/);
assert.doesNotMatch(scaleBlock, /coerceIn\(20f, 200f\)/);

// TikTok keeps its isolated overview-fit path.
assert.match(scaleBlock, /TikTokWebPolicy\.isTikTok\(url\)[\s\S]*?loadWithOverviewMode = true[\s\S]*?setInitialScale\(100\)/);

// Fullscreen WebView itself must occupy the complete projection surface.
assert.match(
  presentation,
  /web\.layoutParams = FrameLayout\.LayoutParams\(ViewGroup\.LayoutParams\.MATCH_PARENT, ViewGroup\.LayoutParams\.MATCH_PARENT\)/
);
assert.match(
  presentation,
  /root\.addView\(web, 0, FrameLayout\.LayoutParams\(ViewGroup\.LayoutParams\.MATCH_PARENT, ViewGroup\.LayoutParams\.MATCH_PARENT\)\)/
);

console.log('PASS v1.0.67 build 258: projected YouTube/IPTV/browser stay at native 100% car-surface scale');
