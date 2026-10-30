const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const src=fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt','utf8');
const js=src.match(/val JS_CLEANUP_AND_ADBLOCK: String = """([\s\S]*?)"""\.trimIndent\(\)/)?.[1];
assert.ok(js); new Function(js);
test('custom overlay removed and real native scrolling video list retained',()=>{
 assert.doesNotMatch(js,/function makePipChooser|function updatePipChooser|function collectPipSuggestions/);
 assert.doesNotMatch(js,/Chọn video tiếp theo|Tìm thêm video/);
 assert.match(js,/function findRecommendationRoot/);
 assert.match(js,/function syncRecommendationPane/);
 assert.match(js,/flex-direction:column!important/);
 assert.match(js,/overflow-y:auto!important/);
 assert.match(js,/syncRecommendationPane\(pip\)/);
 assert.match(js,/if \(pip\) pinMiniPlayerHost\(\)/);
});
test('routing selects a populated YouTube list and cleans up after closing',()=>{
 const start=js.indexOf('var carhudRecommendationRoot = null;');
 const end=js.indexOf('// In some car touch stacks',start);
 const code=js.slice(start,end);
 const pane=(n)=>({attrs:{},getBoundingClientRect(){return {width:200}},
 querySelectorAll(){return Array.from({length:n},(_,i)=>({getAttribute(){return '/watch?v='+i}}))},
 setAttribute(k,v){this.attrs[k]=v},removeAttribute(k){delete this.attrs[k]}});
 const one=pane(1), three=pane(3);let cleared=false;
 const doc={querySelector(s){return s==='ytd-watch-flexy #secondary'?one:s==='ytm-watch .watch-below-the-fold'?three:null},
 getElementById(){return {remove(){cleared=true}}}};
 const f=new Function('document',code+';return {findRecommendationRoot,syncRecommendationPane};')(doc);
 assert.equal(f.findRecommendationRoot(),three);f.syncRecommendationPane(true);
 assert.equal(three.attrs['data-carhud-rec-scroll'],'1');assert.ok(cleared);
 f.syncRecommendationPane(false);assert.equal(three.attrs['data-carhud-rec-scroll'],undefined);
});
