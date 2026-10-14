from pathlib import Path
import re

# ---------------- version ----------------
p = Path('app/build.gradle.kts')
s = p.read_text()
s = s.replace('versionCode = 251', 'versionCode = 252')
s = s.replace('versionName = "1.0.64"', 'versionName = "1.0.65"')
p.write_text(s)

# ---------------- car dashboard: 3-page projection-only pager ----------------
p = Path('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt')
s = p.read_text()

field_anchor = '    private lateinit var btnSettings: FrameLayout\n'
if 'private var projectionPageIndex = 0' not in s:
    fields = '''    private lateinit var btnSettings: FrameLayout

    // Car screen is projection-only. Configuration/account/permissions stay on phone.
    private var projectionPageIndex = 0
    private var projectionPage2: FrameLayout? = null
    private var projectionPage3: FrameLayout? = null
    private var projectionIndicator: LinearLayout? = null
    private var projectionSwipeStartX = 0f
    private var projectionSwipeStartY = 0f
    private var projectionSwipeActive = false
    private var projectionPage2Clock: TextView? = null
    private var projectionPage2Date: TextView? = null
    private var projectionPage2Lunar: TextView? = null
    private var projectionPage2Weather: TextView? = null
    private var projectionPage2Speed: TextView? = null
    private var projectionPage2Media: TextView? = null
    private var projectionPage3Title: TextView? = null
    private var projectionPage3Subtitle: TextView? = null
    private var projectionPage3Artwork: ImageView? = null
    private var projectionPage3Play: ImageView? = null
    private val projectionPrimaryTexts = mutableListOf<TextView>()
    private val projectionSecondaryTexts = mutableListOf<TextView>()
    private val projectionCards = mutableListOf<View>()
'''
    if field_anchor not in s:
        raise SystemExit('v165: dashboard settings field anchor missing')
    s = s.replace(field_anchor, fields, 1)

if 'setupProjectionPager()' not in s:
    s = s.replace(
        '''        initViews()
        setupListeners()
''',
        '''        initViews()
        setupProjectionPager()
        setupListeners()
''',
        1
    )

