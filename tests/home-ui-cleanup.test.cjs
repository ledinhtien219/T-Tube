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

// The projected Surface must use the action-free MapTemplate proven by
// the known-good v1.0.39 APK. Do not provide any host ActionStrip.
assert.doesNotMatch(screen, /import androidx\.car\.app\.model\.ActionStrip/);
assert.doesNotMatch(screen, /ActionStrip\.Builder\(\)/);
assert.doesNotMatch(screen, /setActionStrip\(/);
assert.doesNotMatch(screen, /NavigationTemplate/);
assert.doesNotMatch(screen, /PlaceListNavigationTemplate/);
assert.doesNotMatch(screen, /Action\.BACK/);
assert.match(screen, /carContext\.carAppApiLevel < 5/);
assert.match(screen, /MapTemplate\.Builder\(\)[\s\S]*?setPane\(Pane\.Builder\(\)\.build\(\)\)[\s\S]*?\.build\(\)/);

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
