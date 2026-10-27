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
assert.match(
  presentation,
  /addView\([\s\S]*?mapWeb,[\s\S]*?LinearLayout\.LayoutParams\(0, ViewGroup\.LayoutParams\.MATCH_PARENT, 1f\)[\s\S]*?addView\([\s\S]*?web,[\s\S]*?LinearLayout\.LayoutParams\(0, ViewGroup\.LayoutParams\.MATCH_PARENT, 1f\)/
);

assert.match(
  presentation,
  /if \(app\.id == "maps_youtube_split"\) \{[\s\S]*?showMapsYoutubeSplit\(\)[\s\S]*?return/
);
assert.match(
  presentation,
  /fun dispatchProjectedTouch\(event: MotionEvent\): Boolean \{[\s\S]*?if \(splitModeActive\) \{[\s\S]*?dispatchTouchEvent\(copy\)[\s\S]*?return true/
);

assert.match(presentation, /private fun requestSplitMapSearch\([\s\S]*?initialText: String = ""[\s\S]*?hint: String = "Tìm địa điểm trên Google Maps"/);
assert.match(presentation, /setOnTouchListener \{ _, event ->[\s\S]*?MotionEvent\.ACTION_UP[\s\S]*?openSplitMapInput\(event\.x, event\.y\)[\s\S]*?false/);
assert.match(presentation, /private fun openSplitMapInput\(touchX: Float, touchY: Float\)/);
assert.match(presentation, /document\.elementFromPoint\([\s\S]*?document\.activeElement[\s\S]*?requestSplitMapSearch\(/);
assert.match(presentation, /CarMediaManager\.requestCarNativeSearch\(input\)/);
assert.match(presentation, /CarMediaManager\.mapsYoutubeSplitUrl = target/);
assert.match(presentation, /CarMediaManager\.isMapsYoutubeSplitActive = true/);
assert.match(presentation, /mapWeb\.loadUrl\(CarMediaManager\.mapsYoutubeSplitUrl\.ifBlank \{ splitMapUrl \}\)/);
assert.match(presentation, /www\.google\.com\/maps\/search\/\?api=1&query=/);
assert.match(presentation, /settings\.setGeolocationEnabled\(true\)/);

assert.match(presentation, /private fun injectYouTubeForCurrentViewport/);
assert.match(presentation, /viewportWidth = if \(splitModeActive\) \(carWidth \/ 2\)/);
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

console.log('PASS: independent Google Maps + YouTube split projection mode');