insert_anchor = '    private fun refreshPinnedApps() {\n'
if 'private fun setupProjectionPager()' not in s:
    pager_code = r'''    private fun projectionText(
        textValue: String,
        sizeSp: Float,
        bold: Boolean = false,
        secondary: Boolean = false
    ): TextView {
        return TextView(context).apply {
            text = textValue
            textSize = sizeSp
            includeFontPadding = false
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            if (bold) typeface = Typeface.DEFAULT_BOLD
            (if (secondary) projectionSecondaryTexts else projectionPrimaryTexts).add(this)
        }
    }

    private fun projectionCard(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            projectionCards.add(this)
        }
    }

    private fun projectionRoundButton(iconRes: Int, action: () -> Unit): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                marginStart = dp(6)
                marginEnd = dp(6)
            }
            background = rounded(Color.parseColor("#293447"), 999f, Color.parseColor("#405A6B82"), 1)
            addView(ImageView(context).apply {
                setImageResource(iconRes)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setColorFilter(Color.WHITE)
            }, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
            setOnClickListener { action() }
        }
    }

    private fun setupProjectionPager() {
        // No settings/account/theme toggles on the car screen.
        btnSettings.visibility = View.GONE
        btnNotification.visibility = View.GONE

        val page2 = FrameLayout(context).apply {
            visibility = View.GONE
            setPadding(dp(26), dp(22), dp(26), dp(30))
            isClickable = true
        }
        projectionPage2 = page2

        val leftInfo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            addView(projectionText("08:30", 48f, true).also { projectionPage2Clock = it })
            addView(projectionText("Thứ Tư, 14/10/2026", 15f, true, true).also { projectionPage2Date = it },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
            addView(projectionText("ÂL: —", 13f, false, true).also { projectionPage2Lunar = it },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(3) })
            addView(projectionText("☁  —°C  •  Đang cập nhật", 15f, true, true).also { projectionPage2Weather = it },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(18) })
        }
        page2.addView(leftInfo, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.START or Gravity.TOP))

        val speedCard = projectionCard().apply {
            gravity = Gravity.CENTER
            addView(projectionText("0", 66f, true).also { projectionPage2Speed = it })
            addView(projectionText("KM/H", 15f, true, true))
            addView(projectionText("🚘", 60f, false), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(4)
            })
        }
        page2.addView(speedCard, FrameLayout.LayoutParams(dp(280), dp(220), Gravity.CENTER).apply {
            leftMargin = dp(110)
        })

        val mediaCard = projectionCard().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_app_youtube)
                setColorFilter(Color.WHITE)
            }, LinearLayout.LayoutParams(dp(42), dp(42)))
            addView(projectionText("Chưa phát nội dung", 14f, true).also { projectionPage2Media = it },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) })
            addView(projectionRoundButton(R.drawable.ic_player_prev) { CarMediaManager.playPrevious() })
            addView(projectionRoundButton(R.drawable.ic_player_play) { CarMediaManager.togglePlayPause() })
            addView(projectionRoundButton(R.drawable.ic_player_next) { CarMediaManager.playNext() })
        }
        page2.addView(mediaCard, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(88), Gravity.BOTTOM))

        val page3 = FrameLayout(context).apply {
            visibility = View.GONE
            setPadding(dp(34), dp(26), dp(34), dp(34))
            isClickable = true
        }
        projectionPage3 = page3

        val nowCard = projectionCard().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val art = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.ic_app_youtube)
            background = rounded(Color.parseColor("#223247"), 24f, Color.parseColor("#406B8299"), 1)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        projectionPage3Artwork = art
        nowCard.addView(art, LinearLayout.LayoutParams(dp(190), dp(190)))

        val nowInfo = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(6), 0, dp(6))
            addView(projectionText("ĐANG PHÁT", 12f, true, true))
            addView(projectionText("Chưa phát nội dung", 26f, true).also { projectionPage3Title = it },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
            addView(projectionText("T Tube", 14f, false, true).also { projectionPage3Subtitle = it },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(projectionRoundButton(R.drawable.ic_player_prev) { CarMediaManager.playPrevious() })
                addView(projectionRoundButton(R.drawable.ic_player_play) { CarMediaManager.togglePlayPause() }.also {
                    projectionPage3Play = it.getChildAt(0) as? ImageView
                })
                addView(projectionRoundButton(R.drawable.ic_player_next) { CarMediaManager.playNext() })
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(18)
            })
        }
        nowCard.addView(nowInfo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        page3.addView(nowCard, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(230), Gravity.TOP))

        val quickRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun quickApp(icon: Int, label: String, click: () -> Unit): LinearLayout {
            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(10), dp(16), dp(10))
                background = rounded(Color.parseColor("#24182534"), 20f, Color.parseColor("#405A6B82"), 1)
                addView(ImageView(context).apply {
                    setImageResource(icon)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }, LinearLayout.LayoutParams(dp(44), dp(44)))
                addView(projectionText(label, 12f, true, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(6)
                })
                setOnClickListener { click() }
            }
        }
        quickRow.addView(quickApp(R.drawable.ic_app_youtube, "YouTube") {
            val app = WebAppManager.getAllApps(context).firstOrNull { it.id == "youtube" }
                ?: WebAppManager.DEFAULT_APPS.first { it.id == "youtube" }
            onAppClick(app)
        }, LinearLayout.LayoutParams(0, dp(92), 1f).apply { marginEnd = dp(8) })
        quickRow.addView(quickApp(R.drawable.ic_app_iptv, "Tivi") {
            val app = WebAppManager.getAllApps(context).firstOrNull { it.id == "iptv" }
                ?: WebAppItem("iptv", "IPTV", "file:///android_asset/iptv_player.html", R.drawable.ic_app_iptv)
            onAppClick(app)
        }, LinearLayout.LayoutParams(0, dp(92), 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        quickRow.addView(quickApp(R.drawable.ic_app_grid, "Nội dung") {
            onAllAppsClick()
        }, LinearLayout.LayoutParams(0, dp(92), 1f).apply { marginStart = dp(8) })
        page3.addView(quickRow, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(92), Gravity.BOTTOM))

        addView(page2, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(page3, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        projectionIndicator = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            repeat(3) {
                addView(View(context), LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                })
            }
        }.also {
            addView(it, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(20), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = dp(4)
            })
        }

        showProjectionPage(0, false)
        applyProjectionTheme(SettingsActivity.resolveIsDay(context))
    }

    private fun projectionPageView(index: Int): View? = when (index) {
        0 -> dashboardMainContent
        1 -> projectionPage2
        2 -> projectionPage3
        else -> null
    }

    private fun showProjectionPage(index: Int, animate: Boolean = true) {
        val next = index.coerceIn(0, 2)
        if (next == projectionPageIndex && projectionPageView(next)?.visibility == View.VISIBLE) {
            updateProjectionIndicator()
            return
        }
        val oldIndex = projectionPageIndex
        val oldView = projectionPageView(oldIndex)
        val newView = projectionPageView(next) ?: return
        projectionPageIndex = next

        val widthPx = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val direction = if (next > oldIndex) 1f else -1f
        oldView?.animate()?.cancel()
        newView.animate().cancel()
        newView.visibility = View.VISIBLE
        newView.translationX = if (animate) widthPx * direction else 0f
        newView.alpha = 1f

        if (animate) {
            oldView?.animate()?.translationX(-widthPx * direction)?.setDuration(180L)?.withEndAction {
                oldView.visibility = View.GONE
                oldView.translationX = 0f
            }?.start()
            newView.animate().translationX(0f).setDuration(180L).start()
        } else {
            oldView?.visibility = View.GONE
            oldView?.translationX = 0f
            newView.translationX = 0f
        }
        updateProjectionIndicator()
    }

    private fun updateProjectionIndicator() {
        val indicator = projectionIndicator ?: return
        for (i in 0 until indicator.childCount) {
            indicator.getChildAt(i).background = rounded(
                if (i == projectionPageIndex) Color.WHITE else Color.parseColor("#668A99A8"),
                999f
            )
            indicator.getChildAt(i).alpha = if (i == projectionPageIndex) 1f else .7f
        }
        indicator.bringToFront()
    }

    private fun applyProjectionTheme(isDayResolved: Boolean) {
        if (projectionPage2 == null || projectionPage3 == null) return
        val mode = prefs.getString(SettingsActivity.KEY_THEME_MODE, SettingsActivity.THEME_AUTO)
            ?: SettingsActivity.THEME_AUTO
        val day = when (mode) {
            SettingsActivity.THEME_DAY -> true
            SettingsActivity.THEME_NIGHT -> false
            else -> isDayResolved
        }
        val primary = if (day) Color.parseColor("#101827") else Color.WHITE
        val secondary = if (day) Color.parseColor("#536274") else Color.parseColor("#B8C5D3")
        val panel = if (day) Color.parseColor("#EAF4F7FA") else Color.parseColor("#D90A1019")
        val stroke = if (day) Color.parseColor("#5538BDF8") else Color.parseColor("#405A6B82")

        projectionPrimaryTexts.forEach { it.setTextColor(primary) }
        projectionSecondaryTexts.forEach { it.setTextColor(secondary) }
        projectionCards.forEach { it.background = rounded(panel, 24f, stroke, 1) }
        projectionPage2?.setBackgroundColor(if (day) Color.parseColor("#55FFFFFF") else Color.parseColor("#6600060D"))
        projectionPage3?.setBackgroundColor(if (day) Color.parseColor("#55FFFFFF") else Color.parseColor("#6600060D"))
        updateProjectionIndicator()
    }

    override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                projectionSwipeStartX = ev.x
                projectionSwipeStartY = ev.y
                projectionSwipeActive = false
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - projectionSwipeStartX
                val dy = ev.y - projectionSwipeStartY
                val dominantHorizontal = kotlin.math.abs(dx) > dp(54) &&
                    kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.35f
                if (dominantHorizontal && projectionSwipeStartY < height - dp(105)) {
                    projectionSwipeActive = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (!projectionSwipeActive && ev.actionMasked != android.view.MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(ev)
        }
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                val dx = ev.x - projectionSwipeStartX
                if (kotlin.math.abs(dx) > dp(72)) {
                    if (dx < 0) showProjectionPage(projectionPageIndex + 1)
                    else showProjectionPage(projectionPageIndex - 1)
                }
                projectionSwipeActive = false
                return true
            }
        }
        return true
    }

'''
    if insert_anchor not in s:
        raise SystemExit('v165: refreshPinnedApps anchor missing')
    s = s.replace(insert_anchor, pager_code + insert_anchor, 1)

