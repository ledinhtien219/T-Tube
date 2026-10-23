const fs = require('node:fs');
const assert = require('node:assert/strict');

const helper = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt',
  'utf8'
);
const blocker = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/YouTubeAdBlocker.kt',
  'utf8'
);

assert.match(
  blocker,
  /fun shouldIntercept\(request: WebResourceRequest\?\): WebResourceResponse\?[\s\S]*?request \?: return null[\s\S]*?return null/
);
assert.doesNotMatch(
  blocker.match(/fun shouldIntercept\([\s\S]*?\n    \}/)?.[0] || '',
  /createEmptyResponse/
);

const helperAdBlock =
  helper.match(/function killYouTubeAds\(\)[\s\S]*?var skipYouTubeAds = killYouTubeAds;/)?.[0] || '';
assert.match(helperAdBlock, /video\.currentTime = duration/);
assert.doesNotMatch(helperAdBlock, /playbackRate\s*=\s*16/);
assert.doesNotMatch(helperAdBlock, /video\.muted\s*=\s*true/);

assert.match(
  helper,
  /v\.addEventListener\('playing',[\s\S]*?if \(isPlayerShowingAd\(\)\) skipYouTubeAds\(\)/
);
assert.match(
  helper,
  /v\.addEventListener\('timeupdate',[\s\S]*?if \(isPlayerShowingAd\(\)\) skipYouTubeAds\(\)/
);

assert.match(blocker, /existing\.version === 4/);
assert.match(blocker, /version: 4/);
assert.match(blocker, /v\.currentTime = d/);
assert.doesNotMatch(
  blocker.match(/private val PLAYER_ADBLOCK_JS:[\s\S]*$/)?.[0] || '',
  /playbackRate = 16\.0/
);

console.log('PASS YouTube Fermata playback: no request blocking, no 16x state leak, immediate player-event ad skip');
