from pathlib import Path

# T Tube v1.0.65 / Build 252 — phone Home is the app launcher.
p = Path('app/build.gradle.kts')
s = p.read_text().replace('versionCode = 251', 'versionCode = 252').replace('versionName = "1.0.64"', 'versionName = "1.0.65"')
p.write_text(s)

p = Path('app/src/main/java/com/carhud/aaproxy/MainActivity.kt')
s = p.read_text()

field_anchor = '    private var activeAccentColor = Color.parseColor("#38BDF8")\n'
if 'private var phoneHubPager:' not in s:
    fields = '''    private var activeAccentColor = Color.parseColor("#38BDF8")

    private var phoneHubPager: HorizontalScrollView? = null
    private var phoneHubDots: LinearLayout? = null
    private var phoneHubPage = 0
    private var phoneHubDownX = 0f

    private val PHONE_HOME_PAGE_COUNT = 3
    private val PHONE_HOME_APPS_PER_PAGE = 9
'''
    if field_anchor not in s:
        raise SystemExit('v165 phone: field anchor missing')
    s = s.replace(field_anchor, fields, 1)

if 'setupPhoneHubPager()' not in s:
    needle = '''        setupSettingsTab()
        setupMiniPlayer()
'''
    if needle not in s:
        raise SystemExit('v165 phone: onCreate anchor missing')
    s = s.replace(needle, '''        setupSettingsTab()
        setupPhoneHubPager()
        setupMiniPlayer()
''', 1)

