const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const b=path.join(__dirname,'..','app/src/main/java/com/carhud/aaproxy');
const read=(x)=>fs.readFileSync(path.join(b,x),'utf8');
const ui=read('MimiAssistantPanel.kt'),c=read('CarPresentation.kt'),ai=read('MimiSmartAssistant.kt');
test('bounded card and non-resettable recognition deadline',()=>{
 assert.match(ui,/captureDeadlineMs/);assert.match(ui,/captureTimeout\(12_000L\)/);
 assert.match(ui,/LayoutParams\(d\(315\)/);assert.match(ui,/fun onVoiceIdle\(\)/);
 assert.match(c,/State.IDLE -> mimiPanel\?\.onVoiceIdle\(\)/);
});
test('explicit media bypasses unrelated TikTok/browser searches',()=>{
 assert.match(c,/val explicitMedia = addressed/);assert.match(c,/!isDashboardShowing && !explicitMedia/);
 assert.match(c,/TvVoiceHelper.parse\(commandText\)/);
 assert.match(c,/VoiceQueryResolver.cleanForMediaSearch\(if \(addressed\) commandText else query\)/);
});
test('AI disabled explains instead of searching YouTube for explicit questions',()=>assert.match(ai,/Mimi chưa bật hội thoại AI/));
test('version code is new Play upload',()=>{
 const gradle=fs.readFileSync(path.join(__dirname,'..','app/build.gradle.kts'),'utf8');
 assert.match(gradle,/versionCode = 308/);assert.match(gradle,/versionName = "1.1.10"/);
});