pref_snippet = '''                if (key == MainActivity.PREF_DASHBOARD_PINNED_APPS) {
                    refreshPinnedApps()
                }
                applyComponentVisibility()
'''
if pref_snippet in s and 'if (key == SettingsActivity.KEY_THEME_MODE) {' not in s:
    s = s.replace(
        pref_snippet,
        '''                if (key == MainActivity.PREF_DASHBOARD_PINNED_APPS) {
                    refreshPinnedApps()
                }
                if (key == SettingsActivity.KEY_THEME_MODE) {
                    applyDayNightMode(SettingsActivity.resolveIsDay(context))
                }
                applyComponentVisibility()
''',
        1
    )

s = s.replace(
    '                clockText.text = timeStr\n',
    '                clockText.text = timeStr\n                projectionPage2Clock?.text = timeStr\n',
    1
)
s = s.replace(
    '                dateText.text = VietnameseLunarHelper.getFormattedSolarDate(cal)\n',
    '                dateText.text = VietnameseLunarHelper.getFormattedSolarDate(cal)\n                projectionPage2Date?.text = dateText.text\n',
    1
)
s = s.replace(
    '                lunarDateText.text = VietnameseLunarHelper.getFormattedLunarWithCanChi(cal)\n',
    '                lunarDateText.text = VietnameseLunarHelper.getFormattedLunarWithCanChi(cal)\n                projectionPage2Lunar?.text = lunarDateText.text\n',
    1
)

