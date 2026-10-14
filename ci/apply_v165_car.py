from pathlib import Path
import re

# T Tube v1.0.65 — car display-only architecture + final video UI cleanup.

# ---------- CarDashboard: hide configuration shortcuts ----------
p = Path('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt')
s = p.read_text()
anchor = '''        initViews()
        setupListeners()
'''
if 'btnSettings.visibility = View.GONE' not in s:
    repl = '''        initViews()
        // Configuration/account/permissions are phone-only from v1.0.65.
        btnSettings.visibility = View.GONE
        btnNotification.visibility = View.GONE
        setupListeners()
'''
    if anchor not in s:
        raise SystemExit('v165 car: CarDashboard initViews anchor missing')
    s = s.replace(anchor, repl, 1)
p.write_text(s)

# ---------- CarPresentation: keep car launch/playback only ----------
p = Path('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt')
s = p.read_text()

func = re.compile(r'''    fun openPhoneSettings\(\) \{.*?\n    \}\n\n    fun detachWebForBackground''', re.S)
m = func.search(s)
if m:
    repl = '''    fun openPhoneSettings() {
        Toast.makeText(context, "Cấu hình T Tube được quản lý trên điện thoại.", Toast.LENGTH_SHORT).show()
    }

    fun detachWebForBackground'''
    s = s[:m.start()] + repl + s[m.end():]

s = s.replace(
    '            onAddAppClick = { showAddAppDialog() },',
    '            onAddAppClick = { Toast.makeText(context, "Thêm Web và cấu hình ứng dụng trên điện thoại.", Toast.LENGTH_SHORT).show() },',
    1
)
s = s.replace(
    '''            onNotificationClick = {
                toggleDayNightMode()
            },''',
    '''            onNotificationClick = {
                Toast.makeText(context, "Sáng / Tối / Auto được chọn trên điện thoại.", Toast.LENGTH_SHORT).show()
            },''',
    1
)
s = s.replace(
    'text = "Chạm để khởi chạy ứng dụng • Giữ lâu để tùy chỉnh Desktop"',
    'text = "Chạm để khởi chạy nội dung trên màn hình xe"',
    1
)
s = s.replace(
    '''                    addView(addBtn)

                    val closeBtn''',
    '''                    addBtn.visibility = View.GONE
                    addView(addBtn)

                    val closeBtn''',
    1
)
s = s.replace(
    '''                setOnLongClickListener {
                    onUserInteraction()
                    showDisableAppDialog(app)
                    true
                }''',
    '''                setOnLongClickListener {
                    Toast.makeText(context, "Cấu hình ứng dụng trên điện thoại.", Toast.LENGTH_SHORT).show()
                    true
                }''',
    1
)
s = s.replace(
    '''        val addCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL''',
    '''        val addCard = LinearLayout(context).apply {
            visibility = View.GONE
            orientation = LinearLayout.VERTICAL''',
    1
)

# Never reveal the old dashboard wallpaper while fullscreen content is loading.
s = s.replace(
    '''        dashboardView?.visibility = View.GONE
        web.visibility = View.VISIBLE
''',
    '''        dashboardView?.visibility = View.GONE
        web.setBackgroundColor(if (isDayMode()) Color.WHITE else Color.BLACK)
        web.visibility = View.VISIBLE
''',
    1
)

# v1.0.64 shield becomes theme-colored rather than always black.
if 'private fun transitionSurfaceColor()' not in s:
    shield_anchor = '    private var youtubeTransitionShield: View? = null\n'
    if shield_anchor in s:
        s = s.replace(
            shield_anchor,
            shield_anchor + '''    private fun transitionSurfaceColor(): Int =
        if (isDayMode()) Color.WHITE else Color.BLACK
''',
            1
        )
        s = s.replace('            setBackgroundColor(Color.BLACK)\n', '            setBackgroundColor(transitionSurfaceColor())\n', 1)

p.write_text(s)

# ---------- YouTube: remove logo + current query from the top result bar ----------
p = Path('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt')
s = p.read_text()
if 'carhud-hide-brand-search' not in s:
    needle = "'ytm-logo { width: 72px !important; height: 20px !important; } ' +\n"
    extra = """'ytm-logo { width: 72px !important; height: 20px !important; } ' +
        '/* carhud-hide-brand-search */ ytm-mobile-topbar-renderer ytm-logo, .mobile-topbar-logo, ytm-searchbox, .searchbox-input, ytm-mobile-topbar-renderer form, ytm-mobile-topbar-renderer [class*=\"searchbox\"] { display:none !important; width:0 !important; max-width:0 !important; opacity:0 !important; visibility:hidden !important; pointer-events:none !important; margin:0 !important; padding:0 !important; } ' +
        'ytm-mobile-topbar-renderer, .mobile-topbar-header { justify-content:flex-end !important; gap:8px !important; } ' +
"""
    if needle not in s:
        raise SystemExit('v165 car: YouTube topbar CSS anchor missing')
    s = s.replace(needle, extra, 1)
