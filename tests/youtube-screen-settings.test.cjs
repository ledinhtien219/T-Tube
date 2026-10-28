const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');
const menu = presentation.slice(
  presentation.indexOf('private fun showVideoQualityMenu()'),
  presentation.indexOf('private fun hideVideoQualityMenu()')
);

assert.match(presentation, /YOUTUBE_SCREEN_MODES = listOf\("fill", "contain", "cover", "4:3", "21:9"\)/);
assert.match(presentation, /ASPECT_MODES = listOf\("fill", "contain", "cover", "split", "4:3"\)/);
assert.match(presentation, /toolButton\(R\.drawable\.ic_bar_setting, "Cài đặt"\)[\s\S]*?showVideoQualityMenu\(\)/);

for (const label of [
  'Cài đặt YouTube', 'Màn hình', '↔ Tràn viền', '▣ Chuẩn 16:9',
  '⤢ Phóng to', '4:3 Cổ điển', '21:9 Siêu rộng', 'Chất lượng video'
]) assert.ok(menu.includes(label), 'Missing menu item: ' + label);

assert.match(menu, /prefs\.getString\("car_video_aspect_mode", "fill"\)/);
assert.match(menu, /YOUTUBE_SCREEN_MODES\.chunked\(2\)/);
assert.doesNotMatch(menu, /ASPECT_MODES\.chunked\(2\)/);
assert.match(menu, /applyVideoAspectRatio\(mode\)[\s\S]*?hideVideoQualityMenu\(\)/);
assert.match(menu, /if \(qualityIds\.isEmpty\(\)\)/);
assert.match(menu, /Phần tùy chỉnh màn hình vẫn dùng được/);
assert.match(menu, /YouTubePlayerHelper\.selectVideoQuality/);
assert.ok(menu.indexOf('val overlay = FrameLayout') > menu.indexOf('val qualityIds ='),
  'Overlay must be built even when YouTube quality options are empty');

for (const mode of ['contain', 'fill', 'cover', '4:3', '21:9']) {
  assert.ok(helper.includes("m === '" + mode + "'"), 'YouTube helper lacks ' + mode);
}
assert.match(helper, /aspect-ratio', '21 \/ 9'/);
console.log('PASS YouTube settings: saved screen sizes, 16:9/4:3/21:9, no IPTV split, quality optional');