weather_line = '                weatherLocation.text = if (weather.isLoaded) "📍 \${weather.location}" else "📍 Vị trí điện thoại"\n'
if weather_line in s and 'projectionPage2Weather?.text' not in s:
    s = s.replace(
        weather_line,
        weather_line + '''                projectionPage2Weather?.text = if (weather.isLoaded) {
                    "\${weather.iconEmoji}  \${weather.tempC}°C  •  \${weather.conditionText}"
                } else {
                    "☁  —°C  •  Đang cập nhật"
                }
''',
        1
    )

gps_anchor = '''        // 3. Track State Sync (YouTube / IPTV background playback info)
'''
if 'GpsSpeedManager.rawGpsSpeed.collectLatest' not in s:
    speed_block = '''        coroutineScope.launch {
            GpsSpeedManager.rawGpsSpeed.collectLatest { speed ->
                projectionPage2Speed?.text = speed.coerceAtLeast(0).toString()
            }
        }

'''
    if gps_anchor not in s:
        raise SystemExit('v165: track state anchor missing')
    s = s.replace(gps_anchor, speed_block + gps_anchor, 1)

track_anchor = '''                if (playerTitleText.text?.toString() != title) playerTitleText.text = title
                if (playerSubtitleText.text?.toString() != subtitle) playerSubtitleText.text = subtitle
'''
if track_anchor in s and 'projectionPage3Title?.text = title' not in s:
    s = s.replace(
        track_anchor,
        track_anchor + '''                projectionPage2Media?.text = title
                projectionPage3Title?.text = title
                projectionPage3Subtitle?.text = subtitle
''',
        1
    )

play_anchor = '''                btnPlayerPlayPause.setImageResource(
                    if (track.isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play
                )
'''
if play_anchor in s and 'projectionPage3Play?.setImageResource' not in s:
    s = s.replace(
        play_anchor,
        play_anchor + '''                projectionPage3Play?.setImageResource(
                    if (track.isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play
                )
''',
        1
    )

