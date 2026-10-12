const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const manager = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WebAppModel.kt', 'utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt', 'utf8');
const dashboard = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt', 'utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const screen = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt', 'utf8');

// Preserve the exact action-free host template from the known-good v1.0.39 APK.
assert.doesNotMatch(screen, /import androidx\.car\.app\.model\.ActionStrip/);
assert.doesNotMatch(screen, /ActionStrip\.Builder\(\)/);
assert.doesNotMatch(screen, /setActionStrip\(/);
assert.doesNotMatch(screen, /NavigationTemplate/);
assert.doesNotMatch(screen, /PlaceListNavigationTemplate/);
assert.match(screen, /carContext\.carAppApiLevel < 5/);
assert.match(screen, /MapTemplate\.Builder\(\)[\s\S]*?setPane\(Pane\.Builder\(\)\.build\(\)\)[\s\S]*?\.build\(\)/);
assert.match(presentation, /setPadding\(dp\(4\), dp\(4\), dp\(4\), dp\(4\)\)/);

// Browser uses the full projected viewport at normal CSS scale.
assert.match(presentation, /val isBrowser = currentActiveAppId == "web"/);
assert.match(presentation, /val scalePercent = if \(isBrowser\) \{\s*100/);
assert.match(presentation, /web\.settings\.loadWithOverviewMode = !isBrowser/);
assert.match(presentation, /params\.width = ViewGroup\.LayoutParams\.MATCH_PARENT/);
assert.match(presentation, /params\.rightMargin = 0/);
assert.match(presentation, /web\.setInitialScale\(100\)/);

// TikTok uses the phone's mobile Chrome-like UA and is freshly loaded.
assert.match(presentation, /app\.id != "tiktok"/);
assert.match(presentation, /getDefaultUserAgent\(context\)/);
assert.match(presentation, /replace\("; wv", ""\)/);
assert.match(presentation, /replace\("Version\/4\.0 ", ""\)/);
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
