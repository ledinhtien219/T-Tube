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

assert.match(layout, /android:id="@\+id\/phoneAppPager"/);
assert.match(layout, /android:id="@\+id\/phoneAppPages"/);
assert.match(layout, /android:id="@\+id\/phoneAppPageIndicator"/);
assert.match(layout, /Vuốt ngang để chuyển/);

assert.match(main, /PHONE_HOME_PAGE_COUNT = 3/);
assert.match(main, /PHONE_HOME_APPS_PER_PAGE = 9/);
assert.match(main, /WebAppManager\.getEnabledApps\(this\)/);
assert.match(main, /repeat\(PHONE_HOME_PAGE_COUNT\)/);
assert.match(main, /columnCount = 3/);
assert.match(main, /rowCount = 3/);
assert.match(main, /take\(PHONE_HOME_APPS_PER_PAGE\)/);
assert.match(main, /snapPhoneAppPagerToNearestPage/);

console.log('PASS phone Home launcher: 3x3 apps per page across 3 swipe pages');
