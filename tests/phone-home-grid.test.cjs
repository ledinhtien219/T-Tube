const fs = require('node:fs');
const assert = require('node:assert/strict');

const main = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/MainActivity.kt',
  'utf8'
);
const layout = fs.readFileSync(
  'app/src/main/res/layout/activity_main.xml',
  'utf8'
);
const workflow = fs.readFileSync(
  '.github/workflows/build-apk.yml',
  'utf8'
);

// 1-3. Phone Home must be a 3 x 3 launcher, 9 apps/page, 3 horizontal pages.
assert.match(layout, /android:id="@\+id\/phoneAppPager"/);
assert.match(layout, /android:id="@\+id\/phoneAppPages"/);
assert.match(layout, /android:id="@\+id\/phoneAppPageIndicator"/);
assert.match(layout, /Vuốt ngang để chuyển/);
assert.match(main, /PHONE_HOME_PAGE_COUNT = 3/);
assert.match(main, /PHONE_HOME_APPS_PER_PAGE = 9/);
assert.match(main, /repeat\(PHONE_HOME_PAGE_COUNT\)/);
assert.match(main, /columnCount = 3/);
assert.match(main, /rowCount = 3/);
assert.match(main, /take\(PHONE_HOME_APPS_PER_PAGE\)/);
assert.match(main, /snapPhoneAppPagerToNearestPage/);

// 4-5. App source is WebAppManager.getEnabledApps(), capped at 3 * 9 = 27.
assert.match(main, /WebAppManager\.getEnabledApps\(this\)/);
assert.match(main, /take\(PHONE_HOME_PAGE_COUNT \* PHONE_HOME_APPS_PER_PAGE\)/);

// 6. Configuration/account items must never be hardcoded as Home app tiles.
const renderStart = main.indexOf('private fun renderPhoneAppPages()');
const renderEnd = main.indexOf('private fun createPhoneAppTile', renderStart);
assert.ok(renderStart >= 0 && renderEnd > renderStart);
const renderBlock = main.slice(renderStart, renderEnd);
assert.doesNotMatch(renderBlock, /Quyền truy cập|Tên hiển thị|Bản quyền/);

// 7. Keep the legacy 2x2 block hidden for saved preference/backward compatibility.
assert.match(
  layout,
  /Legacy 2x2 service cards kept only for saved display-setting compatibility\.[\s\S]*?android:visibility="gone"/
);

// 8-9. This regression test must remain wired into APK CI.
assert.match(workflow, /node tests\/phone-home-grid\.test\.cjs/);

console.log(
  'PASS phone Home: 3x3, 9 apps/page, 3 pages, max 27 enabled apps, legacy 2x2 hidden'
);
