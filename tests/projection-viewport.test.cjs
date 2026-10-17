const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');
const template = source.match(/val screenInitJs = """([\s\S]*?)"""\.trimIndent\(\)/)[1];
for (const [width, height, dpi] of [[800,480,160], [1920,480,160], [1920,720,240], [720,1280,240]]) {
  for (const phoneDpi of [160, 420, 560]) {
    const values = {carWidth:width, carHeight:height, carDpi:dpi, effectivePhoneDpi:phoneDpi,
      isUltrawide:width/height>=1.85, isPortrait:width/height<.95, aspectRatio:width/height};
    const js = template.replace(/\$(\w+)/g, (_, key) => {
      assert.ok(key in values, `Unknown Kotlin interpolation ${key}`);
      return String(values[key]);
    });
    const props = {}, attrs = {};
    const meta = {getAttribute:key=>attrs[key], setAttribute:(key,value)=>{attrs[key]=value}};
    const document = {querySelector:()=>meta, documentElement:{classList:{add(){},remove(){}},
      style:{setProperty:(key,value)=>{props[key]=value}}}};
    vm.runInNewContext(js, {document, window:{AndroidVoice:{isAuto:()=>true}}});
    assert.equal(attrs.content, 'width=device-width, initial-scale=1.0, maximum-scale=2.0, user-scalable=yes');
    assert.equal(props['--carhud-screen-w'], `${width*160/dpi}px`);
    assert.equal(props['--carhud-screen-h'], `${height*160/dpi}px`);
  }
}
console.log('PASS projected viewport: portrait/wide/ultrawide CSS follows car density independently of phone DPI');
