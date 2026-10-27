const fs = require('node:fs');
const assert = require('node:assert/strict');

const dashboard = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt', 'utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');

function section(source, startNeedle, endNeedle) {
  const start = source.indexOf(startNeedle);
  assert.notEqual(start, -1, `Missing section start: ${startNeedle}`);
  const end = source.indexOf(endNeedle, start + startNeedle.length);
  assert.notEqual(end, -1, `Missing section end: ${endNeedle}`);
  return source.slice(start, end);
}

const showDashboard = section(presentation, 'fun showDashboard()', 'fun showWeb()');
const visibleAt = showDashboard.indexOf('dashboardView?.visibility = View.VISIBLE');
const updateAt = showDashboard.indexOf('dashboardView?.updateWallpaper()');
assert.ok(visibleAt >= 0 && updateAt >= 0 && visibleAt < updateAt,
  'dashboard must become visible before restoring dynamic wallpaper');

assert.match(dashboard, /PREF_CUSTOM_WALLPAPER_REVISION/);
assert.match(dashboard, /key == PREF_CUSTOM_WALLPAPER_REVISION/);
const picked = section(main, 'private fun handlePickedWallpaper', 'private fun queryDisplayName');
assert.match(picked, /putLong\([\s\S]*PREF_CUSTOM_WALLPAPER_REVISION/);
assert.match(picked, /getLong\(CarDashboardView\.PREF_CUSTOM_WALLPAPER_REVISION, 0L\) \+ 1L/);

assert.match(presentation, /handleMazdaPadKeyDown/);
console.log('PASS Mazda wallpaper IPTV/Home regression and Mazda PAD path retained');
