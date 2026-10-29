from pathlib import Path
def edit(path, before, after):
    p=Path(path); old=p.read_text(encoding="utf-8")
    if old.count(before)!=1: raise RuntimeError(f"patch expected 1 match, found {old.count(before)}: {p} / {before[:80]}")
    p.write_text(old.replace(before,after,1),encoding="utf-8")
u="app/src/main/java/com/carhud/aaproxy/MimiAssistantPanel.kt"
c="app/src/main/java/com/carhud/aaproxy/CarPresentation.kt"
a="app/src/main/java/com/carhud/aaproxy/MimiSmartAssistant.kt"
edit(u,"import android.os.Looper","import android.os.Looper\nimport android.os.SystemClock")
edit(u,"    private var released = false","    private var released = false\n    private var captureDeadlineMs = 0L")
edit(u,"LayoutParams(d(440), ViewGroup.LayoutParams.WRAP_CONTENT","LayoutParams(d(315), ViewGroup.LayoutParams.WRAP_CONTENT")
edit(u,"topMargin = d(22)","topMargin = d(52)")
edit(u,"min(d(440), (w - d(32)).coerceAtLeast(d(220)))","min(d(315), (w - d(28)).coerceAtLeast(d(160)))")
edit(u,"    fun showListening(label: String = \"Đang lắng nghe…\") {",'''    // Repeated partial results cannot keep the panel covering video indefinitely.
    private fun captureTimeout(requestedMs: Long) {
        if (captureDeadlineMs == 0L) captureDeadlineMs = SystemClock.uptimeMillis() + 24_000L
        val left = (captureDeadlineMs - SystemClock.uptimeMillis()).coerceAtLeast(0L)
        collapseLater(min(requestedMs, left))
    }

    fun onVoiceIdle() {
        if (released) return
        if (mode == Mode.LISTENING || mode == Mode.THINKING) {
            face.listening = false
            wave.active = false
            collapseLater(1_200L)
        }
    }

    fun showListening(label: String = "Đang lắng nghe…") {''')
edit(u,"        mode = Mode.LISTENING\n        stateLine.text","        if (mode != Mode.LISTENING && mode != Mode.THINKING) captureDeadlineMs = 0L\n        mode = Mode.LISTENING\n        stateLine.text")
edit(u,"collapseLater(35_000L)","captureTimeout(24_000L)")
edit(u,"collapseLater(15_000L)","captureTimeout(12_000L)")
edit(u,"        mode = Mode.THINKING\n        stateLine.text = \"Mimi đã nghe\"","        captureDeadlineMs = 0L\n        mode = Mode.THINKING\n        stateLine.text = \"Mimi đã nghe\"")
edit(u,"collapseLater(7_000L) // fallback media/search may have no assistant response","collapseLater(2_800L) // return quickly to the mini avatar")
edit(u,"        mode = Mode.ANSWER\n        stateLine.text","        captureDeadlineMs = 0L\n        mode = Mode.ANSWER\n        stateLine.text")
edit(u,"(3_000L + sentence.length * 55L).coerceIn(4_000L, 10_000L)","(2_000L + sentence.length * 30L).coerceIn(3_000L, 6_000L)")
edit(u,"        mode = Mode.ERROR\n        stateLine.text","        captureDeadlineMs = 0L\n        mode = Mode.ERROR\n        stateLine.text")
edit(u,"        collapseLater(4_000L)\n    }\n\n    /** Show Mimi","        collapseLater(3_000L)\n    }\n\n    /** Show Mimi")
edit(u,"        handler.removeCallbacks(autoCollapse)\n        mode = Mode.COLLAPSED","        handler.removeCallbacks(autoCollapse)\n        captureDeadlineMs = 0L\n        mode = Mode.COLLAPSED")
edit(u,"        if (released || mode == Mode.HIDDEN) return\n        handler.removeCallbacks(autoCollapse)\n        mode = Mode.COLLAPSED","        if (released || mode == Mode.HIDDEN) return\n        handler.removeCallbacks(autoCollapse)\n        captureDeadlineMs = 0L\n        mode = Mode.COLLAPSED")
edit(u,"        handler.removeCallbacks(autoCollapse)\n        mode = Mode.HIDDEN","        handler.removeCallbacks(autoCollapse)\n        captureDeadlineMs = 0L\n        mode = Mode.HIDDEN")
edit(c,'        val addressed = afterWake || AssistantIntentParser.hasWake(query, AssistantConfig.name(context))','''        val addressed = afterWake || AssistantIntentParser.hasWake(query, AssistantConfig.name(context))
        val commandText = if (afterWake) query.trim()
            else AssistantIntentParser.stripWake(query, AssistantConfig.name(context)).trim()
        // Explicit music/video/TV requests must not be treated as TikTok/browser searches.
        val folded = AssistantIntentParser.fold1to1(commandText)
        val explicitMedia = addressed && (
            Regex("""^(?:vtv\\d+|htv\\d+|vtc\\d+)(?:\\b|$)""").containsMatchIn(folded) ||
            Regex("""^(?:cho |hay )?(?:phat|mo|bat|xem|nghe|tim|choi) (?:cho )?(?:nhac|bai|video|clip|youtube|kenh|tivi|truyen hinh|vtv\\d*|htv\\d*)\\b""").containsMatchIn(folded)
        )''')
