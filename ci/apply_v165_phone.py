from pathlib import Path

# T Tube v1.0.65 / Build 252 — phone becomes the configuration/control center.
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

    private data class PhoneHubItem(
        val icon: String,
        val title: String,
        val accent: String,
        val action: () -> Unit
    )
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

    private fun openPhoneAdvancedSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun openExternalPhoneUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Throwable) {
            Toast.makeText(this, "Không tìm thấy ứng dụng phù hợp để mở.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPhoneSettingsCard(viewId: Int) {
        switchTab(TAB_SETTINGS)
        mainHandler.post { findViewById<View>(viewId)?.performClick() }
    }

    private fun createPhoneHubCell(item: PhoneHubItem, day: Boolean): LinearLayout {
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

            addView(TextView(this@MainActivity).apply {
                text = item.icon
                textSize = 25f
                gravity = Gravity.CENTER
                background = createCircleBg(Color.parseColor(item.accent))
            }, LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            })

            addView(TextView(this@MainActivity).apply {
                text = item.title
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(primary)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
            })

            setOnClickListener { item.action() }
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
        val target = page.coerceIn(0, 2)
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

        ((phoneCardYoutube.parent as? View)?.parent as? View)?.visibility = View.GONE
        sectionFavChannels.visibility = View.GONE
        phoneSearchBar.visibility = View.GONE

        val day = phoneHubIsDay()
        val secondary = if (day) Color.parseColor("#687789") else Color.parseColor("#94A3B8")

        val page1 = listOf(
            PhoneHubItem("▶", "YouTube", "#E11D48") { switchTab(TAB_YOUTUBE) },
            PhoneHubItem("📺", "IPTV", "#7C3AED") { switchTab(TAB_IPTV) },
            PhoneHubItem("🌐", "Trình duyệt", "#0284C7") {
                switchTab(TAB_YOUTUBE)
                initYoutubeWebView()
                youtubeWeb?.loadUrl("https://www.google.com")
            },
            PhoneHubItem("♪", "TikTok", "#111827") { openExternalPhoneUrl("https://www.tiktok.com/") },
            PhoneHubItem("📍", "Google Maps", "#2563EB") { openExternalPhoneUrl("https://maps.google.com/") },
            PhoneHubItem("🎵", "Zing MP3", "#9333EA") { openExternalPhoneUrl("https://zingmp3.vn/") },
            PhoneHubItem("●", "Spotify", "#16A34A") { openExternalPhoneUrl("https://open.spotify.com/") },
            PhoneHubItem("TV", "VTV Go", "#DC2626") { openExternalPhoneUrl("https://vtvgo.vn/") },
            PhoneHubItem("▤", "Báo Mới", "#0EA5E9") { openExternalPhoneUrl("https://baomoi.com/") }
        )

        val page2 = listOf(
            PhoneHubItem("▦", "Ứng dụng xe", "#334155") { openPhoneAdvancedSettings() },
            PhoneHubItem("◐", "Sáng / Tối / Auto", "#4F46E5") { showSubview(subviewDisplaySettings) },
            PhoneHubItem("▣", "Hình nền", "#0891B2") { showSubview(subviewWallpaper) },
            PhoneHubItem("M3U", "IPTV / M3U", "#7C3AED") { switchTab(TAB_SETTINGS) },
            PhoneHubItem("♫", "Media & vô lăng", "#9333EA") { openPhoneAdvancedSettings() },
            PhoneHubItem("🛡", "HUD & tốc độ", "#059669") { openPhoneAdvancedSettings() },
            PhoneHubItem("🎙", "Mic hệ thống", "#0F766E") { openPhoneAdvancedSettings() },
            PhoneHubItem("▤", "Dock màn hình xe", "#0369A1") { openPhoneAdvancedSettings() },
            PhoneHubItem("⌨", "Bàn phím Web", "#475569") { openPhoneAdvancedSettings() }
        )

        val page3 = listOf(
            PhoneHubItem("🔐", "Quyền truy cập", "#B45309") { showSubview(subviewPermissions) },
            PhoneHubItem("👤", "Tên hiển thị", "#2563EB") { openPhoneSettingsCard(R.id.settingCardUser) },
            PhoneHubItem("✓", "Bản quyền", "#059669") { startActivity(Intent(this, ActivationActivity::class.java)) },
            PhoneHubItem("ID", "Mã thiết bị", "#0F766E") { startActivity(Intent(this, ActivationActivity::class.java)) },
            PhoneHubItem("↻", "Cập nhật", "#0284C7") { UpdateNotificationManager.check(this, manual = true) },
            PhoneHubItem("⇧", "Sao lưu", "#4F46E5") { openPhoneAdvancedSettings() },
            PhoneHubItem("⇩", "Khôi phục", "#7C3AED") { openPhoneAdvancedSettings() },
            PhoneHubItem("≡", "Nhật ký", "#334155") { ChangelogManager.showChangelogDialog(this) },
            PhoneHubItem("i", "Thông tin", "#475569") {
                Toast.makeText(this, "T Tube " + BuildConfig.VERSION_NAME + " • Build " + BuildConfig.VERSION_CODE, Toast.LENGTH_LONG).show()
            }
        )

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

        val pages = listOf(page1, page2, page3)
        val pageWidth = (resources.displayMetrics.widthPixels - dp(32)).coerceAtLeast(dp(280))
        for (items in pages) {
            val grid = android.widget.GridLayout(this).apply {
                columnCount = 3
                rowCount = 3
                setPadding(dp(2), dp(2), dp(2), dp(2))
            }
            items.forEachIndexed { index, item ->
                grid.addView(createPhoneHubCell(item, day), android.widget.GridLayout.LayoutParams().apply {
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
            repeat(3) {
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

p.write_text(s)

checks = ['setupPhoneHubPager()', 'columnCount = 3', 'rowCount = 3', 'Vuốt ngang để chuyển Trang 1 / 2 / 3']
for needle in checks:
    if needle not in s:
        raise SystemExit('v165 phone missing: ' + needle)

print('v1.0.65 phone 3x3 pager transform complete')
