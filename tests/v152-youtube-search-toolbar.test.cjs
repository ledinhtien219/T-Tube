const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');

const versionCode = Number(gradle.match(/versionCode = (\d+)/)?.[1] || 0);
const versionName = gradle.match(/versionName = "([^"]+)"/)?.[1] || '';
assert.ok(versionCode >= 239);
assert.ok(/^1\.0\./.test(versionName));

// Manual search hides the custom empty-home panel and navigates directly to
// explicit results. Any fallback must be delayed and must not cancel an in-flight load.
assert.match(helper, /document\.getElementById\('carhud-search-home'\)/);
assert.match(helper, /view\.loadUrl\(targetUrl\)/);
assert.match(helper, /searchGeneration\.get\(\) != generation/);
assert.match(helper, /now\.contains\("\/results"/);
assert.match(helper, /view\.progress >= 95/);
assert.match(helper, /3000L/);
assert.doesNotMatch(helper.match(/fun search\(view: WebView\?, query: String,[\s\S]*?fun cancelSearchAutoPlay/)?.[0] || '', /view\.stopLoading\(\)/);

// YouTube toolbar is a compact floating dark rail, not a white page-like strip.
assert.match(presentation, /private fun buildYouTubeMediaToolbar/);
assert.match(presentation, /Color\.parseColor\("#E60B1118"\)/);
assert.match(presentation, /Gravity\.START or Gravity\.CENTER_VERTICAL/);
assert.match(presentation, /Phát \/ Dừng/);
assert.doesNotMatch(presentation.match(/private fun buildYouTubeMediaToolbar[\s\S]*?private val autoHideHandler/)?.[0] || '', /#F6FFFFFF/);

console.log('PASS v1.0.52: deterministic YouTube search + compact dark media toolbar');
