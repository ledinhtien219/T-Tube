const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);

const scrollBlock =
  presentation.match(/fun dispatchScroll\(dx: Float, dy: Float\)[\s\S]*?fun dispatchFling/)?.[0] || '';
const flingBlock =
  presentation.match(/fun dispatchFling\(vx: Float, vy: Float\)[\s\S]*?fun reload/)?.[0] || '';

assert.match(scrollBlock, /currentActiveAppId == "youtube"/);
assert.match(scrollBlock, /contains\("\/results", ignoreCase = true\)/);
assert.match(scrollBlock, /scrollScale = if \(isYouTubeResults\) 1\.0f else 2\.0f/);
assert.match(scrollBlock, /dy \* scrollScale/);

assert.match(flingBlock, /currentActiveAppId == "youtube"/);
assert.match(flingBlock, /contains\("\/results", ignoreCase = true\)/);
assert.match(flingBlock, /flingScale = if \(isYouTubeResults\) 0\.18f else 0\.45f/);
assert.match(flingBlock, /-vy \* flingScale/);

console.log('PASS v1.0.70: YouTube results use slower 1:1 drag and damped fling');
