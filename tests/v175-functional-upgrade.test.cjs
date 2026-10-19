const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const car = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const tiktok = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt', 'utf8');

const versionCode = Number(gradle.match(/versionCode = (\d+)/)?.[1] || 0);
const versionName = gradle.match(/versionName = "([^"]+)"/)?.[1] || '';
assert.ok(versionCode >= 270);
assert.equal(versionName, '1.0.75');

// Hard app ownership stays enabled.
assert.match(car, /val canRestoreSession = false/);
assert.match(car, /CarMediaManager\.switchActiveApp\(app\.id\)/);

// YouTube can resume the exact video/position without restoring shared WebView history.
assert.match(car, /private var youtubeResumeUrl: String\? = null/);
assert.match(car, /private var youtubeResumeSec = 0/);
assert.match(car, /youtubeResumePlaying = CarMediaManager\.userWantsPlayback/);
assert.match(car, /"youtube" -> \(if \(isChangingApp\) consumeYouTubeResumeUrl\(\) else null\)/);
assert.match(car, /appendQueryParameter\("t", "\$\{sec\}s"\)/);
assert.match(car, /pauseAfterYouTubeRestore = !playing/);
assert.match(car, /YouTubePlayerHelper\.pausePlayback\(view\)/);
assert.match(car, /if \(app\.id == "youtube" && startUrl != null\)[\s\S]*?youtubeResumeUrl = null/);

// Transition shield identifies the incoming app and blocks taps to stale content.
assert.match(car, /showAppSwitchShield\(switchGeneration, app\)/);
assert.match(car, /private fun showAppSwitchShield\(generation: Long, app: WebAppItem\? = null\)/);
assert.match(car, /isClickable = true/);
assert.match(car, /text = "Đang tải nội dung\.\.\."/);
assert.match(car, /text = "Tải lâu hơn bình thường\.\.\."/);
assert.match(car, /if \(isCurrentAppPage\(web\.url\)\) \{[\s\S]*?hideAppSwitchShield\(\)/);

// Voice feedback remains visible for the recognizer's full active window.
assert.match(car, /postDelayed\(hideVoiceBannerRunnable, 35000L\)/);

// TikTok mutation work is throttled/backed off instead of sweeping every DOM change.
assert.match(tiktok, /POLICY_VERSION = 16/);
assert.match(tiktok, /state\.interval = 900/);
assert.match(tiktok, /function cheapText\(el\)/);
assert.match(tiktok, /state\.lastSweepAt = Date\.now\(\)/);
assert.match(tiktok, /Math\.min\(3000, Math\.round\(\(state\.interval \|\| 900\) \* 1\.4\)\)/);
assert.match(tiktok, /mutations\[i\]\.addedNodes && mutations\[i\]\.addedNodes\.length/);

console.log('PASS v1.0.75 functional upgrade: YouTube resume + safe transitions + voice feedback + TikTok throttling');