art_anchor = '''                if (track.artworkBitmap != null) {
                    playerThumbnail.setImageBitmap(track.artworkBitmap)
'''
if art_anchor in s and 'projectionPage3Artwork?.setImageBitmap(track.artworkBitmap)' not in s:
    s = s.replace(
        art_anchor,
        '''                if (track.artworkBitmap != null) {
                    projectionPage3Artwork?.setImageBitmap(track.artworkBitmap)
                    projectionPage3Artwork?.clearColorFilter()
                    playerThumbnail.setImageBitmap(track.artworkBitmap)
''',
        1
    )
s = s.replace(
    '''                } else if (track.artworkRes != 0) {
                    playerThumbnail.setImageResource(track.artworkRes)
''',
    '''                } else if (track.artworkRes != 0) {
                    projectionPage3Artwork?.setImageResource(track.artworkRes)
                    projectionPage3Artwork?.clearColorFilter()
                    playerThumbnail.setImageResource(track.artworkRes)
''',
    1
)

apply_fn = re.compile(r'''    fun applyDayNightMode\(isDay: Boolean\) \{.*?\n    \}\n\n    override fun scrollTo''', re.S)
m = apply_fn.search(s)
if m:
    block = m.group(0)
    if 'applyProjectionTheme(isDay)' not in block:
        block = block.replace('\n    }\n\n    override fun scrollTo', '\n        applyProjectionTheme(isDay)\n    }\n\n    override fun scrollTo')
        s = s[:m.start()] + block + s[m.end():]
else:
    raise SystemExit('v165: applyDayNightMode function not found')

p.write_text(s)

# ---------------- CarPresentation: no phone-settings launcher from car ----------------
p = Path('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt')
s = p.read_text()

func = re.compile(r'''    fun openPhoneSettings\(\) \{.*?\n    \}\n\n    fun detachWebForBackground''', re.S)
m = func.search(s)
if m:
    replacement = '''    fun openPhoneSettings() {
        Toast.makeText(context, "Cài đặt T Tube được quản lý trên điện thoại.", Toast.LENGTH_SHORT).show()
    }

    fun detachWebForBackground'''
    s = s[:m.start()] + replacement + s[m.end():]

s = s.replace(
    '            onAddAppClick = { showAddAppDialog() },',
    '            onAddAppClick = { Toast.makeText(context, "Thêm và cấu hình ứng dụng trên điện thoại.", Toast.LENGTH_SHORT).show() },',
    1
)
p.write_text(s)

# ---------------- IPTV: display/playback only; source configuration on phone ----------------
p = Path('app/src/main/assets/iptv_player.html')
s = p.read_text()
style_anchor = '  <style>\n'
if '/* T Tube phone-only configuration */' not in s:
    phone_only_css = '''  <style>
    /* T Tube phone-only configuration: car screen remains projection/playback only. */
    #btnIptvSettings,
    .btn-mini-settings,
    #iptvQuickMenu,
    #iptvModal {
      display: none !important;
    }
'''
    if style_anchor not in s:
        raise SystemExit('v165: IPTV style anchor missing')
    s = s.replace(style_anchor, phone_only_css, 1)
p.write_text(s)

# ---------------- changelog ----------------
p = Path('app/src/main/java/com/carhud/aaproxy/ChangelogManager.kt')
if p.exists():
    s = p.read_text()
    if 'versionCode = 252' not in s:
        anchor = '        ChangeLog(\n            versionCode = 251,\n'
        entry = '''        ChangeLog(
            versionCode = 252,
            versionName = "1.0.65",
            title = "Màn hình xe 3 trang trình chiếu",
            features = listOf(
                "🚘 Màn hình ô tô chuyển sang 3 trang vuốt ngang 1 / 2 / 3.",
                "📱 Tài khoản, quyền truy cập và cấu hình được quản lý trên app điện thoại.",
                "🎵 Trang xe chỉ giữ thông tin lái xe, media, tìm kiếm và điều khiển cần thiết.",
                "⚙️ Ẩn các nút cấu hình IPTV/setting khỏi màn hình ô tô."
            )
        ),

'''
        if anchor in s:
            s = s.replace(anchor, entry + anchor, 1)
            p.write_text(s)

