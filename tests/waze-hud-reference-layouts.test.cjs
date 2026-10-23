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
assert.match(overlay, /minimumWidth = dp\(360\)/);
assert.match(overlay, /textSize = 46f/);
assert.match(overlay, /referenceReportButton = TextView\(context\)[\s\S]*text = "⚠  Báo cáo"/);
assert.match(overlay, /laneGuidanceText = TextView\(context\)/);
assert.match(overlay, /createReferenceAlertCell\(dp\(76\), dp\(46\), compact = false\)/);

assert.match(overlay, /private fun buildStyle7WazeThanhNgang\(\)/);
assert.match(overlay, /minimumWidth = dp\(410\)/);
assert.match(overlay, /createReferenceAlertCell\(dp\(54\), dp\(42\), compact = true\)/);
assert.match(overlay, /text = "⚠"/);

assert.match(overlay, /val extras = if \(primaryAlert != null\) data\.upcomingAlerts\.drop\(1\) else data\.upcomingAlerts/);
assert.match(overlay, /referenceAlertIcons\.getOrNull\(index\)\?\.setAlert/);
assert.match(overlay, /laneGuidance = live\.laneGuidance \?: "↑  ↑  ↗"/);
assert.match(overlay, /applyStyleBackground\(overSpeed = false\)/);

assert.match(settings, /val allStyles = WazeHudManager\.STYLES/);
assert.match(settings, /WazeHudManager\.setActiveStyleId\(this@SettingsActivity, style\.id\)/);

console.log('PASS Waze HUD photo-match layouts: large panel + compact strip + lanes + stacked alerts + report button');
