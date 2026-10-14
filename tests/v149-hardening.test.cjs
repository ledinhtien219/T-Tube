const assert = require('assert');
const fs = require('fs');

const manifest = fs.readFileSync('app/src/main/AndroidManifest.xml', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const media = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt', 'utf8');
const voice = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/VoiceQueryResolver.kt', 'utf8');
const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const wrapper = fs.readFileSync('gradlew');

const versionCode = Number(gradle.match(/versionCode = (\d+)/)?.[1] || 0);
assert.ok(versionCode >= 236, 'v1.0.49 hardening must remain in later builds');
assert.doesNotMatch(manifest, /android\.permission\.BIND_APPWIDGET/);
assert.doesNotMatch(manifest, /android\.permission\.SYSTEM_ALERT_WINDOW/);
for (const activity of ['SettingsActivity', 'ActivationActivity', 'PhoneSearchActivity']) {
  const re = new RegExp(`android:name="com\\.carhud\\.aaproxy\\.${activity}"[\\s\\S]{0,180}?android:exported="false"`);
  assert.match(manifest, re, `${activity} must remain internal`);
}
assert.doesNotMatch(presentation, /lastUrl!!/);
assert.doesNotMatch(presentation, /web\.loadUrl\(target!!\)/);
assert.doesNotMatch(presentation, /options!!\.getString\("page"\)/);
assert.doesNotMatch(media, /id!! to input/);
assert.doesNotMatch(voice, /best!!\.score/);
assert.equal(wrapper.includes(Buffer.from('\r\n')), false, 'gradlew must use LF line endings');

console.log('PASS v1.0.49 hardening: private activities, lean permissions, null safety, Linux wrapper');
