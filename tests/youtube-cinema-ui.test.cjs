const fs = require('node:fs');
const assert = require('node:assert/strict');

const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');
const car = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const block = helper.match(/val JS_CLEANUP_AND_ADBLOCK: String = """([\s\S]*?)"""\.trimIndent\(\)/);
assert.ok(block, 'Injected Youtube Javascript exists');
const js=block[1];
new Function(js);

// Preserve Android WebView video surfaces: don't force video to an absolute
// GPU layer during PiP or duplicate the video/player node.
const pipVideoRule=js.match(/'html\.carhud-youtube-pip \[data-carhud-pip-host="1"\] video \{([^']+)\} '/);
assert.ok(pipVideoRule, 'PiP video style exists');
assert.doesNotMatch(pipVideoRule[1],/position:(fixed|absolute)/);
assert.match(pipVideoRule[1],/object-fit:contain/);
assert.match(js,/video\.closest\('ytd-player'\)/);
assert.match(js,/video\.closest\('#player-container-outer'\)/);
assert.match(js,/function pinMiniPlayerHost\(\)/);
assert.match(js,/data-carhud-pip-host/);
assert.match(js,/function scheduleMiniPlayerVideoCheck\(\)/);
assert.match(js,/video\.videoWidth > 0 && video\.videoHeight > 0/);
assert.match(js,/window\.__carhudSetPipMode\(false\)/);

// Test the actual outer-host choice rather than checking an isolated CSS string.
const from=js.indexOf('function unpinMiniPlayerHost()');
const to=js.indexOf('var carhudRecommendationRoot = null;',from);
assert.ok(from>0 && to>from);
const routines=js.slice(from,to);
const marked=[];
const outer={setAttribute(name,value){marked.push([name,value]);},removeAttribute(){}};
const inner={setAttribute(){throw Error('Wrong: pinned inner YouTube movie player');}};
const video={
 closest(selector){
  if(selector==='ytd-player')return outer;
  if(selector==='#movie_player')return inner;
  return null;
 }
};
const hostApi=new Function('getMainVideo','document','window',
 routines+'\nreturn {pinMiniPlayerHost,unpinMiniPlayerHost};'
)(()=>video,{body:{},documentElement:{}},{});
hostApi.pinMiniPlayerHost();
assert.deepEqual(marked,[['data-carhud-pip-host','1']]);

// Cinema browsing should show several smaller recommendations rather than
// two 500px-wide desktop cards; preserve the native clickable YouTube links.
assert.match(js,/grid-template-columns:repeat\(auto-fill,minmax\(205px,1fr\)\)/);
assert.match(js,/ytd-watch-next-secondary-results-renderer #items/);
assert.match(js,/pointer-events:auto!important/);

// Seven compact icon-only buttons have accessible names for Mazda/TalkBack.
const toolbar=car.slice(car.indexOf('private fun buildYouTubeMediaToolbar('),car.indexOf('private val autoHideHandler',car.indexOf('private fun buildYouTubeMediaToolbar(')));
assert.match(toolbar,/val btnWidth = \(\(48 \* scale\) \/ 100\)\.coerceIn\(43, 56\)/);
assert.match(toolbar,/contentDescription = label \?: "Chế độ xem YouTube"/);
assert.doesNotMatch(toolbar,/addView\(TextView\(context\)\.apply \{\s*text = label/);
assert.match(car,/if \(prefs\.getBoolean\(youtubeBrowsePref, false\)\) return false/);
assert.match(car,/if \(enabled\) hideYouTubeSeekOverlayImmediately\(\)/);
assert.match(car,/Color\.parseColor\("#67E8F9"\)/);
console.log('PASS: native video host, no forced GPU plane, graceful fallback, compact Cinema cards/controls');
