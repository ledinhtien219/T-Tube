const fs = require('node:fs');
const assert = require('node:assert/strict');

const manager = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WazeHudManager.kt', 'utf8');
const overlay = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/VietmapHudOverlay.kt', 'utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt', 'utf8');

assert.match(manager, /id = 6,[\s\S]*name = "#6 Waze Panel Lớn"/);
assert.match(manager, /id = 7,[\s\S]*name = "#7 Waze Thanh Ngang"/);
assert.match(manager, /biển giới hạn \+ tốc độ nổi bật, rẽ và cảnh báo xếp bên phải/);
assert.match(manager, /giới hạn \+ tốc độ \+ rẽ \+ cảnh báo trên một hàng/);

assert.match(overlay, /6 -> buildStyle6WazePanelLon\(\)/);
assert.match(overlay, /7 -> buildStyle7WazeThanhNgang\(\)/);
assert.match(overlay, /private fun buildStyle6WazePanelLon\(\)/);
assert.match(overlay, /minimumWidth = dp\(330\)/);
assert.match(overlay, /private fun buildStyle7WazeThanhNgang\(\)/);
assert.match(overlay, /minimumWidth = dp\(285\)/);
assert.match(overlay, /Color\.parseColor\("#1669C9"\)/);

assert.match(settings, /val allStyles = WazeHudManager\.STYLES/);
assert.match(settings, /WazeHudManager\.setActiveStyleId\(this@SettingsActivity, style\.id\)/);

console.log('PASS Waze HUD reference layouts: styles 6/7 exposed, rendered and selectable');
