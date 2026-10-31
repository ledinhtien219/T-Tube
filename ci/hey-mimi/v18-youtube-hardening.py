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

# Match the actual direct-card parent, including YouTube mobile wrappers.
once(
    "                syncRecommendationPane(pip || hiddenPip);",
    "                syncRecommendationPane(pip || hiddenPip);\n"
    "                syncCarhudFourByFour(hiddenPip);",
    "refresh-native-4x4",
)
once(
    "        // In some car touch stacks the native WebView scroll lands on the",
    """        // Use the existing native card nodes: keep their YouTube click handlers.
        var carhudGridList = null;
        function syncCarhudFourByFour(enabled) {
            if (!enabled || !carhudRecommendationRoot) {
                if (carhudGridList) carhudGridList.removeAttribute('data-carhud-grid-list');
                carhudGridList = null;
                return;
            }
            if (carhudGridList && carhudGridList.isConnected &&
                carhudRecommendationRoot.contains(carhudGridList)) return;
            if (carhudGridList) carhudGridList.removeAttribute('data-carhud-grid-list');
            carhudGridList = null;
            var root = carhudRecommendationRoot;
            var cards = root.querySelectorAll(
                'ytd-compact-video-renderer,ytm-compact-video-renderer,' +
                'ytm-video-with-context-renderer,ytd-rich-item-renderer,' +
                'ytm-rich-item-renderer,.compact-media-item,.yt-lockup-view-model'
            );
            var parents = new Map();
            for (var i = 0; i < cards.length; i++) {
                var parent = cards[i].parentElement;
                if (!parent || !root.contains(parent)) continue;
                parents.set(parent, (parents.get(parent) || 0) + 1);
            }
            var chosen = null, max = 1;
            parents.forEach(function(count, parent) {
                if (count > max) { chosen = parent; max = count; }
            });
            if (!chosen) return;
            chosen.setAttribute('data-carhud-grid-list', '1');
            carhudGridList = chosen;
        }

        // In some car touch stacks the native WebView scroll lands on the""",
    "native-4x4-parent",
)
grid_css = (
    "        // Four columns and four visible rows, with additional rows swipable.\n"
    "        'html.carhud-youtube-pip-hidden [data-carhud-grid-list=\"1\"] { display:grid!important;grid-template-columns:repeat(4,minmax(0,1fr))!important;grid-auto-rows:minmax(98px,calc((100dvh - 58px)/4))!important;gap:6px!important;width:100%!important;max-width:100%!important;min-width:0!important;overflow:visible!important; } ' +\n"
    "        'html.carhud-youtube-pip-hidden [data-carhud-grid-list=\"1\"] > * { display:flex!important;flex-direction:column!important;min-width:0!important;max-width:100%!important;height:100%!important;max-height:100%!important;margin:0!important;padding:0!important;overflow:hidden!important; } ' +\n"
    "        'html.carhud-youtube-pip-hidden [data-carhud-grid-list=\"1\"] > * img { max-width:100%!important;object-fit:cover!important; } ' +\n"
    "        'html.carhud-youtube-pip-hidden [data-carhud-rec-scroll=\"1\"] { overflow-y:auto!important;overflow-x:hidden!important;touch-action:pan-y!important;overscroll-behavior-y:contain!important; } ' +\n"
)
marker = "        'html.carhud-youtube-pip [data-carhud-rec-scroll=\"1\"]::-webkit-scrollbar { width:7px!important; } ' +"
once(marker, grid_css + marker, "native-4x4-css")
once(
    "            if (!window.__carhudPipMode || !pane || !e.touches ||",
    "            if (!(window.__carhudPipMode || window.__carhudPipHidden) || !pane || !e.touches ||",
    "hidden-pip-gesture",
)
once(
    "            if (!target || pane.contains(target) ||",
    "            if (!target || (!window.__carhudPipHidden && pane.contains(target)) ||",
    "allow-swipes-over-cards",
)
once(
    "                (target.closest && target.closest(\n                    'input,textarea,button,a,[role=\"slider\"],[contenteditable=\"true\"]'\n                ))) return;",
    "                (target.closest && target.closest(\n                    window.__carhudPipHidden\n                    ? 'input,textarea,button,[role=\"slider\"],[contenteditable=\"true\"]'\n                    : 'input,textarea,button,a,[role=\"slider\"],[contenteditable=\"true\"]'\n                ))) return;",
    "touch-swipes-on-video-card-links",
)
once(
    "                e.touches.length !== 1 || !window.__carhudPipMode) return;",
    "                e.touches.length !== 1 || !(window.__carhudPipMode || window.__carhudPipHidden)) return;",
    "hidden-pip-swipe-move",
)
# Avoid compositing every element into GPU layers on budget car head units.
once(
    "        'lazy-list, ytm-rich-grid-renderer, .page-container, ytm-app, body { transform: translateZ(0) !important; -webkit-transform: translateZ(0) !important; backface-visibility: hidden !important; } ' +",
    "        // Do not force global GPU layers for the entire YouTube document.",
    "global-gpu-heat",
)


# Keep YouTube's actual Skip Ad control visible and accessible. Hiding parent
# ad containers makes their descendants unclickable even if the button has a
# high z-index. Only decorative ad chrome may be hidden.
once(
    "        '.video-ads, .ytp-ad-module, .ytp-ad-player-overlay-layout, .ytp-ad-image-overlay, .ytp-ad-text-overlay,",
    "        '.ytp-ad-image-overlay, .ytp-ad-text-overlay,",
    "preserve-native-skip-parent-containers",
)
once(
    "                    '.videoAdUiSkipButton',\n                    '.ytp-ad-skip-button-text'",
    "                    '.videoAdUiSkipButton',\n"
    "                    '.ytp-skip-ad-button__button',\n"
    "                    '.ytp-ad-skip-button-container button',\n"
    "                    '.ytp-ad-skip-button-modern button',\n"
    "                    '.ytp-ad-skip-button-text'",
    "modern-skip-button-variants",
)

assert "video.playbackRate = 16.0" not in s
assert "__carhudHidePip" in s
assert "grid-auto-rows:minmax(102px" in s
# Avoid offscreen GPU tile prerendering in the YouTube WebView.
if "offscreenPreRaster = true" in s:
    s = s.replace("offscreenPreRaster = true", "offscreenPreRaster = false")
p.write_text(s, encoding="utf-8")
print("v18 YouTube hardening applied after v17 patch chain")
