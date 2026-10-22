const fs = require('node:fs');
const assert = require('node:assert/strict');

const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const projection = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarHudProjectionHost.kt','utf8');
const car = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');

assert.ok(Number(gradle.match(/versionCode = (\d+)/)?.[1]) >= 272);

// Phone app: do not keep both hidden Chromium instances alive behind Maps.
assert.match(main, /override fun onPause\(\)[\s\S]*?suspendPhoneWebViewsForExternalApp\(\)/);
assert.match(main, /val keepYoutubeAudio = backgroundAudio && CarMediaManager\.isPlaying && activeMediaWeb === youtubeWeb/);
assert.match(main, /val keepIptvAudio = backgroundAudio && CarMediaManager\.isPlaying && activeMediaWeb === iptvWeb/);
assert.match(main, /web\.enableBackgroundAudio = keepYoutubeAudio[\s\S]*?if \(keepYoutubeAudio\) web\.onResume\(\) else web\.onPause\(\)/);
assert.match(main, /web\.enableBackgroundAudio = keepIptvAudio[\s\S]*?if \(keepIptvAudio\) web\.onResume\(\) else web\.onPause\(\)/);
assert.doesNotMatch(main.match(/override fun onPause\(\)[\s\S]*?override fun onDestroy\(\)/)?.[0] || '', /resumeTimers\(\)/);

// Android Auto: stop producing frames while another AA app owns foreground.
assert.match(projection, /private var hostForeground = false/);
assert.match(projection, /override fun onStart\(\)[\s\S]*?hostForeground = true[\s\S]*?reattachProjectionSurfaceIfReady\(\)/);
assert.match(projection, /override fun onStop\(\)[\s\S]*?hostForeground = false[\s\S]*?virtualDisplay\?\.setSurface\(null\)/);
assert.match(projection, /if \(!hostForeground\) return@setOnTouchListener false/);
assert.match(projection, /virtualDisplay\?\.setSurface\(surface\)[\s\S]*?onProjectionHostVisibilityChanged\(true\)/);

// Car WebView pauses unless real background audio is playing.
assert.match(car, /fun onProjectionHostVisibilityChanged\(foreground: Boolean\)/);
assert.match(car, /val keepBackgroundAudio = backgroundAudio &&[\s\S]*?CarMediaManager\.isPlaying &&[\s\S]*?CarMediaManager\.userWantsPlayback/);
assert.match(car, /mediaWeb\.onPause\(\)/);
assert.match(car, /CarMediaManager\.releaseWakeLock\(\)/);

console.log('PASS Maps handoff: hidden phone WebViews pause and Android Auto VirtualDisplay detaches while Maps is foreground');
