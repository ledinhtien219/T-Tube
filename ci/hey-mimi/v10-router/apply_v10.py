from pathlib import Path
import shutil
base=Path('app/src/main/java/com/carhud/aaproxy')
p=base/'CarPresentation.kt'
s=p.read_text(encoding='utf8')
key='''        // Trợ lý đi trước luồng cũ. Ở TikTok/trình duyệt, giọng nói bình thường
        // vẫn là tìm kiếm; riêng câu sau "Hey Mimi" là lệnh trợ lý.
        val voiceIsSearchBox = !isDashboardShowing && (currentActiveAppId == "tiktok" || isBrowserApp())
'''
fix='''        // Explicit app switching wins over TikTok/browser voice search.
        if (AssistantConfig.isEnabled(context)) {
            val appId = MimiAppSwitchRouter.target(query, AssistantConfig.name(context))
            if (appId != null) {
                cancelHeyMimiFollowUp()
                hideSearchOverlay()
                val destination = WebAppManager.getAllApps(context).firstOrNull { it.id == appId }
                    ?: WebAppManager.DEFAULT_APPS.firstOrNull { it.id == appId }
                if (destination != null) {
                    YouTubePlayerHelper.setDuckingVolume(web, 1.0f)
                    switchWebApp(destination, embedded = false)
                    showAssistantBanner("Đang mở " + destination.name + ".")
                } else {
                    showAssistantBanner("Mimi không tìm thấy ứng dụng bạn muốn mở.")
                }
                return
            }
        }

'''+key
assert s.count(key)==1,"CarPresentation anchor missing"
p.write_text(s.replace(key,fix),encoding='utf8')
g=Path('app/build.gradle.kts'); s=g.read_text(encoding='utf8')
assert s.count('versionCode = 310')==1
assert s.count('versionName = "1.1.12"')==1
g.write_text(s.replace('versionCode = 310','versionCode = 311').replace('versionName = "1.1.12"','versionName = "1.1.13"'),encoding='utf8')
shutil.copyfile('ci/hey-mimi/v10-router/MimiAppSwitchRouter.kt',base/'MimiAppSwitchRouter.kt')
shutil.copyfile('ci/hey-mimi/v10-router/MimiAppSwitchRouterTest.kt',Path('app/src/test/java/com/carhud/aaproxy/MimiAppSwitchRouterTest.kt'))
print('Mimi Pro v10 applied')
