const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);
const tiktok = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt',
  'utf8'
);
const helper = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt',
  'utf8'
);
const main = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/MainActivity.kt',
  'utf8'
);
const layout = fs.readFileSync(
  'app/src/main/res/layout/activity_main.xml',
  'utf8'
);

// TikTok owns its toolbar search by selected app ownership. Stale WebView
// pixels/URLs must never reclassify the active app during a transition.
assert.match(presentation, /return currentActiveAppId !in setOf\("youtube", "iptv", "tiktok"\)/);
assert.match(presentation, /val tiktokActive = currentActiveAppId == "tiktok"/);
assert.match(presentation, /searchOverlayMode = if \(tiktokActive\) "tiktok" else "youtube"/);

// TikTok search is left to the real desktop website.
assert.match(tiktok, /"https:\/\/www\.tiktok\.com\/search\?q=" \+ Uri\.encode/);
assert.match(tiktok, /DESKTOP_UA/);
assert.doesNotMatch(tiktok, /searchClickHandlerVersion/);
assert.doesNotMatch(tiktok, /location\.assign\(canonical\)/);
assert.doesNotMatch(tiktok, /ttube-tiktok-video-only/);

// Generic browser day/night inversion must seed the pre-filter background with
// the opposite tone so the rendered result lands on the requested theme.
assert.match(helper, /var renderedBg = mismatch/);
assert.match(helper, /\? \(isDay \? '#000000' : '#ffffff'\)/);
assert.match(helper, /document\.elementFromPoint/);

// Phone day mode uses stable card IDs; legacy quick-card toggles are no longer shown.
assert.match(layout, /android:id="@\+id\/phoneDisplayToggleCard"/);
assert.match(layout, /android:id="@\+id\/phoneThemeCard"/);
for (const id of ['switchShowCardYoutube','switchShowCardIptv','switchShowCardM3u','switchShowCardBrowser']) {
  const pos = layout.indexOf('android:id="@+id/' + id + '"');
  assert.ok(pos >= 0, id + ' missing');
  const rowStart = layout.lastIndexOf('<LinearLayout', pos);
  const rowOpenEnd = layout.indexOf('>', rowStart);
  assert.match(layout.slice(rowStart, rowOpenEnd + 1), /android:visibility="gone"/, id + ' row should be hidden');
}
assert.match(main, /R\.id\.phoneDisplayToggleCard/);
assert.match(main, /R\.id\.phoneThemeCard/);
assert.match(main, /stylePhoneDynamicCard\(itemView\)/);

console.log('PASS browser theme + owner-bound TikTok search + phone day-mode regressions');
