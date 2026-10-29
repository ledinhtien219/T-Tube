from pathlib import Path
p = Path("app/src/main/java/com/carhud/aaproxy/CarPresentation.kt")
s = p.read_text(encoding="utf8")
before = "val afterWake = heyMimiConversation.consume(now)"
after = "val afterWake = heyMimiConversation.consume(now) || mimiTapSession.also { mimiTapSession = false }"
if s.count(before) != 1: raise RuntimeError("missing afterWake anchor")
s = s.replace(before, after)
before = "val appId = MimiAppSwitchRouter.target(query, AssistantConfig.name(context))"
after = '''val appId = MimiAppSwitchRouter.target(
                if (afterWake && !AssistantIntentParser.hasWake(query, AssistantConfig.name(context))) "Mimi $query" else query,
                AssistantConfig.name(context)
            )'''
if s.count(before) != 1: raise RuntimeError("missing app switch anchor")
s = s.replace(before, after)
p.write_text(s, encoding="utf8")
print("Mimi v11 afterWake app routing applied")
