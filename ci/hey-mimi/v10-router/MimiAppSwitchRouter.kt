package com.carhud.aaproxy
/** Explicit app switching is independent from TikTok/browser search. */
internal object MimiAppSwitchRouter {
    private val lead = Regex("""^(?:(?:hay|vui long|lam on|giup toi|giup minh|cho toi|cho minh|toi muon|minh muon|toi can)\s+)+""")
    private val action = Regex("""^(?:chuyen sang|chuyen qua|chuyen den|chuyen toi|doi sang|mo|bat|vao|chuyen|sang|qua|truy cap|goi|open|launch|switch to|xem)\s+(?:(?:cho toi|cho minh)\s+)?(?:(?:ung dung|app|trang|nen tang)\s+)?(.+)$""")
    private val tail = Regex("""(?:\s+(?:di|nhe|voi|nao|len|ngay|luon|giup toi|giup minh|cho toi|cho minh|duoc khong))+$""")
    private val aliases = mapOf(
        "youtube" to setOf("youtube","you tube","yutub","diu tup","iu tup","youtube pro","youtup"),
        "tiktok" to setOf("tiktok","tik tok","tit tok","tich tok"),
        "web" to setOf("trinh duyet","trinh duyet web","web","google chrome","chrome"),
        "spotify" to setOf("spotify"),
        "vtvgo" to setOf("vtvgo","vtv go"),
        "tv360" to setOf("tv360","tv 360")
    )
    fun target(query: String, assistantName: String): String? {
        val addressed = AssistantIntentParser.hasWake(query, assistantName)
        val body = AssistantIntentParser.stripWake(query, assistantName)
        val norm = AssistantIntentParser.fold1to1(body)
            .replace(Regex("""[,.!?:;]+""")," ")
            .replace(Regex("""\s+""")," ").trim().replace(lead,"")
        val target = action.matchEntire(norm)?.groupValues?.get(1)
            ?: if (addressed) norm else return null
        val exact = target.replace(tail,"").trim()
        if (exact.isBlank()) return null
        return aliases.entries.firstOrNull { exact in it.value }?.key
    }
}
