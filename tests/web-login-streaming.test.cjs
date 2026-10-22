const fs = require('node:fs');
const assert = require('node:assert/strict');

const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt', 'utf8');
const streaming = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/StreamingWebPolicy.kt', 'utf8');
const car = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const phone = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');

// TikTok login must use a browser-like mobile identity and keep the real login UI.
assert.match(tiktok, /POLICY_VERSION = 17/);
assert.match(tiktok, /fun isLoginRoute/);
assert.match(tiktok, /fun isSessionUrl/);
assert.match(tiktok, /accounts\.google\.com/);
assert.match(tiktok, /appleid\.apple\.com/);
assert.match(tiktok, /state\.loginRoute/);
assert.match(tiktok, /if \(state\.loginRoute\) \{[\s\S]*?fitPageWidth\(\);[\s\S]*?return;/);
assert.match(tiktok, /target\.closest\('a\[target="_blank"\]'\)/);
assert.match(tiktok, /location\.assign\(url\)/);

// VTVgo and TV360 must treat service subdomains as the same app/session.
assert.match(streaming, /host == root \|\| host\.endsWith\("\.\$root"\)/);
assert.match(streaming, /"tv360\.vn"/);
assert.match(streaming, /"vtvgo\.vn"/);
assert.match(streaming, /setAcceptThirdPartyCookies\(view, true\)/);
assert.match(streaming, /mixedContentMode = WebSettings\.MIXED_CONTENT_COMPATIBILITY_MODE/);
assert.match(streaming, /window\.__ttubeLoginBridgeInstalled/);
assert.match(streaming, /location\.assign\(url\)/);

// Both projected and phone WebViews must use the service policy.
assert.match(car, /StreamingWebPolicy\.configure\(view, currentActiveAppId, target\)/);
assert.match(car, /"tv360" -> StreamingWebPolicy\.isTv360\(url\)/);
assert.match(car, /"vtvgo" -> StreamingWebPolicy\.isVtvGo\(url\)/);
assert.match(car, /"tiktok" -> TikTokWebPolicy\.isSessionUrl\(url\)/);
assert.match(phone, /"tv360" -> StreamingWebPolicy\.isTv360\(current\)/);
assert.match(phone, /"vtvgo" -> StreamingWebPolicy\.isVtvGo\(current\)/);
assert.match(phone, /StreamingWebPolicy\.onPageFinished\(view, activePhoneWebAppId, url\)/);

// Never let YouTube-specific request blocking affect TikTok/VTVgo/TV360 login or streams.
assert.match(car, /if \(currentActiveAppId != "youtube"\) \{[\s\S]*?return super\.shouldInterceptRequest/);

console.log('PASS TikTok login + VTVgo/TV360 session compatibility + scoped request filtering');