insert_anchor = '    private fun setupHomeActions() {\n'
if 'private fun setupPhoneHubPager(' not in s:
    code = r'''    private fun phoneHubIsDay(): Boolean {
        return when (prefs.getString(PREF_THEME_MODE, "auto") ?: "auto") {
            "light" -> true
            "dark" -> false
            else -> {
                val mask = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                mask != android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun openPhoneHomeApp(app: WebAppItem) {
        when (app.id) {
            "youtube" -> {
                switchTab(TAB_YOUTUBE)
                initYoutubeWebView()
                youtubeWeb?.loadUrl(app.url)
            }
            "iptv" -> switchTab(TAB_IPTV)
            else -> {
                switchTab(TAB_YOUTUBE)
                initYoutubeWebView()
                youtubeWeb?.loadUrl(app.url)
            }
        }
    }

    private fun createPhoneHubCell(app: WebAppItem, day: Boolean): LinearLayout {
        val cardBg = if (day) Color.parseColor("#F6FFFFFF") else Color.parseColor("#E80D1522")
        val stroke = if (day) Color.parseColor("#D6E0E9") else Color.parseColor("#26384D")
        val primary = if (day) Color.parseColor("#172033") else Color.WHITE

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(7), dp(5), dp(6))
            background = createCardBg(cardBg, 16f, stroke, 1)
            isClickable = true
            isFocusable = true
            elevation = dp(1).toFloat()
            contentDescription = app.name

            val iconHolder = FrameLayout(this@MainActivity).apply {
                background = createCircleBg(
                    try { Color.parseColor(app.colorHex) } catch (_: Throwable) { activeAccentColor }
                )
            }
            val icon = ImageView(this@MainActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                val custom = app.iconPath?.takeIf { it.isNotBlank() }?.let {
                    try { BitmapFactory.decodeFile(it) } catch (_: Throwable) { null }
                }
                if (custom != null) setImageBitmap(custom) else setImageResource(app.iconRes)
            }
            iconHolder.addView(icon, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
            addView(iconHolder, LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            })

            addView(TextView(this@MainActivity).apply {
                text = app.name
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(primary)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
            })

            setOnClickListener { openPhoneHomeApp(app) }
            enableTouchBounce(0.96f)
        }
    }

    private fun updatePhoneHubDots() {
        val dots = phoneHubDots ?: return
        val day = phoneHubIsDay()
        for (i in 0 until dots.childCount) {
            val selected = i == phoneHubPage
            dots.getChildAt(i).apply {
                background = createCircleBg(
                    if (selected) activeAccentColor
                    else if (day) Color.parseColor("#A8B4C0") else Color.parseColor("#526175")
                )
                alpha = if (selected) 1f else .65f
            }
        }
    }

    private fun showPhoneHubPage(page: Int, smooth: Boolean = true) {
        val pager = phoneHubPager ?: return
        val target = page.coerceIn(0, PHONE_HOME_PAGE_COUNT - 1)
        phoneHubPage = target
        val width = pager.width.takeIf { it > 0 }
            ?: (resources.displayMetrics.widthPixels - dp(32)).coerceAtLeast(dp(280))
        if (smooth) pager.smoothScrollTo(width * target, 0) else pager.scrollTo(width * target, 0)
        updatePhoneHubDots()
    }

    private fun setupPhoneHubPager(rebuild: Boolean = false) {
        val homeColumn = cardPhoneCockpitPreview.parent as? LinearLayout ?: return
        val old = homeColumn.findViewWithTag<View>("ttube-phone-hub")
        if (old != null) {
            if (!rebuild) return
            homeColumn.removeView(old)
        }

        // Keep the old 2x2 service block in the layout for preference compatibility,
        // but never show it on the v1.0.65 phone Home.
        ((phoneCardYoutube.parent as? View)?.parent as? View)?.visibility = View.GONE

        val day = phoneHubIsDay()
        val secondary = if (day) Color.parseColor("#687789") else Color.parseColor("#94A3B8")
        val apps = WebAppManager.getEnabledApps(this)
            .take(PHONE_HOME_PAGE_COUNT * PHONE_HOME_APPS_PER_PAGE)

        val section = LinearLayout(this).apply {
            tag = "ttube-phone-hub"
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(6))
        }

        val pager = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        phoneHubPager = pager

        val track = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        pager.addView(track, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(286)))

        val pageWidth = (resources.displayMetrics.widthPixels - dp(32)).coerceAtLeast(dp(280))
        repeat(PHONE_HOME_PAGE_COUNT) { pageIndex ->
            val grid = android.widget.GridLayout(this).apply {
                columnCount = 3
                rowCount = 3
                setPadding(dp(2), dp(2), dp(2), dp(2))
            }

            apps.drop(pageIndex * PHONE_HOME_APPS_PER_PAGE)
                .take(PHONE_HOME_APPS_PER_PAGE)
                .forEachIndexed { index, app ->
                    grid.addView(createPhoneHubCell(app, day), android.widget.GridLayout.LayoutParams().apply {
                        width = 0
                        height = dp(88)
                        columnSpec = android.widget.GridLayout.spec(index % 3, 1, 1f)
                        rowSpec = android.widget.GridLayout.spec(index / 3, 1, 1f)
                        setMargins(dp(4), dp(4), dp(4), dp(4))
                    })
                }

            track.addView(grid, LinearLayout.LayoutParams(pageWidth, dp(286)))
        }

        val dots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            repeat(PHONE_HOME_PAGE_COUNT) {
                addView(View(this@MainActivity), LinearLayout.LayoutParams(dp(7), dp(7)).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                })
            }
        }
        phoneHubDots = dots

        section.addView(pager, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(286)))
        section.addView(dots, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)).apply { topMargin = dp(3) })
        section.addView(TextView(this).apply {
            text = "Vuốt ngang để chuyển Trang 1 / 2 / 3"
            textSize = 9.5f
            gravity = Gravity.CENTER
            setTextColor(secondary)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })

        homeColumn.addView(section, (homeColumn.indexOfChild(cardPhoneCockpitPreview) + 1).coerceAtLeast(0))

        pager.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> phoneHubDownX = event.x
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    val dx = event.x - phoneHubDownX
                    val width = pager.width.coerceAtLeast(1)
                    val target = when {
                        dx < -dp(44) -> phoneHubPage + 1
                        dx > dp(44) -> phoneHubPage - 1
                        else -> ((pager.scrollX + width / 2) / width)
                    }
                    pager.post { showPhoneHubPage(target) }
                }
            }
            false
        }

        updatePhoneHubDots()
        pager.post { showPhoneHubPage(phoneHubPage, smooth = false) }
    }

'''
    if insert_anchor not in s:
        raise SystemExit('v165 phone: setupHomeActions anchor missing')
    s = s.replace(insert_anchor, code + insert_anchor, 1)

