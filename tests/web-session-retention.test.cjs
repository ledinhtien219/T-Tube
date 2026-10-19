const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);
const media = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt',
  'utf8'
);

// Browser and YouTube each keep their own navigation snapshot.
assert.match(media, /data class WebAppSession\(/);
assert.match(media, /private val webAppSessions = mutableMapOf<String, WebAppSession>\(\)/);
assert.match(media, /fun saveWebAppSession\(appId: String, web: WebView\?\)/);
assert.match(media, /web\.saveState\(state\)/);
assert.match(media, /scrollX = web\.scrollX/);
assert.match(media, /scrollY = web\.scrollY/);
assert.match(media, /videoPositionSec = if \(appId == "youtube" && extractYouTubeVideoId\(url\) != null\) currentPositionSec else 0/);

assert.match(media, /fun restoreWebAppSession\(appId: String, web: WebView\?\): Boolean/);
assert.match(media, /web\.restoreState\(android\.os\.Bundle\(session\.state\)\)/);
assert.match(media, /activeAppId != appId \|\| getPersistentWebView\(\) !== web/);
assert.match(media, /web\.scrollTo\(session\.scrollX, session\.scrollY\)/);
assert.match(media, /YouTubePlayerHelper\.seekTo\(web, session\.videoPositionSec\.toLong\(\)\)/);

// v1.0.74 hard-isolates app ownership: shared WebView state must not be
// restored across app switches because stale history can resurrect the old app.
assert.match(presentation, /val canRestoreSession = false/);
assert.doesNotMatch(presentation, /CarMediaManager\.saveWebAppSession\(previousAppId, web\)/);
assert.doesNotMatch(presentation, /CarMediaManager\.restoreWebAppSession\(app\.id, web\)/);
assert.match(presentation, /isUrlForApp\(app, currentUrl\)/);
assert.match(presentation, /web\.clearHistory\(\)/);
assert.match(presentation, /shouldNavigateFresh/);

// Switching to another Android Auto app also captures the current state.
assert.match(presentation, /override fun onStop\(\)[\s\S]*?saveWebAppSession\(currentActiveAppId, web\)/);
assert.match(presentation, /override fun dismiss\(\)[\s\S]*?saveWebAppSession\(currentActiveAppId, web\)/);

console.log('PASS web session ownership: lifecycle snapshots remain available but cross-app restore is disabled');
