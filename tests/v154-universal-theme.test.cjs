const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const iptv = fs.readFileSync('app/src/main/assets/iptv_player.html', 'utf8');

const versionCode = Number(gradle.match(/versionCode = (\d+)/)?.[1] || 0);
const versionName = gradle.match(/versionName = "([^"]+)"/)?.[1] || '';
assert.ok(versionCode >= 241);
assert.ok(/^1\.0\./.test(versionName));

// TikTok must no longer be forced to day mode.
assert.match(presentation, /fun applyUniversalWebTheme\(view: WebView\?, isDay: Boolean\)/);
assert.match(presentation, /YouTubePlayerHelper\.applyUniversalWebTheme\(view, isDay\)/);
assert.doesNotMatch(presentation, /TikTokWebPolicy\.isTikTok\(view\?\.url\)\) true else isDay/);

// Theme follows SPA/history updates and re-applies after framework rerenders.
assert.match(presentation, /doUpdateVisitedHistory[\s\S]*?applyUniversalWebTheme\(view, isDayMode\(\)\)/);
assert.match(presentation, /onPageCommitVisible[\s\S]*?applyUniversalWebTheme\(view, isDayMode\(\)\)/);
assert.match(presentation, /postDelayed\(\{ if \(!isDashboardShowing\) syncWebTheme\(\) \}, 650L\)/);

// Generic enabled web apps get explicit color-scheme plus luminance mismatch fallback.
assert.match(helper, /carhud-theme-invert/);
assert.match(helper, /pageLuminance/);
assert.match(helper, /var mismatch = isDay \? lum < 0\.38 : lum > 0\.62/);
assert.match(helper, /MutationObserver/);
assert.match(helper, /view\.settings\.forceDark = if \(effectiveIsDay\)/);
assert.match(helper, /isAlgorithmicDarkeningAllowed = !effectiveIsDay/);

// YouTube and IPTV remain specialized but use the same selected mode.
assert.match(helper, /if \(url\.contains\("youtube\.com", true\)/);
assert.match(helper, /window\.setIptvTheme\(isDay\)/);
assert.match(main, /applyUniversalWebTheme\(w, SettingsActivity\.resolveIsDay\(this\)\)/);
assert.match(main, /initIptvWebView[\s\S]*?applyUniversalWebTheme\(view, SettingsActivity\.resolveIsDay\(this@MainActivity\)\)/);
assert.match(iptv, /window\.__carhudCurrentIsDay/);

console.log('PASS universal theme: all enabled web apps follow the selected day/night mode');
