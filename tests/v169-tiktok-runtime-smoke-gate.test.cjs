const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const workflow = fs.readFileSync('.github/workflows/build-apk.yml','utf8');
const smokeActivity = fs.readFileSync('app/src/debug/java/com/carhud/aaproxy/TikTokSmokeActivity.kt','utf8');
const smokeTest = fs.readFileSync('app/src/androidTest/java/com/carhud/aaproxy/TikTokRuntimeSmokeTest.kt','utf8');

assert.ok(Number(gradle.match(/versionCode = (\d+)/)?.[1]) >= 261);
assert.match(gradle, /androidx\.test\.uiautomator:uiautomator:2\.3\.0/);
assert.match(gradle, /androidx\.test:core-ktx:1\.6\.1/);

assert.match(smokeActivity, /https:\/\/www\.tiktok\.com\//);
assert.match(smokeActivity, /TikTokWebPolicy\.configure/);
assert.match(smokeActivity, /TikTokWebPolicy\.fitLayout/);
assert.match(smokeActivity, /TikTokWebPolicy\.isAppRedirect/);

assert.match(smokeTest, /repeat\(12\)/);
assert.match(smokeTest, /device\.swipe/);
assert.match(smokeTest, /activity\.loadSearch\("cat"\)/);
assert.match(smokeTest, /searchUiCount\(activity\) >= 1 \|\| videoLinkCount\(activity\) >= 1/);
assert.match(smokeTest, /01-home\.png/);
assert.match(smokeTest, /03-search\.png/);
assert.match(smokeTest, /fitsViewport\(activity\)/);
assert.match(smokeTest, /state\.fitRawWidth/);
assert.match(smokeTest, /rawWidth \* scale <= viewport \* 1\.035/);

const smokePos = workflow.indexOf('Run live TikTok runtime smoke test');
const uploadPos = workflow.indexOf('- name: Upload APK');
assert.ok(smokePos >= 0, 'runtime smoke step missing');
assert.ok(uploadPos > smokePos, 'APK upload must happen after smoke test');
assert.match(workflow, /ReactiveCircus\/android-emulator-runner@v2/);
assert.match(workflow, /connectedDebugAndroidTest/);
assert.match(workflow, /build(?:261|270)-smoke-evidence/);
assert.match(workflow, /grep -E "TTUBE_SMOKE\|CarDisconnectWatchdog\|CarAppShutdownManager"/);
assert.match(workflow, /final-screen\.png/);

console.log('PASS live Android TikTok smoke test gates APK upload');
