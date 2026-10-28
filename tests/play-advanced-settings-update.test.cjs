const fs = require('node:fs');
const assert = require('node:assert/strict');
const dir = 'app/src/main/java/com/carhud/aaproxy/';
const main = fs.readFileSync(dir+'MainActivity.kt','utf8');
const settings = fs.readFileSync(dir+'SettingsActivity.kt','utf8');
const updater = fs.readFileSync(dir+'PlayInAppUpdater.kt','utf8');
const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml','utf8');
const layout = fs.readFileSync('app/src/main/res/layout/activity_main.xml','utf8');

assert.match(gradle, /versionCode = 305/);
assert.match(gradle, /versionName = "1\.1\.7"/);
assert.match(gradle, /implementation\("com\.google\.android\.play:app-update:2\.1\.0"\)/);
assert.match(gradle, /applicationId = "com\.ledinhtien\.ttube"/);

// Root regression: SettingsActivity previously redirected both lifecycle
// callbacks to ActivationActivity even though Play deliberately excludes it.
const settingsCreate = settings.slice(settings.indexOf('override fun onCreate('),settings.indexOf('override fun onResume('));
const settingsResume = settings.slice(settings.indexOf('override fun onResume('),settings.indexOf('private fun showDashboardPreview()'));
assert.match(settingsCreate, /if \(!BuildConfig\.PLAY_DISTRIBUTION && !LicenseManager\.isLicensed\(this\)\)/);
assert.match(settingsResume, /if \(BuildConfig\.PLAY_DISTRIBUTION\) \{[\s\S]*?playUpdater\.onResume\(\)[\s\S]*?return/);
assert.match(settingsResume, /if \(!LicenseManager\.isLicensed\(this\)\)/);
assert.match(settingsResume, /LicenseManager\.checkStatus/);
assert.ok(settingsResume.indexOf('return') < settingsResume.indexOf('LicenseManager.checkStatus'), 'Play must return before sideload licensing');
assert.match(settingsCreate, /if \(BuildConfig\.PLAY_DISTRIBUTION\) playUpdater \/\/ register launcher before STARTED/);
assert.doesNotMatch(manifest, /android:name="com\.carhud\.aaproxy\.ActivationActivity"/);
assert.match(manifest, /android:name="com\.carhud\.aaproxy\.SettingsActivity"/);

// Both the whole advanced-settings card and its nested button must work.
assert.match(layout, /android:id="@\+id\/settingCardAdvanced"/);
assert.match(layout, /android:id="@\+id\/btnOpenAdvancedSettings"/);
const mainSetup = main.slice(main.indexOf('private fun setupSettingsTab()'), main.indexOf('private fun updateUserSettingsText()'));
assert.match(mainSetup, /findViewById<LinearLayout>\(R\.id\.settingCardAdvanced\)/);
assert.match(mainSetup, /findViewById<Button>\(R\.id\.btnOpenAdvancedSettings\)/);
assert.match(mainSetup, /setOnClickListener\(openAdvancedSettings\)/);
assert.match(mainSetup, /Intent\(this, SettingsActivity::class\.java\)/);
assert.match(main, /if \(BuildConfig\.PLAY_DISTRIBUTION\) playUpdater \/\/ register launcher before STARTED/);

// Google Play is the only Play build update source, not external APK/GitHub.
assert.match(main, /if \(BuildConfig\.PLAY_DISTRIBUTION\) \{[\s\S]{0,180}playUpdater\.checkForUpdates\(manual = false\)/);
assert.match(settings, /onClick = \{ checkAppUpdate\(\) \}/);
assert.match(settings, /if \(BuildConfig\.PLAY_DISTRIBUTION\) \{[\s\S]{0,110}playUpdater\.checkForUpdates\(manual = true\)/);
assert.match(updater, /AppUpdateManagerFactory\.create/);
assert.match(updater, /AppUpdateType\.FLEXIBLE/);
assert.match(updater, /UpdateAvailability\.UPDATE_AVAILABLE/);
assert.match(updater, /ActivityResultContracts\.StartIntentSenderForResult/);
assert.match(updater, /manager\.startUpdateFlowForResult/);
assert.match(updater, /InstallStatus\.DOWNLOADED/);
assert.match(updater, /manager\.completeUpdate\(\)/);
assert.match(updater, /manager\.unregisterListener\(listener\)/);
assert.match(updater, /Để sau/);
assert.match(updater, /Cài đặt và khởi động lại/);
assert.match(updater, /activity\.lifecycle\.currentState\.isAtLeast\(Lifecycle\.State\.RESUMED\)/);
console.log('PASS: Advanced Settings launch on Play, no activation redirect, official flexible updates and restart consent');
