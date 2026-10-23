const fs = require('node:fs');
const assert = require('node:assert/strict');

const manager = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WazeHudManager.kt', 'utf8');
const overlay = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/VietmapHudOverlay.kt', 'utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt', 'utf8');

assert.match(manager, /id = 6,[\s\S]*name = "#6 Waze Panel Lớn"/);
assert.match(manager, /id = 7,[\s\S]*name = "#7 Waze Thanh Ngang"/);

assert.match(overlay, /6 -> buildStyle6WazePanelLon\(\)/);
assert.match(overlay, /7 -> buildStyle7WazeThanhNgang\(\)/);

assert.match(overlay, /private fun buildStyle6WazePanelLon\(\)/);
assert.match(overlay, /private fun buildStyle6WazePanelLon\(\)[\s\S]*?minimumWidth = 0/);
assert.match(overlay, /private fun buildStyle6WazePanelLon\(\)[\s\S]*?textSize = 34f/);
assert.match(overlay, /referenceReportButton = TextView\(context\)[\s\S]*text = "⚠  Báo cáo"/);
assert.match(overlay, /laneGuidanceText = TextView\(context\)/);
assert.match(overlay, /createReferenceAlertCell\(dp\(50\), dp\(38\), compact = false\)/);

assert.match(overlay, /private fun buildStyle7WazeThanhNgang\(\)/);
assert.match(overlay, /private fun buildStyle7WazeThanhNgang\(\)[\s\S]*?minimumWidth = 0/);
assert.match(overlay, /createReferenceAlertCell\(dp\(40\), dp\(25\), compact = true\)/);
assert.match(overlay, /text = "⚠"/);

assert.match(overlay, /val extras = if \(primaryAlert != null\) data\.upcomingAlerts\.drop\(1\) else data\.upcomingAlerts/);
assert.match(overlay, /referenceAlertIcons\.getOrNull\(index\)\?\.setAlert/);
assert.match(overlay, /laneGuidance = live\.laneGuidance \?: "↑  ↑  ↗"/);
assert.match(overlay, /applyStyleBackground\(overSpeed = false\)/);

assert.match(manager, /_style_\$\{styleId\}_photo_v3/);
assert.match(overlay, /pivotX = 0f/);
assert.match(overlay, /pivotY = 0f/);
assert.match(overlay, /activeStyleId == 6 \|\| activeStyleId == 7 -> INVISIBLE/);

assert.match(settings, /val allStyles = WazeHudManager\.STYLES/);
assert.match(settings, /WazeHudManager\.setActiveStyleId\(this@SettingsActivity, style\.id\)/);

console.log('PASS Waze HUD photo-match layouts: large panel + compact strip + lanes + stacked alerts + report button');

assert.match(overlay, /fun applyProjectionViewport\(widthPx: Int, heightPx: Int\)/);
assert.match(overlay, /projectionViewportWidthPx \* 0\.255f/);
assert.match(overlay, /projectionViewportWidthPx \* 0\.43f/);
assert.match(overlay, /configuredScale \/ 120f/);
assert.match(settings, /WazeHudManager\.STYLES/);
console.log('PASS Waze HUD viewport sizing: photo layouts scale from AA viewport instead of phone density');
