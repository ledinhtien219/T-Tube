const fs = require('node:fs');
const assert = require('node:assert/strict');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');
const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt','utf8');
const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const min = fs.readFileSync('app/src/main/res/drawable/ic_bar_minimize.xml','utf8');
const max = fs.readFileSync('app/src/main/res/drawable/ic_bar_expand.xml','utf8');

assert.match(gradle, /versionCode = 305/);
assert.match(gradle, /versionName = "1\.1\.7"/);
assert.match(presentation, /fun toolButton\(iconRes: Int, label: String\?,/);
assert.match(presentation, /if \(label != null\)/);
assert.match(presentation, /val browseToggle = toolButton\([\s\S]*?R\.drawable\.ic_bar_minimize,[\s\S]*?null[\s\S]*?toggleYouTubeBrowseMode\(\)/);
assert.match(presentation, /browseToggle\.isFocusable = true/);
assert.match(presentation, /YouTubePlayerHelper\.setMiniPlayerMode\(web, enabled\)/);
assert.match(presentation, /YouTubePlayerHelper\.setMiniPlayerMode\(view, prefs\.getBoolean\(youtubeBrowsePref, false\)\)/);
assert.match(helper, /window\.__carhudSetBrowseMode = function\(enabled\)/);
assert.match(helper, /window\.__carhudUpdateWatchLayout = updateWatchLayout/);
assert.match(helper, /var browsing = watch && \(!!window\.__carhudBrowseMode \|\| pip\)/);
assert.match(helper, /if \(browsing \|\| !watch\)/);
assert.match(helper, /releaseVideoFill\(\)/);
assert.match(helper, /if \(browsing\) releaseInlineVideoStyle\(\)/);
assert.match(helper, /sessionStorage\.setItem/);
assert.match(helper, /if \(window\.__carhudBrowseMode\) return;/);
assert.match(helper, /carhud-youtube-browse/);
assert.match(helper, /html\.carhud-youtube-browse \.watch-below-the-fold/);
assert.doesNotMatch(presentation, /toggleYouTubeBrowseMode\([\s\S]{0,260}loadUrl\(/);
assert.match(min, /android:pathData=/);
assert.match(max, /android:pathData=/);

// Validate the embedded JS parser before loading it into the car WebView.
const block = helper.match(/val JS_CLEANUP_AND_ADBLOCK: String = """([\s\S]*?)"""\.trimIndent\(\)/);
assert.ok(block, 'YouTube script not found');
new Function(block[1]);
console.log('PASS: icon-only Mini PiP control retains native YouTube scroll and player');
