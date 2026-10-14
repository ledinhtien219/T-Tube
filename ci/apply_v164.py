from pathlib import Path
import re

# ---------- version ----------
p = Path('app/build.gradle.kts')
s = p.read_text()
s = s.replace('versionCode = 250', 'versionCode = 251')
s = s.replace('versionName = "1.0.63"', 'versionName = "1.0.64"')
p.write_text(s)

# ---------- CarPresentation: only toolbar/theme/transition/TikTok back ----------
p = Path('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt')
s = p.read_text()

# YouTube transition shield state/helpers.
if 'private var youtubeTransitionShield: View? = null' not in s:
    anchor = '    private var videoTimePill: TextView? = null\n'
    helper = '''    private var videoTimePill: TextView? = null
    private var youtubeTransitionShield: View? = null

    private fun showYouTubeTransitionShield() {
        if (currentActiveAppId != "youtube" || isDashboardShowing) return
        val shield = youtubeTransitionShield ?: View(context).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = false
            elevation = dp(8).toFloat()
            youtubeTransitionShield = this
            root.addView(this, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        shield.visibility = View.VISIBLE
        shield.alpha = 1f
        root.bringChildToFront(shield)
        sidebarContainer?.let { root.bringChildToFront(it) }
        topToolbarContainer?.let { root.bringChildToFront(it) }
        hudOverlay?.let { root.bringChildToFront(it) }
    }

    private fun hideYouTubeTransitionShield() {
        youtubeTransitionShield?.visibility = View.GONE
    }
'''
    if anchor not in s:
        raise SystemExit('v164: videoTimePill anchor missing')
    s = s.replace(anchor, helper, 1)

if 'if (playing && currentActiveAppId == "youtube") hideYouTubeTransitionShield()' not in s:
    s = s.replace(
        '    fun setPlaybackState(playing: Boolean) {\n        isCurrentlyPlaying = playing\n',
        '    fun setPlaybackState(playing: Boolean) {\n        isCurrentlyPlaying = playing\n        if (playing && currentActiveAppId == "youtube") hideYouTubeTransitionShield()\n',
        1
    )

if 'showYouTubeTransitionShield()' in s and 'if (url.contains("/watch", true)' not in s:
    s = s.replace(
        '                browserLoading = true\n                updateBrowserToolbar(url)\n',
        '''                if (currentActiveAppId == "youtube") {
                    if (url.contains("/watch", true) || url.contains("youtu.be/", true)) showYouTubeTransitionShield()
                    else hideYouTubeTransitionShield()
                }
                browserLoading = true
                updateBrowserToolbar(url)
''',
        1
    )

# Replace TikTok toolbar block as one isolated block.
pattern = re.compile(
    r'''        if \(currentActiveAppId == "tiktok"\) \{.*?            autoHideHandler\.removeCallbacks\(hideBarsRunnable\)\n            return\n        \}\n''',
    re.S
)
replacement = '''        if (currentActiveAppId == "tiktok") {
            // TikTok controls stay vertical at the far left so they do not cover video content.
            val isDay = isDayMode()
            val tiktokIconSizeDp = ((58 * scale) / 100).coerceIn(54, 72)
            val controls = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(
                    if (isDay) Color.parseColor("#F5FFFFFF") else Color.parseColor("#F20B1324"),
                    22f,
                    if (isDay) Color.parseColor("#9938BDF8") else Color.parseColor("#8838BDF8"),
                    2
                )
                setPadding(dp(5), dp(5), dp(5), dp(5))
                addView(createBarIcon(R.drawable.ic_bar_back, tiktokIconSizeDp) { goBackTikTokOnly() })
                addView(createBarIcon(R.drawable.ic_bar_home, tiktokIconSizeDp) { showDashboard() })
                addView(createBarIcon(R.drawable.ic_bar_search, tiktokIconSizeDp) { showTikTokSearchOverlay() })
                elevation = dp(18).toFloat()
            }
            val wrapper = FrameLayout(context).apply {
                isClickable = false
                addView(controls)
            }
            sidebarView = controls
            sidebarContainer = wrapper
            videoTimePill = null
            root.addView(wrapper, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.TOP).apply {
                    leftMargin = dp(8)
                    topMargin = dp(24)
                })
            root.bringChildToFront(wrapper)
            autoHideHandler.removeCallbacks(hideBarsRunnable)
            return
        }
'''
s, n = pattern.subn(replacement, s, count=1)
if n != 1:
    raise SystemExit('v164: TikTok toolbar block not found')

