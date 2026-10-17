const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const car = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');
const apps = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WebAppModel.kt','utf8');

assert.ok(Number(gradle.match(/versionCode = (\d+)/)?.[1]) >= 261);

// Native tiktok.com desktop web owns feed, scrolling, video navigation and search.
assert.match(tiktok, /POLICY_VERSION = 15/);
assert.match(tiktok, /DESKTOP_UA/);
assert.match(tiktok, /Chrome\/128\.0\.0\.0 Safari\/537\.36/);
assert.match(tiktok, /"https:\/\/www\.tiktok\.com\/search\?q=" \+ Uri\.encode/);
assert.doesNotMatch(tiktok, /search\/video\?q=/);
assert.doesNotMatch(tiktok, /externalFallbackSearchUrl/);
assert.doesNotMatch(tiktok, /LAYER_TYPE_SOFTWARE/);
assert.doesNotMatch(tiktok, /guestQueue/);
assert.doesNotMatch(tiktok, /ttube-tiktok-video-only/);

// TikTok stays inside WebView and native app/store redirects are blocked.
assert.match(tiktok, /fun isAppRedirect/);
assert.match(tiktok, /startsWith\("tiktok:"\)/);
assert.match(tiktok, /startsWith\("tiktoklite:"\)/);
assert.match(main, /TikTokWebPolicy\.isAppRedirect\(target\)/);
assert.match(car, /TikTokWebPolicy\.isAppRedirect\(target\)/);

// App-only prompts are suppressed without touching the real web login UI.
assert.match(tiktok, /confirm you're not a bot/);
assert.match(tiktok, /continue with tiktok app/);
assert.match(tiktok, /function isAppOnlyWall/);
assert.match(tiktok, /data-ttube-app-promo-wall/);
assert.doesNotMatch(tiktok, /data-ttube-login-wall/);

// Opening TikTok starts from a clean tiktok.com home while cookies remain available.
assert.match(apps, /url = "https:\/\/www\.tiktok\.com\/"[\s\S]*?isDesktop = true/);
// Reopening a native TikTok feed/search must keep that page instead of loading home again.
assert.match(main, /"tiktok" -> TikTokWebPolicy\.isTikTok\(current\)/);
assert.match(car, /app\.id != "tiktok"[\s\S]*?CarMediaManager\.hasWebAppSession/);

console.log('PASS v1.0.70 build 261: native TikTok desktop web + clean home + web-only redirects');
