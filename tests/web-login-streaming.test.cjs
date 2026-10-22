const fs = require('node:fs');
const assert = require('node:assert/strict');

const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt', 'utf8');
const streaming = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/StreamingWebPolicy.kt', 'utf8');
const car = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const phone = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');

// TikTok login uses TikTok's own web auth. Embedded Google OAuth is replaced
// before navigation with the official TikTok QR flow so cookies stay in WebView.
assert.match(tiktok, /POLICY_VERSION = 18/);
assert.match(tiktok, /fun isLoginRoute/);
assert.match(tiktok, /fun isSessionUrl/);
assert.match(tiktok, /accounts\.google\.com/);
assert.match(tiktok, /appleid\.apple\.com/);
assert.match(tiktok, /state\.loginRoute/);
assert.match(tiktok, /state\.qrLoginUrl/);
assert.match(tiktok, /ttube-tiktok-login-helper/);
assert.match(tiktok, /Đăng nhập QR/);
assert.match(tiktok, /text\.indexOf\('google'\) >= 0/);
assert.match(tiktok, /stopImmediatePropagation/);
assert.match(tiktok, /location\.assign\(state\.qrLoginUrl\)/);
assert.match(tiktok, /Never scale the QR\/auth UI/);
assert.match(tiktok, /fun isGoogleOAuth/);
assert.match(tiktok, /accounts\.google\.com/);
assert.match(tiktok, /oauth2\.googleapis\.com/);
assert.match(tiktok, /fun qrLoginUrl\(\): String = QR_LOGIN_URL/);
assert.match(tiktok, /QR_LOGIN_URL = "https:\/\/in\.tiktok\.com\/login\/qrcode"/);
assert.match(car, /currentActiveAppId == "tiktok" && TikTokWebPolicy\.isGoogleOAuth\(target\)/);
assert.match(phone, /activePhoneWebAppId == "tiktok" && TikTokWebPolicy\.isGoogleOAuth\(target\)/);
assert.match(car, /view\.loadUrl\(TikTokWebPolicy\.qrLoginUrl\(\)\)/);
assert.match(phone, /view\.loadUrl\(TikTokWebPolicy\.qrLoginUrl\(\)\)/);
assert.match(car, /Google không cho đăng nhập trong WebView/);
assert.match(phone, /Google không cho đăng nhập trong WebView/);
assert.match(helper, /TikTokWebPolicy\.isTikTok\(pageUrl\)/);
assert.match(helper, /StreamingWebPolicy\.isTv360\(pageUrl\)/);
assert.match(helper, /StreamingWebPolicy\.isVtvGo\(pageUrl\)/);

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

console.log('PASS TikTok QR auth fallback + VTVgo/TV360 session compatibility + scoped request filtering');
