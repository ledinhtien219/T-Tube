const fs = require('node:fs');
const assert = require('node:assert/strict');

const helper = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/TikTokPlayerHelper.kt',
  'utf8'
);
const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);

assert.match(helper, /object TikTokPlayerHelper/);
assert.match(helper, /scroll-snap-type/);
assert.match(helper, /height:100vh/);
assert.match(helper, /object-fit:contain/);
assert.match(helper, /managePlayback/);
assert.match(helper, /window\.__thtvTikTok/);
assert.match(helper, /fun scrollBy/);
assert.match(helper, /fun fling/);

assert.match(presentation, /isTikTok -> 100/);
assert.match(presentation, /loadWithOverviewMode = !isTikTok/);
assert.match(presentation, /TikTokPlayerHelper\.install\(view\)/);
assert.match(presentation, /currentActiveAppId == "tiktok"[\s\S]*?TikTokPlayerHelper\.scrollBy/);
assert.match(presentation, /currentActiveAppId == "tiktok"[\s\S]*?TikTokPlayerHelper\.fling/);
assert.match(presentation, /if \(currentActiveAppId == "tiktok"\) 2500L else 10000L/);

console.log('PASS TikTok fullscreen: true-scale viewport + snap feed + projected gestures');
