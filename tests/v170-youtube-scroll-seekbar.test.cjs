const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);
const helper = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt',
  'utf8'
);

const scrollBlock =
  presentation.match(/fun dispatchScroll\(dx: Float, dy: Float\)[\s\S]*?fun dispatchFling/)?.[0] || '';
const flingBlock =
  presentation.match(/fun dispatchFling\(vx: Float, vy: Float\)[\s\S]*?fun reload/)?.[0] || '';
const toolbarBlock =
  presentation.match(/private fun buildYouTubeMediaToolbar\(scale: Int\)[\s\S]*?private val autoHideHandler/)?.[0] || '';

assert.match(scrollBlock, /contains\("\/results", ignoreCase = true\)/);
assert.match(scrollBlock, /scrollScale = if \(isYouTubeResults\) 1\.5f else 2\.0f/);
assert.match(flingBlock, /contains\("\/results", ignoreCase = true\)/);
assert.match(flingBlock, /flingScale = if \(isYouTubeResults\) 0\.28f else 0\.45f/);

assert.match(presentation, /private var youtubeSeekBar: SeekBar\?/);
assert.match(presentation, /private var youtubeSeekDragging = false/);
assert.match(presentation, /private fun isYouTubeVideoPage\(\): Boolean/);
assert.match(presentation, /url\.contains\("\/watch", ignoreCase = true\)/);
assert.match(presentation, /url\.contains\("\/shorts\/", ignoreCase = true\)/);
assert.match(presentation, /val youtubeSeekTouch = currentActiveAppId == "youtube"/);
assert.match(presentation, /projectedTouchActive = editing \|\| tikTok \|\| youtubeSeekTouch/);
assert.match(toolbarBlock, /SeekBar\.OnSeekBarChangeListener/);
assert.match(toolbarBlock, /YouTubePlayerHelper\.seekTo\(activeWeb, target\.toLong\(\)\)/);
assert.match(toolbarBlock, /CarMediaManager\.updatePlaybackProgress\(target, duration\)/);
assert.match(presentation, /youtubeSeekTimeLabel\?\.text/);
assert.match(helper, /fun seekTo\(view: WebView\?, seconds: Long\)/);
assert.match(helper, /player\.seekTo\(\$seconds, true\)/);

console.log('PASS main: YouTube results use 1.5x/0.28x swipe tuning and native seekable timeline');