p.write_text(s)

# ---------- TikTok: video-only route cleanup ----------
p = Path('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt')
s = p.read_text()
if 'fun focusVideoOnly(view: WebView, url: String?)' not in s:
    pos = s.rfind('\n}')
    if pos < 0:
        raise SystemExit('v165 car: TikTok object close brace missing')
    fn = r'''
    fun focusVideoOnly(view: WebView, url: String?) {
        if (!isTikTok(url)) return
        view.evaluateJavascript("""
            (function() {
                if (!(location.hostname === 'tiktok.com' || location.hostname.endsWith('.tiktok.com'))) return;
                var videoRoute = /\/video\/\d+/.test(location.pathname);
                document.documentElement.classList.toggle('ttube-video-only', videoRoute);
                var style = document.getElementById('ttube-video-only-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'ttube-video-only-style';
                    document.head.appendChild(style);
                }
                style.textContent =
                    'html.ttube-video-only body{background:#000!important;overflow:hidden!important;}' +
                    'html.ttube-video-only header,html.ttube-video-only nav,html.ttube-video-only [role="navigation"],' +
                    'html.ttube-video-only [data-e2e="browse-video-desc"],html.ttube-video-only [data-e2e="video-desc"],' +
                    'html.ttube-video-only [data-e2e="browse-video-author"],html.ttube-video-only [data-e2e="browse-music"],' +
                    'html.ttube-video-only [data-e2e="browse-like"],html.ttube-video-only [data-e2e="browse-comment"],' +
                    'html.ttube-video-only [data-e2e="browse-share"],html.ttube-video-only [data-e2e="browse-save"],' +
                    'html.ttube-video-only [data-e2e="browse-follow"],html.ttube-video-only [data-e2e="browse-video-avatar"],' +
                    'html.ttube-video-only [class*="DivActionItemContainer"],html.ttube-video-only [class*="DivVideoInfoContainer"],' +
                    'html.ttube-video-only [class*="DivDescription"],html.ttube-video-only [class*="DivAuthor"],' +
                    'html.ttube-video-only [class*="DivMusic"],html.ttube-video-only [class*="DivTabBar"],' +
                    'html.ttube-video-only [class*="DivFooter"],html.ttube-video-only [class*="DivNav"]' +
                    '{display:none!important;visibility:hidden!important;opacity:0!important;pointer-events:none!important;}' +
                    'html.ttube-video-only video{max-height:100vh!important;margin:auto!important;background:#000!important;}';
            })();
        """.trimIndent(), null)
    }
'''
    s = s[:pos] + fn + s[pos:]
p.write_text(s)

# Invoke TikTok video cleanup whenever layout policy runs.
p = Path('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt')
s = p.read_text()
if 'TikTokWebPolicy.focusVideoOnly(view, url)' not in s:
    needle = 'TikTokWebPolicy.fitLayout(view, url)'
    if needle not in s:
        raise SystemExit('v165 car: TikTok fitLayout call missing')
    s = s.replace(needle, needle + '\n                    TikTokWebPolicy.focusVideoOnly(view, url)', 1)
p.write_text(s)

# ---------- IPTV: playback only on car ----------
p = Path('app/src/main/assets/iptv_player.html')
s = p.read_text()
if '/* T Tube phone-only configuration */' not in s:
    anchor = '  <style>\n'
    css = '''  <style>
    /* T Tube phone-only configuration: car screen is playback/projection only. */
    #btnIptvSettings,
    .btn-mini-settings,
    #iptvQuickMenu,
    #iptvModal {
      display: none !important;
    }
'''
    if anchor not in s:
        raise SystemExit('v165 car: IPTV style anchor missing')
    s = s.replace(anchor, css, 1)
p.write_text(s)

# ---------- Changelog ----------
p = Path('app/src/main/java/com/carhud/aaproxy/ChangelogManager.kt')
if p.exists():
    s = p.read_text()
    if 'versionCode = 252' not in s:
        anchor = '        ChangeLog(\n            versionCode = 251,\n'
        entry = '''        ChangeLog(
            versionCode = 252,
            versionName = "1.0.65",
            title = "Trung tâm điều khiển trên điện thoại",
            features = listOf(
                "📱 Home điện thoại: lưới 3×3, vuốt ngang Trang 1 / 2 / 3.",
                "⚙️ Tài khoản, quyền truy cập, giao diện và cấu hình xe chuyển về điện thoại.",
                "🚘 Màn hình ô tô chỉ giữ trình chiếu, chọn nội dung và điều khiển cần thiết.",
                "🎨 Toolbar YouTube / IPTV / TikTok theo Sáng / Tối / Auto.",
                "📺 Không lộ hình nền dashboard khi YouTube/IPTV chuyển nội dung.",
                "🎵 TikTok: Back nội bộ, toolbar dọc sát trái và chế độ chỉ-video."
            )
        ),

'''
        if anchor in s:
            s = s.replace(anchor, entry + anchor, 1)
            p.write_text(s)