theme_save = '            prefs.edit().putString(PREF_THEME_MODE, mode).apply()\n'
if theme_save in s and 'setupPhoneHubPager(rebuild = true)' not in s:
    s = s.replace(theme_save, theme_save + '            setupPhoneHubPager(rebuild = true)\n', 1)

# Refresh the app launcher when returning from app/settings management.
resume_anchor = '        updatePermissionStatusDisplay()\n'
if resume_anchor in s and 'setupPhoneHubPager(rebuild = true)\n        val imported' not in s:
    s = s.replace(
        resume_anchor,
        resume_anchor + '        if (phoneHubPager != null) setupPhoneHubPager(rebuild = true)\n',
        1
    )

p.write_text(s)

# T Tube launcher branding — replace the legacy THTV launcher mark in the final v1.0.65 overlay.
for values in [
    Path('app/src/main/res/values/strings.xml'),
    Path('app/src/main/res/values-vi/strings.xml')
]:
    if values.exists():
        text = values.read_text()
        text = text.replace('<string name="app_name">THTV</string>', '<string name="app_name">T Tube</string>')
        values.write_text(text)

Path('app/src/main/res/drawable/ic_launcher_foreground.xml').write_text(r'''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:gravity="center" android:width="88dp" android:height="88dp">
        <shape android:shape="rectangle">
            <corners android:radius="24dp"/>
            <solid android:color="#E50914"/>
        </shape>
    </item>
    <item android:gravity="center" android:width="34dp" android:height="34dp">
        <vector
            android:width="34dp"
            android:height="34dp"
            android:viewportWidth="24"
            android:viewportHeight="24">
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M8,5v14l11,-7z"/>
        </vector>
    </item>
</layer-list>
''')

# Regression test: exactly the 9 requirements for phone Home.
test = Path('tests/phone-home-grid.test.cjs')
test.write_text(r'''const fs = require('node:fs');
const assert = require('node:assert/strict');

const main = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/MainActivity.kt', 'utf8');

assert.match(main, /PHONE_HOME_PAGE_COUNT = 3/);
assert.match(main, /PHONE_HOME_APPS_PER_PAGE = 9/);
assert.match(main, /WebAppManager\.getEnabledApps\(this\)/);
assert.match(main, /take\(PHONE_HOME_PAGE_COUNT \* PHONE_HOME_APPS_PER_PAGE\)/);
assert.match(main, /repeat\(PHONE_HOME_PAGE_COUNT\)/);
assert.match(main, /columnCount = 3/);
assert.match(main, /rowCount = 3/);
assert.match(main, /take\(PHONE_HOME_APPS_PER_PAGE\)/);
assert.match(main, /Vuốt ngang để chuyển Trang 1 \/ 2 \/ 3/);
assert.match(main, /phoneCardYoutube\.parent[\s\S]*?visibility = View\.GONE/);

const pagerStart = main.indexOf('private fun setupPhoneHubPager');
const pagerEnd = main.indexOf('private fun setupHomeActions', pagerStart);
assert.ok(pagerStart >= 0 && pagerEnd > pagerStart);
const pager = main.slice(pagerStart, pagerEnd);
assert.doesNotMatch(pager, /Quyền truy cập|Tên hiển thị|Bản quyền/);

console.log('PASS phone Home: 3x3, 9 apps/page, 3 pages, max 27 enabled apps, legacy 2x2 hidden');
''')

checks = [
    'setupPhoneHubPager()',
    'PHONE_HOME_PAGE_COUNT = 3',
    'PHONE_HOME_APPS_PER_PAGE = 9',
    'WebAppManager.getEnabledApps(this)',
    'columnCount = 3',
    'rowCount = 3',
    'Vuốt ngang để chuyển Trang 1 / 2 / 3'
]
for needle in checks:
    if needle not in s:
        raise SystemExit('v165 phone missing: ' + needle)

print('v1.0.65 phone 3x3 app launcher transform complete')
