const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const js = fs.readFileSync(
  path.join('app','src','main','assets','youtube_feed_pip_enhancer.js'), 'utf8'
);
const kotlin = fs.readFileSync(
  path.join('app','src','main','java','com','carhud','aaproxy','YouTubeFeedPipEnhancer.kt'), 'utf8'
);
const presentation = fs.readFileSync(
  path.join('app','src','main','java','com','carhud','aaproxy','CarPresentation.kt'), 'utf8'
);
test('feed and PiP script parses as JavaScript', () => {
  assert.doesNotThrow(() => new vm.Script(js));
});
test('filters desktop and mobile ad renderers including promoted feed slots', () => {
  for (const sel of [
    'ytd-ad-slot-renderer',
    'ytd-promoted-sparkles-web-renderer',
    'ytd-in-feed-ad-layout-renderer',
    'ytd-rich-item-renderer:has(ytd-ad-slot-renderer)',
    'ytm-ad-slot-renderer',
    'ytm-promoted-video-renderer',
    'ytm-promoted-sparkles-web-renderer'
  ]) assert.ok(js.includes(sel), 'Missing renderer ' + sel);
  assert.match(js, /markAdOnlySlots/);
  assert.match(js, /data-ttube-sponsored/);
});
test('native PiP recommendations are exactly four columns, scrollable, and clickable', () => {
  assert.match(js, /grid-template-columns:repeat\(4,minmax\(0,1fr\)\)/);
  assert.match(js, /data-carhud-rec-scroll/);
  assert.match(js, /data-ttube-four-col/);
  assert.match(js, /touch-action:pan-y/);
  assert.match(js, /pointer-events:auto/);
  assert.match(js, /window\.__carhudUpdateWatchLayout/);
  assert.doesNotMatch(js, /document\.createElement\(['"]video['"]\)/);
  assert.doesNotMatch(js, /document\.createElement\(['"]iframe['"]\)/);
  assert.doesNotMatch(js, /video\.playbackRate\s*=/);
  assert.doesNotMatch(js, /video\.currentTime\s*=/);
});
test('filters are document-start with SPA updates and bounded DOM observation', () => {
  assert.match(kotlin, /WebViewFeature\.DOCUMENT_START_SCRIPT/);
  assert.match(kotlin, /WebViewCompat\.addDocumentStartJavaScript/);
  assert.match(js, /yt-navigate-finish/);
  assert.match(js, /yt-page-data-updated/);
  assert.match(js, /MutationObserver/);
  assert.match(js, /setTimeout\(refresh, 180\)/);
  assert.match(js, /Math\.min\(ads\.length, 80\)/);
  assert.match(presentation, /YouTubeFeedPipEnhancer\.install\(web\)/);
  assert.match(presentation, /YouTubeFeedPipEnhancer\.inject\(view\)/);
});
