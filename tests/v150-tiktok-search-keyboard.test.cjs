const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const media = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt', 'utf8');
const auto = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt', 'utf8');
const search = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarSearchScreen.kt', 'utf8');

const versionCode = Number(gradle.match(/versionCode = (\d+)/)?.[1] || 0);
const versionName = gradle.match(/versionName = "([^"]+)"/)?.[1] || '';
assert.ok(versionCode >= 237);
assert.ok(/^1\.0\./.test(versionName));

// TikTok now stays on the native desktop web experience.
assert.match(tiktok, /DESKTOP_UA/);
assert.match(tiktok, /Chrome\/128\.0\.0\.0 Safari\/537\.36/);
assert.match(tiktok, /"https:\/\/www\.tiktok\.com\/search\?q=" \+ Uri\.encode/);
assert.match(tiktok, /MutationObserver/);
assert.doesNotMatch(tiktok, /ttube-tiktok-video-only/);
assert.doesNotMatch(tiktok, /guestQueue/);

// v1.0.74 search is bound to the selected app owner. A stale rendered
// YouTube frame must not reclaim ownership after the user switches elsewhere.
assert.match(presentation, /searchOverlayMode != "youtube"/);
assert.match(presentation, /currentActiveAppId != "youtube"/);
assert.match(presentation, /CarMediaManager\.activeAppId != "youtube"/);
assert.match(presentation, /isUrlForApp\(youtubeApp, web\.url\)/);
assert.match(presentation, /switchWebApp\(youtubeApp, embedded = isDashboardShowing, startUrl = target\)/);
assert.match(presentation, /YouTubePlayerHelper\.search\(web, q, autoPlayFirst = false\)/);

// Native Android Auto keyboard: page URL equality only applies to DOM-bound web input.
const submitBlock = presentation.match(/onSubmit = \{ value, complete ->[\s\S]*?onCancel =/s)?.[0] || '';
assert.notEqual(submitBlock, '');
assert.match(submitBlock, /CarMediaManager\.activeAppId != owner \|\| currentActiveAppId != owner/);
assert.match(submitBlock, /else if \(mode == "web"\)[\s\S]*?if \(web\.url != page\)/);
assert.match(submitBlock, /it\.currentActiveAppId == owner[\s\S]*?CarMediaManager\.activeAppId == owner/);
assert.doesNotMatch(submitBlock, /youtubeSession/);

// Only one live host gets a native input session; stale screen input is replaced and released.
assert.match(media, /val listener = carNativeSearchListeners\.lastOrNull\(\) \?: return false/);
assert.doesNotMatch(media, /for \(l in carNativeSearchListeners\)/);
assert.match(auto, /nativeInput\?\.takeIf \{ it !== input && !it\.closed \}\?\.cancel\(\)/);
assert.match(auto, /CarSearchScreen\(carContext, input\) \{/);
assert.match(search, /private val onClosed: \(\) -> Unit = \{\}/);
assert.match(search, /onClosed\(\)/);

console.log('PASS keyboard ownership: TikTok native web + strict YouTube submit + native Android Auto keyboard');
