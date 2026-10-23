const fs = require('node:fs');
const assert = require('node:assert/strict');

const manager = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WazeHudManager.kt', 'utf8');
const overlay = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/VietmapHudOverlay.kt', 'utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt', 'utf8');

assert.match(manager, /id = 6,[\s\S]*name = "#6 Waze Panel Lớn"/);
assert.match(manager, /id = 7,[\s\S]*name = "#7 Waze Thanh Ngang"/);

assert.match(overlay, /6 -> buildStyle6WazePanelLon\(\)/);
assert.match(overlay, /7 -> \{[\s\S]*designWidthPx = 510f \* density[\s\S]*projectionViewportWidthPx \* 0\.65f \/ designWidthPx[\s\S]*configuredScale \/ 100f[\s\S]*\}/);

assert.match(overlay, /private fun buildStyle6WazePanelLon\(\)/);
assert.match(overlay, /private fun buildStyle6WazePanelLon\(\)[\s\S]*?minimumWidth = 0/);
assert.match(overlay, /private fun buildStyle6WazePanelLon\(\)[\s\S]*?textSize = 34f/);
assert.match(overlay, /referenceReportButton = TextView\(context\)[\s\S]*text = "⚠  Báo cáo"/);
assert.match(overlay, /laneGuidanceText = TextView\(context\)/);
assert.match(overlay, /createReferenceAlertCell\(dp\(50\), dp\(38\), compact = false\)/);

assert.match(overlay, /private fun buildStyle7WazeThanhNgang\(\)/);
assert.match(overlay, /private fun buildStyle7WazeThanhNgang\(\)[\s\S]*?minimumWidth = dp\(430\)/);
assert.match(overlay, /createReferenceAlertCell\(dp\(54\), dp\(38\), compact = true\)/);
assert.match(overlay, /text = "⚠"/);
assert.match(
  overlay,
  /private fun buildStyle7WazeThanhNgang\(\)[\s\S]*createLockButton\(\)[\s\S]*LayoutParams\(dp\(34\), dp\(34\)\)/
);
assert.match(
  overlay,
  /7 -> background = rounded\([\s\S]*#800B0D10[\s\S]*#24FFFFFF/
);
assert.match(
  overlay,
  /alertContainer\?\.visibility = if \(activeStyleId == 6\) INVISIBLE else GONE/
);
assert.match(
  overlay,
  /cell\.visibility = if \(activeStyleId == 6\) INVISIBLE else GONE/
);
assert.match(
  overlay,
  /activeStyleId == 7 -> if \(isPreviewMode \|\| \(data\.isConnected && hasAnyVisibleAlert\)\) VISIBLE else GONE/
);

assert.match(overlay, /val extras = if \(primaryAlert != null\) data\.upcomingAlerts\.drop\(1\) else data\.upcomingAlerts/);
assert.match(overlay, /referenceAlertIcons\.getOrNull\(index\)\?\.setAlert/);
assert.match(overlay, /laneGuidance = live\.laneGuidance \?: "↑  ↑  ↗"/);
assert.match(overlay, /applyStyleBackground\(overSpeed = false\)/);

assert.match(manager, /_style_\$\{styleId\}_photo_v3/);
assert.match(overlay, /pivotX = 0f/);
assert.match(overlay, /pivotY = 0f/);
assert.match(overlay, /activeStyleId == 6 -> INVISIBLE/);

assert.match(settings, /val allStyles = WazeHudManager\.STYLES/);
assert.match(settings, /WazeHudManager\.setActiveStyleId\(this@SettingsActivity, style\.id\)/);

console.log('PASS Waze HUD photo-match layouts: #7 lock + translucent background + auto-collapsing alerts');

assert.match(overlay, /fun applyProjectionViewport\(widthPx: Int, heightPx: Int\)/);
assert.match(overlay, /projectionViewportWidthPx \* 0\.255f/);
assert.match(overlay, /7 -> \{[\s\S]*designWidthPx = 510f \* density[\s\S]*projectionViewportWidthPx \* 0\.65f \/ designWidthPx[\s\S]*configuredScale \/ 100f[\s\S]*\}/);
assert.match(settings, /WazeHudManager\.STYLES/);
console.log('PASS Waze HUD sizing: #6 stays viewport-adaptive while #7 grows to car-screen reference size');
