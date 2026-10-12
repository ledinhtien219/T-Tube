const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');
const block = source.slice(source.indexOf('    fun applyTheme('), source.indexOf('    fun inject('));
const script = block.match(/val js = """([\s\S]*?)"""\.trimIndent\(\)/)[1];
let style;
const element = {classList:{contains:()=>false, add(){}, remove(){}},setAttribute(){}};
const context = {window:{},document:{
  getElementById:()=>style,
  createElement:()=>({}),
  head:{appendChild(value){style=value;}},
  documentElement:element, body:element, querySelector:()=>element
}};
// First install and later updates must both run, including when the style already exists.
vm.runInNewContext(script.replaceAll('$isDay','true'),context);
assert.match(style.textContent,/color-scheme: light/);
vm.runInNewContext(script.replaceAll('$isDay','false'),context);
assert.match(style.textContent,/color-scheme: dark/);
assert.equal(context.window.__carhudCurrentIsDay,false);
console.log('PASS YouTube theme: script parses, initial install and subsequent changes apply');
