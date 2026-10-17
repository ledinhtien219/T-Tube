const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);
const screen = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt',
  'utf8'
);

// Screen auto-detect must actually affect layout placement, not just read a preference.
assert.match(
  presentation,
  /val autoDetect = prefs\.getBoolean\(SettingsActivity\.KEY_AUTO_DETECT_SCREEN, true\)/
);
assert.match(
  presentation,
  /val positionKey = if \(autoDetect\)[\s\S]*?isPortrait -> "bottom"[\s\S]*?isUltrawide -> "left"[\s\S]*?else -> "bottom"/
);
assert.match(
  presentation,
  /val webAppPos = if \(autoDetect\)[\s\S]*?if \(isPortrait\) "top" else "left"/
);
assert.match(
  presentation,
  /key == SettingsActivity\.KEY_AUTO_DETECT_SCREEN[\s\S]*?applyEffectiveViewportFromRoot\(force = true\)/
);

// Empty non-loading Pane is illegal. Use a real zero-width row instead.
assert.match(screen, /val invisiblePane = Pane\.Builder\(\)/);
assert.match(screen, /addRow\([\s\S]*?Row\.Builder\(\)[\s\S]*?setTitle\("\\u200B"\)/);
assert.doesNotMatch(screen, /setPane\(Pane\.Builder\(\)\.build\(\)\)/);
assert.doesNotMatch(screen, /Pane\.Builder\(\)\.setLoading\(false\)/);

// IPTV viewport repair must happen AFTER its restore helpers, otherwise those
// helpers can shrink the player back into the top-left corner.
const iptvBlock =
  presentation.match(/if \(url\.contains\("iptv_player\.html"\)\) \{[\s\S]*?\} else if \(isYouTube\)/)?.[0] || '';
const restoreIndex = iptvBlock.indexOf('restoreVideoAspectRatio');
const viewportIndex = iptvBlock.indexOf('forceIptvViewportFit(view)');
assert.ok(restoreIndex >= 0, 'IPTV restore helper missing');
assert.ok(viewportIndex > restoreIndex, 'IPTV viewport fit must run after aspect restore');
assert.match(presentation, /onPageCommitVisible[\s\S]*?iptv_player\.html[\s\S]*?forceIptvViewportFit/);

console.log('PASS v1.0.70 screen auto-detect + valid full-surface pane + IPTV viewport repair');
