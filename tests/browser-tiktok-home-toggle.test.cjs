const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);
const settings = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt',
  'utf8'
);
const dashboard = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt',
  'utf8'
);
const main = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/MainActivity.kt',
  'utf8'
);

// Browser should occupy the full Android Auto viewport instead of shrinking
// into the left side under a wide/overview initial scale.
assert.match(presentation, /val isBrowser = currentActiveAppId == "web"/);
assert.match(presentation, /val scalePercent = if \(isBrowser\) \{\s*100/);
assert.match(presentation, /web\.settings\.loadWithOverviewMode = !isBrowser/);
assert.match(presentation, /params\.width = ViewGroup\.LayoutParams\.MATCH_PARENT/);
assert.match(presentation, /params\.rightMargin = 0/);
assert.match(presentation, /setPadding\(dp\(4\), dp\(4\), dp\(4\), dp\(4\)\)/);

// TikTok should use the phone's mobile Chrome-like UA, not a hard-coded
// Windows desktop UA that commonly causes an awkward desktop challenge flow.
assert.match(presentation, /app\.id != "tiktok"/);
assert.match(presentation, /getDefaultUserAgent\(context\)/);
assert.match(presentation, /replace\("; wv", ""\)/);
assert.match(presentation, /web\.settings\.loadWithOverviewMode = false/);
assert.doesNotMatch(
  presentation,
  /if \(app\.id == "tiktok"[\s\S]{0,500}Windows NT 10\.0/
);

// Settings must let the user pin/unpin any app from "Tất cả ứng dụng"
// onto the Android Auto dashboard without removing it from the app center.
assert.match(main, /PREF_DASHBOARD_PINNED_APPS = "pref_dashboard_pinned_apps"/);
assert.match(main, /PREF_SHOW_CARD_BOOKMARK = "pref_show_card_bookmark"/);
assert.match(settings, /ỨNG DỤNG TRÊN MÀN HÌNH CHÍNH/);
assert.match(settings, /GHIM ỨNG DỤNG RA DASHBOARD/);
assert.match(settings, /WebAppManager\.getAllApps\(this@SettingsActivity\)\.forEach \{ app ->/);
assert.match(settings, /createWebAppPinRow\(app\)/);
assert.match(settings, /WebAppManager\.isAppPinnedToDashboard\(this@SettingsActivity, app\.id\)/);
assert.match(settings, /WebAppManager\.setAppPinnedToDashboard\(this@SettingsActivity, app\.id, pinned\)/);
assert.match(settings, /MainActivity\.PREF_SHOW_CARD_M3U/);
assert.match(settings, /MainActivity\.PREF_SHOW_CARD_BOOKMARK/);

assert.match(dashboard, /private fun refreshPinnedApps\(\)/);
assert.match(dashboard, /WebAppManager\.getPinnedDashboardApps\(context\)/);
assert.match(dashboard, /pinnedAppsRow/);
assert.match(dashboard, /setOnClickListener \{ onAppClick\(app\) \}/);
assert.match(dashboard, /cardYoutube\.visibility = View\.GONE/);
assert.match(dashboard, /cardVtv\.visibility = View\.GONE/);
assert.match(dashboard, /cardBrowser\.visibility = View\.GONE/);

// "Tất cả ứng dụng" still shows the full app catalog; pinning is a separate concern.
assert.match(presentation, /val apps = WebAppManager\.getAllApps\(context\)/);

console.log('PASS browser/TikTok compatibility + dashboard app pin controls');
