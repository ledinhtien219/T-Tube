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
assert.match(tiktok, /try out more features/);
assert.match(tiktok, /open tiktok lite/);
assert.match(tiktok, /watch your favorite videos with less data/);
assert.match(tiktok, /candidateList/);
assert.match(tiktok, /button, a, \[role="button"\]/);
assert.match(tiktok, /characterData: true/);
assert.match(tiktok, /attributeFilter: \['class', 'style', 'aria-hidden'\]/);
assert.doesNotMatch(tiktok, /setInterval\s*\(/);

console.log('PASS mini-player tab scope + delayed TikTok Lite popup suppression');
