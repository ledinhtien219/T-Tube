const fs = require('node:fs');
const assert = require('node:assert/strict');

const presentation = fs.readFileSync(
  'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt',
  'utf8'
);

assert.match(presentation, /currentActiveAppId != "web"[\s\S]*?applyUniversalWebTheme/);
assert.match(presentation, /setPadding\(dp\(6\), dp\(4\), dp\(6\), dp\(4\)\)/);
assert.doesNotMatch(presentation, /setPadding\(dp\(4\), dp\(4\), dp\(76\), dp\(4\)\)/);
assert.match(presentation, /params\.topMargin = if \(currentActiveAppId == "web" && !isDashboardShowing\) dp\(58\) else 0/);
assert.match(presentation, /web\.setBackgroundColor\(if \(isDayMode\(\)\) Color\.WHITE else Color\.BLACK\)/);

console.log('PASS browser projection UI: full-width toolbar + browser pages keep native styling');
