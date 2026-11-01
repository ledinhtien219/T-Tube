const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');

const path = 'app/src/main/java/com/carhud/aaproxy/';
const shields = fs.readFileSync(path + 'BraveYouTubeShields.kt', 'utf8');
const presentation = fs.readFileSync(path + 'CarPresentation.kt', 'utf8');
const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');

test('uses real Brave Rust engine, not DNS-only or Skip-only filtering', () => {
  assert.match(gradle, /io\.github\.mlm-games:adblock:0\.1\.3/);
  assert.match(shields, /import org\.mlm\.adblock\.AdblockEngine/);
  assert.match(shields, /engine\?\.checkNetworkRequest/);
  assert.match(shields, /cosmeticResources/);
  assert.match(shields, /WebViewCompat\.addDocumentStartJavaScript/);
  assert.match(shields, /uBlockOrigin\/uAssets\/master\/filters\/quick-fixes/);
});

test('does not intercept non-YouTube or core media/player traffic', () => {
  assert.match(shields, /!youtubeActive/);
  assert.match(shields, /request\.isForMainFrame/);
  assert.match(shields, /request\.method\.equals\("GET"/);
  assert.match(shields, /host\.endsWith\("\.googlevideo\.com"\)/);
  assert.match(shields, /path\.contains\("\/youtubei\/v1\/player"\)/);
  assert.match(shields, /path\.contains\("\/videoplayback"\)/);
  assert.match(shields, /type == "media"/);
  assert.match(shields, /sourceHost != null && !isYouTubeHost/);
  assert.match(presentation, /BraveYouTubeShields\.pageStarted\(view, url\)/);
  assert.match(presentation, /BraveYouTubeShields\.applyPageCosmetics\(view, url\)/);
  assert.match(presentation, /BraveYouTubeShields\.intercept\(request\)/);
});

test('filter compilation is off UI/request thread, bounded and fail-open', () => {
  assert.match(shields, /Executors\.newSingleThreadExecutor/);
  assert.match(shields, /MAX_LIST_BYTES/);
  assert.match(shields, /MAX_RULE_BYTES/);
  assert.match(shields, /CACHE_AGE_MS/);
  assert.match(shields, /catch \(_: Throwable\) \{ false \}/);
  assert.match(shields, /engine\?\.checkNetworkRequest/);
  assert.match(shields, /synchronized\(gate\)/);
  assert.match(shields, /java\.io\.ByteArrayOutputStream/);
});

test('feed ads are hidden without removing real video cards', () => {
  const feed = fs.readFileSync('app/src/main/assets/youtube_feed_pip_enhancer.js', 'utf8');
  assert.match(feed, /ytd-ad-slot-renderer/);
  assert.match(feed, /ytd-in-feed-ad-layout-renderer/);
  assert.match(feed, /ytm-promoted-sparkles-web-renderer/);
  assert.match(feed, /data-ttube-sponsored/);
  assert.match(feed, /markAdOnlySlots/);
  assert.doesNotMatch(feed, /\.remove\(\)|removeChild\(/);
});

test('native YouTube related cards stay clickable in a four-column scrollable PiP grid', () => {
  const feed = fs.readFileSync('app/src/main/assets/youtube_feed_pip_enhancer.js', 'utf8');
  assert.match(feed, /grid-template-columns:repeat\(4,minmax\(0,1fr\)\)/);
  assert.match(feed, /data-carhud-rec-scroll/);
  assert.match(feed, /data-ttube-four-col/);
  assert.match(feed, /touch-action:pan-y/);
  assert.match(feed, /ytd-watch-next-secondary-results-renderer/);
  assert.match(feed, /ytm-item-section-renderer/);
  assert.match(presentation, /YouTubeFeedPipEnhancer\.install\(web\)/);
  assert.match(presentation, /YouTubeFeedPipEnhancer\.inject\(view\)/);
});
