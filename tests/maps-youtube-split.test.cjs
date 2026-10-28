const fs = require('node:fs');
const assert = require('node:assert/strict');

const model = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/WebAppModel.kt',
  'utf8'
);
const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);
const media = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarMediaManager.kt',
  'utf8'
);
const navigationHtml = fs.readFileSync(
  'app/src/main/assets/car_split_navigation.html', 'utf8'
);

assert.match(
  model,
  /DEFAULT_DASHBOARD_PINNED_IDS = setOf\("maps_youtube_split", "youtube", "iptv", "web"\)/
);
assert.match(model, /id = "maps_youtube_split"/);
assert.match(model, /name = "Maps \+ YouTube"/);
assert.match(model, /iconRes = R\.drawable\.ic_app_split/);

assert.match(presentation, /private var splitModeActive = false/);
assert.match(presentation, /splitMapUrl = "https:\/\/www\.google\.com\/maps"/);
assert.match(presentation, /fun showMapsYoutubeSplit\(\)/);
assert.match(presentation, /orientation = LinearLayout\.HORIZONTAL/);
assert.match(presentation, /private val splitRatioOptions = listOf\([\s\S]*?0\.5f to 0\.5f[\s\S]*?0\.3f to 0\.7f[\s\S]*?0\.7f to 0\.3f/);
assert.match(presentation, /private fun applySplitLayout\(\)/);
assert.match(presentation, /leftView: View = if \(CarMediaManager\.mapsYoutubeSplitSwapped\) web else map/);
assert.match(presentation, /rightView: View = if \(CarMediaManager\.mapsYoutubeSplitSwapped\) map else web/);
assert.match(presentation, /LinearLayout\.LayoutParams\(0, ViewGroup\.LayoutParams\.MATCH_PARENT, leftWeight\)/);
assert.match(presentation, /LinearLayout\.LayoutParams\(0, ViewGroup\.LayoutParams\.MATCH_PARENT, rightWeight\)/);
assert.match(media, /var mapsYoutubeSplitRatioIndex: Int = 0/);
assert.match(media, /var mapsYoutubeSplitSwapped: Boolean = false/);
assert.match(media, /var mapsYoutubeSplitLastRouteUrl: String = ""/);
assert.match(media, /mapsYoutubeSplitYoutubeHistory: MutableList<String> = mutableListOf\(\)/);

assert.match(presentation, /splitControlsContainer = controls/);
assert.match(presentation, /splitBackButton = TextView\(context\)[\s\S]*?text = "←"[\s\S]*?goBackWithinSplit\(\)/);
assert.match(presentation, /splitHomeButton = TextView\(context\)[\s\S]*?text = "⌂"[\s\S]*?LinearLayout\.LayoutParams\(dp\(70\), dp\(58\)\)/);
assert.match(presentation, /private fun goBackWithinSplit\(\)[\s\S]*?splitLastTouchedPane == "youtube"[\s\S]*?goBackWithinSplitYouTube\(\)/);
assert.match(presentation, /private fun goBackWithinSplitYouTube\(\)[\s\S]*?mapsYoutubeSplitYoutubeHistory[\s\S]*?web\.loadUrl\(previous\)/);
assert.match(presentation, /private fun recordSplitYouTubeHistory\(raw: String\?\)/);
assert.match(presentation, /doUpdateVisitedHistory[\s\S]*?recordSplitYouTubeHistory\(url\)/);
assert.match(presentation, /fun goBack\(\)[\s\S]*?if \(splitModeActive\) \{[\s\S]*?goBackWithinSplit\(\)[\s\S]*?return/);
assert.match(presentation, /splitRatioButton = TextView\(context\)[\s\S]*?text = splitRatioLabel\(\)/);
assert.match(presentation, /splitSwapButton = TextView\(context\)[\s\S]*?text = "⇄"/);
assert.match(presentation, /Gravity\.TOP or Gravity\.CENTER_HORIZONTAL/);
assert.match(presentation, /postDelayed\(splitHideControlsRunnable, 10_000L\)/);
assert.match(presentation, /if \(event\.actionMasked == MotionEvent\.ACTION_DOWN\) \{[\s\S]*?showSplitControlsTemporarily\(\)/);
assert.match(presentation, /private fun cycleSplitRatio\(\)/);
assert.match(presentation, /private fun swapSplitSides\(\)/);

assert.match(
  presentation,
  /if \(app\.id == "maps_youtube_split"\) \{[\s\S]*?showMapsYoutubeSplit\(\)[\s\S]*?return/
);
assert.match(
  presentation,
  /fun dispatchProjectedTouch\(event: MotionEvent\): Boolean \{[\s\S]*?if \(splitModeActive\) \{[\s\S]*?dispatchTouchEvent\(copy\)[\s\S]*?return true/
);
assert.match(presentation, /val onOverlayControl =[\s\S]*?containsScreenPoint\(splitControlsContainer\)[\s\S]*?containsScreenPoint\(splitMapDirectionsButton\)/);
assert.match(presentation, /if \(!onOverlayControl\) \{[\s\S]*?splitLastTouchedPane = "maps"[\s\S]*?splitLastTouchedPane = "youtube"/);

assert.match(presentation, /private fun requestSplitMapSearch\([\s\S]*?initialText: String = ""[\s\S]*?hint: String = "Tìm địa điểm trên Google Maps"/);
assert.match(presentation, /searchOverlayMode = "maps"/);
assert.match(presentation, /showKeyboardOverlayInternal\(requestNative = false, showKeys = true\)/);
assert.match(presentation, /requestNative && !splitModeActive/);
assert.match(presentation, /topSearchBand = touchY <= kotlin\.math\.max\(/);
assert.match(presentation, /insideSearchWidth/);
assert.match(presentation, /showKeyboardOverlayInternal\(requestNative = !splitModeActive\)/);
assert.match(presentation, /if \(searchOverlayMode == "maps"\)[\s\S]*?www\.google\.com\/maps\/search\/\?api=1&query=/);
assert.match(presentation, /if \(splitModeActive\) \{[\s\S]*?showSplitControlsTemporarily\(\)[\s\S]*?return/);
assert.match(presentation, /is SystemVoiceCommand\.Music ->[\s\S]*?if \(splitModeActive\)[\s\S]*?YouTubePlayerHelper\.search\(web, query, autoPlayFirst = true\)[\s\S]*?else \{[\s\S]*?switchWebApp\(youtube, startUrl = target\)/);
assert.match(presentation, /is SystemVoiceCommand\.Tv ->[\s\S]*?if \(splitModeActive\)[\s\S]*?YouTubePlayerHelper\.search\(web, query, autoPlayFirst = true\)[\s\S]*?else if \(command\.channel\.isBlank\(\)\)/);
assert.match(presentation, /private fun handleVoiceQuery\(query: String\)[\s\S]*?if \(splitModeActive\)[\s\S]*?YouTubePlayerHelper\.search\(web, mediaQuery, autoPlayFirst = true\)[\s\S]*?return/);
assert.doesNotMatch(
  presentation.match(/private fun handleVoiceQuery\(query: String\)[\s\S]*?if \(currentActiveAppId == "tiktok"/)?.[0] || '',
  /switchWebApp/
);

assert.match(presentation, /setOnTouchListener \{ _, event ->[\s\S]*?MotionEvent\.ACTION_UP[\s\S]*?openSplitMapInput\(event\.x, event\.y\)[\s\S]*?false/);
assert.match(presentation, /private fun openSplitMapInput\(touchX: Float, touchY: Float\)/);
assert.match(presentation, /document\.elementFromPoint\([\s\S]*?document\.activeElement[\s\S]*?requestSplitMapSearch\(/);
assert.match(presentation, /CarMediaManager\.requestCarNativeSearch\(input\)/);
assert.match(presentation, /CarMediaManager\.mapsYoutubeSplitUrl = target/);
assert.match(presentation, /CarMediaManager\.isMapsYoutubeSplitActive = true/);
assert.match(presentation, /val startUrl = when \{[\s\S]*?isSplitMapsWebUrl\(savedMapUrl\)[\s\S]*?mapsYoutubeSplitLastRouteUrl[\s\S]*?else -> splitMapUrl/);
assert.match(presentation, /private fun isSplitMapsWebUrl\(raw: String\?\)/);
assert.match(presentation, /host == "play\.google\.com"[\s\S]*?return false/);
assert.match(presentation, /private fun isSplitMapsRouteUrl\(raw: String\?\)/);
assert.match(presentation, /private fun restoreSplitMapsRoute\(map: WebView\): Boolean/);
assert.match(presentation, /mapsYoutubeSplitLastRouteUrl/);
assert.match(presentation, /www\.google\.com\/maps\/search\/\?api=1&query=/);
assert.match(presentation, /settings\.setGeolocationEnabled\(true\)/);
assert.match(presentation, /splitMapDirectionsButton = TextView\(context\)[\s\S]*?text = "➤"[\s\S]*?requestSplitMapDirections\(\)/);
assert.match(presentation, /private fun requestSplitMapDirections\(initialDestination: String = ""\)/);
assert.match(presentation, /searchOverlayMode = "maps_directions"/);
assert.match(presentation, /private fun openSplitDirections\(destination: String\)/);
assert.match(presentation, /CarMediaManager\.mapsYoutubeSplitLastRouteUrl = target/);
assert.match(presentation, /GpsSpeedManager\.lastLocation\.value/);
assert.match(presentation, /&origin=" \+ android\.net\.Uri\.encode\("\$\{current\.latitude\},\$\{current\.longitude\}"\)/);
assert.match(presentation, /&travelmode=driving/);
assert.match(presentation, /if \(searchOverlayMode == "maps_directions"\)/);
assert.match(presentation, /settings\.userAgentString = mobileUserAgent/);
assert.match(presentation, /private fun handleSplitMapNavigation\(view: WebView\?, rawTarget: String\)/);
assert.match(presentation, /"https:\/\/www\.google\.com\/maps\/dir\/\?api=1" \+/);
assert.match(presentation, /scheme == "geo"/);
assert.match(presentation, /scheme == "google\.navigation"/);
assert.match(presentation, /scheme == "intent"/);
assert.match(presentation, /Intent\.parseUri\(target, Intent\.URI_INTENT_SCHEME\)/);
assert.match(presentation, /browser_fallback_url/);
assert.match(presentation, /isSplitMapsWebUrl\(fallback\)/);
assert.match(presentation, /package=com\.google\.android\.apps\.maps/);
assert.match(presentation, /return restoreSplitMapsRoute\(map\)/);
assert.match(presentation, /host == "play\.google\.com" \|\| host == "market\.android\.com"/);
assert.match(presentation, /splitLastTouchedPane = "maps"/);
assert.match(presentation, /splitLastTouchedPane = "youtube"/);

assert.match(presentation, /private fun injectYouTubeForCurrentViewport/);
assert.match(presentation, /val youtubeWeight = if \(CarMediaManager\.mapsYoutubeSplitSwapped\) leftWeight else rightWeight/);
assert.match(presentation, /\(usableWidth \* youtubeWeight\)\.roundToInt\(\)/);
assert.match(presentation, /carWidth = viewportWidth/);
assert.match(presentation, /carHeight = viewportHeight/);

assert.match(
  presentation,
  /fun showDashboard\(\) \{[\s\S]*?if \(splitModeActive\) leaveMapsYoutubeSplit/
);
assert.match(
  presentation,
  /override fun dismiss\(\) \{[\s\S]*?leaveMapsYoutubeSplit\(reattachYoutube = false, preservePersistentState = true\)[\s\S]*?releaseSplitMapWeb\(\)/
);


// In-pane navigation must not hand Google's Start button to Play Store.
assert.match(presentation, /splitNavigationUrl = "https:\/\/appassets\.androidplatform\.net\/assets\/car_split_navigation\.html"/);
assert.match(presentation, /uri\.host == "appassets\.androidplatform\.net"/);
assert.match(presentation, /"car_split_navigation\.html" -> "text\/html"/);
assert.match(presentation, /"leaflet\.js" -> "text\/javascript"/);
assert.match(presentation, /"leaflet\.css" -> "text\/css"/);
assert.match(presentation, /@android\.webkit\.JavascriptInterface[\s\S]*?fun startNavigation\(currentUrl: String, destination: String\)/);
assert.match(presentation, /injectSplitMapsStartButtonListener\(view\)/);
assert.match(presentation, /document\.addEventListener\('click'[\s\S]*?event\.stopImmediatePropagation\(\)/);
assert.match(presentation, /private fun startSplitNavigation\(routeUrl: String\? = null, destinationHint: String\? = null\)/);
assert.match(presentation, /splitMapWeb\?\.evaluateJavascript\("window\.ttubeNav&&ttubeNav\.updatePosition\(\$args\)"/);
assert.match(presentation, /onSplitNavigationPageReady\(view\)/);
assert.match(presentation, /private fun stopSplitNavigation\(\)/);
assert.match(presentation, /startSplitNavigation\(CarMediaManager\.mapsYoutubeSplitLastRouteUrl\)/);
assert.match(presentation, /uri\?\.getQueryParameter\("dir_action"\) == "navigate"/);
assert.match(media, /var mapsYoutubeNavigationDestination: String = ""/);
assert.match(media, /var mapsYoutubeLastDestination: String = ""/);
assert.match(navigationHtml, /window\.ttubeNav =?\s*\{|window\.ttubeNav=\{/);
assert.match(navigationHtml, /window\.ttubeNav=\{[\s\S]*?updatePosition:function/);
assert.match(navigationHtml, /geometries=geojson&steps=true/);
assert.match(navigationHtml, /routePending=true/);
assert.match(navigationHtml, /nearestRoutePoint\(\)\.distance>85/);
assert.match(navigationHtml, /function nearestRoutePoint\(\)/);
assert.match(navigationHtml, /step\.routeIndex>idx\+1/);
assert.match(navigationHtml, /cumulative\[idx\]/);
assert.match(navigationHtml, /OpenStreetMap/);
assert.doesNotMatch(navigationHtml, /mt\{s\}\.google\.com/);

console.log('PASS: independent Google Maps + YouTube split projection mode');