# YouTube toolbar: theme-aware colors, scoped to function only.
start = s.find('    private fun buildYouTubeMediaToolbar(scale: Int) {')
end = s.find('    private val autoHideHandler', start)
if start < 0 or end < 0:
    raise SystemExit('v164: YouTube toolbar function not found')
yt = s[start:end]
if 'val panelBg = if (isDay)' not in yt:
    yt = yt.replace(
        '    private fun buildYouTubeMediaToolbar(scale: Int) {\n',
        '''    private fun buildYouTubeMediaToolbar(scale: Int) {
        val isDay = isDayMode()
        val panelBg = if (isDay) Color.parseColor("#F5FFFFFF") else Color.parseColor("#E60B1118")
        val panelBorder = if (isDay) Color.parseColor("#8038BDF8") else Color.parseColor("#506B8299")
        val buttonBg = if (isDay) Color.parseColor("#EEF6FAFF") else Color.parseColor("#D91A2430")
        val buttonBorder = if (isDay) Color.parseColor("#A7C9D9E8") else Color.parseColor("#33485C70")
        val ink = if (isDay) Color.parseColor("#142033") else Color.parseColor("#EAF2F8")
''',
        1
    )
    yt = yt.replace('Color.parseColor("#D91A2430"),\n                    12f,\n                    Color.parseColor("#33485C70"),',
                    'buttonBg,\n                    12f,\n                    buttonBorder,', 1)
    yt = yt.replace('setColorFilter(Color.WHITE)', 'setColorFilter(ink)', 1)
    yt = yt.replace('setTextColor(Color.parseColor("#EAF2F8"))', 'setTextColor(ink)', 1)
    yt = yt.replace('Color.parseColor("#E60B1118"),\n                18f,\n                Color.parseColor("#506B8299"),',
                    'panelBg,\n                18f,\n                panelBorder,', 1)
s = s[:start] + yt + s[end:]

# TikTok Back never leaves TikTok, even if activeAppId is momentarily stale.
back_start = s.find('    private fun goBackTikTokOnly()')
back_end = s.find('    private fun showWebKeyboardOverlay', back_start)
if back_start < 0 or back_end < 0:
    raise SystemExit('v164: TikTok back function bounds not found')
back_fn = '''    private fun goBackTikTokOnly() {
        val visibleTikTok = TikTokWebPolicy.isTikTok(web.url)
        if (!visibleTikTok && currentActiveAppId != "tiktok") return
        currentActiveAppId = "tiktok"
        CarMediaManager.activeAppId = "tiktok"
        if (searchOverlay.visibility == View.VISIBLE) { hideSearchOverlay(); return }
        if (web.canGoBack()) {
            val history = web.copyBackForwardList()
            val previous = history.getItemAtIndex(history.currentIndex - 1)?.url
            if (TikTokWebPolicy.isTikTok(previous)) {
                web.goBack()
                return
            }
        }
        // Stay inside TikTok instead of escaping to another web app/history entry.
        val home = "https://www.tiktok.com/"
        TikTokWebPolicy.configure(web, home)
        web.loadUrl(home)
    }

'''
s = s[:back_start] + back_fn + s[back_end:]

p.write_text(s)

# ---------- TikTok policy: stable search route + theme hint ----------
p = Path('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt')
s = p.read_text()
s = s.replace('private const val POLICY_VERSION = 8', 'private const val POLICY_VERSION = 9')
s = re.sub(
    r'''fun searchUrl\(query: String\): String =\s*"https://www\.tiktok\.com/search\?q=" \+ Uri\.encode\(query\.trim\(\)\)''',
    'fun searchUrl(query: String): String =\n        "https://www.tiktok.com/search/video?q=" + Uri.encode(query.trim())',
    s,
    count=1
)
old_theme = '''                    function applyThemeHint() {
                        var isDay = window.__carhudCurrentIsDay !== false;
                        var scheme = isDay ? 'light' : 'dark';
                        document.documentElement.style.setProperty('color-scheme', scheme, 'important');
                        if (document.body) document.body.style.setProperty('color-scheme', scheme, 'important');
                    }
'''
new_theme = '''                    function applyThemeHint() {
                        var isDay = window.__carhudCurrentIsDay !== false;
                        var scheme = isDay ? 'light' : 'dark';
                        document.documentElement.style.setProperty('color-scheme', scheme, 'important');
                        document.documentElement.classList.toggle('ttube-theme-day', isDay);
                        document.documentElement.classList.toggle('ttube-theme-night', !isDay);
                        document.documentElement.style.setProperty('background-color', isDay ? '#ffffff' : '#070b12', 'important');
                        if (document.body) {
                            document.body.style.setProperty('color-scheme', scheme, 'important');
                            if (!isVideoRoute()) document.body.style.setProperty('background-color', isDay ? '#ffffff' : '#070b12', 'important');
                        }
                    }
'''
if old_theme in s:
    s = s.replace(old_theme, new_theme, 1)
