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
                    if…28193 tokens truncated…)
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

                phoneClockText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                phoneDateText.text = VietnameseLunarHelper.getFormattedSolarDate(cal)
                phoneLunarDateText.text = VietnameseLunarHelper.getFormattedLunarWithCanChi(cal)
                updateGreetingText()

                mainHandler.postDelayed(this, 1000L)
            }
        }
        mainHandler.post(runnable)
    }

    private fun updateGreetingText() {
        val curName = prefs.getString(CarDashboardView.PREF_USER_NAME, CarDashboardView.DEFAULT_USER_NAME) ?: CarDashboardView.DEFAULT_USER_NAME
        phoneGreetingText.text = VietnameseLunarHelper.getGreeting(Calendar.getInstance(), curName)
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
        if (!LicenseManager.isLicensed(this)) {
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
