// Regression guard for a crash on first launch of Hey Mimi debug APK.
// On debug BuildConfig.PLAY_DISTRIBUTION is false, and MainActivity starts ActivationActivity.
// That Activity must be declared in the debug manifest for a fresh install.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {test} = require("node:test");
const root = path.join(__dirname, "..");
test("debug manifest registers ActivationActivity used by MainActivity", () => {
  const manifest = fs.readFileSync(path.join(root,"app/src/debug/AndroidManifest.xml"),"utf8");
  const source = fs.readFileSync(path.join(root,"app/src/main/java/com/carhud/aaproxy/MainActivity.kt"),"utf8");
  assert.match(source, /Intent\(this, ActivationActivity::class\.java\)/);
  assert.match(manifest, /android:name="com\.carhud\.aaproxy\.ActivationActivity"/);
  assert.match(manifest, /android:exported="false"/);
});
