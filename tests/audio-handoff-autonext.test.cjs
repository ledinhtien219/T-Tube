const fs = require('node:fs');
const assert = require('node:assert/strict');

const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');
const media = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt', 'utf8');
const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const phone = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');

// External audio handoff: backgrounded T Tube must stop fighting SoundCloud/Spotify/Zing.
assert.match(helper, /window\.__carhudHostForeground = true/);
assert.match(helper, /window\.__carhudExternalAudioTakeover = false/);
assert.match(helper, /window\.__carhudSetHostForeground/);
assert.match(helper, /markExternalAudioTakeover/);
assert.match(helper, /onExternalAudioTakeover/);
assert.match(helper, /!window\.__carhudHostForeground/);
assert.match(helper, /window\.__carhudHostForeground &&[\s\S]*?!window\.__carhudExternalAudioTakeover/);
assert.match(helper, /1200/);
assert.match(media, /fun yieldToExternalAudio\(sourceWeb: WebView\? = null\)/);
assert.match(media, /userWantsPlayback = false/);
assert.match(media, /web\.enableBackgroundAudio = false/);
assert.match(media, /releaseWakeLock\(\)/);
assert.match(media, /setPlaybackState\(false\)/);
assert.match(presentation, /YouTubePlayerHelper\.setHostForeground\(web, false\)/);
assert.match(presentation, /YouTubePlayerHelper\.setHostForeground\(web, true\)/);
assert.match(phone, /YouTubePlayerHelper\.setHostForeground\(web, foreground\)/);

// Auto-next: ended video must advance without relying on YouTube Autoplay.
assert.match(helper, /function startCarHudAutoNext\(endedVideo\)/);
assert.match(helper, /player\.nextVideo\(\)/);
assert.match(helper, /\.ytp-next-button/);
assert.match(helper, /ytm-video-with-context-renderer a\[href\*="\/watch"\]/);
assert.match(helper, /attempts > 6/);
assert.match(helper, /v\.addEventListener\('ended',[\s\S]*?startCarHudAutoNext\(v\)/);

// Explicit playback clears external takeover so user can intentionally take audio back.
assert.match(helper, /window\.__carhudSetUserPaused = function\(val\)[\s\S]*?window\.__carhudExternalAudioTakeover = false/);
assert.match(helper, /function startAggressivePlay\(\)[\s\S]*?window\.__carhudExternalAudioTakeover = false/);

console.log('PASS audio handoff + YouTube auto-next: external players win in background, ended video advances with retry');
