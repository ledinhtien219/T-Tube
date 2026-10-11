const fs = require('node:fs');
const assert = require('node:assert/strict');

const screen = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt',
  'utf8'
);
const projectionHost = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt',
  'utf8'
);
const manifest = fs.readFileSync(
  'app/src/main/AndroidManifest.xml',
  'utf8'
);
const automotiveDesc = fs.readFileSync(
  'app/src/main/res/xml/automotive_app_desc.xml',
  'utf8'
);
const dashboardKt = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt',
  'utf8'
);
const dashboardXml = fs.readFileSync(
  'app/src/main/res/layout/dashboard.xml',
  'utf8'
);

// Android Auto test branch: use Fermata's projection host path instead of
// AndroidX CarAppService templates. The host Activity hides the Android Auto
// app header/menu and renders THTV into a full-screen SurfaceView.
assert.match(projectionHost, /class CarHudProjectionService\s*:\s*CarActivityService\(\)/);
assert.match(projectionHost, /class CarHudProjectionActivity\s*:\s*CarActivity\(\)/);
assert.match(projectionHost, /hideAppHeader\(\)/);
assert.match(projectionHost, /hideMenuButton\(\)/);
assert.match(projectionHost, /SurfaceView\(this\)/);
assert.match(projectionHost, /CarPresentation\(this, vd\.display, persistentWeb\)/);
assert.doesNotMatch(manifest, /android:name="com\.carhud\.aaproxy\.CarHudAutoService"[\s\S]*?androidx\.car\.app\.CarAppService/);
assert.match(manifest, /android:name="com\.carhud\.aaproxy\.CarHudProjectionService"/);
assert.match(manifest, /com\.google\.android\.gms\.car\.category\.CATEGORY_PROJECTION/);
assert.doesNotMatch(automotiveDesc, /<uses name="template"\s*\/>/);
assert.match(automotiveDesc, /name="projection"/);
assert.match(automotiveDesc, /name="service"/);

// Keep the existing weather-card bounds, but render weather with vector icons.
assert.match(dashboardXml, /android:id="@\+id\/weatherCard"[\s\S]*?android:layout_width="150dp"[\s\S]*?android:layout_height="76dp"/);
assert.match(dashboardXml, /<ImageView[\s\S]*?android:id="@\+id\/weatherIcon"[\s\S]*?android:layout_width="38dp"/);
assert.match(dashboardXml, /android:src="@drawable\/ic_weather_partly_cloudy"/);
assert.match(dashboardKt, /private lateinit var weatherIcon: ImageView/);
assert.match(dashboardKt, /fun weatherIconRes\(iconEmoji: String\): Int/);
assert.match(dashboardKt, /weatherIcon\.setImageResource\(weatherIconRes\(weather\.iconEmoji\)\)/);
assert.match(dashboardKt, /Color\.parseColor\("#66F8FAFC"\)/);
assert.match(dashboardKt, /Color\.parseColor\("#66101A24"\)/);
assert.match(dashboardKt, /Color\.parseColor\("#80FFFFFF"\)/);
assert.match(dashboardKt, /Color\.parseColor\("#4DFFFFFF"\)/);

for (const file of [
  'ic_weather_sunny.xml',
  'ic_weather_partly_cloudy.xml',
  'ic_weather_fog.xml',
  'ic_weather_rain.xml',
  'ic_weather_storm.xml'
]) {
  assert.ok(fs.existsSync('app/src/main/res/drawable/' + file), file + ' missing');
}

console.log('PASS home UI cleanup: back action removed + vector weather icons');
