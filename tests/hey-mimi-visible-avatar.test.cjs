const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const base=path.resolve(__dirname,'..','app/src/main/java/com/carhud/aaproxy');
const panel=fs.readFileSync(path.join(base,'MimiAssistantPanel.kt'),'utf8');
const presentation=fs.readFileSync(path.join(base,'CarPresentation.kt'),'utf8');
test('Mimi can show passive avatar before voice starts',()=>{
 const ready=panel.split('fun showReadyBubble()')[1]?.split('fun collapse()')[0]||'';
 assert.match(ready,/mode = Mode.COLLAPSED/);
 assert.match(ready,/bubble.visibility = View.VISIBLE/);
 assert.match(ready,/card.visibility = View.GONE/);
 assert.doesNotMatch(ready,/onBubbleTap\(/);
 assert.match(presentation,/if \(AssistantConfig\.isEnabled\(context\)\) panel\.showReadyBubble\(\)/);
});
test('enable and disable preferences update Mimi bubble',()=>{
 const logic=presentation.split('if (key == AssistantConfig.PREF_ENABLED)')[1]?.split('if (key == SettingsActivity.KEY_DESKTOP_MODE')[0]||'';
 assert.match(logic,/mimiPanel\?\.showReadyBubble\(\)/);
 assert.match(logic,/mimiPanel\?\.hide\(\)/);
});
test('bubble avoids top and bottom bars and YouTube uses Mimi icon',()=>{
 assert.match(panel,/Gravity.CENTER_VERTICAL or Gravity.END/);
 assert.match(presentation,/val youtubeMicButton = toolButton\([\s\S]*?R.drawable.ic_bar_assistant/);
});
