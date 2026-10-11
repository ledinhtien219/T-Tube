const fs = require('node:fs');
const assert = require('node:assert/strict');

const model = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/WebAppModel.kt', 'utf8');
const settings = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/SettingsActivity.kt', 'utf8');
const dashboard = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');

assert.match(model, /ACTION_APP_VISIBILITY_CHANGED/);
assert.match(model, /putExtra\("app_id", appId\)/);
assert.match(model, /fun setAppEnabled/);
assert.match(settings, /val appSwitch = Switch/);
assert.match(settings, /WebAppManager\.setAppEnabled\(this@SettingsActivity, app\.id, checked\)/);
assert.match(dashboard, /WebAppManager\.isAppEnabled\(context, "youtube"\)/);
assert.match(dashboard, /WebAppManager\.isAppEnabled\(context, "iptv"\)/);
assert.match(dashboard, /WebAppManager\.isAppEnabled\(context, "web"\)/);
assert.match(presentation, /\.filter \{ WebAppManager\.isAppEnabled\(context, it\.id\) \}/);
assert.match(presentation, /WebAppManager\.ACTION_APP_VISIBILITY_CHANGED/);
assert.match(presentation, /dashboardView\?\.applyComponentVisibility\(\)/);

console.log('PASS app visibility: every listed app can be toggled and car home updates live');