edit(c,"if (mimiSmartAssistant.tryHandle(query, addressed)","if (!explicitMedia && mimiSmartAssistant.tryHandle(query, addressed)")
edit(c,'if (currentActiveAppId == "tiktok" && !isDashboardShowing) {','if (currentActiveAppId == "tiktok" && !isDashboardShowing && !explicitMedia) {')
edit(c,'if (isBrowserApp() && !isDashboardShowing) {\n            YouTubePlayerHelper.setDuckingVolume(web, 1.0f)\n            navigateBrowser(query)','if (isBrowserApp() && !isDashboardShowing && !explicitMedia) {\n            YouTubePlayerHelper.setDuckingVolume(web, 1.0f)\n            navigateBrowser(query)')
edit(c,'val tvCmd = TvVoiceHelper.parse(query)','val tvCmd = TvVoiceHelper.parse(query) ?: TvVoiceHelper.parse(commandText)')
edit(c,"val mediaQuery = VoiceQueryResolver.cleanForMediaSearch(query)","val mediaQuery = VoiceQueryResolver.cleanForMediaSearch(if (addressed) commandText else query)")
edit(c,"                VoiceSearchManager.State.SUCCESS, VoiceSearchManager.State.IDLE -> Unit","                VoiceSearchManager.State.SUCCESS -> Unit\n                VoiceSearchManager.State.IDLE -> mimiPanel?.onVoiceIdle()")
edit(c,'        val isChangingApp = previousAppId != app.id','        val isChangingApp = previousAppId != app.id\n        if (isChangingApp) mimiPanel?.collapse()')
edit(a,"        if (!AssistantConfig.aiEnabled(context)) return false",'''        if (!AssistantConfig.aiEnabled(context)) {
            val s = AssistantIntentParser.fold1to1(utterance).trim()
            if (s.endsWith("?") || Regex("""^(tai sao|vi sao|giai thich|la gi|nhu the nao|co the|cho toi biet|ke cho)\\b""").containsMatchIn(s)) {
                reply(show, "Mimi chưa bật hội thoại AI. Hãy bật AI trong Cài đặt trợ lý trên điện thoại.")
                return true
            }
            return false
        }''')
g="app/build.gradle.kts"
edit(g,"versionCode = 307","versionCode = 308")
edit(g,'versionName = "1.1.9"','versionName = "1.1.10"')
print("Hey Mimi v7 stability changes applied")
