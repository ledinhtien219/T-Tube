const fs = require('node:fs');
const assert = require('node:assert/strict');

const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt','utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt','utf8');

// Android Auto YouTube must obey Day/Night/Auto instead of forcing car screens dark.
const themeBlock = helper.slice(helper.indexOf('    fun applyTheme('), helper.indexOf('    fun inject('));
assert.match(themeBlock, /if \(isDay\) \{/);
assert.doesNotMatch(themeBlock, /if \(isDay && !isAutoScreen\)/);
assert.match(themeBlock, /\.carhud-auto-screen[^']*background: #ffffff/);
assert.match(themeBlock, /\.carhud-auto-screen \.compact-media-item-headline[^']*#0f0f0f/);
assert.match(helper, /screenInitJs adds carhud-auto-screen[\s\S]*?applyTheme\(view, isDay\)/);

// Standalone web browser stays light regardless of the media theme.
assert.match(presentation, /private fun effectiveWebThemeIsDay/);
assert.match(presentation, /currentActiveAppId == "web"[\s\S]*?true/);
assert.match(presentation, /Browser chrome is always light[\s\S]*?val day = true/);
assert.match(presentation, /applyUniversalWebTheme\(view, effectiveWebThemeIsDay\(url\)\)/);
assert.match(presentation, /web\.setBackgroundColor\(if \(effectiveWebThemeIsDay\(web\.url\)\) Color\.WHITE else Color\.BLACK\)/);

// TikTok search uses the standard desktop tiktok.com route and does not
// inject feed/search layout rewrites.
assert.match(tiktok, /"https:\/\/www\.tiktok\.com\/search\?q=" \+ Uri\.encode/);
assert.doesNotMatch(tiktok, /search\/video\?q=/);
assert.doesNotMatch(tiktok, /ttube-tiktok-search/);
assert.doesNotMatch(tiktok, /DivMainNavContainer/);

console.log('PASS AA YouTube theme + always-light browser + TikTok native desktop search');
