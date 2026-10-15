const fs = require('node:fs');
const assert = require('node:assert/strict');

const main = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/MainActivity.kt',
  'utf8'
);
const tiktok = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt',
  'utf8'
);

// Phone mini-player should stay off Home/Settings/YouTube and only appear on IPTV.
assert.match(
  main,
  /phoneMiniPlayer\.visibility = if \(tab == TAB_IPTV && prefs\.getBoolean\(PREF_SHOW_MINI_PLAYER, true\)\) View\.VISIBLE else View\.GONE/
);
assert.match(
  main,
  /phoneMiniPlayer\.visibility = if \(it && activeTab == TAB_IPTV\) View\.VISIBLE else View\.GONE/
);
assert.match(
  main,
  /phoneMiniPlayer\.visibility = if \(activeTab == TAB_IPTV && prefs\.getBoolean\(PREF_SHOW_MINI_PLAYER, true\)\) View\.VISIBLE else View\.GONE/
);

// Delayed TikTok Lite promo overlays must be found even when mounted via anonymous portals.
assert.match(tiktok, /continue with tiktok app/);
assert.match(tiktok, /open tiktok lite/);
assert.match(tiktok, /function isAppOnlyWall/);
assert.match(tiktok, /candidates/);
assert.match(tiktok, /button, a, \[role="button"\]/);
assert.match(tiktok, /childList: true/);
assert.match(tiktok, /subtree: true/);
assert.doesNotMatch(tiktok, /attributeFilter/);
assert.doesNotMatch(tiktok, /setInterval\s*\(/);

console.log('PASS mini-player tab scope + delayed TikTok Lite popup suppression');
