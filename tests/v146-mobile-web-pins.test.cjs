const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const manager = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WebAppModel.kt', 'utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt', 'utf8');
const dashboard = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt', 'utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const screen = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt', 'utf8');

// Android Auto uses the working Fermata-style CarActivity projection path,
// not AndroidX NavigationTemplate/MapTemplate as the runtime entry.
const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const projectionHost = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt',
  'utf8'
);
const descriptor = fs.readFileSync('app/src/main/res/xml/automotive_app_desc.xml', 'utf8');

assert.match(manifest, /CarHudProjectionService/);
assert.match(manifest, /CATEGORY_PROJECTION/);
assert.doesNotMatch(manifest, /CarHudAutoService/);
assert.match(descriptor, /name="service"/);
assert.match(descriptor, /name="projection"/);
assert.match(projectionHost, /CarActivityService/);
assert.match(projectionHost, /CarActivity\(\)/);
assert.match(projectionHost, /SurfaceView\(this\)/);
assert.match(projectionHost, /CarPresentation\(this, vd\.display, persistentWeb\)/);
assert.match(projectionHost, /hideAppHeader\(\)/);
assert.match(projectionHost, /hideMenuButton\(\)/);

// Projected WebViews use the full car surface at native 100% scale.
assert.match(presentation, /web\.setInitialScale\(100\)/);
assert.match(presentation, /web\.settings\.loadWithOverviewMode = false/);
assert.match(presentation, /params\.width = ViewGroup\.LayoutParams\.MATCH_PARENT/);
assert.match(presentation, /params\.rightMargin = 0/);
assert.doesNotMatch(presentation, /effectiveCarDpi[\s\S]*?effectivePhoneDpi[\s\S]*?scalePercent/);
assert.doesNotMatch(presentation, /effectiveCarDpi \/ effectivePhoneDpi/);

// TikTok uses a consistent desktop identity. v1.0.74 hard-isolates all
// app ownership instead of special-casing TikTok restore logic.
assert.match(presentation, /val canRestoreSession = false/);
assert.match(presentation, /TikTokWebPolicy\.configure\(web, startUrl \?: app\.url\)/);
assert.match(manager, /url = "https:\/\/www\.tiktok\.com\/"[\s\S]*?isDesktop = true/);

// Per-app dashboard pin state is separate from app availability.
assert.match(main, /PREF_DASHBOARD_PINNED_APPS = "pref_dashboard_pinned_apps"/);
assert.match(manager, /DEFAULT_DASHBOARD_PINNED_IDS = setOf\("youtube", "iptv", "web"\)/);
assert.match(manager, /fun setAppPinnedToDashboard\(/);
assert.match(manager, /fun getPinnedDashboardApps\(/);
assert.match(settings, /ỨNG DỤNG TRÊN MÀN HÌNH CHÍNH/);
assert.match(settings, /WebAppManager\.getAllApps\(this@SettingsActivity\)\.forEach \{ app ->/);
assert.match(settings, /WebAppManager\.setAppPinnedToDashboard\(this@SettingsActivity, app\.id, pinned\)/);
assert.match(dashboard, /private fun refreshPinnedApps\(\)/);
assert.match(dashboard, /WebAppManager\.getPinnedDashboardApps\(context\)/);
assert.match(dashboard, /setOnClickListener \{ onAppClick\(app\) \}/);

console.log('PASS legacy AA host + TikTok desktop-web/browser fixes + dashboard pin switches under hard ownership');
