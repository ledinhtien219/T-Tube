package com.carhud.aaproxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class MimiAppSwitchRouterTest {
    @Test fun youtubeLaunch() {
        listOf("mở YouTube","Mimi mở YouTube","Hey Mimi mở YouTube đi",
            "Mi mi ơi mở You Tube","chuyển sang YouTube","đổi sang YouTube",
            "Mimi ơi YouTube","Mimi bật Youtube","mở ứng dụng YouTube",
            "giúp tôi mở YouTube","cho tôi mở Youtube","mở Youtube Pro",
            "open youtube","Mimi gọi Youtube"
        ).forEach { assertEquals(it, "youtube", MimiAppSwitchRouter.target(it,"Mimi")) }
    }
    @Test fun otherApps() {
        assertEquals("tiktok",MimiAppSwitchRouter.target("Hey Mimi mở TikTok","Mimi"))
        assertEquals("web",MimiAppSwitchRouter.target("mở trình duyệt web","Mimi"))
        assertEquals("spotify",MimiAppSwitchRouter.target("mở Spotify","Mimi"))
    }
    @Test fun neverStealVideoSearch() {
        listOf("YouTube","tìm video YouTube","mở video YouTube",
            "Mimi mở video YouTube","Mimi tìm nhạc Sơn Tùng trên YouTube",
            "tìm kiếm YouTube","mở bài hát YouTube","mở kênh VTV3",
            "Mimi phát nhạc Sơn Tùng","Mimi mở YouTube tìm Sơn Tùng",
            "YouTube review","mở Youtube của Sơn Tùng"
        ).forEach { assertNull(it,MimiAppSwitchRouter.target(it,"Mimi")) }
    }
}