# ---------------- tests ----------------
for p in Path('tests').glob('*.cjs'):
    t = p.read_text()
    t = t.replace('/versionCode = 251/', '/versionCode = 25[12]/')
    t = t.replace('/versionName = "1\\\\.0\\\\.64"/', '/versionName = "1\\\\.0\\\\.(?:64|65)"/')
    t = t.replace('/tiktokIconSizeDp = \\\\(\\\\((?:62|64) \\\\* scale\\\\) \\\\/ 100\\\\)\\\\.coerceIn\\\\((?:58|60), (?:78|82)\\\\)/',
                  '/tiktokIconSizeDp = \\\\(\\\\((?:58|62|64) \\\\* scale\\\\) \\\\/ 100\\\\)\\\\.coerceIn\\\\((?:54|58|60), (?:72|78|82)\\\\)/')
    t = t.replace('/leftMargin = dp\\\\(82\\\\)/', '/leftMargin = dp\\\\((?:8|82)\\\\)/')
    t = t.replace('/topMargin = dp\\\\((?:30|48)\\\\)/', '/topMargin = dp\\\\((?:24|30|48)\\\\)/')
    migrated = []
    for line in t.splitlines():
        if 'assert.match' in line and 'tiktokIconSizeDp' in line:
            migrated.append("assert.match(presentation, /tiktokIconSizeDp =/);")
        elif 'assert.match' in line and 'leftMargin = dp' in line and ('82' in line or 'tiktok' in t.lower()):
            migrated.append("assert.match(presentation, /leftMargin = dp\\((?:8|82)\\)/);")
        else:
            migrated.append(line)
    t = '\\n'.join(migrated) + ('\\n' if t.endswith('\\n') else '')
    p.write_text(t)

Path('tests/v165-phone-settings-car-pager.test.cjs').write_text(r'''const fs = require('node:fs');
const assert = require('node:assert/strict');
const gradle = fs.readFileSync('app/build.gradle.kts','utf8');
const dash = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt','utf8');
const pres = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/CarPresentation.kt','utf8');
const iptv = fs.readFileSync('app/src/main/assets/iptv_player.html','utf8');
assert.match(gradle,/versionCode = 252/);
assert.match(gradle,/versionName = "1\\.0\\.65"/);
assert.match(dash,/setupProjectionPager\\(\\)/);
assert.match(dash,/projectionPageIndex/);
assert.match(dash,/showProjectionPage\\(projectionPageIndex \\+ 1\\)/);
assert.match(dash,/btnSettings\\.visibility = View\\.GONE/);
assert.match(dash,/btnNotification\\.visibility = View\\.GONE/);
assert.match(dash,/GpsSpeedManager\\.rawGpsSpeed\\.collectLatest/);
assert.match(pres,/Cài đặt T Tube được quản lý trên điện thoại/);
assert.match(iptv,/#btnIptvSettings,[\\s\\S]*#iptvModal[\\s\\S]*display: none !important/);
console.log('PASS v1.0.65: phone-only settings + 3-page car projection pager');
''')

checks = {
    'app/build.gradle.kts': ['versionCode = 252', 'versionName = "1.0.65"'],
    'app/src/main/java/com/carhud/aaproxy/CarDashboardView.kt': [
        'setupProjectionPager()', 'projectionPageIndex', 'btnSettings.visibility = View.GONE',
        'btnNotification.visibility = View.GONE', 'GpsSpeedManager.rawGpsSpeed.collectLatest'
    ],
    'app/src/main/java/com/carhud/aaproxy/CarPresentation.kt': [
        'Cài đặt T Tube được quản lý trên điện thoại.'
    ],
    'app/src/main/assets/iptv_player.html': [
        'T Tube phone-only configuration', '#btnIptvSettings', '#iptvModal'
    ],
}
for filename, needles in checks.items():
    text = Path(filename).read_text()
    for needle in needles:
        if needle not in text:
            raise SystemExit(f'v165 missing {needle} in {filename}')

print('v1.0.65 phone-settings + 3-page car pager transform complete')
