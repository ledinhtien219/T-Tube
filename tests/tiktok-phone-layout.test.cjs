const fs = require('node:fs');
const assert = require('node:assert/strict');

const policy = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt', 'utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const car = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');

assert.match(policy, /private const val PHONE_UA =[sS]*Android 15; Mobile[sS]*Mobile Safari/);
assert.match(policy, /fun configurePhone\(view: WebView, url: String\?\)/);
assert.match(policy, /loadWithOverviewMode = false/);
assert.match(policy, /fun fitPhoneLayout\(view: WebView, url: String\?\)/);
assert.match(policy, /width=device-width, initial-scale=1/);
assert.match(policy, /body\.style\.removeProperty\('zoom'\)/);
assert.match(policy, /body\.style\.removeProperty\('width'\)/);
assert.match(policy, /max-width:100vw !important/);

assert.match(main, /TikTokWebPolicy\.configurePhone\(view, targetUrl\)/);
assert.match(main, /TikTokWebPolicy\.configurePhone\(view, url\)/);
assert.match(main, /TikTokWebPolicy\.fitPhoneLayout\(view, url\)/);
assert.doesNotMatch(main, /TikTokWebPolicy\.configure\(view,/);
assert.doesNotMatch(main, /TikTokWebPolicy\.fitLayout\(view,/);

assert.match(car, /TikTokWebPolicy\.configure\(/);
assert.match(car, /TikTokWebPolicy\.fitLayout\(/);
assert.doesNotMatch(car, /TikTokWebPolicy\.configurePhone\(/);
assert.doesNotMatch(car, /TikTokWebPolicy\.fitPhoneLayout\(/);

console.log('PASS TikTok phone layout: mobile UA + real viewport on phone, desktop policy preserved on Android Auto');
