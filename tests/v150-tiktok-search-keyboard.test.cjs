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

// TikTok video detail pages must be video-only, while feed/search remains intact.
assert.match(tiktok, /\/video\\\/\\d\+/);
assert.match(tiktok, /ttube-tiktok-video-only/);
assert.match(tiktok, /search-comment-container/);
assert.match(tiktok, /browse-video-desc/);
assert.match(tiktok, /video\[data-ttube-video-only="1"\]/);
assert.match(tiktok, /object-fit: contain/);
assert.match(tiktok, /MutationObserver/);

// THTV keyboard search may not silently fail on a stale currentActiveAppId.
assert.match(presentation, /val youtubeVisible = currentUrl\.contains\("youtube\.com"/);
assert.match(presentation, /searchOverlayMode != "youtube"/);
assert.match(presentation, /currentActiveAppId = "youtube"/);
assert.match(presentation, /CarMediaManager\.activeAppId = "youtube"/);
assert.match(presentation, /YouTubePlayerHelper\.search\(web, q, autoPlayFirst = false\)/);

// Native Android Auto keyboard: page URL equality only applies to DOM-bound web input.
const submitBlock = presentation.match(/onSubmit = \{ value, complete ->[\s\S]*?onCancel =/s)?.[0] || '';
assert.notEqual(submitBlock, '');
assert.match(submitBlock, /val youtubeSession = mode == \"youtube\"/);
assert.match(submitBlock, /CarMediaManager\.activeAppId != owner && !youtubeSession/);
assert.match(submitBlock, /else if \(mode == "web"\)[\s\S]*?if \(web\.url != page\)/);
assert.doesNotMatch(submitBlock, /CarMediaManager\.activeAppId != owner \|\| web\.url != page/);

// Only one live host gets a native input session; stale screen input is replaced and released.
assert.match(media, /val listener = carNativeSearchListeners\.lastOrNull\(\) \?: return false/);
assert.doesNotMatch(media, /for \(l in carNativeSearchListeners\)/);
assert.match(auto, /nativeInput\?\.takeIf \{ it !== input && !it\.closed \}\?\.cancel\(\)/);
assert.match(auto, /CarSearchScreen\(carContext, input\) \{/);
assert.match(search, /private val onClosed: \(\) -> Unit = \{\}/);
assert.match(search, /onClosed\(\)/);

console.log('PASS v1.0.50: TikTok video-only + YouTube submit + native Android Auto keyboard');
