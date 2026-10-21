const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt', 'utf8');
const helper = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');

assert.match(
  presentation,
  /ASPECT_MODES = listOf\("fill", "contain", "cover", "4:3", "21:9"\)/,
  'YouTube/HTML5 aspect modes should expose five supported screen modes'
);

assert.match(
  presentation,
  /toolButton\(R\.drawable\.ic_bar_setting, "Cài đặt"\)[\s\S]*?showVideoQualityMenu\(\)/,
  'YouTube gear button should open the unified settings menu'
);

for (const label of [
  'Cài đặt YouTube',
  'Màn hình',
  '↔ Tràn viền',
  '▣ Chuẩn 16:9',
  '⤢ Phóng to',
  '4:3 Cổ điển',
  '21:9 Siêu rộng',
  'Chất lượng video'
]) {
  assert.ok(presentation.includes(label), `Missing YouTube settings label: ${label}`);
}

assert.match(
  presentation,
  /prefs\.getString\("car_video_aspect_mode", "fill"\)/,
  'YouTube screen mode should restore from persisted preferences'
);

assert.match(
  presentation,
  /applyVideoAspectRatio\(mode\)[\s\S]*?hideVideoQualityMenu\(\)/,
  'Selecting a screen mode should apply immediately and close the menu'
);

assert.ok(
  !/ASPECT_MODES = listOf\([^\n]*"split"/.test(presentation),
  'Unsupported split mode must not appear in the shared YouTube screen mode list'
);

for (const mode of ['contain', 'fill', 'cover', '4:3', '21:9']) {
  assert.ok(helper.includes(`m === '${mode}'`) || helper.includes(`m === "${mode}"`) || helper.includes(`'$\{mode}'`), `Helper should support ${mode}`);
}

assert.match(helper, /aspect-ratio', '21 \/ 9'/, 'YouTube helper should support ultrawide 21:9');
assert.match(helper, /object-fit', 'cover'/, 'YouTube helper should support crop/zoom cover mode');

console.log('PASS YouTube gear settings: screen modes + quality menu + persisted aspect selection');