elif 'ttube-theme-day' not in s:
    raise SystemExit('v164: TikTok applyThemeHint block not found')
p.write_text(s)

# ---------- IPTV theme + stream transition shield ----------
p = Path('app/src/main/assets/iptv_player.html')
s = p.read_text()

if '.iptv-stream-shield {' not in s:
    anchor = '    .iptv-touch-wake-layer {\n'
    css = '''    .iptv-stream-shield {
      position: absolute;
      inset: 0;
      background: #000;
      z-index: 7;
      opacity: 0;
      visibility: hidden;
      pointer-events: none;
      transition: opacity 0.08s linear;
    }
    .iptv-stream-shield.active {
      opacity: 1;
      visibility: visible;
    }

'''
    if anchor not in s: raise SystemExit('v164: IPTV touch layer CSS anchor missing')
    s = s.replace(anchor, css + anchor, 1)

s = re.sub(
    r'''    body\.iptv-day \{.*?    \}\n    body\.iptv-night''',
    '''    body.iptv-day {
      --iptv-dock-bg: rgba(248, 250, 252, 0.96);
      --iptv-dock-border: rgba(2, 132, 199, 0.42);
      --iptv-btn-bg: rgba(226, 232, 240, 0.72);
      --iptv-btn-border: rgba(100, 116, 139, 0.24);
      --iptv-icon-color: #172033;
      --iptv-label-color: #172033;
      --iptv-active: #0284C7;
      --iptv-active-bg: rgba(14, 165, 233, 0.16);
      --iptv-active-border: rgba(2, 132, 199, 0.72);
      --iptv-shadow: rgba(15, 23, 42, 0.24);
    }
    body.iptv-night''',
    s,
    count=1,
    flags=re.S
)
s = s.replace('      color: #F8FAFC;\n      font-size: 11px;', '      color: var(--iptv-label-color);\n      font-size: 11px;', 1)

if 'id="iptvStreamShield"' not in s:
    s = s.replace(
        '        <video id="videoPlayer" autoplay playsinline webkit-playsinline></video>\n',
        '        <video id="videoPlayer" autoplay playsinline webkit-playsinline></video>\n        <div id="iptvStreamShield" class="iptv-stream-shield" aria-hidden="true"></div>\n',
        1
    )

if 'function setIptvStreamShield(active)' not in s:
    s = s.replace(
        '    let lastIptvReportedState = null;\n',
        '''    let lastIptvReportedState = null;
    function setIptvStreamShield(active) {
      const shield = document.getElementById('iptvStreamShield');
      if (shield) shield.classList.toggle('active', !!active);
    }
''',
        1
    )

if 'setIptvStreamShield(false);\n        updateIptvPlayPauseButton();' not in s:
    s = s.replace(
        '''      video.addEventListener('playing', () => {
        if (window.__iptvUserPaused) { video.pause(); return; }
        updateIptvPlayPauseButton();
''',
        '''      video.addEventListener('playing', () => {
        if (window.__iptvUserPaused) { video.pause(); return; }
        setIptvStreamShield(false);
        updateIptvPlayPauseButton();
''',
        1
    )

if 'function playStream(channel, el) {\n      if (!channel) return;\n      setIptvStreamShield(true);' not in s:
    s = s.replace(
        '    function playStream(channel, el) {\n      if (!channel) return;\n',
        '    function playStream(channel, el) {\n      if (!channel) return;\n      setIptvStreamShield(true);\n',
        1
    )

if "setIptvStreamShield(false);\n              showToast('Không tải được kênh." not in s:
    s = s.replace(
        "              hlsInstance = null;\n              showToast('Không tải được kênh. Chọn Tải lại nguồn để thử lại.');",
        "              hlsInstance = null;\n              setIptvStreamShield(false);\n              showToast('Không tải được kênh. Chọn Tải lại nguồn để thử lại.');",
        1
    )
p.write_text(s)

