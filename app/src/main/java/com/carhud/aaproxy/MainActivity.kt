package com.carhud.aaproxy

import com.carhud.app.BuildConfig
import com.carhud.app.R
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * TCar Auto - Standardized Mobile Automotive & Multimedia Center
 * Pixel-perfect implementation matching user's 6-screen UI specification:
 * 1. Trang chủ (Hero card with wallpaper, large clock, 2x2 services grid, favorite channels, media player)
 * 2. Chọn hình nền (Header, 4 filter pills, 2-column grid with checkmark badges, custom picker)
 * 3. Cài đặt giao diện (9 component toggles, restore button, theme selector, accent color palette)
 * 4. Cấp quyền cần thiết (Location, Mic, Notification permission cards)
 * 5. YouTube (Integrated player, video info, recommendations, adblock & background audio)
 * 6. IPTV (Header, category pills, high-res channel items with logos and favorites)
 */
class MainActivity : AppCompatActivity() {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val playUpdater by lazy { PlayInAppUpdater(this) }
    private var fullExitReceiver: BroadcastReceiver? = null

    // Tabs
    private lateinit var tabHomeView: ScrollView
    private lateinit var tabYoutubeView: LinearLayout
    private lateinit var tabIptvView: LinearLayout
    private lateinit var tabSettingsView: ScrollView

    // Subviews (Screens 2, 3, 4)
    private lateinit var subviewWallpaper: ScrollView
    private lateinit var subviewDisplaySettings: ScrollView
    private lateinit var subviewPermissions: ScrollView

    // Nav Icons & Labels
    private lateinit var navIconHome: ImageView
    private lateinit var navLabelHome: TextView
    private lateinit var navIconYoutube: ImageView
    private lateinit var navLabelYoutube: TextView
    private lateinit var navIconIptv: ImageView
    private lateinit var navLabelIptv: TextView
    private lateinit var navIconSettings: ImageView
    private lateinit var navLabelSettings: TextView

    // Home Views
    private lateinit var cardPhoneCockpitPreview: FrameLayout
    private lateinit var phoneWallpaperView: ImageView
    private lateinit var phoneGreetingText: TextView
    private lateinit var phoneClockText: TextView
    private lateinit var phoneDateText: TextView
    private lateinit var phoneLunarDateText: TextView
    private lateinit var phoneSearchBar: LinearLayout
    private lateinit var phoneMicBtn: ImageView
    private lateinit var phoneCardYoutube: LinearLayout
    private lateinit var phoneCardIptv: LinearLayout
    private lateinit var phoneCardM3u: LinearLayout
    private lateinit var phoneCardBrowser: LinearLayout
    private lateinit var sectionFavChannels: LinearLayout
    private lateinit var quickChannelsRow: LinearLayout

    // Mini Player
    private lateinit var phoneMiniPlayer: LinearLayout
    private lateinit var phonePlayerThumbnail: ImageView
    private lateinit var phonePlayerFallbackIcon: ImageView
    private lateinit var phonePlayerTitleText: TextView
    private lateinit var phonePlayerSubtitleText: TextView
    private lateinit var phonePlayerProgressBar: ProgressBar
    private lateinit var btnPhonePlayerPrev: ImageView
    private lateinit var btnPhonePlayerPlayPause: ImageView
    private lateinit var btnPhonePlayerNext: ImageView
    private lateinit var phoneBottomNav: LinearLayout
    private lateinit var phoneAppsPager: HorizontalScrollView
    private lateinit var phoneAppsPages: LinearLayout
    private lateinit var phoneAppsPageIndicator: TextView

    // WebViews
    private var youtubeWeb: BackgroundAudioWebView? = null
    private var iptvWeb: BackgroundAudioWebView? = null
    private var activePhoneWebAppId: String = "youtube"
    private var pendingPhoneWebUrl: String? = null

    // Wallpaper Badges
    private var wpBadgeBugatti: ImageView? = null
    private var wpBadgeSilver: ImageView? = null
    private var wpBadgeBlue: ImageView? = null
    private var wpBadgeCyber: ImageView? = null
    private var wpBadgeSunset: ImageView? = null
    private var wpBadgeScenic: ImageView? = null
    private var wpBadgeDark: ImageView? = null
    private var wpBadgeCustom: ImageView? = null
    private var phoneAnimatedWallpaper: AnimatedImageDrawable? = null

    // Settings elements
    private lateinit var settingUserSubtext: TextView
    private lateinit var settingWallpaperSubtext: TextView
    private lateinit var settingIptvSubtext: TextView

    private var activeTab = TAB_HOME
    private var activeAccentColor = Color.parseColor("#38BDF8")

    companion object {
        const val TAB_HOME = "home"
        const val TAB_YOUTUBE = "youtube"
        const val TAB_IPTV = "iptv"
        const val TAB_SETTINGS = "settings"

        const val PREF_SHOW_HERO_CLOCK = "pref_show_hero_clock"
        const val PREF_SHOW_SEARCH_BAR = "pref_show_search_bar"
        const val PREF_SHOW_CARD_YOUTUBE = "pref_show_card_youtube"
        const val PREF_SHOW_CARD_IPTV = "pref_show_card_iptv"
        const val PREF_SHOW_CARD_M3U = "pref_show_card_m3u"
        const val PREF_SHOW_CARD_BROWSER = "pref_show_card_browser"
        const val PREF_DASHBOARD_PINNED_APPS = "pref_dashboard_pinned_apps"
        const val PREF_SHOW_FAV_CHANNELS = "pref_show_fav_channels"
        const val PREF_SHOW_MINI_PLAYER = "pref_show_mini_player"
        const val PREF_SHOW_BOTTOM_NAV = "pref_show_bottom_nav"
        const val PREF_ACCENT_COLOR = "pref_phone_accent_color"
        const val PREF_THEME_MODE = SettingsActivity.KEY_THEME_MODE
        const val PREF_FAV_CHANNELS_SET = "pref_fav_channels_set"
    }

    data class ChannelItem(
        val id: String,
        val name: String,
        val category: String,
        val iconRes: Int,
        val streamUrl: String,
        var isFavorite: Boolean = false,
        val logoUrl: String = ""
    )

    private val iptvChannelList = mutableListOf(
        ChannelItem("vtv1", "VTV1 HD", "Truyền hình Việt Nam", R.drawable.ic_ch_vtv1, "https://live.fptplay53.net/live/media/vtv1/live247-hls-avc/index.m3u8", true),
        ChannelItem("vtv3", "VTV3 HD", "Truyền hình Việt Nam", R.drawable.ic_ch_vtv3, "https://live.fptplay53.net/live/media/vtv3/live247-hls-avc/index.m3u8", true),
        ChannelItem("vtv6", "VTV6 HD", "Truyền hình Việt Nam", R.drawable.ic_ch_vtv6, "https://live.fptplay53.net/live/media/vtv6/live247-hls-avc/index.m3u8", false),
        ChannelItem("htv7", "HTV7 HD", "Truyền hình TP.HCM", R.drawable.ic_ch_htv7, "https://live.fptplay53.net/epzhd1/htv7hd_vhls.smil/chunklist_b5000000.m3u8", true),
        ChannelItem("htv9", "HTV9 HD", "Truyền hình TP.HCM", R.drawable.ic_ch_htv9, "https://live.fptplay53.net/epzhd1/htv9hd_vhls.smil/chunklist_b5000000.m3u8", false),
        ChannelItem("kplus1", "K+ Sport 1 HD", "Thể thao", R.drawable.ic_ch_kplus, "https://live.fptplay53.net/live/media/vtv1/live247-hls-avc/index.m3u8", true),
        ChannelItem("kplus2", "K+ Sport 2 HD", "Thể thao", R.drawable.ic_ch_kplus, "https://live.fptplay53.net/live/media/vtv2/live247-hls-avc/index.m3u8", false)
    )

    private var currentIptvFilter: String = "all"

    private fun resolveChannelIcon(name: String, id: String): Int {
        val lower = (name + " " + id).lowercase()
        return when {
            lower.contains("vtv10") -> R.drawable.ic_ch_vtv10
            lower.contains(Regex("""vtv1(?!\d)""")) -> R.drawable.ic_ch_vtv1
            lower.contains(Regex("""vtv2(?!\d)""")) -> R.drawable.ic_ch_vtv2
            lower.contains(Regex("""vtv3(?!\d)""")) -> R.drawable.ic_ch_vtv3
            lower.contains(Regex("""vtv4(?!\d)""")) -> R.drawable.ic_ch_vtv4
            lower.contains(Regex("""vtv5(?!\d)""")) -> R.drawable.ic_ch_vtv5
            lower.contains(Regex("""vtv6(?!\d)""")) -> R.drawable.ic_ch_vtv6
            lower.contains(Regex("""vtv7(?!\d)""")) -> R.drawable.ic_ch_vtv7
            lower.contains(Regex("""vtv8(?!\d)""")) -> R.drawable.ic_ch_vtv8
            lower.contains(Regex("""vtv9(?!\d)""")) -> R.drawable.ic_ch_vtv9
            lower.contains("htv7") -> R.drawable.ic_ch_htv7
            lower.contains("htv9") -> R.drawable.ic_ch_htv9
            lower.contains("vtv") -> R.drawable.ic_ch_vtv
            lower.contains("k+") || lower.contains("sport") || lower.contains("thể thao") -> R.drawable.ic_ch_kplus
            else -> R.drawable.ic_app_iptv
        }
    }