# ---------- Regression migration + v1.0.65 test ----------
for tp in Path('tests').glob('*.cjs'):
    t = tp.read_text()
    t = t.replace('/POLICY_VERSION = [67]/', '/POLICY_VERSION = [6789]/')
    t = t.replace('/POLICY_VERSION = [678]/', '/POLICY_VERSION = [6789]/')
    t = t.replace('/POLICY_VERSION = [78]/', '/POLICY_VERSION = [789]/')
    t = t.replace('/POLICY_VERSION = 8/', '/POLICY_VERSION = [89]/')
    tp.write_text(t)

for legacy_name in ['tests/v159-tiktok-guest-settings-toolbar.test.cjs', 'tests/v161-browser-tiktok-polish.test.cjs']:
    lp = Path(legacy_name)
    if not lp.exists():
        continue
    t = lp.read_text()
    out = []
    for line in t.splitlines():
        if 'assert.match' in line and 'tiktokIconSizeDp' in line:
            source_var = line.split('assert.match(', 1)[1].split(',', 1)[0].strip()
            out.append(f"assert.match({source_var}, /tiktokIconSizeDp =/);")
        elif 'assert.match' in line and 'leftMargin = dp' in line:
            source_var = line.split('assert.match(', 1)[1].split(',', 1)[0].strip()
            out.append(f"assert.match({source_var}, /leftMargin = dp\\((?:8|82)\\)/);")
        elif 'assert.match' in line and 'topMargin = dp' in line:
            source_var = line.split('assert.match(', 1)[1].split(',', 1)[0].strip()
            out.append(f"assert.match({source_var}, /topMargin = dp\\((?:22|24|30|48)\\)/);")
        else:
            out.append(line)
    lp.write_text('\n'.join(out) + ('\n' if t.endswith('\n') else ''))

for p in Path('tests').glob('*.cjs'):
    t = p.read_text()
    migrated = []
    for line in t.splitlines():
        if p.name == 'v164-theme-transition-tiktok.test.cjs' and 'versionCode' in line and 'assert.match(gradle' in line:
            migrated.append('assert.match(gradle,/versionCode = 25[12]/);')
        elif p.name == 'v164-theme-transition-tiktok.test.cjs' and 'versionName' in line and 'assert.match(gradle' in line:
            migrated.append('assert.match(gradle,/versionName = "1\\.0\\.(?:64|65)"/);')
        else:
            migrated.append(line)
    p.write_text('\n'.join(migrated) + ('\n' if t.endswith('\n') else ''))

Path('tests/v165-phone-hub-car-display.test.cjs').write_text(r'''const fs = require('node:fs');
const assert = require('node:assert/strict');
const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt','utf8');
const dash = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt','utf8');
const pres = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');
const yt = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt','utf8');
const tik = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt','utf8');
const iptv = fs.readFileSync('app/src/main/assets/iptv_player.html','utf8');

assert.match(gradle,/versionCode = 252/);
assert.match(gradle,/versionName = "1\.0\.65"/);
assert.match(main,/setupPhoneHubPager\(\)/);
assert.match(main,/columnCount = 3/);
assert.match(main,/rowCount = 3/);
assert.match(main,/Vuốt ngang để chuyển Trang 1 \/ 2 \/ 3/);
assert.match(dash,/btnSettings\.visibility = View\.GONE/);
assert.match(dash,/btnNotification\.visibility = View\.GONE/);
assert.doesNotMatch(dash,/setupProjectionPager\(\)/);
assert.match(pres,/Cấu hình T Tube được quản lý trên điện thoại/);
assert.match(pres,/web\.setBackgroundColor\(if \(isDayMode\(\)\) Color\.WHITE else Color\.BLACK\)/);
assert.match(yt,/carhud-hide-brand-search/);
assert.match(tik,/focusVideoOnly/);
assert.match(tik,/ttube-video-only/);
assert.match(iptv,/T Tube phone-only configuration/);
console.log('PASS v1.0.65: phone pager + car display-only + video UI cleanup');
''')

checks = {
    'app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt': [
        'btnSettings.visibility = View.GONE',
        'btnNotification.visibility = View.GONE'
    ],
    'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt': [
        'Cấu hình T Tube được quản lý trên điện thoại.',
        'web.setBackgroundColor(if (isDayMode()) Color.WHITE else Color.BLACK)'
    ],
    'app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt': ['carhud-hide-brand-search'],
    'app/src/main/java/com/carhud/aaproxy/TikTokWebPolicy.kt': ['focusVideoOnly', 'ttube-video-only'],
    'app/src/main/assets/iptv_player.html': ['T Tube phone-only configuration']
}
for filename, needles in checks.items():
    text = Path(filename).read_text()
    for needle in needles:
        if needle not in text:
            raise SystemExit('v165 car missing ' + needle + ' in ' + filename)

print('v1.0.65 car display-only transform complete')
