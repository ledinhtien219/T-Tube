const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs');
const s=fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');
const r=fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MimiAppSwitchRouter.kt','utf8');
test('app switch runs before TikTok search',()=>{
 const a=s.indexOf('MimiAppSwitchRouter.target(query, AssistantConfig.name(context))');
 const b=s.indexOf('TtubeAssistant.handle(assistantHost, query');
 const c=s.indexOf('if (currentActiveAppId == "tiktok" && !isDashboardShowing && !explicitMedia)');
 assert.ok(a>0&&a<b&&b<c);
});
test('switch opens actual app rather than video results',()=>{
 const x=s.split('MimiAppSwitchRouter.target(query, AssistantConfig.name(context))')[1].split('val voiceIsSearchBox')[0];
 assert.match(x,/switchWebApp\(destination, embedded = false\)/);
 assert.match(x,/return/);
});
test('bare YouTube remains TikTok keyword',()=>{
 assert.match(r,/if \(addressed\) norm else return null/);
});
