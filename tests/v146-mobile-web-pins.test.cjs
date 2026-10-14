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

// Browser uses the full projected viewport at normal CSS scale.
assert.match(presentation, /val isBrowser = currentActiveAppId == "web"/);
assert.match(presentation, /val scalePercent = if \(isBrowser\) \{\s*100/);
assert.match(presentation, /web\.settings\.loadWithOverviewMode = !isBrowser/);
assert.match(presentation, /params\.width = ViewGroup\.LayoutParams\.MATCH_PARENT/);
assert.match(presentation, /params\.rightMargin = 0/);
assert.match(presentation, /web\.setInitialScale\(100\)/);

// TikTok uses a consistent desktop identity and can restore its own session.
assert.doesNotMatch(presentation, /app\.id != "tiktok"/);
assert.match(presentation, /getDefaultUserAgent\(context\)/);
assert.match(presentation, /replace\("; wv", ""\)/);
assert.match(presentation, /TikTokWebPolicy\.configure\(web, startUrl \?: app\.url\)/);
const tikTokBlock = presentation.match(/if \(app\.id == "tiktok"[\s\S]*?\} else if \(isDesktop\)/)?.[0] || '';
assert.notEqual(tikTokBlock, '');
assert.doesNotMatch(tikTokBlock, /Windows NT 10\.0/);

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

console.log('PASS v1.0.46: v1.0.39 AA host preserved + TikTok/browser fixes + dashboard pin switches');
