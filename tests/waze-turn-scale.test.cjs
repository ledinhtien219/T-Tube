const fs = require('node:fs');
const assert = require('node:assert/strict');

const overlay = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/VietmapHudOverlay.kt', 'utf8');
const hlp = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WazeHlpWebSocketManager.kt', 'utf8');

assert.match(
  overlay,
  /7 -> \{[\s\S]*projectionViewportWidthPx \/ 1280f[\s\S]*1\.55f \* viewportFactor \* \(configuredScale \/ 100f\)[\s\S]*\}/
);
assert.match(overlay, /private fun buildStyle7WazeThanhNgang\(\)[\s\S]*minimumWidth = 0/);
assert.match(overlay, /val size = dp\(36\)/);
assert.match(overlay, /LayoutParams\(dp\(68\), dp\(38\)\)/);
assert.match(overlay, /minimumWidth = dp\(70\)[\s\S]*LayoutParams\(LayoutParams\.WRAP_CONTENT, dp\(38\)\)/);
assert.match(overlay, /createReferenceAlertCell\(dp\(48\), dp\(40\), compact = true\)/);
assert.match(overlay, /dp\(if \(compact\) 22 else 24\)/);

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

console.log('PASS Waze turn + scale: style #7 compacts cleanly, preserves proportions, and stale maneuver state cannot force straight arrow');
