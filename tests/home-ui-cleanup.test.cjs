const fs = require('node:fs');
const assert = require('node:assert/strict');

const screen = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudAutoScreen.kt',
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

// Android Auto packages two mutually-exclusive host entries.
// Stable projection is the manifest default; the ultrawide CarAppService is
// enabled only after a safe disconnect/reconnect transition.
const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const projectionHost = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt',
  'utf8'
);
const hostMode = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarHostModeManager.kt',
  'utf8'
);
const descriptor = fs.readFileSync('app/src/main/res/xml/automotive_app_desc.xml', 'utf8');
const templateFactory = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarUltrawideTemplateFactory.kt',
  'utf8'
);

assert.match(manifest, /CarHudProjectionService"[\s\S]*?android:enabled="true"/);
assert.match(manifest, /CarHudAutoService"[\s\S]*?android:enabled="false"/);
assert.match(manifest, /CATEGORY_PROJECTION/);
assert.match(manifest, /androidx\.car\.app\.category\.NAVIGATION/);
assert.match(descriptor, /<uses\s+name="template"/);
assert.match(descriptor, /<uses\s+name="service"/);
assert.match(descriptor, /<uses\s+name="projection"/);
assert.match(hostMode, /if \(!isAndroidAutoSessionActive\(\)\)/);
assert.match(hostMode, /CarHudAutoService::class\.java/);
assert.match(hostMode, /CarHudProjectionService::class\.java/);
assert.match(projectionHost, /class CarHudProjectionService : CarActivityService\(\)/);
assert.match(projectionHost, /class CarHudProjectionActivity : CarActivity\(\), SurfaceHolder\.Callback/);

// Modern ultrawide mode must build a valid action-free MapTemplate: Pane + Header,
// no ActionStrip call. Legacy hosts retain the tiny NavigationTemplate fallback.
assert.match(templateFactory, /MapTemplate\.Builder\(\)[\s\S]*?setPane\(pane\)[\s\S]*?setHeader\(header\)[\s\S]*?build\(\)/);
const modernTemplateBlock = templateFactory.match(/if \(carApiLevel >= 5\)[\s\S]*?return MapTemplate\.Builder\(\)[\s\S]*?\.build\(\)/)?.[0] || '';
assert.doesNotMatch(modernTemplateBlock, /setActionStrip/);
assert.match(templateFactory, /Header\.Builder\(\)[\s\S]*?setTitle\("\\u200B"\)/);
assert.match(templateFactory, /NavigationTemplate\.Builder\(\)[\s\S]*?setActionStrip\(hiddenActionStrip\)/);

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
