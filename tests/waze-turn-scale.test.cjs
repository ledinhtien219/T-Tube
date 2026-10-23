const fs = require('node:fs');
const assert = require('node:assert/strict');

const overlay = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/VietmapHudOverlay.kt', 'utf8');
const hlp = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WazeHlpWebSocketManager.kt', 'utf8');

assert.match(overlay, /7 -> \{[\s\S]*designWidthPx = 510f \* density[\s\S]*projectionViewportWidthPx \* 0\.65f \/ designWidthPx[\s\S]*configuredScale \/ 100f[\s\S]*\}/);
assert.match(overlay, /private fun buildStyle7WazeThanhNgang\(\)[\s\S]*minimumWidth = dp\(430\)/);
assert.match(overlay, /val size = dp\(36\)/);
assert.match(overlay, /LayoutParams\(dp\(72\), dp\(40\)\)/);
assert.match(overlay, /LayoutParams\(dp\(88\), dp\(38\)\)/);
assert.match(overlay, /createReferenceAlertCell\(dp\(54\), dp\(38\), compact = true\)/);

assert.match(overlay, /val arrowStr = when \(data\.turnCode\)/);
assert.match(overlay, /2 -> "↰"/);
assert.match(overlay, /3 -> "↱"/);
assert.match(overlay, /8, 9 -> "↩"/);
assert.match(overlay, /10 -> "⟳"/);
assert.match(overlay, /else -> ""/);
assert.doesNotMatch(overlay, /!data\.nextRoadName\.isNullOrBlank\(\)[\s\S]{0,100}turnVis/);

assert.match(hlp, /val navigationPresent = hasAny\(json, "nav"/);
assert.match(hlp, /navigationPresent && navigationActive == false -> 0/);
assert.match(hlp, /turnDistancePresent -> parsedTurnDistance\?\.takeIf \{ it >= 0 \} \?: 0/);
assert.match(hlp, /nextRoadPresent -> ""/);
assert.match(hlp, /lanePresent -> ""/);

console.log('PASS Waze turn + scale: style #7 100% is viewport-sized and stale maneuver state cannot force straight arrow');
