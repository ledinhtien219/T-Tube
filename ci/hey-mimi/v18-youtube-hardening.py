#!/usr/bin/env python3
"""Apply v1.1.20 YouTube hardening AFTER the full Hey Mimi v17 patch chain.

Strict assertions deliberately fail CI if an upstream patch changes these
anchors. Never silently ship an APK with missing PiP or audio fixes.
"""
from pathlib import Path

p = Path("app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt")
s = p.read_text(encoding="utf-8")

def once(old: str, new: str, label: str):
    global s
    count = s.count(old)
    if count != 1:
        raise AssertionError(f"{label}: expected exactly 1 anchor, found {count}: {old[:110]!r}")
    s = s.replace(old, new, 1)

# Preserve the native ad pipeline. Forcing 16x speed and muting ad content is
# known to interfere with transition back into normal video/audio.
ad_start = "                // 4. For video ads: accelerate at 16x speed and mute during ad playback"
ad_end = "                // 5. Global touch listener to show toolbars on touch"
if ad_start in s:
    start = s.index(ad_start)
    end = s.index(ad_end, start)
    s = s[:start] + (
        "                // Let YouTube control mandatory ad timing and its audio state.\n"
        "                // Do not mute the shared HTML5 video or alter playbackRate.\n\n"
    ) + s[end:]
else:
    assert "video.playbackRate = 16.0" not in s, "Ad speedup still present"
once(
    "                        if (v.playbackRate !== 1.0) v.playbackRate = 1.0;",
    "                        // Do not reset native playback speed every second.",
    "audio-rate-watchdog",
)

# Keep the real video element and playback session. Hide only its host surface.
# Show 4 viewport rows (and extra rows scroll) rather than reserving the PiP corner.
grid = (
    "        // Four visible rows of native video recommendations when PiP is hidden.\n"
    "        'html.carhud-youtube-pip-hidden [data-carhud-pip-host=\"1\"] { opacity:0!important;pointer-events:none!important; } ' +\n"
    "        'html.carhud-youtube-pip-hidden [data-carhud-rec-scroll=\"1\"] { padding:6px 8px 20px!important;width:100vw!important; } ' +\n"
    "        'html.carhud-youtube-pip-hidden [data-carhud-rec-scroll=\"1\"] ytd-watch-next-secondary-results-renderer #items, html.carhud-youtube-pip-hidden [data-carhud-rec-scroll=\"1\"] #related #items, html.carhud-youtube-pip-hidden [data-carhud-rec-scroll=\"1\"] ytm-item-section-renderer lazy-list { grid-template-columns:repeat(4,minmax(0,1fr))!important;grid-auto-rows:minmax(102px,calc((100dvh - 54px)/4))!important;column-gap:7px!important;row-gap:7px!important;align-items:stretch!important; } ' +\n"
    "        'html.carhud-youtube-pip-hidden [data-carhud-rec-scroll=\"1\"] ytd-thumbnail, html.carhud-youtube-pip-hidden [data-carhud-rec-scroll=\"1\"] a#thumbnail { aspect-ratio:auto!important;height:clamp(58px,calc((100dvh - 54px)/4 - 40px),145px)!important; } ' +\n"
    "        'html.carhud-youtube-pip-hidden [data-carhud-rec-scroll=\"1\"] h3, html.carhud-youtube-pip-hidden [data-carhud-rec-scroll=\"1\"] .video-title { font-size:11px!important;line-height:1.2!important;max-height:2.4em!important;overflow:hidden!important; } ' +\n"
)
marker = "        'html.carhud-youtube-pip [data-carhud-rec-scroll=\"1\"]::-webkit-scrollbar { width:7px!important; } ' +"
once(marker, grid + marker, "hidden-grid-css")

once(
    "                var browsing = watch && (!!window.__carhudBrowseMode || pip);",
    "                var hiddenPip = watch && !!window.__carhudPipHidden;\n"
    "                var browsing = watch && (!!window.__carhudBrowseMode || pip || hiddenPip);",
    "layout-hidden-flag",
)
once(
    "                if (!pip && window.__carhudMiniPlayerHost) unpinMiniPlayerHost();",
    "                if (!pip && !hiddenPip && window.__carhudMiniPlayerHost) unpinMiniPlayerHost();",
    "retain-player-host",
)
once(
    "                html.classList.toggle('carhud-youtube-pip', pip);",
    "                html.classList.toggle('carhud-youtube-pip', pip || hiddenPip);\n"
    "                html.classList.toggle('carhud-youtube-pip-hidden', hiddenPip);",
    "layout-html-class",
)
once(
    "                if (body) body.classList.toggle('carhud-youtube-pip', pip);",
    "                if (body) {\n"
    "                    body.classList.toggle('carhud-youtube-pip', pip || hiddenPip);\n"
    "                    body.classList.toggle('carhud-youtube-pip-hidden', hiddenPip);\n"
    "                }",
    "layout-body-class",
)
once("                if (pip) pinMiniPlayerHost();",
     "                if (pip || hiddenPip) pinMiniPlayerHost();", "keep-player-host")
once("                syncRecommendationPane(pip);",
     "                syncRecommendationPane(pip || hiddenPip);", "grid-native-scroll")

once(
    "        window.__carhudSetPipMode = function(enabled) {",
    """        // Hide PiP for native full-screen recommendations without unloading video.
        window.__carhudHidePip = function() {
            if (location.pathname.indexOf('/watch') !== 0) return;
            window.__carhudPipHidden = true;
            window.__carhudPipMode = false;
            window.__carhudBrowseMode = true;
            try {
                sessionStorage.setItem('carhud_youtube_pip_mode', '0');
                sessionStorage.setItem('carhud_youtube_browse_mode', '1');
            } catch(e) {}
            updateWatchLayout();
        };
        window.__carhudSetPipMode = function(enabled) {""",
    "hide-pip-action",
)
once(
    "            window.__carhudBrowseMode = newMode;",
    "            // Toolbar toggling always exits the hidden state.\n"
    "            window.__carhudPipHidden = false;\n"
    "            window.__carhudBrowseMode = newMode;",
    "toggle-exits-hidden",
)
once(
    "    fun setBrowseMode(view: WebView?, enabled: Boolean) {",
    """    /** Hide the PiP surface and keep WebView/video playing while browsing. */
    fun hideMiniPlayer(view: WebView?) {
        try {
            view?.evaluateJavascript(
                "(function(){if(window.__carhudHidePip)window.__carhudHidePip();})();",
                null
            )
        } catch (_: Exception) {}
    }

    fun setBrowseMode(view: WebView?, enabled: Boolean) {""",
    "android-hide-pip-bridge",
)

# Selecting a recommendation must not trap the next video in hidden mode.
once(
    "        function handleWatchPage() {",
    """        // Selecting a new video from the grid returns to the normal player.
        document.addEventListener('click', function(e) {
            if (!window.__carhudPipHidden || !e.target || !e.target.closest) return;
            var link = e.target.closest('a[href*="/watch"]');
            if (!link || !carhudRecommendationRoot ||
                !carhudRecommendationRoot.contains(link)) return;
            window.__carhudPipHidden = false;
            window.__carhudBrowseMode = false;
        }, true);

        function handleWatchPage() {""",
    "grid-selection-reopens-player",
)
assert "video.playbackRate = 16.0" not in s
assert "__carhudHidePip" in s
assert "grid-auto-rows:minmax(102px" in s
p.write_text(s, encoding="utf-8")
print("v18 YouTube hardening applied after v17 patch chain")