    /**
     * Phone UI: prefer the real channel logo declared by tvg-logo in the M3U.
     * The bundled drawable remains visible as a fallback while the remote logo
     * is loading or when the logo URL is missing/broken.
     */
    private fun bindPhoneChannelLogo(imageView: ImageView, channel: ChannelItem) {
        imageView.setImageResource(channel.iconRes)
        val logoUrl = channel.logoUrl.trim()
        imageView.tag = logoUrl
        if (logoUrl.isBlank()) return

        CarMediaManager.loadArtworkBitmap(logoUrl) { bitmap ->
            if (bitmap != null && imageView.tag == logoUrl && !isFinishing && !isDestroyed) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun loadIptvChannelsFromM3u(onLoaded: (() -> Unit)? = null) {
        IptvManager.getChannels(this) { channels ->
            if (channels.isNotEmpty()) {
                val favSet = prefs.getStringSet(PREF_FAV_CHANNELS_SET, null)
                val mapped = channels.map { ch ->
                    val icon = resolveChannelIcon(ch.name, ch.id)
                    val isFav = if (favSet != null) {
                        favSet.contains(ch.id) || favSet.contains(ch.name.lowercase().replace(" ", ""))
                    } else {
                        ch.name.contains("VTV1", ignoreCase = true) ||
                        ch.name.contains("VTV3", ignoreCase = true) ||
                        ch.name.contains("HTV7", ignoreCase = true)
                    }
                    ChannelItem(
                        id = ch.id,
                        name = ch.name,
                        category = if (ch.groupTitle.isNotBlank()) ch.groupTitle else "Truyền hình",
                        iconRes = icon,
                        streamUrl = ch.streamUrl,
                        isFavorite = isFav,
                        logoUrl = ch.logoUrl
                    )
                }
                iptvChannelList.clear()
                iptvChannelList.addAll(mapped)
                loadQuickChannels()
                renderIptvChannelsList(currentIptvFilter)
            }
            onLoaded?.invoke()
        }
    }

    // Launchers
    private val pickM3uFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var displayName = "playlist.m3u"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx != -1) {
                        displayName = cursor.getString(nameIdx)
                    }
                }
                contentResolver.openInputStream(uri)?.use { input ->
                    IptvManager.setM3uFile(this, input, displayName)
                }
                IptvManager.refresh(this) { list ->
                    Toast.makeText(this, "✅ Đã nạp file $displayName (${list.size} kênh)!", Toast.LENGTH_LONG).show()
                    updateIptvStatusText()
                    reloadIptvChannels()
                    loadIptvChannelsFromM3u()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi khi đọc file M3U: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickWallpaperLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handlePickedWallpaper(uri)
        }
    }

    private data class PreparedWallpaper(val file: File, val kind: String)

    private fun handlePickedWallpaper(uri: Uri) {
        // Scan file metadata and copy large media on the IO dispatcher instead
        // of blocking the phone/Android Auto UI thread.
        lifecycleScope.launch {
            try {
                val picked = withContext(Dispatchers.IO) { preparePickedWallpaper(uri) }
                prefs.edit()
                    .putString(CarDashboardView.PREF_WALLPAPER_TYPE, CarDashboardView.WALLPAPER_CUSTOM)
                    .putString(CarDashboardView.PREF_CUSTOM_WALLPAPER_PATH, picked.file.absolutePath)
                    .putString(CarDashboardView.PREF_CUSTOM_WALLPAPER_KIND, picked.kind)
                    .putLong(
                        CarDashboardView.PREF_CUSTOM_WALLPAPER_REVISION,
                        prefs.getLong(CarDashboardView.PREF_CUSTOM_WALLPAPER_REVISION, 0L) + 1L
                    )
                    .apply()
                updateWallpaperDisplay()
                updateWallpaperSettingsText()
                Toast.makeText(this@MainActivity, "✅ Đã chọn hình nền mới.", Toast.LENGTH_LONG).show()

                // Keep the old file briefly so a car-side player can release its
                // descriptor before we prune older selections.
                lifecycleScope.launch {
                    delay(30_000L)
                    withContext(Dispatchers.IO) {
                        val current = prefs.getString(CarDashboardView.PREF_CUSTOM_WALLPAPER_PATH, null)
                        filesDir.listFiles()?.filter {
                            it.name.startsWith("custom_wallpaper.") &&
                            it.absolutePath != current && !it.name.endsWith(".pending")
                        }?.forEach { try { it.delete() } catch (_: Throwable) {} }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity, "Lỗi khi chọn hình nền: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun preparePickedWallpaper(uri: Uri): PreparedWallpaper {
        val mime = contentResolver.getType(uri)?.lowercase(Locale.US).orEmpty()
        val name = queryDisplayName(uri).lowercase(Locale.US)
        val kind = when {
            mime.startsWith("video/") || listOf(".mp4", ".webm", ".mkv").any(name::endsWith) ->
                CarDashboardView.WALLPAPER_KIND_VIDEO
            mime == "image/gif" || name.endsWith(".gif") ->
                CarDashboardView.WALLPAPER_KIND_GIF
            mime.startsWith("image/") || listOf(".jpg", ".jpeg", ".png", ".webp").any(name::endsWith) ->
                CarDashboardView.WALLPAPER_KIND_IMAGE
            else -> throw IllegalArgumentException("Chỉ hỗ trợ JPG/PNG/WEBP, GIF hoặc MP4/WebM.")
        }
        val maxBytes = if (kind == CarDashboardView.WALLPAPER_KIND_VIDEO)
            80L * 1024L * 1024L else 30L * 1024L * 1024L
        if (queryContentSize(uri) > maxBytes) {
            throw IllegalArgumentException("File quá lớn. Giới hạn ${maxBytes / (1024L * 1024L)} MB.")
        }
        if (kind == CarDashboardView.WALLPAPER_KIND_VIDEO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                if (duration > 60_000L) throw IllegalArgumentException(
                    "Video hình nền tối đa 60 giây."
                )
            } finally {
                try { retriever.release() } catch (_: Throwable) {}
            }
        }

        val ext = when (kind) {
            CarDashboardView.WALLPAPER_KIND_VIDEO ->
                if (name.endsWith(".webm")) "webm" else "mp4"
            CarDashboardView.WALLPAPER_KIND_GIF -> "gif"
            else -> when {
                name.endsWith(".webp") -> "webp"
                name.endsWith(".jpg") || name.endsWith(".jpeg") -> "jpg"
                else -> "png"
            }
        }
        // Unique destination and atomic commit avoid clobbering a file that
        // MediaPlayer on the car is still reading.
        val dest = File(filesDir, "custom_wallpaper.${System.nanoTime()}.$ext")
        val pending = File(filesDir, "${dest.name}.pending")
        try {
            val stream = contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Không đọc được file đã chọn")
            stream.use { input ->
                pending.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        copied += n
                        if (copied > maxBytes) throw IllegalArgumentException(
                            "File vượt giới hạn cho phép."
                        )
                        output.write(buffer, 0, n)
                    }
                    if (copied == 0L) throw IllegalArgumentException("File hình nền rỗng.")
                }
            }
            if (!pending.renameTo(dest)) throw IllegalStateException(
                "Không thể lưu hình nền."
            )
            return PreparedWallpaper(dest, kind)
        } finally {
            if (pending.exists()) pending.delete()
        }
    }


    private fun queryDisplayName(uri: Uri): String {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            }.orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun queryContentSize(uri: Uri): Long {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else 0L
            } ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    private val requestLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        updatePermissionStatusDisplay()
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            Toast.makeText(this, "✅ Đã cấp quyền Vị Trí (GPS & Cảnh báo tốc độ)!", Toast.LENGTH_SHORT).show()
            GpsSpeedManager.start(applicationContext)
        } else {
            Toast.makeText(this, "⚠️ Bạn đã từ chối cấp quyền Vị Trí", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestMicLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updatePermissionStatusDisplay()
        if (granted) {
            Toast.makeText(this, "✅ Đã cấp quyền Ghi âm (Tìm kiếm giọng nói)!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "⚠️ Bạn đã từ chối cấp quyền Ghi âm", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updatePermissionStatusDisplay()
        if (granted) {
            Toast.makeText(this, "✅ Đã cấp quyền Thông báo phát nền!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StableProjectionHost.ensureEnabled(this)
        registerFullExitReceiver()

        // v1.0.63 build 251: phone UI starts in Night mode on first launch.
        // Existing users keep their explicit Day/Night/Auto choice.
        if (!prefs.contains(PREF_THEME_MODE)) {
            prefs.edit()
                .putString(PREF_THEME_MODE, SettingsActivity.THEME_NIGHT)
                .putBoolean("carhud_day_mode", false)
                .apply()
        }

        // Google Play distribution is free/unlocked. Keep the legacy external
        // activation flow only for sideload builds.
        if (!BuildConfig.PLAY_DISTRIBUTION) {
            val auditKey = "audit_activation_check_v171"
            if (!prefs.getBoolean(auditKey, false)) {
                LicenseManager.resetLicense(this)
                prefs.edit().putBoolean(auditKey, true).apply()
            }

            if (!LicenseManager.isLicensed(this)) {
                val intent = Intent(this, ActivationActivity::class.java)
                startActivity(intent)
                finish()
                return
            }
        }

        if (BuildConfig.PLAY_DISTRIBUTION) playUpdater // register launcher before STARTED
        setContentView(R.layout.activity_main)

        CarMediaManager.mainActivityRoot = findViewById(android.R.id.content)
        CarMediaManager.registerService(this)

        initViews()
        loadSavedFavorites()
        setupBottomNav()
        setupHomeActions()
        setupPhoneAppGrid()
        setupWallpaperChooserScreen()
        setupDisplaySettingsScreen()
        setupPermissionsScreen()
        setupYouTubeTab()
        setupIptvTab()
        loadIptvChannelsFromM3u()
        setupSettingsTab()
        setupMiniPlayer()
        applyDisplaySettings()
        applyPhoneTheme()
        updateWallpaperDisplay()
        startClockTicker()

        // Hiển thị nhật ký cập nhật phiên bản mới khi vừa cập nhật app
        ChangelogManager.checkAndShowChangelog(this, onDismiss = {
            if (BuildConfig.PLAY_DISTRIBUTION) {
                playUpdater.checkForUpdates(manual = false)
            } else {
                UpdateNotificationManager.check(this)
            }
        })
        startMediaObserver()
        loadQuickChannels()
    }

    private fun registerFullExitReceiver() {
        if (fullExitReceiver != null) return
        fullExitReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != CarAppShutdownManager.ACTION_FULL_EXIT) return
                try {
                    // Remove the whole phone task immediately. The shutdown manager
                    // then stops services and terminates the process a moment later.
                    finishAndRemoveTask()
                } catch (_: Throwable) {
                    try { finishAffinity() } catch (_: Throwable) { finish() }
                }
            }
        }
        val filter = IntentFilter(CarAppShutdownManager.ACTION_FULL_EXIT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(fullExitReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(fullExitReceiver, filter)
        }
    }

    private fun initViews() {
        tabHomeView = findViewById(R.id.tabHomeView)
        tabYoutubeView = findViewById(R.id.tabYoutubeView)
        tabIptvView = findViewById(R.id.tabIptvView)
        tabSettingsView = findViewById(R.id.tabSettingsView)

        subviewWallpaper = findViewById(R.id.subviewWallpaper)
        subviewDisplaySettings = findViewById(R.id.subviewDisplaySettings)
        subviewPermissions = findViewById(R.id.subviewPermissions)

        navIconHome = findViewById(R.id.navIconHome)
        navLabelHome = findViewById(R.id.navLabelHome)
        navIconYoutube = findViewById(R.id.navIconYoutube)
        navLabelYoutube = findViewById(R.id.navLabelYoutube)
        navIconIptv = findViewById(R.id.navIconIptv)
        navLabelIptv = findViewById(R.id.navLabelIptv)
        navIconSettings = findViewById(R.id.navIconSettings)
        navLabelSettings = findViewById(R.id.navLabelSettings)

        cardPhoneCockpitPreview = findViewById(R.id.cardPhoneCockpitPreview)
        phoneWallpaperView = findViewById(R.id.phoneWallpaperView)
        phoneGreetingText = findViewById(R.id.phoneGreetingText)
        phoneClockText = findViewById(R.id.phoneClockText)
        phoneDateText = findViewById(R.id.phoneDateText)
        phoneLunarDateText = findViewById(R.id.phoneLunarDateText)
        phoneSearchBar = findViewById(R.id.phoneSearchBar)
        phoneMicBtn = findViewById(R.id.phoneMicBtn)
        phoneCardYoutube = findViewById(R.id.phoneCardYoutube)
        phoneCardIptv = findViewById(R.id.phoneCardIptv)
        phoneCardM3u = findViewById(R.id.phoneCardM3u)
        phoneCardBrowser = findViewById(R.id.phoneCardBrowser)
        sectionFavChannels = findViewById(R.id.sectionFavChannels)
        quickChannelsRow = findViewById(R.id.quickChannelsRow)

        phoneMiniPlayer = findViewById(R.id.phoneMiniPlayer)
        phonePlayerThumbnail = findViewById(R.id.phonePlayerThumbnail)
        phonePlayerFallbackIcon = findViewById(R.id.phonePlayerFallbackIcon)
        phonePlayerTitleText = findViewById(R.id.phonePlayerTitleText)
        phonePlayerSubtitleText = findViewById(R.id.phonePlayerSubtitleText)
        phonePlayerProgressBar = findViewById(R.id.phonePlayerProgressBar)
        btnPhonePlayerPrev = findViewById(R.id.btnPhonePlayerPrev)
        btnPhonePlayerPlayPause = findViewById(R.id.btnPhonePlayerPlayPause)
        btnPhonePlayerNext = findViewById(R.id.btnPhonePlayerNext)
        phoneBottomNav = findViewById(R.id.phoneBottomNav)
        phoneAppsPager = findViewById(R.id.phoneAppsPager)
        phoneAppsPages = findViewById(R.id.phoneAppsPages)
        phoneAppsPageIndicator = findViewById(R.id.phoneAppsPageIndicator)

        wpBadgeBugatti = findViewById(R.id.wpBadgeBugatti)
        wpBadgeSilver = findViewById(R.id.wpBadgeSilver)
        wpBadgeBlue = findViewById(R.id.wpBadgeBlue)
        wpBadgeCyber = findViewById(R.id.wpBadgeCyber)
        wpBadgeSunset = findViewById(R.id.wpBadgeSunset)
        wpBadgeScenic = findViewById(R.id.wpBadgeScenic)
        wpBadgeDark = findViewById(R.id.wpBadgeDark)
        wpBadgeCustom = findViewById(R.id.wpBadgeCustom)

        settingUserSubtext = findViewById(R.id.settingUserSubtext)
        settingWallpaperSubtext = findViewById(R.id.settingWallpaperSubtext)
        settingIptvSubtext = findViewById(R.id.settingIptvSubtext)

        phonePlayerTitleText.isSelected = true
    }

    private fun showSubview(v: View) {
        subviewWallpaper.visibility = View.GONE
        subviewDisplaySettings.visibility = View.GONE
        subviewPermissions.visibility = View.GONE

        v.visibility = View.VISIBLE
        v.bringToFront()
    }

    private fun hideAllSubviews() {
        subviewWallpaper.visibility = View.GONE
        subviewDisplaySettings.visibility = View.GONE
        subviewPermissions.visibility = View.GONE
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navTabHome).setOnClickListener { switchTab(TAB_HOME) }
        findViewById<LinearLayout>(R.id.navTabYoutube).setOnClickListener { switchTab(TAB_YOUTUBE) }
        findViewById<LinearLayout>(R.id.navTabIptv).setOnClickListener { switchTab(TAB_IPTV) }
        findViewById<LinearLayout>(R.id.navTabSettings).setOnClickListener { switchTab(TAB_SETTINGS) }
    }

    private fun switchTab(tab: String) {
        hideAllSubviews()
        activeTab = tab
        // Keep the phone navigation toolbar docked at the bottom on every tab.
        // The mini-player is only useful on the IPTV screen; keep Home and Settings clean.
        phoneBottomNav.visibility = if (prefs.getBoolean(PREF_SHOW_BOTTOM_NAV, true)) View.VISIBLE else View.GONE
        phoneMiniPlayer.visibility = if (tab == TAB_IPTV && prefs.getBoolean(PREF_SHOW_MINI_PLAYER, true)) View.VISIBLE else View.GONE

        val bgAudio = prefs.getBoolean(SettingsActivity.KEY_BACKGROUND_AUDIO, true)

        tabHomeView.visibility = if (tab == TAB_HOME) View.VISIBLE else View.GONE
        tabSettingsView.visibility = if (tab == TAB_SETTINGS) View.VISIBLE else View.GONE

        if (tab == TAB_YOUTUBE) {
            tabYoutubeView.visibility = View.VISIBLE
            tabYoutubeView.translationX = 0f
            tabYoutubeView.bringToFront()
        } else if (!bgAudio) {
            tabYoutubeView.visibility = View.GONE
            tabYoutubeView.translationX = -50000f
        } else {
            tabYoutubeView.visibility = View.VISIBLE
            tabYoutubeView.translationX = -50000f
        }

        if (tab == TAB_IPTV) {
            tabIptvView.visibility = View.VISIBLE
            tabIptvView.translationX = 0f
            tabIptvView.bringToFront()
        } else if (!bgAudio) {
            tabIptvView.visibility = View.GONE
            tabIptvView.translationX = -50000f
        } else {
            tabIptvView.visibility = View.VISIBLE
            tabIptvView.translationX = -50000f
        }

        when (tab) {
            TAB_HOME -> tabHomeView.bringToFront()
            TAB_SETTINGS -> tabSettingsView.bringToFront()
            TAB_YOUTUBE -> tabYoutubeView.bringToFront()
            TAB_IPTV -> tabIptvView.bringToFront()
        }

        // Tab Highlight Colors
        val activeColor = activeAccentColor
        val inactiveColor = Color.parseColor("#94A3B8")

        navIconHome.setColorFilter(if (tab == TAB_HOME) activeColor else inactiveColor)
        navLabelHome.setTextColor(if (tab == TAB_HOME) activeColor else inactiveColor)

        navIconYoutube.setColorFilter(if (tab == TAB_YOUTUBE) activeColor else inactiveColor)
        navLabelYoutube.setTextColor(if (tab == TAB_YOUTUBE) activeColor else inactiveColor)

        navIconIptv.setColorFilter(if (tab == TAB_IPTV) activeColor else inactiveColor)
        navLabelIptv.setTextColor(if (tab == TAB_IPTV) activeColor else inactiveColor)

        navIconSettings.setColorFilter(if (tab == TAB_SETTINGS) activeColor else inactiveColor)
        navLabelSettings.setTextColor(if (tab == TAB_SETTINGS) activeColor else inactiveColor)

        if (tab == TAB_YOUTUBE) {
            initYoutubeWebView()
            youtubeWeb?.let {
                CarMediaManager.registerPhoneWebView(it, this)
                it.onResume()
                it.resumeTimers()
            }
        } else if (tab == TAB_IPTV) {
            initIptvWebView()
            iptvWeb?.let {
                CarMediaManager.registerPhoneWebView(it, this)
                it.onResume()
                it.resumeTimers()
            }
        }
    }

    // ==========================================
    // SCREEN 1: HOME ACTIONS & FAVORITE CHANNELS
    // ==========================================

    private fun setupHomeActions() {
        // Top bar Settings gear button -> opens Screen 3: Cài đặt giao diện
        findViewById<FrameLayout>(R.id.btnPhoneSettings)?.setOnClickListener {
            showSubview(subviewDisplaySettings)
        }

        // Hero card click -> opens Screen 2: Chọn hình nền
        cardPhoneCockpitPreview.setOnClickListener {
            showSubview(subviewWallpaper)
        }

        // 2x2 Services Grid
        phoneCardYoutube.setOnClickListener { switchTab(TAB_YOUTUBE) }
        phoneCardIptv.setOnClickListener { switchTab(TAB_IPTV) }
        phoneCardM3u.setOnClickListener {
            switchTab(TAB_SETTINGS)
            pickM3uFileLauncher.launch("*/*")
        }
        phoneCardBrowser.setOnClickListener {
            switchTab(TAB_YOUTUBE)
            youtubeWeb?.loadUrl("https://google.com")
        }

        // Search bar
        phoneSearchBar.setOnClickListener { showSearchDialog() }
        phoneMicBtn.setOnClickListener { showSearchDialog() }
    }


    private fun setupPhoneAppGrid() {
        if (!::phoneAppsPages.isInitialized) return
        val apps = WebAppManager.getAllApps(this).filter { WebAppManager.isAppEnabled(this, it.id) }
        val pageSize = 9
        val pageCount = kotlin.math.max(1, (apps.size + pageSize - 1) / pageSize)
        val pageWidth = (resources.displayMetrics.widthPixels - dp(32)).coerceAtLeast(dp(300))

        phoneAppsPages.removeAllViews()
        if (apps.isEmpty()) {
            phoneAppsPages.addView(TextView(this).apply {
                text = "Chưa bật ứng dụng nào. Nhấn giữ để mở Cài đặt."
                gravity = Gravity.CENTER
                setTextColor(if (SettingsActivity.resolveIsDay(this@MainActivity)) Color.parseColor("#475569") else Color.parseColor("#94A3B8"))
                layoutParams = LinearLayout.LayoutParams(pageWidth, dp(92))
                setOnLongClickListener {
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    true
                }
            })
        } else {
            apps.chunked(pageSize).forEach { pageApps ->
                val grid = GridLayout(this).apply {
                    columnCount = 3
                    rowCount = 3
                    alignmentMode = GridLayout.ALIGN_BOUNDS
                    useDefaultMargins = false
                    layoutParams = LinearLayout.LayoutParams(pageWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                val cardWidth = ((pageWidth - dp(16)) / 3).coerceAtLeast(dp(88))
                for (slot in 0 until pageSize) {
                    val row = slot / 3
                    val col = slot % 3
                    val app = pageApps.getOrNull(slot)
                    val child = if (app != null) createPhoneAppCard(app, cardWidth) else View(this).apply { visibility = View.INVISIBLE }
                    val lp = GridLayout.LayoutParams(
                        GridLayout.spec(row, 1),
                        GridLayout.spec(col, 1)
                    ).apply {
                        width = cardWidth
                        height = dp(78)
                        setMargins(dp(3), dp(4), dp(3), dp(4))
                    }
                    grid.addView(child, lp)
                }
                phoneAppsPages.addView(grid)
            }
        }

        fun updateIndicator(page: Int) {
            val safePage = page.coerceIn(0, pageCount - 1)
            val dots = (0 until pageCount).joinToString("  ") { if (it == safePage) "•" else "◦" }
            phoneAppsPageIndicator.text = "$dots   Vuốt ngang để chuyển · Trang ${safePage + 1} / $pageCount"
        }
        updateIndicator(0)
        phoneAppsPager.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            val page = kotlin.math.round(scrollX.toFloat() / pageWidth.toFloat()).toInt()
            updateIndicator(page)
        }
    }

    private fun createPhoneAppCard(app: WebAppItem, cardWidth: Int): View {
        val isDay = SettingsActivity.resolveIsDay(this)
        val cardBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
            setColor(if (isDay) Color.parseColor("#F8FAFC") else Color.parseColor("#0B111D"))
            setStroke(dp(1), if (isDay) Color.parseColor("#CBD5E1") else Color.parseColor("#334155"))
        }
        val iconBgColor = runCatching { Color.parseColor(app.colorHex) }.getOrDefault(Color.parseColor("#0284C7"))

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = cardBg
            isClickable = true
            isFocusable = true
            setPadding(dp(4), dp(7), dp(4), dp(5))

            val iconFrame = FrameLayout(this@MainActivity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(iconBgColor)
                }
            }
            val icon = ImageView(this@MainActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                val customPath = app.iconPath
                if (!customPath.isNullOrBlank()) {
                    val bmp = runCatching { BitmapFactory.decodeFile(customPath) }.getOrNull()
                    if (bmp != null) setImageBitmap(bmp) else setImageResource(app.iconRes)
                } else {
                    setImageResource(app.iconRes)
                }
            }
            iconFrame.addView(icon, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
            addView(iconFrame, LinearLayout.LayoutParams(dp(36), dp(36)))

            addView(TextView(this@MainActivity).apply {
                text = app.name
                textSize = 9.5f
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isDay) Color.parseColor("#0F172A") else Color.WHITE)
            }, LinearLayout.LayoutParams(cardWidth - dp(8), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(5)
            })

            setOnClickListener { openPhoneApp(app) }
            setOnLongClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java).apply {
                    putExtra("focus_app_id", app.id)
                })
                true
            }
        }
    }

    private fun openPhoneApp(app: WebAppItem) {
        when (app.id) {
            "iptv" -> {
                activePhoneWebAppId = "iptv"
                switchTab(TAB_IPTV)
            }
            else -> {
                activePhoneWebAppId = app.id
                pendingPhoneWebUrl = app.url
                val existing = youtubeWeb
                switchTab(TAB_YOUTUBE)
                val target = youtubeWeb ?: return
                configurePhoneWebViewForApp(target, app, app.url)
                if (existing != null) {
                    val current = target.url.orEmpty()
                    val sameTarget = when (app.id) {
                        "youtube" -> current.contains("youtube.com", true) || current.contains("youtu.be", true)
                        "tiktok" -> TikTokWebPolicy.isTikTok(current)
                        else -> runCatching { Uri.parse(current).host == Uri.parse(app.url).host }.getOrDefault(false)
                    }
                    if (!sameTarget) target.loadUrl(app.url)
                    pendingPhoneWebUrl = null
                }
            }
        }
    }

    private fun applyPhoneTheme() {
        if (!::tabHomeView.isInitialized) return

        val isDay = SettingsActivity.resolveIsDay(this)
        val bg = if (isDay) Color.parseColor("#F1F5F9") else Color.parseColor("#070A11")
        val surface = if (isDay) Color.parseColor("#FFFFFF") else Color.parseColor("#0B111D")
        val card = if (isDay) Color.parseColor("#D1D5DB") else Color.parseColor("#CC162033")
        val cardBorder = if (isDay) Color.parseColor("#CBD5E1") else Color.parseColor("#334155")
        val pill = if (isDay) Color.parseColor("#B8C0CC") else Color.parseColor("#401E293B")
        val pillBorder = if (isDay) Color.parseColor("#94A3B8") else Color.parseColor("#475569")
        val muted = if (isDay) Color.parseColor("#475569") else Color.parseColor("#94A3B8")

        // Main screens / sub-screens follow Day, Night or Auto. The T Tube header,
        // mini-player and bottom navigation keep their familiar dark chrome.
        findViewById<View>(R.id.phoneRoot)?.setBackgroundColor(bg)
        findViewById<View>(R.id.phoneTopBar)?.setBackgroundColor(Color.parseColor("#0B111D"))
        tabHomeView.setBackgroundColor(bg)
        tabYoutubeView.setBackgroundColor(surface)
        tabIptvView.setBackgroundColor(bg)
        tabSettingsView.setBackgroundColor(bg)
        subviewWallpaper.setBackgroundColor(bg)
        subviewDisplaySettings.setBackgroundColor(bg)
        subviewPermissions.setBackgroundColor(bg)

        phoneAppsPageIndicator.setTextColor(muted)
        phoneSearchBar.background = createCardBg(
            if (isDay) Color.parseColor("#E2E8F0") else Color.parseColor("#CC111827"),
            22f,
            if (isDay) Color.parseColor("#CBD5E1") else Color.parseColor("#475569"),
            1
        )

        // Settings hub cards match the old light/dark phone interface.
        listOf(
            R.id.settingCardWallpaper,
            R.id.settingCardDisplay,
            R.id.settingCardPermissions,
            R.id.settingCardUser,
            R.id.settingCardIptv,
            R.id.settingCardAdvanced,
            R.id.phoneDisplayToggleCard,
            R.id.phoneThemeCard
        ).forEach { id ->
            findViewById<View>(id)?.background = createCardBg(card, 16f, cardBorder, 1)
        }

        findViewById<Button>(R.id.btnRestoreDefaultLayout)?.apply {
            background = createCardBg(Color.parseColor("#0284C7"), 12f, Color.parseColor("#38BDF8"), 1)
            setTextColor(Color.WHITE)
        }

        listOf(
            R.id.btnPermGrantLocation,
            R.id.btnPermGrantMic,
            R.id.btnPermGrantNotification,
            R.id.btnPickM3uFile,
            R.id.btnInputM3uUrl,
            R.id.btnOpenAdvancedSettings
        ).forEach { id ->
            findViewById<View>(id)?.background = createCardBg(pill, 20f, pillBorder, 1)
        }

        // Convert the static XML text palette in both directions. Accent labels stay
        // cyan; dark chrome (hero, player and bottom navigation) is deliberately skipped.
        applyPhoneTextTheme(tabHomeView, isDay)
        applyPhoneTextTheme(tabIptvView, isDay)
        applyPhoneTextTheme(tabSettingsView, isDay)
        applyPhoneTextTheme(subviewWallpaper, isDay)
        applyPhoneTextTheme(subviewDisplaySettings, isDay)
        applyPhoneTextTheme(subviewPermissions, isDay)

        // Hero copy always overlays a photo and must remain white.
        phoneGreetingText.setTextColor(Color.WHITE)
        phoneClockText.setTextColor(Color.WHITE)
        phoneDateText.setTextColor(Color.WHITE)
        phoneLunarDateText.setTextColor(Color.WHITE)

        val themeMode = prefs.getString(PREF_THEME_MODE, SettingsActivity.THEME_AUTO) ?: SettingsActivity.THEME_AUTO
        val selectedThemeButton = when (themeMode) {
            SettingsActivity.THEME_DAY, "light" -> findViewById<TextView>(R.id.btnThemeLight)
            SettingsActivity.THEME_NIGHT, "dark" -> findViewById<TextView>(R.id.btnThemeDark)
            else -> findViewById<TextView>(R.id.btnThemeAuto)
        }
        listOf(
            findViewById<TextView>(R.id.btnThemeDark),
            findViewById<TextView>(R.id.btnThemeLight),
            findViewById<TextView>(R.id.btnThemeAuto)
        ).filterNotNull().forEach { button ->
            val selected = button === selectedThemeButton
            button.background = if (selected) {
                createCardBg(Color.parseColor("#0284C7"), 9f, Color.parseColor("#38BDF8"), 1)
            } else {
                createCardBg(Color.TRANSPARENT, 9f, Color.TRANSPARENT, 0)
            }
            button.setTextColor(
                if (selected) Color.WHITE
                else if (isDay) Color.parseColor("#475569") else Color.parseColor("#94A3B8")
            )
            button.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        // Header/back icons need contrast on light sub-screens.
        val iconColor = if (isDay) Color.parseColor("#0F172A") else Color.WHITE
        listOf(
            R.id.btnBackWallpaper,
            R.id.btnBackDisplaySettings,
            R.id.btnBackPermissions,
            R.id.btnBackIptv
        ).forEach { id ->
            findViewById<ImageView>(id)?.setColorFilter(iconColor)
        }

        window.statusBarColor = bg
        window.navigationBarColor = bg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = window.decorView.systemUiVisibility
            flags = if (isDay) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = if (isDay) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
            }
            window.decorView.systemUiVisibility = flags
        }

        youtubeWeb?.let { YouTubePlayerHelper.applyUniversalWebTheme(it, isDay) }
        iptvWeb?.let {
            YouTubePlayerHelper.applyUniversalWebTheme(it, isDay)
            it.evaluateJavascript(
                "if (typeof window.setIptvTheme === 'function') window.setIptvTheme(${if (isDay) "true" else "false"});",
                null
            )
        }

        if (::phoneAppsPages.isInitialized) setupPhoneAppGrid()
    }

    private fun applyPhoneTextTheme(root: View, isDay: Boolean) {
        val skipTreeIds = setOf(
            R.id.cardPhoneCockpitPreview,
            R.id.phoneMiniPlayer,
            R.id.phoneBottomNav,
            R.id.phoneTopBar
        )
        val skipTextIds = setOf(
            R.id.btnThemeDark,
            R.id.btnThemeLight,
            R.id.btnThemeAuto
        )

        fun mappedColor(current: Int): Int? {
            val white = Color.WHITE
            val light1 = Color.parseColor("#E2E8F0")
            val light2 = Color.parseColor("#CBD5E1")
            val light3 = Color.parseColor("#94A3B8")
            val dark1 = Color.parseColor("#0F172A")
            val dark2 = Color.parseColor("#334155")
            val dark3 = Color.parseColor("#475569")
            val dark4 = Color.parseColor("#64748B")

            return if (isDay) {
                when (current) {
                    white -> dark1
                    light1 -> dark2
                    light2 -> dark3
                    light3 -> dark4
                    else -> null
                }
            } else {
                when (current) {
                    dark1 -> white
                    dark2 -> light1
                    dark3 -> light2
                    dark4 -> light3
                    else -> null
                }
            }
        }

        fun walk(view: View) {
            if (view.id in skipTreeIds) return
            if (view is TextView && view.id !in skipTextIds) {
                mappedColor(view.currentTextColor)?.let(view::setTextColor)
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    walk(view.getChildAt(i))
                }
            }
        }

        walk(root)
    }

    private fun stylePhoneDynamicCard(view: View) {
        val isDay = SettingsActivity.resolveIsDay(this)
        val bg = if (isDay) Color.parseColor("#F8FAFC") else Color.parseColor("#CC162033")
        val border = if (isDay) Color.parseColor("#CBD5E1") else Color.parseColor("#334155")
        view.background = createCardBg(bg, 14f, border, 1)

        fun walk(v: View) {
            if (v is TextView) {
                val current = v.currentTextColor
                if (isDay) {
                    when (current) {
                        Color.WHITE, Color.parseColor("#E2E8F0") -> v.setTextColor(Color.parseColor("#0F172A"))
                        Color.parseColor("#CBD5E1"), Color.parseColor("#94A3B8") -> v.setTextColor(Color.parseColor("#475569"))
                    }
                } else {
                    when (current) {
                        Color.parseColor("#0F172A") -> v.setTextColor(Color.WHITE)
                        Color.parseColor("#334155"), Color.parseColor("#475569"), Color.parseColor("#64748B") ->
                            v.setTextColor(Color.parseColor("#94A3B8"))
                    }
                }
            }
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
        }
        walk(view)
    }

    private fun loadSavedFavorites() {
        val favSet = prefs.getStringSet(PREF_FAV_CHANNELS_SET, null)
        if (favSet != null) {
            for (ch in iptvChannelList) {
                ch.isFavorite = favSet.contains(ch.id)
            }
        }
    }

    private fun saveFavorites() {
        val favSet = iptvChannelList.filter { it.isFavorite }.map { it.id }.toSet()
        prefs.edit().putStringSet(PREF_FAV_CHANNELS_SET, favSet).apply()
        loadQuickChannels()
    }

    private fun loadQuickChannels() {
        quickChannelsRow.removeAllViews()
        val favs = iptvChannelList.filter { it.isFavorite }

        for (ch in favs) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_quick_channel, quickChannelsRow, false)
            stylePhoneDynamicCard(itemView)
            val nameTv = itemView.findViewById<TextView>(R.id.channelName)
            val logoIv = itemView.findViewById<ImageView>(R.id.channelLogo)

            nameTv.text = ch.name.replace(" HD", "")
            bindPhoneChannelLogo(logoIv, ch)

            itemView.setOnClickListener {
                playChannelStream(ch)
            }
            quickChannelsRow.addView(itemView)
        }

        // Plus Add Button at end of row
        val addView = LayoutInflater.from(this).inflate(R.layout.item_quick_channel, quickChannelsRow, false)
        stylePhoneDynamicCard(addView)
        addView.findViewById<TextView>(R.id.channelName).text = "+"
        addView.findViewById<ImageView>(R.id.channelLogo).setImageResource(R.drawable.ic_ch_add)
        addView.setOnClickListener {
            switchTab(TAB_IPTV)
        }
        quickChannelsRow.addView(addView)
    }

    private fun playChannelStream(ch: ChannelItem) {
        switchTab(TAB_IPTV)
        initIptvWebView()
        iptvWeb?.evaluateJavascript("if (typeof playChannelByUrl === 'function') playChannelByUrl('${ch.streamUrl}', '${ch.name}');", null)

        phonePlayerTitleText.text = ch.name
        phonePlayerSubtitleText.text = "IPTV • " + ch.category
        bindPhoneChannelLogo(phonePlayerThumbnail, ch)
        phonePlayerThumbnail.visibility = View.VISIBLE
        phonePlayerFallbackIcon.visibility = View.GONE
        btnPhonePlayerPlayPause.setImageResource(R.drawable.ic_player_pause)
        CarMediaManager.updateTrack(ch.name, "IPTV • " + ch.category, artworkRes = ch.iconRes)
        CarMediaManager.setPlaybackState(true)
        CarMediaManager.acquireWakeLock(this)
        Toast.makeText(this, "📺 Đang phát: ${ch.name}", Toast.LENGTH_SHORT).show()
    }

    // ==========================================
    // SCREEN 2: CHỌN HÌNH NỀN (WALLPAPER CHOOSER)
    // ==========================================

    private fun setupWallpaperChooserScreen() {
        findViewById<ImageView>(R.id.btnBackWallpaper)?.setOnClickListener {
            subviewWallpaper.visibility = View.GONE
        }

        // Filter Pills
        val btnDefault = findViewById<TextView>(R.id.btnWpCatDefault)
        val btnCars = findViewById<TextView>(R.id.btnWpCatCars)
        val btnAnime = findViewById<TextView>(R.id.btnWpCatAnime)
        val btnCustom = findViewById<TextView>(R.id.btnWpCatCustom)

        fun highlightPill(selected: TextView) {
            listOf(btnDefault, btnCars, btnAnime, btnCustom).forEach { pill ->
                pill.setBackgroundResource(if (pill == selected) R.drawable.bg_pill_blue else R.drawable.bg_pill_dark)
                pill.setTextColor(if (pill == selected) Color.WHITE else Color.parseColor("#CBD5E1"))
            }
        }

        btnDefault.setOnClickListener {
            highlightPill(btnDefault)
            selectWallpaper(CarDashboardView.WALLPAPER_BUGATTI, "Siêu xe Bugatti Red")
        }
        btnCars.setOnClickListener {
            highlightPill(btnCars)
            selectWallpaper(CarDashboardView.WALLPAPER_SILVER, "Siêu xe Bạc Porsche GT")
        }
        btnAnime.setOnClickListener {
            highlightPill(btnAnime)
            selectWallpaper(CarDashboardView.WALLPAPER_SCENIC, "Đồng cỏ Anime Scenic")
        }
        btnCustom.setOnClickListener {
            highlightPill(btnCustom)
            pickWallpaperLauncher.launch("*/*")
        }

        // Grid Cards
        findViewById<FrameLayout>(R.id.wpCardBugatti)?.setOnClickListener {
            selectWallpaper(CarDashboardView.WALLPAPER_BUGATTI, "Siêu xe Bugatti Red")
        }
        findViewById<FrameLayout>(R.id.wpCardSilver)?.setOnClickListener {
            selectWallpaper(CarDashboardView.WALLPAPER_SILVER, "Siêu xe Bạc Porsche GT")
        }
        findViewById<FrameLayout>(R.id.wpCardBlue)?.setOnClickListener {
            selectWallpaper(CarDashboardView.WALLPAPER_BLUE, "Siêu xe Xanh Alpine")
        }
        findViewById<FrameLayout>(R.id.wpCardCyber)?.setOnClickListener {
            selectWallpaper(CarDashboardView.WALLPAPER_CYBER, "Phố đêm Cyberpunk & Xe hơi")
        }
        findViewById<FrameLayout>(R.id.wpCardSunset)?.setOnClickListener {
            selectWallpaper(CarDashboardView.WALLPAPER_SUNSET, "Hoàng hôn Sunset Supercar")
        }
        findViewById<FrameLayout>(R.id.wpCardScenic)?.setOnClickListener {
            selectWallpaper(CarDashboardView.WALLPAPER_SCENIC, "Đồng cỏ Anime Scenic")
        }
        findViewById<FrameLayout>(R.id.wpCardDark)?.setOnClickListener {
            selectWallpaper(CarDashboardView.WALLPAPER_DARK, "Nền Ghi Đen Tối Giản")
        }
        findViewById<FrameLayout>(R.id.wpCardCustom)?.setOnClickListener {
            pickWallpaperLauncher.launch("*/*")
        }
    }

    private fun selectWallpaper(type: String, name: String) {
        prefs.edit().putString(CarDashboardView.PREF_WALLPAPER_TYPE, type).apply()
        updateWallpaperDisplay()
        updateWallpaperSettingsText()
        Toast.makeText(this, "✅ Đã chọn hình nền: $name", Toast.LENGTH_SHORT).show()
    }

    private fun updateWallpaperSelectionBadges(activeType: String) {
        wpBadgeBugatti?.visibility = if (activeType == CarDashboardView.WALLPAPER_BUGATTI) View.VISIBLE else View.GONE
        wpBadgeSilver?.visibility = if (activeType == CarDashboardView.WALLPAPER_SILVER) View.VISIBLE else View.GONE
        wpBadgeBlue?.visibility = if (activeType == CarDashboardView.WALLPAPER_BLUE) View.VISIBLE else View.GONE
        wpBadgeCyber?.visibility = if (activeType == CarDashboardView.WALLPAPER_CYBER) View.VISIBLE else View.GONE
        wpBadgeSunset?.visibility = if (activeType == CarDashboardView.WALLPAPER_SUNSET) View.VISIBLE else View.GONE
        wpBadgeScenic?.visibility = if (activeType == CarDashboardView.WALLPAPER_SCENIC) View.VISIBLE else View.GONE
        wpBadgeDark?.visibility = if (activeType == CarDashboardView.WALLPAPER_DARK) View.VISIBLE else View.GONE
        wpBadgeCustom?.visibility = if (activeType == CarDashboardView.WALLPAPER_CUSTOM) View.VISIBLE else View.GONE
    }

    private fun updateWallpaperDisplay() {
        try { phoneAnimatedWallpaper?.stop() } catch (_: Throwable) {}
        phoneAnimatedWallpaper = null

        val rawType = prefs.getString(CarDashboardView.PREF_WALLPAPER_TYPE, CarDashboardView.WALLPAPER_BUGATTI) ?: CarDashboardView.WALLPAPER_BUGATTI
        val type = if (rawType == "homer") CarDashboardView.WALLPAPER_BUGATTI else rawType
        when (type) {
            CarDashboardView.WALLPAPER_BUGATTI -> {
                phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_bugatti)
                phoneWallpaperView.visibility = View.VISIBLE
            }
            CarDashboardView.WALLPAPER_SILVER -> {
                phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_silver)
                phoneWallpaperView.visibility = View.VISIBLE
            }
            CarDashboardView.WALLPAPER_BLUE -> {
                phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_blue)
                phoneWallpaperView.visibility = View.VISIBLE
            }
            CarDashboardView.WALLPAPER_CYBER -> {
                phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_cyber)
                phoneWallpaperView.visibility = View.VISIBLE
            }
            CarDashboardView.WALLPAPER_SUNSET -> {
                phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_sunset)
                phoneWallpaperView.visibility = View.VISIBLE
            }
            CarDashboardView.WALLPAPER_SCENIC -> {
                phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_scenic)
                phoneWallpaperView.visibility = View.VISIBLE
            }
            CarDashboardView.WALLPAPER_DARK -> {
                phoneWallpaperView.setImageDrawable(null)
                phoneWallpaperView.setBackgroundResource(R.drawable.bg_cockpit_charcoal)
                phoneWallpaperView.visibility = View.GONE
            }
            CarDashboardView.WALLPAPER_CUSTOM -> {
                val path = prefs.getString(CarDashboardView.PREF_CUSTOM_WALLPAPER_PATH, null)
                val kind = prefs.getString(
                    CarDashboardView.PREF_CUSTOM_WALLPAPER_KIND,
                    CarDashboardView.WALLPAPER_KIND_IMAGE
                ) ?: CarDashboardView.WALLPAPER_KIND_IMAGE

                if (!path.isNullOrBlank() && File(path).exists()) {
                    try {
                        when (kind) {
                            CarDashboardView.WALLPAPER_KIND_GIF -> {
                                val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(File(path)))
                                phoneWallpaperView.setImageDrawable(drawable)
                                phoneWallpaperView.visibility = View.VISIBLE
                                if (drawable is AnimatedImageDrawable) {
                                    drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                                    phoneAnimatedWallpaper = drawable
                                    drawable.start()
                                }
                            }
                            CarDashboardView.WALLPAPER_KIND_VIDEO -> {
                                val retriever = MediaMetadataRetriever()
                                try {
                                    retriever.setDataSource(path)
                                    val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    if (frame != null) {
                                        phoneWallpaperView.setImageBitmap(frame)
                                        phoneWallpaperView.visibility = View.VISIBLE
                                    } else {
                                        phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_bugatti)
                                        phoneWallpaperView.visibility = View.VISIBLE
                                    }
                                } finally {
                                    try { retriever.release() } catch (_: Throwable) {}
                                }
                            }
                            else -> {
                                val bmp = BitmapFactory.decodeFile(path)
                                if (bmp != null) {
                                    phoneWallpaperView.setImageBitmap(bmp)
                                    phoneWallpaperView.visibility = View.VISIBLE
                                } else {
                                    phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_bugatti)
                                    phoneWallpaperView.visibility = View.VISIBLE
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_bugatti)
                        phoneWallpaperView.visibility = View.VISIBLE
                    }
                } else {
                    phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_bugatti)
                    phoneWallpaperView.visibility = View.VISIBLE
                }
            }
            else -> {
                phoneWallpaperView.setImageResource(R.drawable.bg_wallpaper_bugatti)
                phoneWallpaperView.visibility = View.VISIBLE
            }
        }
        updateWallpaperSelectionBadges(type)
    }

    // ==========================================
    // SCREEN 3: CÀI ĐẶT GIAO DIỆN & HIỂN THỊ
    // ==========================================

    private fun setupDisplaySettingsScreen() {
        findViewById<ImageView>(R.id.btnBackDisplaySettings)?.setOnClickListener {
            subviewDisplaySettings.visibility = View.GONE
        }

        val sHeroClock = findViewById<SwitchCompat>(R.id.switchShowHeroClock)
        val sSearchBar = findViewById<SwitchCompat>(R.id.switchShowSearchBar)
        val sCardYt = findViewById<SwitchCompat>(R.id.switchShowCardYoutube)
        val sCardIptv = findViewById<SwitchCompat>(R.id.switchShowCardIptv)
        val sCardM3u = findViewById<SwitchCompat>(R.id.switchShowCardM3u)
        val sCardBrowser = findViewById<SwitchCompat>(R.id.switchShowCardBrowser)
        val sFav = findViewById<SwitchCompat>(R.id.switchShowFavChannels)
        val sPlayer = findViewById<SwitchCompat>(R.id.switchShowMiniPlayer)
        val sNav = findViewById<SwitchCompat>(R.id.switchShowBottomNav)

        // Read current preferences
        sHeroClock.isChecked = prefs.getBoolean(PREF_SHOW_HERO_CLOCK, true)
        sSearchBar.isChecked = prefs.getBoolean(PREF_SHOW_SEARCH_BAR, true)
        sCardYt.isChecked = prefs.getBoolean(PREF_SHOW_CARD_YOUTUBE, true)
        sCardIptv.isChecked = prefs.getBoolean(PREF_SHOW_CARD_IPTV, true)
        sCardM3u.isChecked = prefs.getBoolean(PREF_SHOW_CARD_M3U, false)
        sCardBrowser.isChecked = prefs.getBoolean(PREF_SHOW_CARD_BROWSER, true)
        sFav.isChecked = prefs.getBoolean(PREF_SHOW_FAV_CHANNELS, false)
        sPlayer.isChecked = prefs.getBoolean(PREF_SHOW_MINI_PLAYER, true)
        sNav.isChecked = prefs.getBoolean(PREF_SHOW_BOTTOM_NAV, true)

        fun bindSwitch(sw: SwitchCompat, key: String, onToggle: (Boolean) -> Unit) {
            sw.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(key, isChecked).apply()
                onToggle(isChecked)
            }
        }

        bindSwitch(sHeroClock, PREF_SHOW_HERO_CLOCK) { cardPhoneCockpitPreview.visibility = if (it) View.VISIBLE else View.GONE }
        bindSwitch(sSearchBar, PREF_SHOW_SEARCH_BAR) { phoneSearchBar.visibility = if (it) View.VISIBLE else View.GONE }
        bindSwitch(sCardYt, PREF_SHOW_CARD_YOUTUBE) { phoneCardYoutube.visibility = if (it) View.VISIBLE else View.GONE }
        bindSwitch(sCardIptv, PREF_SHOW_CARD_IPTV) { phoneCardIptv.visibility = if (it) View.VISIBLE else View.GONE }
        bindSwitch(sCardM3u, PREF_SHOW_CARD_M3U) { phoneCardM3u.visibility = if (it) View.VISIBLE else View.GONE }
        bindSwitch(sCardBrowser, PREF_SHOW_CARD_BROWSER) { phoneCardBrowser.visibility = if (it) View.VISIBLE else View.GONE }
        bindSwitch(sFav, PREF_SHOW_FAV_CHANNELS) { sectionFavChannels.visibility = if (it) View.VISIBLE else View.GONE }
        bindSwitch(sPlayer, PREF_SHOW_MINI_PLAYER) {
            phoneMiniPlayer.visibility = if (it && activeTab == TAB_IPTV) View.VISIBLE else View.GONE
        }
        bindSwitch(sNav, PREF_SHOW_BOTTOM_NAV) {
            phoneBottomNav.visibility = if (it) View.VISIBLE else View.GONE
        }

        // Restore button
        findViewById<Button>(R.id.btnRestoreDefaultLayout)?.setOnClickListener {
            prefs.edit()
                .putBoolean(PREF_SHOW_HERO_CLOCK, true)
                .putBoolean(PREF_SHOW_SEARCH_BAR, true)
                .putBoolean(PREF_SHOW_CARD_YOUTUBE, true)
                .putBoolean(PREF_SHOW_CARD_IPTV, true)
                .putBoolean(PREF_SHOW_CARD_M3U, false)
                .putBoolean(PREF_SHOW_CARD_BROWSER, true)
                .putBoolean(PREF_SHOW_FAV_CHANNELS, false)
                .putBoolean(PREF_SHOW_MINI_PLAYER, true)
                .putBoolean(PREF_SHOW_BOTTOM_NAV, true)
                .apply()

            sHeroClock.isChecked = true
            sSearchBar.isChecked = true
            sCardYt.isChecked = true
            sCardIptv.isChecked = true
            sCardM3u.isChecked = false
            sCardBrowser.isChecked = true
            sFav.isChecked = false
            sPlayer.isChecked = true
            sNav.isChecked = true

            applyDisplaySettings()
            Toast.makeText(this, "✅ Đã khôi phục bố cục mặc định!", Toast.LENGTH_SHORT).show()
        }

        // Theme 3-way segment
        val btnDark = findViewById<TextView>(R.id.btnThemeDark)
        val btnLight = findViewById<TextView>(R.id.btnThemeLight)
        val btnAuto = findViewById<TextView>(R.id.btnThemeAuto)

        fun selectThemeSegment(target: TextView, mode: String, persist: Boolean = true) {
            listOfNotNull(btnDark, btnLight, btnAuto).forEach {
                it.setBackgroundResource(if (it == target) R.drawable.bg_segment_selected else 0)
                it.setTextColor(if (it == target) Color.WHITE else Color.parseColor("#94A3B8"))
                it.typeface = if (it == target) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            if (persist) {
                prefs.edit().putString(PREF_THEME_MODE, mode).apply()
                applyPhoneTheme()
                Toast.makeText(this, "Đã chọn chủ đề: ${target.text}", Toast.LENGTH_SHORT).show()
            }
        }

        val savedTheme = when (prefs.getString(PREF_THEME_MODE, SettingsActivity.THEME_AUTO)) {
            "dark" -> SettingsActivity.THEME_NIGHT
            "light" -> SettingsActivity.THEME_DAY
            SettingsActivity.THEME_DAY -> SettingsActivity.THEME_DAY
            SettingsActivity.THEME_NIGHT -> SettingsActivity.THEME_NIGHT
            else -> SettingsActivity.THEME_AUTO
        }
        if (prefs.getString(PREF_THEME_MODE, null) != savedTheme) {
            prefs.edit().putString(PREF_THEME_MODE, savedTheme).apply()
        }
        when (savedTheme) {
            SettingsActivity.THEME_NIGHT -> btnDark?.let { selectThemeSegment(it, savedTheme, false) }
            SettingsActivity.THEME_DAY -> btnLight?.let { selectThemeSegment(it, savedTheme, false) }
            else -> btnAuto?.let { selectThemeSegment(it, SettingsActivity.THEME_AUTO, false) }
        }

        btnDark?.setOnClickListener { selectThemeSegment(btnDark, SettingsActivity.THEME_NIGHT) }
        btnLight?.setOnClickListener { selectThemeSegment(btnLight, SettingsActivity.THEME_DAY) }
        btnAuto?.setOnClickListener { selectThemeSegment(btnAuto, SettingsActivity.THEME_AUTO) }

        // Accent Palette
        val paletteContainer = findViewById<LinearLayout>(R.id.accentPaletteContainer)
        val colors = listOf(
            "#00E5FF", "#EF4444", "#F97316", "#F59E0B",
            "#10B981", "#3B82F6", "#8B5CF6", "#EC4899", "#14B8A6"
        )
        paletteContainer?.removeAllViews()

        val savedColorHex = prefs.getString(PREF_ACCENT_COLOR, "#38BDF8") ?: "#38BDF8"
        activeAccentColor = Color.parseColor(savedColorHex)

        for (cHex in colors) {
            val cInt = Color.parseColor(cHex)
            val isSelected = (cHex.equals(savedColorHex, ignoreCase = true))

            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                    marginEnd = dp(10)
                }
                background = createCircleBg(cInt, if (isSelected) Color.WHITE else Color.TRANSPARENT, if (isSelected) 2 else 0)
                setOnClickListener {
                    activeAccentColor = cInt
                    prefs.edit().putString(PREF_ACCENT_COLOR, cHex).apply()
                    setupDisplaySettingsScreen()
                    findViewById<TextView>(R.id.phoneBrandTitle)?.setTextColor(cInt)
                    switchTab(activeTab)
                    Toast.makeText(this@MainActivity, "Đã đổi màu nhấn!", Toast.LENGTH_SHORT).show()
                }
            }
            paletteContainer?.addView(dot)
        }
    }

    private fun applyDisplaySettings() {
        cardPhoneCockpitPreview.visibility = if (prefs.getBoolean(PREF_SHOW_HERO_CLOCK, true)) View.VISIBLE else View.GONE
        phoneSearchBar.visibility = if (prefs.getBoolean(PREF_SHOW_SEARCH_BAR, true)) View.VISIBLE else View.GONE
        phoneCardYoutube.visibility = if (prefs.getBoolean(PREF_SHOW_CARD_YOUTUBE, true)) View.VISIBLE else View.GONE
        phoneCardIptv.visibility = if (prefs.getBoolean(PREF_SHOW_CARD_IPTV, true)) View.VISIBLE else View.GONE
        phoneCardM3u.visibility = if (prefs.getBoolean(PREF_SHOW_CARD_M3U, false)) View.VISIBLE else View.GONE
        phoneCardBrowser.visibility = if (prefs.getBoolean(PREF_SHOW_CARD_BROWSER, true)) View.VISIBLE else View.GONE
        sectionFavChannels.visibility = if (prefs.getBoolean(PREF_SHOW_FAV_CHANNELS, false)) View.VISIBLE else View.GONE
        phoneMiniPlayer.visibility = if (activeTab == TAB_IPTV && prefs.getBoolean(PREF_SHOW_MINI_PLAYER, true)) View.VISIBLE else View.GONE
        phoneBottomNav.visibility = if (prefs.getBoolean(PREF_SHOW_BOTTOM_NAV, true)) View.VISIBLE else View.GONE
    }

    // ==========================================
    // SCREEN 4: CẤP QUYỀN CẦN THIẾT
    // ==========================================

    private fun setupPermissionsScreen() {
        findViewById<ImageView>(R.id.btnBackPermissions)?.setOnClickListener {
            subviewPermissions.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnPermGrantLocation)?.setOnClickListener {
            requestLocationLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        findViewById<Button>(R.id.btnPermGrantMic)?.setOnClickListener {
            requestMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        findViewById<Button>(R.id.btnPermGrantNotification)?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Toast.makeText(this, "✅ Quyền thông báo chạy nền đã được bật sẵn!", Toast.LENGTH_SHORT).show()
                updatePermissionStatusDisplay()
            }
        }

        updatePermissionStatusDisplay()
    }

    private fun updatePermissionStatusDisplay() {
        val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        findViewById<Button>(R.id.btnPermGrantLocation)?.apply {
            text = if (hasLocation) "✓ Đã cấp quyền vị trí" else "Cấp quyền vị trí"
            isEnabled = !hasLocation
            alpha = if (hasLocation) 0.6f else 1.0f
        }

        findViewById<Button>(R.id.btnPermGrantMic)?.apply {
            text = if (hasMic) "✓ Đã cấp quyền ghi âm" else "Cấp quyền ghi âm"
            isEnabled = !hasMic
            alpha = if (hasMic) 0.6f else 1.0f
        }

        findViewById<Button>(R.id.btnPermGrantNotification)?.apply {
            text = if (hasNotif) "✓ Đã cấp quyền thông báo" else "Cấp quyền thông báo"
            isEnabled = !hasNotif
            alpha = if (hasNotif) 0.6f else 1.0f
        }
    }

    // ==========================================
    // SCREEN 5: TAB YOUTUBE (PHÁT NỀN)
    // ==========================================

    private fun setupYouTubeTab() {
        findViewById<ImageView>(R.id.btnBackYoutube)?.setOnClickListener {
            switchTab(TAB_HOME)
        }
        findViewById<ImageView>(R.id.btnSearchYoutube)?.setOnClickListener {
            showSearchDialog()
        }
        findViewById<TextView>(R.id.btnYoutubeAccount)?.setOnClickListener {
            val currentUrl = youtubeWeb?.url ?: ""
            if (currentUrl.contains("accounts.google.com") || currentUrl.contains("signin")) {
                youtubeWeb?.loadUrl("https://m.youtube.com")
            } else {
                youtubeWeb?.loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fm.youtube.com%2F")
            }
        }
    }

    private fun configurePhoneWebViewForApp(view: WebView, app: WebAppItem, targetUrl: String = app.url) {
        val defaultUa = android.webkit.WebSettings.getDefaultUserAgent(this)
        val cleanUa = defaultUa.replace("; wv", "").replace(Regex("Version/\\d+\\.\\d+\\s?"), "")
        if (app.id == "tiktok" || TikTokWebPolicy.isTikTok(targetUrl)) {
            TikTokWebPolicy.configure(view, targetUrl)
        } else if (WebAppManager.isAppDesktop(this, app.id) && app.id != "youtube") {
            view.settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        } else {
            view.settings.userAgentString = cleanUa
            view.settings.useWideViewPort = true
            view.settings.loadWithOverviewMode = app.id != "youtube"
        }
        view.setBackgroundColor(if (SettingsActivity.resolveIsDay(this)) Color.WHITE else Color.BLACK)
        YouTubePlayerHelper.applyUniversalWebTheme(view, SettingsActivity.resolveIsDay(this))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initYoutubeWebView() {
        if (youtubeWeb != null) return
        val w = findViewById<BackgroundAudioWebView>(R.id.phoneYoutubeWeb) ?: BackgroundAudioWebView(this).also {
            it.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            findViewById<FrameLayout>(R.id.youtubeContainer)?.addView(it)
        }
        youtubeWeb = w
        val bgAudio = prefs.getBoolean(SettingsActivity.KEY_BACKGROUND_AUDIO, true)
        w.enableBackgroundAudio = bgAudio

        w.isFocusable = true
        w.isFocusableInTouchMode = true
        w.requestFocus()
        w.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                if (!v.hasFocus()) v.requestFocus()
            } else if (event.action == android.view.MotionEvent.ACTION_UP) {
                v.performClick()
            }
            false
        }

        YouTubePlayerHelper.applyUltraPerformance(w)
        w.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            val cleanUa = android.webkit.WebSettings.getDefaultUserAgent(this@MainActivity)
                .replace("; wv", "")
                .replace(Regex("Version/\\d+\\.\\d+\\s?"), "")
            userAgentString = cleanUa
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
        }
        YouTubePlayerHelper.applyUniversalWebTheme(w, SettingsActivity.resolveIsDay(this))
        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(w, true)
        }
        w.webChromeClient = WebChromeClient()
        w.webViewClient = object : android.webkit.WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                val target = request?.url?.toString() ?: return false
                if (target == "intent:home") {
                    switchTab(TAB_HOME)
                    return true
                }
                if (TikTokWebPolicy.isAppRedirect(target)) {
                    return true
                }
                if (view != null && request.isForMainFrame && TikTokWebPolicy.isTikTok(target)) {
                    TikTokWebPolicy.configure(view, target)
                }
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                TikTokWebPolicy.applyRenderingPolicy(view, url)
                YouTubePlayerHelper.applyUniversalWebTheme(view, SettingsActivity.resolveIsDay(this@MainActivity))
                if (TikTokWebPolicy.isTikTok(url)) {
                    activePhoneWebAppId = "tiktok"
                    TikTokWebPolicy.configure(view, url)
                    TikTokWebPolicy.fitLayout(view, url)
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (view != null) {
                    YouTubePlayerHelper.applyUniversalWebTheme(view, SettingsActivity.resolveIsDay(this@MainActivity))
                    TikTokWebPolicy.fitLayout(view, url)
                }
            }

            override fun onPageCommitVisible(view: WebView, url: String) {
                super.onPageCommitVisible(view, url)
                YouTubePlayerHelper.applyUniversalWebTheme(view, SettingsActivity.resolveIsDay(this@MainActivity))
                TikTokWebPolicy.fitLayout(view, url)
            }

            override fun shouldInterceptRequest(view: WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                val url = request?.url?.toString()
                val isYoutube = url?.contains("youtube.com", true) == true || url?.contains("youtu.be", true) == true
                if (!isYoutube) return super.shouldInterceptRequest(view, request)
                if (url.contains("accounts.google.com") || url.contains("ssl.gstatic.com/accounts") || url.contains("myaccount.google.com")) {
                    return super.shouldInterceptRequest(view, request)
                }
                YouTubeAdBlocker.shouldIntercept(request)?.let { return it }
                if (YouTubePlayerHelper.isAdUrl(url)) {
                    val origin = request.requestHeaders?.get("Origin") ?: request.requestHeaders?.get("origin")
                    return YouTubePlayerHelper.createEmptyResponse(origin)
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (view.url != url) return
                val isYoutube = url.contains("youtube.com", true) || url.contains("youtu.be", true)
                if (isYoutube) {
                    YouTubeAdBlocker.onPageFinished(view, url)
                    YouTubePlayerHelper.trackVideoHistory(view)
                    activePhoneWebAppId = "youtube"
                }
                if (TikTokWebPolicy.isTikTok(url)) {
                    activePhoneWebAppId = "tiktok"
                    TikTokWebPolicy.fitLayout(view, url)
                    TikTokWebPolicy.recoverSearchIfBlank(view, url)
                }
                YouTubePlayerHelper.applyUniversalWebTheme(view, SettingsActivity.resolveIsDay(this@MainActivity))
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching { android.webkit.CookieManager.getInstance().flush() }
                }
            }
        }

        CarMediaManager.setupAndroidVoiceBridge(w, this, isAuto = false)
        CarMediaManager.registerPhoneWebView(w, this)
        val startUrl = pendingPhoneWebUrl ?: if (activePhoneWebAppId == "youtube") "https://m.youtube.com" else
            WebAppManager.getAllApps(this).find { it.id == activePhoneWebAppId }?.url ?: "https://m.youtube.com"
        val startApp = WebAppManager.getAllApps(this).find { it.id == activePhoneWebAppId }
            ?: WebAppManager.DEFAULT_APPS.first { it.id == "youtube" }
        configurePhoneWebViewForApp(w, startApp, startUrl)
        pendingPhoneWebUrl = null
        w.loadUrl(startUrl)
    }

    // ==========================================
    // SCREEN 6: TAB IPTV (DANH SÁCH KÊNH)
    // ==========================================

    private fun setupIptvTab() {
        findViewById<ImageView>(R.id.btnBackIptv)?.setOnClickListener {
            switchTab(TAB_HOME)
        }
        findViewById<ImageView>(R.id.btnSearchIptv)?.setOnClickListener {
            showSearchDialog()
        }
        findViewById<ImageView>(R.id.btnSettingsIptv)?.setOnClickListener {
            switchTab(TAB_SETTINGS)
        }

        // Category Pills
        val btnAll = findViewById<TextView>(R.id.btnIptvCatAll)
        val btnFav = findViewById<TextView>(R.id.btnIptvCatFav)
        val btnVtv = findViewById<TextView>(R.id.btnIptvCatVtv)
        val btnHtv = findViewById<TextView>(R.id.btnIptvCatHtv)
        val btnSport = findViewById<TextView>(R.id.btnIptvCatSport)

        fun highlightIptvPill(selected: TextView?, filterCat: String) {
            currentIptvFilter = filterCat
            listOfNotNull(btnAll, btnFav, btnVtv, btnHtv, btnSport).forEach { pill ->
                pill.setBackgroundResource(if (pill == selected) R.drawable.bg_pill_blue else R.drawable.bg_pill_dark)
                pill.setTextColor(if (pill == selected) Color.WHITE else Color.parseColor("#CBD5E1"))
            }
            renderIptvChannelsList(filterCat)
        }

        btnAll?.setOnClickListener { highlightIptvPill(btnAll, "all") }
        btnFav?.setOnClickListener { highlightIptvPill(btnFav, "fav") }
        btnVtv?.setOnClickListener { highlightIptvPill(btnVtv, "vtv") }
        btnHtv?.setOnClickListener { highlightIptvPill(btnHtv, "htv") }
        btnSport?.setOnClickListener { highlightIptvPill(btnSport, "sport") }

        highlightIptvPill(btnAll, "all")
    }

    private fun renderIptvChannelsList(filterCategory: String?) {
        val container = findViewById<LinearLayout>(R.id.iptvChannelsContainer) ?: return
        container.removeAllViews()

        val list = when (filterCategory) {
            "fav" -> iptvChannelList.filter { it.isFavorite }
            "vtv" -> iptvChannelList.filter {
                it.category.contains("vtv", ignoreCase = true) ||
                it.name.contains("vtv", ignoreCase = true) ||
                it.id.contains("vtv", ignoreCase = true)
            }
            "htv" -> iptvChannelList.filter {
                it.category.contains("htv", ignoreCase = true) ||
                it.name.contains("htv", ignoreCase = true) ||
                it.id.contains("htv", ignoreCase = true)
            }
            "sport" -> iptvChannelList.filter {
                it.category.contains("thể thao", ignoreCase = true) ||
                it.category.contains("sport", ignoreCase = true) ||
                it.name.contains("sport", ignoreCase = true) ||
                it.name.contains("k+", ignoreCase = true)
            }
            else -> iptvChannelList
        }

        if (list.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = if (filterCategory == "fav") {
                    "Chưa có kênh yêu thích nào.\nHãy nhấn ⭐ trên các kênh bạn muốn để lưu lại!"
                } else {
                    "Không tìm thấy kênh nào."
                }
                setTextColor(if (SettingsActivity.resolveIsDay(this@MainActivity)) Color.parseColor("#64748B") else Color.parseColor("#94A3B8"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(32), dp(16), dp(32))
            }
            container.addView(emptyTv)
            return
        }

        for (ch in list) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_iptv_channel, container, false)
            stylePhoneDynamicCard(itemView)
            val logoIv = itemView.findViewById<ImageView>(R.id.iptvItemLogo)
            val titleTv = itemView.findViewById<TextView>(R.id.iptvItemTitle)
            val catTv = itemView.findViewById<TextView>(R.id.iptvItemCategory)
            val starIv = itemView.findViewById<ImageView>(R.id.iptvItemStar)

            bindPhoneChannelLogo(logoIv, ch)
            titleTv.text = ch.name
            catTv.text = ch.category
            starIv.setImageResource(if (ch.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)

            itemView.setOnClickListener {
                playChannelStream(ch)
            }

            starIv.setOnClickListener {
                ch.isFavorite = !ch.isFavorite
                starIv.setImageResource(if (ch.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
                saveFavorites()
                Toast.makeText(this, if (ch.isFavorite) "⭐ Đã thêm ${ch.name} vào Yêu thích" else "Đã bỏ ${ch.name} khỏi Yêu thích", Toast.LENGTH_SHORT).show()
                if (currentIptvFilter == "fav") {
                    renderIptvChannelsList("fav")
                }
            }

            container.addView(itemView)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initIptvWebView() {
        if (iptvWeb != null) return
        val w = findViewById<BackgroundAudioWebView>(R.id.phoneIptvWeb) ?: BackgroundAudioWebView(this)
        iptvWeb = w
        IptvAspectRatio.attach(w)
        val bgAudio = prefs.getBoolean(SettingsActivity.KEY_BACKGROUND_AUDIO, true)
        w.enableBackgroundAudio = bgAudio

        w.isFocusable = true
        w.isFocusableInTouchMode = true
        w.requestFocus()
        w.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                if (!v.hasFocus()) {
                    v.requestFocus()
                }
            } else if (event.action == android.view.MotionEvent.ACTION_UP) {
                v.performClick()
            }
            false
        }

        // Keep IPTV on the same accelerated WebView path as YouTube/browser.
        YouTubePlayerHelper.applyUltraPerformance(w)

        w.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
        }
        w.webChromeClient = WebChromeClient()
        w.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                YouTubePlayerHelper.applyUniversalWebTheme(view, SettingsActivity.resolveIsDay(this@MainActivity))
            }
        }
        YouTubePlayerHelper.applyUniversalWebTheme(w, SettingsActivity.resolveIsDay(this))

        CarMediaManager.setupAndroidVoiceBridge(w, this, isAuto = false)
        CarMediaManager.registerPhoneWebView(w, this)

        w.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun getChannelsJson(): String {
                return IptvManager.getCachedJson(this@MainActivity)
            }
            @JavascriptInterface
            fun getM3uUrl(): String {
                return IptvManager.getM3uUrl(this@MainActivity)
            }
            @JavascriptInterface
            fun isDayTheme(): Boolean {
                return SettingsActivity.resolveIsDay(this@MainActivity)
            }
            @JavascriptInterface
            fun reloadChannels() {
                mainHandler.post {
                    IptvManager.refresh(this@MainActivity) {
                        reloadIptvChannels()
                    }
                }
            }
        }, "AndroidIptv")

        w.loadUrl("file:///android_asset/iptv_player.html")
    }

    private fun reloadIptvChannels() {
        iptvWeb?.evaluateJavascript("if (typeof reloadChannelList === 'function') reloadChannelList();", null)
    }

    // ==========================================
    // MINI MEDIA PLAYER
    // ==========================================

    private fun setupMiniPlayer() {
        phoneMiniPlayer.setOnClickListener {
            val currentUrl = CarMediaManager.currentLoadingUrl.orEmpty()
            if (currentUrl.contains("iptv_player.html")) {
                switchTab(TAB_IPTV)
            } else {
                val wasUninitialized = youtubeWeb == null
                switchTab(TAB_YOUTUBE)
                if (wasUninitialized && (currentUrl.contains("youtube.com/watch", true) || currentUrl.contains("youtu.be/", true))) {
                    val resumeUrl = if (CarMediaManager.currentPositionSec > 0 && currentUrl.contains("youtube.com/watch", true)) {
                        val sep = if (currentUrl.contains("?")) "&" else "?"
                        currentUrl.substringBefore("&t=").substringBefore("?t=") + sep + "t=${CarMediaManager.currentPositionSec}s"
                    } else currentUrl
                    youtubeWeb?.loadUrl(resumeUrl)
                }
            }
        }

        btnPhonePlayerPlayPause.setOnClickListener {
            CarMediaManager.togglePlayPause()
        }

        btnPhonePlayerPrev.setOnClickListener {
            CarMediaManager.playPrevious()
        }

        btnPhonePlayerNext.setOnClickListener {
            CarMediaManager.playNext()
        }
    }

    private fun startMediaObserver() {
        lifecycleScope.launch {
            CarMediaManager.trackState.collectLatest { track ->
                val title = if (track.title.isNotBlank()) track.title else "YouTube Music"
                val subtitle = if (track.artist.isNotBlank()) track.artist else "TCar Auto"

                if (phonePlayerTitleText.text?.toString() != title) {
                    phonePlayerTitleText.text = title
                }
                if (phonePlayerSubtitleText.text?.toString() != subtitle) {
                    phonePlayerSubtitleText.text = subtitle
                }

                if (phonePlayerProgressBar.progress != track.progressPercent) {
                    phonePlayerProgressBar.progress = track.progressPercent
                }

                btnPhonePlayerPlayPause.setImageResource(
                    if (track.isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play
                )

                if (track.artworkBitmap != null) {
                    phonePlayerThumbnail.setImageBitmap(track.artworkBitmap)
                    phonePlayerThumbnail.visibility = View.VISIBLE
                    phonePlayerFallbackIcon.visibility = View.GONE
                } else if (track.artworkRes != 0) {
                    phonePlayerThumbnail.setImageResource(track.artworkRes)
                    phonePlayerThumbnail.visibility = View.VISIBLE
                    phonePlayerFallbackIcon.visibility = View.GONE
                }
            }
        }
    }

    // ==========================================
    // TAB 4: SETTINGS HUB
    // ==========================================

    private fun setupSettingsTab() {
        findViewById<TextView>(R.id.phoneAppVersionText)?.visibility = View.GONE
        updateUserSettingsText()
        updateWallpaperSettingsText()
        updateIptvStatusText()

        // 1. Wallpaper (Screen 2 entry)
        findViewById<LinearLayout>(R.id.settingCardWallpaper)?.setOnClickListener {
            showSubview(subviewWallpaper)
        }

        // 2. Display / Interface Settings (Screen 3 entry)
        findViewById<LinearLayout>(R.id.settingCardDisplay)?.setOnClickListener {
            showSubview(subviewDisplaySettings)
        }

        // 3. Permissions (Screen 4 entry)
        findViewById<LinearLayout>(R.id.settingCardPermissions)?.setOnClickListener {
            showSubview(subviewPermissions)
        }

        // 4. User Name dialog
        findViewById<LinearLayout>(R.id.settingCardUser)?.setOnClickListener {
            val curName = prefs.getString(CarDashboardView.PREF_USER_NAME, CarDashboardView.DEFAULT_USER_NAME) ?: CarDashboardView.DEFAULT_USER_NAME
            val input = EditText(this).apply {
                setText(curName)
                hint = "Nhập tên bạn (ví dụ: Phạm Nam)"
                setSingleLine(true)
                setPadding(40, 30, 40, 30)
            }
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("👤 Đổi tên hiển thị lời chào")
                .setMessage("Tên này sẽ hiển thị trong câu chào trên màn hình xe và điện thoại:")
                .setView(input)
                .setPositiveButton("Lưu") { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotBlank()) {
                        prefs.edit().putString(CarDashboardView.PREF_USER_NAME, newName).apply()
                        updateUserSettingsText()
                        updateGreetingText()
                        Toast.makeText(this, "Đã lưu tên mới: $newName", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // 5. M3U Actions
        findViewById<Button>(R.id.btnPickM3uFile)?.setOnClickListener {
            pickM3uFileLauncher.launch("*/*")
        }

        findViewById<Button>(R.id.btnInputM3uUrl)?.setOnClickListener {
            val curUrl = IptvManager.getM3uUrl(this)
            val input = EditText(this).apply {
                setText(curUrl)
                hint = "Nhập URL danh sách kênh M3U..."
                setSingleLine(true)
                setPadding(40, 30, 40, 30)
            }
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("🔗 Nhập link playlist M3U trực tuyến")
                .setMessage("Dán đường link file .m3u từ GitHub, Pastebin hoặc máy chủ:")
                .setView(input)
                .setPositiveButton("Tải danh sách") { _, _ ->
                    val newUrl = input.text.toString().trim()
                    if (newUrl.isNotBlank()) {
                        IptvManager.setM3uUrl(this, newUrl)
                        IptvManager.refresh(this) { list ->
                            Toast.makeText(this, "✅ Đã tải ${list.size} kênh từ link mới!", Toast.LENGTH_SHORT).show()
                            updateIptvStatusText()
                            reloadIptvChannels()
                            loadIptvChannelsFromM3u()
                        }
                    }
                }
                .setNeutralButton("Mặc định") { _, _ ->
                    IptvManager.setM3uUrl(this, IptvManager.DEFAULT_IPTV_URL)
                    IptvManager.refresh(this) {
                        Toast.makeText(this, "Đã khôi phục danh sách kênh mặc định!", Toast.LENGTH_SHORT).show()
                        updateIptvStatusText()
                        reloadIptvChannels()
                        loadIptvChannelsFromM3u()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // The entire Advanced Settings card is actionable, not just its
        // internal button. Play builds must open Settings without activation.
        val openAdvancedSettings = View.OnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Không mở được Cài đặt nâng cao: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        findViewById<LinearLayout>(R.id.settingCardAdvanced)?.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener(openAdvancedSettings)
        }
        findViewById<Button>(R.id.btnOpenAdvancedSettings)?.setOnClickListener(openAdvancedSettings)
    }

    private fun updateUserSettingsText() {
        val curName = prefs.getString(CarDashboardView.PREF_USER_NAME, CarDashboardView.DEFAULT_USER_NAME) ?: CarDashboardView.DEFAULT_USER_NAME
        settingUserSubtext.text = "Tên hiện tại: $curName (Nhấn để đổi tên)"
    }

    private fun updateWallpaperSettingsText() {
        val wpType = prefs.getString(CarDashboardView.PREF_WALLPAPER_TYPE, CarDashboardView.WALLPAPER_BUGATTI) ?: CarDashboardView.WALLPAPER_BUGATTI
        val name = when (wpType) {
            CarDashboardView.WALLPAPER_BUGATTI -> "Siêu xe Bugatti Red (Mặc định)"
            CarDashboardView.WALLPAPER_SILVER -> "Siêu xe Bạc Porsche GT"
            CarDashboardView.WALLPAPER_BLUE -> "Siêu xe Xanh Alpine"
            CarDashboardView.WALLPAPER_CYBER -> "Phố đêm Cyberpunk & Xe hơi"
            CarDashboardView.WALLPAPER_SUNSET -> "Hoàng hôn Sunset Supercar"
            CarDashboardView.WALLPAPER_SCENIC -> "Đồng cỏ Anime Scenic"
            CarDashboardView.WALLPAPER_DARK -> "Nền Ghi Đen Tối Giản"
            CarDashboardView.WALLPAPER_CUSTOM -> {
                when (prefs.getString(CarDashboardView.PREF_CUSTOM_WALLPAPER_KIND, CarDashboardView.WALLPAPER_KIND_IMAGE)) {
                    CarDashboardView.WALLPAPER_KIND_VIDEO -> "Video động tùy chỉnh (loop, tắt tiếng)"
                    CarDashboardView.WALLPAPER_KIND_GIF -> "GIF động tùy chỉnh"
                    else -> "Ảnh tùy chỉnh từ máy"
                }
            }
            else -> "Siêu xe Bugatti Red"
        }
        settingWallpaperSubtext.text = "Đang dùng: $name (Chạm để đổi ảnh / GIF / video)"
    }

    private fun updateIptvStatusText() {
        val isLocal = IptvManager.isUsingLocalFile(this)
        val localName = IptvManager.getLocalFileName(this)
        settingIptvSubtext.text = if (isLocal && !localName.isNullOrBlank()) {
            "Nguồn kênh: File nội bộ ($localName)"
        } else {
            val url = IptvManager.getM3uUrl(this)
            if (url == IptvManager.DEFAULT_IPTV_URL) "Nguồn kênh: Gói mặc định (VTV, HTV, Thể thao)" else "Nguồn kênh: $url"
        }
    }

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = "Nhập tên video YouTube hoặc kênh TV..."
            setSingleLine(true)
            setPadding(40, 30, 40, 30)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("🔍 Tìm kiếm nội dung")
            .setView(input)
            .setPositiveButton("Tìm YouTube") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotBlank()) {
                    switchTab(TAB_YOUTUBE)
                    initYoutubeWebView()
                    val searchUrl = "https://m.youtube.com/results?search_query=" + Uri.encode(query)
                    youtubeWeb?.loadUrl(searchUrl)
                }
            }
            .setNeutralButton("Tìm Kênh TV") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotBlank()) {
                    switchTab(TAB_IPTV)
                    initIptvWebView()
                    iptvWeb?.evaluateJavascript("if (typeof filterByQuery === 'function') filterByQuery('${query.replace("'", "\\'")}');", null)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun createCardBg(bgColor: Int, cornerRadiusDp: Float, strokeColor: Int = Color.TRANSPARENT, strokeWidthDp: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusDp * resources.displayMetrics.density
            setColor(bgColor)
            if (strokeWidthDp > 0) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
    }

    private fun createCircleBg(bgColor: Int, strokeColor: Int = Color.TRANSPARENT, strokeWidthDp: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(bgColor)
            if (strokeWidthDp > 0) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
    }

    private fun showHudStyleChooserDialog() {
        val styles = WazeHudManager.STYLES
        val curId = WazeHudManager.getActiveStyleId(this)

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#090E17"))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))

            val icon = TextView(this@MainActivity).apply {
                text = "🧭"
                textSize = 20f
                setPadding(0, 0, dp(8), 0)
            }
            addView(icon)

            val titleCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                val titleTv = TextView(this@MainActivity).apply {
                    text = "MẪU GIAO DIỆN CẢNH BÁO HUD"
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                }
                addView(titleTv)

                val subTv = TextView(this@MainActivity).apply {
                    text = "Chạm vào mẫu hình ảnh để áp dụng ngay lên xe"
                    textSize = 11f
                    setTextColor(Color.parseColor("#94A3B8"))
                }
                addView(subTv)
            }
            addView(titleCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        dialogLayout.addView(header)

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val cardsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        var activeDialog: AlertDialog? = null

        for (style in styles) {
            val isSel = (style.id == curId)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                val cardBg = if (isSel) Color.parseColor("#132235") else Color.parseColor("#0F1724")
                val strokeColor = if (isSel) Color.parseColor("#00E5FF") else Color.parseColor("#1E2D40")
                background = createCardBg(cardBg, 14f, strokeColor, if (isSel) 2 else 1)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(10)
                }
                isClickable = true
                isFocusable = true

                val previewBox = FrameLayout(this@MainActivity).apply {
                    background = createCardBg(Color.parseColor("#070B12"), 10f, Color.parseColor("#15202E"), 1)
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
                        bottomMargin = dp(8)
                    }

                    when (style.id) {
                        1 -> {
                            val bubble = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER
                                background = createCardBg(Color.parseColor("#1A2332"), 16f, Color.parseColor("#00E5FF"), 1)
                                setPadding(dp(8), dp(4), dp(10), dp(4))
                                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                    gravity = Gravity.CENTER
                                }

                                val sign = TextView(context).apply {
                                    text = "60"
                                    textSize = 10f
                                    typeface = Typeface.DEFAULT_BOLD
                                    setTextColor(Color.BLACK)
                                    gravity = Gravity.CENTER
                                    background = createCircleBg(Color.WHITE, Color.parseColor("#EF4444"), 2)
                                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                                        marginEnd = dp(6)
                                    }
                                }
                                addView(sign)

                                val speedTv = TextView(context).apply {
                                    text = "68"
                                    textSize = 15f
                                    typeface = Typeface.DEFAULT_BOLD
                                    setTextColor(Color.parseColor("#00E5FF"))
                                }
                                addView(speedTv)

                                val kmhTv = TextView(context).apply {
                                    text = " km/h"
                                    textSize = 8.5f
                                    setTextColor(Color.parseColor("#94A3B8"))
                                }
                                addView(kmhTv)

                                val lockIcon = TextView(context).apply {
                                    text = " 🔒"
                                    textSize = 9f
                                    setPadding(dp(4), 0, 0, 0)
                                }
                                addView(lockIcon)
                            }
                            addView(bubble)
                        }
                        2 -> {
                            val bubble = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER
                                background = createCardBg(Color.parseColor("#1A2332"), 16f, Color.parseColor("#38BDF8"), 1)
                                setPadding(dp(8), dp(4), dp(8), dp(4))
                                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                    gravity = Gravity.CENTER
                                }

                                val sign = TextView(context).apply {
                                    text = "60"
                                    textSize = 10f
                                    typeface = Typeface.DEFAULT_BOLD
                                    setTextColor(Color.BLACK)
                                    gravity = Gravity.CENTER
                                    background = createCircleBg(Color.WHITE, Color.parseColor("#EF4444"), 2)
                                    layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                                        marginEnd = dp(6)
                                    }
                                }
                                addView(sign)

                                val speedTv = TextView(context).apply {
                                    text = "68"
                                    textSize = 14f
                                    typeface = Typeface.DEFAULT_BOLD
                                    setTextColor(Color.WHITE)
                                }
                                addView(speedTv)

                                val camBadge = TextView(context).apply {
                                    text = "📸 350m"
                                    textSize = 8.5f
                                    typeface = Typeface.DEFAULT_BOLD
                                    setTextColor(Color.parseColor("#FDE047"))
                                    background = createCardBg(Color.parseColor("#352A0A"), 6f, Color.parseColor("#CA8A04"), 1)
                                    setPadding(dp(5), dp(2), dp(5), dp(2))
                                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                        marginStart = dp(5)
                                    }
                                }
                                addView(camBadge)

                                val roadBadge = TextView(context).apply {
                                    text = "QL 1A"
                                    textSize = 8.5f
                                    setTextColor(Color.parseColor("#38BDF8"))
                                    background = createCardBg(Color.parseColor("#0C2A44"), 6f, Color.parseColor("#0284C7"), 1)
                                    setPadding(dp(5), dp(2), dp(5), dp(2))
                                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                        marginStart = dp(4)
                                    }
                                }
                                addView(roadBadge)
                            }
                            addView(bubble)
                        }
                        3 -> {
                            val dockRow = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER_VERTICAL
                                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                                val pill = LinearLayout(context).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    gravity = Gravity.CENTER_VERTICAL
                                    background = createCardBg(Color.parseColor("#152233"), 12f, Color.parseColor("#38BDF8"), 1)
                                    setPadding(dp(6), dp(2), dp(6), dp(2))

                                    val speed = TextView(context).apply {
                                        text = "68"
                                        textSize = 12f
                                        typeface = Typeface.DEFAULT_BOLD
                                        setTextColor(Color.parseColor("#00E5FF"))
                                    }
                                    addView(speed)

                                    val sign = TextView(context).apply {
                                        text = "60"
                                        textSize = 8f
                                        typeface = Typeface.DEFAULT_BOLD
                                        setTextColor(Color.BLACK)
                                        gravity = Gravity.CENTER
                                        background = createCircleBg(Color.WHITE, Color.parseColor("#EF4444"), 1)
                                        layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                                            marginStart = dp(4)
                                        }
                                    }
                                    addView(sign)

                                    val cam = TextView(context).apply {
                                        text = " 📷 350m"
                                        textSize = 8f
                                        setTextColor(Color.parseColor("#FDE047"))
                                    }
                                    addView(cam)
                                }
                                addView(pill)

                                val label = TextView(context).apply {
                                    text = "   ← Cột dọc mép trái buồng lái"
                                    textSize = 10f
                                    setTextColor(Color.parseColor("#64748B"))
                                }
                                addView(label)
                            }
                            addView(dockRow)
                        }
                        4 -> {
                            val dockRow = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                                val label = TextView(context).apply {
                                    text = "Cột dọc mép phải buồng lái →   "
                                    textSize = 10f
                                    setTextColor(Color.parseColor("#64748B"))
                                }
                                addView(label)

                                val pill = LinearLayout(context).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    gravity = Gravity.CENTER_VERTICAL
                                    background = createCardBg(Color.parseColor("#152233"), 12f, Color.parseColor("#38BDF8"), 1)
                                    setPadding(dp(6), dp(2), dp(6), dp(2))

                                    val sign = TextView(context).apply {
                                        text = "60"
                                        textSize = 8f
                                        typeface = Typeface.DEFAULT_BOLD
                                        setTextColor(Color.BLACK)
                                        gravity = Gravity.CENTER
                                        background = createCircleBg(Color.WHITE, Color.parseColor("#EF4444"), 1)
                                        layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                                            marginEnd = dp(4)
                                        }
                                    }
                                    addView(sign)

                                    val speed = TextView(context).apply {
                                        text = "68"
                                        textSize = 12f
                                        typeface = Typeface.DEFAULT_BOLD
                                        setTextColor(Color.parseColor("#00E5FF"))
                                    }
                                    addView(speed)

                                    val cam = TextView(context).apply {
                                        text = " 📷 350m"
                                        textSize = 8f
                                        setTextColor(Color.parseColor("#FDE047"))
                                    }
                                    addView(cam)
                                }
                                addView(pill)
                            }
                            addView(dockRow)
                        }
                        else -> {
                            val bubble = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER
                                background = createCardBg(Color.parseColor("#141D2A"), 14f, Color.parseColor("#38BDF8"), 1)
                                setPadding(dp(8), dp(4), dp(8), dp(4))
                                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                    gravity = Gravity.CENTER
                                }

                                val sign = TextView(context).apply {
                                    text = "60"
                                    textSize = 10f
                                    typeface = Typeface.DEFAULT_BOLD
                                    setTextColor(Color.BLACK)
                                    gravity = Gravity.CENTER
                                    background = createCircleBg(Color.WHITE, Color.parseColor("#EF4444"), 2)
                                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                                        marginEnd = dp(6)
                                    }
                                }
                                addView(sign)

                                val speedTv = TextView(context).apply {
                                    text = "68 KM/H"
                                    textSize = 12.5f
                                    typeface = Typeface.DEFAULT_BOLD
                                    setTextColor(Color.WHITE)
                                }
                                addView(speedTv)

                                val lockIcon = TextView(context).apply {
                                    text = " 🔒"
                                    textSize = 9f
                                    setPadding(dp(4), 0, 0, 0)
                                }
                                addView(lockIcon)
                            }
                            addView(bubble)
                        }
                    }
                }
                addView(previewBox)

                val infoRow = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL

                    val titleCol = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL

                        val nameTv = TextView(this@MainActivity).apply {
                            text = style.name
                            textSize = 12.5f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(if (isSel) Color.parseColor("#00E5FF") else Color.WHITE)
                        }
                        addView(nameTv)

                        val descTv = TextView(this@MainActivity).apply {
                            text = style.description
                            textSize = 10.5f
                            setTextColor(Color.parseColor("#94A3B8"))
                        }
                        addView(descTv)
                    }
                    addView(titleCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

                    val statusBadge = TextView(this@MainActivity).apply {
                        text = if (isSel) "✓ ĐANG CHỌN" else "CHỌN"
                        textSize = 10.5f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(if (isSel) Color.parseColor("#00E5FF") else Color.parseColor("#64748B"))
                        val badgeBg = if (isSel) Color.parseColor("#18354A") else Color.parseColor("#15202E")
                        val badgeStroke = if (isSel) Color.parseColor("#00E5FF") else Color.parseColor("#26384C")
                        background = createCardBg(badgeBg, 6f, badgeStroke, 1)
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                    }
                    addView(statusBadge)
                }
                addView(infoRow)

                setOnClickListener {
                    WazeHudManager.setActiveStyleId(this@MainActivity, style.id)
                    activeDialog?.dismiss()
                    Toast.makeText(this@MainActivity, "Đã chọn: ${style.name}", Toast.LENGTH_SHORT).show()
                }
            }
            cardsContainer.addView(card)
        }

        scroll.addView(cardsContainer)
        dialogLayout.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(380)))

        val closeBtn = Button(this).apply {
            text = "Đóng"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = createCardBg(Color.parseColor("#1E293B"), 10f, Color.parseColor("#334155"), 1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)).apply {
                topMargin = dp(12)
            }
            setOnClickListener {
                activeDialog?.dismiss()
            }
        }
        dialogLayout.addView(closeBtn)

        activeDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(dialogLayout)
            .create()
        activeDialog.show()
    }

    private fun testWazeAlertDemo() {
        val sample = VietmapAlertData(
            isConnected = true,
            currentSpeed = 68,
            speedLimit = 60,
            warningType = VietmapWarningType.SPEED_CAMERA,
            alertTitle = "Camera phạt nguội tốc độ 60 km/h",
            distanceMeters = 350,
            roadName = "Đoạn đường có giám sát camera",
            isOverSpeed = true,
            source = "DEMO_TEST"
        )
        VietmapStateRepository.updateState(sample)
        CarTtsManager.speakTestWarning(this, VietmapWarningType.SPEED_CAMERA, isOverspeed = true)
        Toast.makeText(this, "🧪 Đang phát cảnh báo mẫu: Camera phạt nguội 60 km/h (Vượt tốc)", Toast.LENGTH_LONG).show()

        mainHandler.postDelayed({
            VietmapStateRepository.updateState(VietmapAlertData(
                isConnected = true,
                currentSpeed = 0,
                speedLimit = 60,
                warningType = VietmapWarningType.NONE,
                source = "IDLE"
            ))
        }, 6000L)
    }

    // ==========================================
    // TICKER & LIVE UPDATES
    // ==========================================

    private fun startClockTicker() {
        val runnable = object : Runnable {
            override fun run() {
                val cal = Calendar.getInstance()
                val now = cal.time

                val clockStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                if (phoneClockText.text.toString() != clockStr) phoneClockText.text = clockStr
                val solarStr = VietnameseLunarHelper.getFormattedSolarDate(cal)
                if (phoneDateText.text.toString() != solarStr) phoneDateText.text = solarStr
                val lunarStr = VietnameseLunarHelper.getFormattedLunarWithCanChi(cal)
                if (phoneLunarDateText.text.toString() != lunarStr) phoneLunarDateText.text = lunarStr
                updateGreetingText()

                mainHandler.postDelayed(this, 1000L)
            }
        }
        mainHandler.post(runnable)
    }

    private fun updateGreetingText() {
        val curName = prefs.getString(CarDashboardView.PREF_USER_NAME, CarDashboardView.DEFAULT_USER_NAME) ?: CarDashboardView.DEFAULT_USER_NAME
        val greeting = VietnameseLunarHelper.getGreeting(Calendar.getInstance(), curName)
        if (phoneGreetingText.text.toString() != greeting) phoneGreetingText.text = greeting
    }

    override fun onBackPressed() {
        if (subviewWallpaper.visibility == View.VISIBLE) {
            subviewWallpaper.visibility = View.GONE
        } else if (subviewDisplaySettings.visibility == View.VISIBLE) {
            subviewDisplaySettings.visibility = View.GONE
        } else if (subviewPermissions.visibility == View.VISIBLE) {
            subviewPermissions.visibility = View.GONE
        } else if (activeTab == TAB_YOUTUBE && youtubeWeb?.canGoBack() == true) {
            youtubeWeb?.goBack()
        } else if (activeTab != TAB_HOME) {
            switchTab(TAB_HOME)
        } else {
            val bgAudio = prefs.getBoolean(SettingsActivity.KEY_BACKGROUND_AUDIO, true)
            if (bgAudio) {
                moveTaskToBack(true)
            } else {
                super.onBackPressed()
            }
        }
    }

    private var appliedSettingsImport = 0L

    override fun onResume() {
        super.onResume()
        if (BuildConfig.PLAY_DISTRIBUTION) playUpdater.onResume()
        if (!BuildConfig.PLAY_DISTRIBUTION && !LicenseManager.isLicensed(this)) {
            val intent = Intent(this, ActivationActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        youtubeWeb?.onResume()
        iptvWeb?.onResume()
        youtubeWeb?.resumeTimers()
        iptvWeb?.resumeTimers()
        applyPhoneTheme()
        setupPhoneAppGrid()
        updatePermissionStatusDisplay()
        val imported = prefs.getLong(SettingsBackupManager.IMPORT_REVISION, 0L)
        if (imported != appliedSettingsImport) {
            appliedSettingsImport = imported
            applyDisplaySettings()
            updateWallpaperDisplay()
            loadIptvChannelsFromM3u()
        }
    }

    override fun onPause() {
        super.onPause()
        val bgAudio = prefs.getBoolean(SettingsActivity.KEY_BACKGROUND_AUDIO, true)
        if (bgAudio) {
            youtubeWeb?.onResume()
            iptvWeb?.onResume()
            youtubeWeb?.resumeTimers()
            iptvWeb?.resumeTimers()
            if (CarMediaManager.isPlaying || CarMediaManager.userWantsPlayback) {
                CarMediaManager.acquireWakeLock(this)
                ensureBackgroundPlaybackActive()
            } else {
                CarMediaManager.releaseWakeLock()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val bgAudio = prefs.getBoolean(SettingsActivity.KEY_BACKGROUND_AUDIO, true)
        if (bgAudio) {
            youtubeWeb?.onResume()
            iptvWeb?.onResume()
            youtubeWeb?.resumeTimers()
            iptvWeb?.resumeTimers()
            if (CarMediaManager.isPlaying || CarMediaManager.userWantsPlayback) {
                CarMediaManager.acquireWakeLock(this)
                ensureBackgroundPlaybackActive()
            } else {
                CarMediaManager.releaseWakeLock()
            }
        }
    }

    private fun ensureBackgroundPlaybackActive() {
        try {
            if (CarMediaManager.isPlaying || CarMediaManager.userWantsPlayback) {
                youtubeWeb?.evaluateJavascript(
                    """
                    (function() {
                        try {
                            var v = document.querySelector('#movie_player video, .html5-video-player video, video');
                            if (v && v.paused && !window.__carhudUserPaused) {
                                var p = v.play();
                                if (p && typeof p.catch === 'function') p.catch(function() {});
                            }
                        } catch(e) {}
                    })();
                    """.trimIndent(),
                    null
                )
                iptvWeb?.evaluateJavascript(
                    """
                    (function() {
                        try {
                            var v = document.querySelector('video');
                            if (v && v.paused && !window.userPaused) {
                                var p = v.play();
                                if (p && typeof p.catch === 'function') p.catch(function() {});
                            }
                        } catch(e) {}
                    })();
                    """.trimIndent(),
                    null
                )
            }
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        if (BuildConfig.PLAY_DISTRIBUTION) playUpdater.dispose()
        try {
            fullExitReceiver?.let { unregisterReceiver(it) }
            fullExitReceiver = null
        } catch (_: Throwable) {}

        if (CarMediaManager.mainActivityRoot == findViewById<ViewGroup>(android.R.id.content)) {
            CarMediaManager.mainActivityRoot = null
        }
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
