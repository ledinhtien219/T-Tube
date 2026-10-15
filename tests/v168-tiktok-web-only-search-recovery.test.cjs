const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const car = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');

assert.match(gradle, /versionCode = 252/);

// Black-search recovery: alternate TikTok route + software compositor + web fallback.
assert.match(tiktok, /search\/video\?q=/);
assert.match(tiktok, /alternateSearchUrl/);
assert.match(tiktok, /externalFallbackSearchUrl/);
assert.match(tiktok, /View\.LAYER_TYPE_SOFTWARE/);
assert.match(tiktok, /fun recoverSearchIfBlank/);
assert.match(main, /TikTokWebPolicy\.recoverSearchIfBlank\(view, url\)/);
assert.match(car, /TikTokWebPolicy\.recoverSearchIfBlank\(view, url\)/);

// Web-only: app/store deep links never leave WebView.
assert.match(tiktok, /fun isAppRedirect/);
assert.match(tiktok, /startsWith\("tiktok:"\)/);
assert.match(tiktok, /startsWith\("tiktoklite:"\)/);
assert.match(main, /TikTokWebPolicy\.isAppRedirect\(target\)/);
assert.match(car, /TikTokWebPolicy\.isAppRedirect\(target\)/);

// App-only bot wall is suppressed, while real web login remains allowed.
assert.match(tiktok, /confirm you're not a bot/);
assert.match(tiktok, /continue with tiktok app/);
assert.match(tiktok, /var appPromo = looksLikeAppPromoWall\(el\);[\s\S]*?if \(appPromo\)/);
assert.match(tiktok, /Real web login forms remain available/);

console.log('PASS v1.0.63 build 252: TikTok web-only wall suppression + search recovery');
