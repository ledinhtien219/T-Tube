const fs = require('node:fs');
const assert = require('node:assert/strict');

const helper = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt',
  'utf8'
);

assert.match(helper, /addEventListener\('ended',[\s\S]*?__carhudAutoNextKey/);
assert.match(helper, /typeof player\.nextVideo === 'function'/);
assert.match(helper, /\.ytp-next-button/);
assert.match(helper, /ytm-video-with-context-renderer a\[href\*="\/watch\?v="\]/);
assert.match(helper, /setTimeout\(function\(\)[\s\S]*?500\)/);
assert.match(helper, /window\.__carhudUserPaused/);

console.log('PASS YouTube auto-next: ended -> player.nextVideo -> Next button -> recommendation fallback');
