const fs = require('node:fs');
const assert = require('node:assert/strict');

const settings = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt',
  'utf8'
);
const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);
const dashboard = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt',
  'utf8'
);

assert.match(settings, /KEY_AUTO_DETECT_SCREEN = "auto_detect_screen_layout"/);
assert.match(settings, /Auto layout:/);
assert.match(settings, /car_layout_class/);

assert.match(
  presentation,
  /dashboardView\?\.applyAdaptiveScreen\(carWidth, carHeight, autoDetect\)/
);
assert.match(
  presentation,
  /configuredPos == "auto" && autoDetect[\s\S]*?if \(isPortrait\) "bottom" else "left"/
);
assert.match(
  presentation,
  /configuredPos == "auto" -> "bottom"/
);
assert.match(
  presentation,
  /key == SettingsActivity\.KEY_AUTO_DETECT_SCREEN[\s\S]*?applyEffectiveViewportFromRoot\(force = true\)/
);

assert.match(
  dashboard,
  /fun applyAdaptiveScreen\([\s\S]*?autoDetect: Boolean = true/
);
assert.match(dashboard, /if \(autoDetect\) widthPx \/ density else 1024f/);
assert.match(dashboard, /if \(autoDetect\) heightPx \/ density else 600f/);
assert.match(dashboard, /val ultrawideViewport = viewportAspect >= 1\.85f/);
assert.match(dashboard, /val compactWidth = !ultrawideViewport && widthDp < 820f/);
assert.match(settings, /ratioVal >= 2\.40f -> "Siêu rộng mở rộng \(Super Ultra-wide\)"/);
assert.match(settings, /ratioVal >= 1\.85f -> "Siêu rộng \(Ultra-wide\)"/);

console.log('PASS v1.0.70 auto screen detect: switch drives adaptive layout and runtime-aligned screen classification');
