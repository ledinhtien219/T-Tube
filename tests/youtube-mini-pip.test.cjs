const assert = require('node:assert/strict');
const fs = require('node:fs');

const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt','utf8');
const car = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');
const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const jsMatch = helper.match(/val JS_CLEANUP_AND_ADBLOCK: String = """([\s\S]*?)"""\.trimIndent\(\)/);
assert.ok(jsMatch, 'Missing YouTube injected script');
const js = jsMatch[1];
new Function(js);

assert.match(gradle,/versionCode = 303/);
assert.match(gradle,/versionName = "1\.1\.5"/);
assert.match(js,/window\.__carhudSetPipMode = function\(enabled\)/);
assert.match(js,/window\.__carhudPipMode = newMode/);
assert.match(js,/window\.__carhudBrowseMode = newMode/);
assert.match(js,/window\.__carhudUpdateWatchLayout = updateWatchLayout/);
assert.match(js,/var pip = watch && !!window\.__carhudPipMode/);
assert.match(js,/var browsing = watch && \(!!window\.__carhudBrowseMode \|\| pip\)/);
assert.match(js,/html\.classList\.toggle\('carhud-youtube-pip', pip\)/);
assert.match(js,/data-carhud-pip-host/);
assert.match(js,/function pinMiniPlayerHost\(\)/);
assert.match(js,/function unpinMiniPlayerHost\(\)/);
assert.match(js,/pinMiniPlayerHost\(\)/);
assert.match(js,/position:fixed!important;top:auto!important;left:auto!important;right:14px!important;bottom:14px!important/);
assert.match(js,/html\.carhud-youtube-pip \[data-carhud-pip-host="1"\] video/);
assert.match(js,/video\.closest\('#player-container-id'\)/);
assert.match(js,/document\.addEventListener\('touchstart'/);
assert.match(js,/document\.addEventListener\('touchend'/);
assert.match(js,/dy > 72 && dy > Math\.abs\(dx\) \* 1\.4/);
assert.match(js,/Date\.now\(\) - started\.time < 1200/);
assert.match(js,/window\.__carhudMiniIgnoreClickUntil/);
assert.match(js,/window\.__carhudMiniPlayerHost\.contains\(target\)/);
assert.match(js,/window\.__carhudSetPipMode\(false\)/);
assert.match(js,/window\.AndroidVoice\.onYouTubeMiniPlayerChanged\(newMode\)/);
assert.match(js,/if \(enabled && window\.location\.pathname\.indexOf\('\/watch'\) !== 0\) return/);
assert.match(js,/if \(pip\) pinMiniPlayerHost\(\)/);
assert.match(js,/if \(!pip && window\.__carhudMiniPlayerHost\) unpinMiniPlayerHost\(\)/);

// The player is not cloned/reparented or replaced; selecting other videos is
// delegated to native YouTube cards in the same WebView.
const section = js.slice(js.indexOf('function unpinMiniPlayerHost()'),js.indexOf('function handleWatchPage()'));
assert.doesNotMatch(section,/document\.createElement\(['"]video['"]\)/);
assert.doesNotMatch(section,/appendChild\(video\)|replaceChild\(/);
assert.doesNotMatch(section,/location\.(href|assign|replace)\s*=/);
assert.doesNotMatch(section,/window\.open\(/);

assert.match(car,/@JavascriptInterface\s+fun onYouTubeMiniPlayerChanged\(enabled: Boolean\)/);
assert.match(car,/mainHandler\.post \{\s+if \(currentActiveAppId == "youtube"\)/);
assert.match(car,/YouTubePlayerHelper\.setMiniPlayerMode\(web, enabled\)/);
assert.match(car,/YouTubePlayerHelper\.setMiniPlayerMode\(view, prefs\.getBoolean\(youtubeBrowsePref, false\)\)/);
assert.match(car,/updateYouTubeMiniPlayerUi\(enabled\)/);
assert.match(car,/if \(!enabled && currentActiveAppId == "youtube" && isYouTubeVideoPage\(\)\)/);
assert.match(car,/browseToggle = toolButton\([\s\S]*?R\.drawable\.ic_bar_minimize,\s+null/);
assert.match(car,/browseToggle\.isFocusable = true/);
console.log('PASS: swipe-down Mini PiP, native scrolling behind, tap-to-expand, no second player, icon-only');