# ---------- historical tests: only update assertions invalidated by requested UI move ----------
for name in ['tests/v159-tiktok-guest-settings-toolbar.test.cjs', 'tests/v161-browser-tiktok-polish.test.cjs']:
    p = Path(name)
    if not p.exists(): continue
    t = p.read_text()
    t = t.replace('/tiktokIconSizeDp = \\(\\(62 \\* scale\\) \\/ 100\\)\\.coerceIn\\(58, 78\\)/',
                  '/tiktokIconSizeDp = \\(\\(5[8-9] \\* scale\\) \\/ 100\\)\\.coerceIn\\(5[4-8], 7[2-8]\\)/')
    t = t.replace('/leftMargin = dp\\(82\\)/', '/leftMargin = dp\\((?:8|82)\\)/')
    t = t.replace('/topMargin = dp\\((?:30|48)\\)/', '/topMargin = dp\\((?:24|30|48)\\)/')
    p.write_text(t)

p = Path('tests/v163-tiktok-popup-youtube-restore.test.cjs')
if p.exists():
    t = p.read_text()
    t = t.replace('/versionCode = 250/', '/versionCode = 25[01]/')
    t = t.replace('/versionName = "1\\.0\\.63"/', '/versionName = "1\\.0\\.(?:63|64)"/')
    t = t.replace('/POLICY_VERSION = 8/', '/POLICY_VERSION = [89]/')
    p.write_text(t)

Path('tests/v164-theme-transition-tiktok.test.cjs').write_text(r"""const fs = require('node:fs');
const assert = require('node:assert/strict');
const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const pres = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');
const tik = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt','utf8');
const iptv = fs.readFileSync('app/src/main/assets/iptv_player.html','utf8');
assert.match(gradle,/versionCode = 251/);
assert.match(gradle,/versionName = "1\.0\.64"/);
assert.match(pres,/val panelBg = if \(isDay\)/);
assert.match(pres,/orientation = LinearLayout\.VERTICAL/);
assert.match(pres,/leftMargin = dp\(8\)/);
assert.match(pres,/showYouTubeTransitionShield/);
assert.match(pres,/hideYouTubeTransitionShield/);
assert.match(pres,/val visibleTikTok = TikTokWebPolicy\.isTikTok\(web\.url\)/);
assert.doesNotMatch(pres,/if \(currentActiveAppId != "tiktok"\) \{ goBack\(\); return \}/);
assert.match(tik,/POLICY_VERSION = 9/);
assert.match(tik,/https:\/\/www\.tiktok\.com\/search\/video\?q=/);
assert.match(iptv,/iptv-stream-shield/);
assert.match(iptv,/setIptvStreamShield\(true\)/);
assert.match(iptv,/body\.iptv-day[\s\S]*--iptv-icon-color: #172033/);
console.log('PASS v1.0.64: targeted theme, transition shield and TikTok fixes');
""")

# ---------- changelog ----------
p = Path('app/src/main/java/com/carhud/aaproxy/ChangelogManager.kt')
if p.exists():
    s = p.read_text()
    if 'versionCode = 251' not in s:
        needle = '        ChangeLog(\n            versionCode = 250,\n'
        entry = '''        ChangeLog(
            versionCode = 251,
            versionName = "1.0.64",
            title = "Đồng bộ giao diện và ổn định TikTok",
            features = listOf(
                "🎨 Đồng bộ toolbar YouTube, IPTV và TikTok theo Sáng / Tối / Auto.",
                "📺 Ẩn khung hình cũ trong lúc YouTube/IPTV chuyển sang video mới.",
                "🔎 TikTok dùng tuyến tìm kiếm video ổn định hơn; Back chỉ hoạt động trong TikTok.",
                "↕️ Thanh công cụ TikTok chuyển dọc, sát góc trái."
            )
        ),

'''
        if needle in s:
            s = s.replace(needle, entry + needle, 1)
            p.write_text(s)

# Essential checks
checks = {
    'app/build.gradle.kts': ['versionCode = 251', 'versionName = "1.0.64"'],
    'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt': ['youtubeTransitionShield', 'val panelBg = if (isDay)', 'leftMargin = dp(8)', 'visibleTikTok'],
    'app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt': ['POLICY_VERSION = 9', 'search/video?q='],
    'app/src/main/assets/iptv_player.html': ['iptv-stream-shield', 'setIptvStreamShield(true)', '--iptv-icon-color: #172033'],
}
for filename, needles in checks.items():
    text = Path(filename).read_text()
    for needle in needles:
        if needle not in text:
            raise SystemExit(f'v164 missing {needle} in {filename}')

print('v1.0.64 targeted source transform complete')
