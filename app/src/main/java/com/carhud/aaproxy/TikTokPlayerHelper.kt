package com.carhud.aaproxy

import android.graphics.Color
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView

/**
 * Lightweight TikTok adaptation for the Android Auto projection surface.
 *
 * TikTok's desktop site normally lays out a narrow feed with large navigation
 * chrome. On a car display that wastes most of the viewport and makes projected
 * gesture scrolling feel disconnected. This helper keeps TikTok itself as the
 * player, but normalizes the feed to one video per projected viewport.
 */
object TikTokPlayerHelper {

    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    fun configure(view: WebView) {
        try {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            view.setBackgroundColor(Color.BLACK)
            view.settings.javaScriptEnabled = true
            view.settings.domStorageEnabled = true
            view.settings.databaseEnabled = true
            view.settings.mediaPlaybackRequiresUserGesture = false
            view.settings.setSupportMultipleWindows(false)
            view.settings.useWideViewPort = true
            view.settings.loadWithOverviewMode = false
            view.settings.textZoom = 100
            view.settings.userAgentString = DESKTOP_UA
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
        } catch (_: Throwable) {}
    }

    fun install(view: WebView?) {
        view ?: return
        view.post {
            try {
                view.evaluateJavascript(JS_FULLSCREEN_FEED, null)
            } catch (_: Throwable) {}
        }
    }

    /**
     * GestureDetector distanceY semantics: positive means the finger moved up,
     * so the feed should move forward/down by the same positive amount.
     */
    fun scrollBy(view: WebView?, distanceY: Float) {
        view ?: return
        val dy = (distanceY * 1.7f).coerceIn(-900f, 900f)
        try {
            view.evaluateJavascript(
                "if(window.__thtvTikTok&&window.__thtvTikTok.scrollBy){window.__thtvTikTok.scrollBy($dy);}",
                null
            )
        } catch (_: Throwable) {}
    }

    /**
     * Snap to the next/previous card at the end of a fling. velocityY is
     * negative for an upward finger fling, hence -velocityY maps to feed motion.
     */
    fun fling(view: WebView?, velocityY: Float) {
        view ?: return
        val feedVelocity = (-velocityY).coerceIn(-12000f, 12000f)
        try {
            view.evaluateJavascript(
                "if(window.__thtvTikTok&&window.__thtvTikTok.fling){window.__thtvTikTok.fling($feedVelocity);}",
                null
            )
        } catch (_: Throwable) {}
    }

    private val JS_FULLSCREEN_FEED = """
        (function() {
            try {
                var host = (location.hostname || '').toLowerCase();
                if (host !== 'tiktok.com' && host.indexOf('.tiktok.com') < 0) return false;

                var state = window.__thtvTikTok || {};
                window.__thtvTikTok = state;

                var viewport = document.querySelector('meta[name="viewport"]');
                if (!viewport) {
                    viewport = document.createElement('meta');
                    viewport.name = 'viewport';
                    (document.head || document.documentElement).appendChild(viewport);
                }
                viewport.content = 'width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover';

                var style = document.getElementById('thtv-tiktok-fullscreen-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'thtv-tiktok-fullscreen-style';
                    (document.head || document.documentElement).appendChild(style);
                }
                style.textContent = [
                    'html,body{margin:0!important;padding:0!important;width:100vw!important;max-width:100vw!important;min-height:100vh!important;background:#000!important;overscroll-behavior-y:contain!important;scrollbar-width:none!important;}',
                    'html::-webkit-scrollbar,body::-webkit-scrollbar,*::-webkit-scrollbar{width:0!important;height:0!important;display:none!important;}',
                    'header,footer,aside,[data-e2e="top-nav"],[data-e2e="left-nav"],[data-e2e="nav-search"],[data-e2e="download-app"],[data-e2e="open-app"],[class*="DivHeaderContainer"],[class*="DivSideNavContainer"],[class*="DivFooterContainer"],[class*="DivDownloadContainer"],[class*="DivCookieBanner"]{display:none!important;}',
                    '[class*="DivMainContainer"],[class*="DivBodyContainer"],[class*="DivContentContainer"],main{margin:0!important;padding:0!important;width:100vw!important;max-width:100vw!important;}',
                    '.__thtv_tiktok_item{box-sizing:border-box!important;width:100vw!important;max-width:100vw!important;height:100vh!important;min-height:100vh!important;margin:0!important;padding:0!important;overflow:hidden!important;background:#000!important;scroll-snap-align:start!important;scroll-snap-stop:always!important;contain:layout paint!important;}',
                    '.__thtv_tiktok_video_wrap{box-sizing:border-box!important;width:100%!important;max-width:100%!important;height:100%!important;max-height:100%!important;margin:0!important;padding:0!important;background:#000!important;display:flex!important;align-items:center!important;justify-content:center!important;}',
                    'video.__thtv_tiktok_video{box-sizing:border-box!important;width:100%!important;max-width:none!important;height:100%!important;max-height:none!important;margin:0!important;padding:0!important;object-fit:contain!important;background:#000!important;transform:translateZ(0)!important;-webkit-transform:translateZ(0)!important;backface-visibility:hidden!important;-webkit-backface-visibility:hidden!important;}',
                    '[data-e2e="browse-video"],[data-e2e="feed-video"],[data-e2e="video-player"]{max-width:100vw!important;}'
                ].join('');

                function videos() {
                    return Array.prototype.slice.call(document.querySelectorAll('video'));
                }

                function itemFor(video) {
                    if (!video) return null;
                    var exact = video.closest(
                        '[data-e2e="recommend-list-item-container"],' +
                        '[data-e2e="feed-item"],' +
                        '[data-e2e="browse-video"],' +
                        'article'
                    );
                    if (exact) return exact;

                    var node = video.parentElement;
                    var best = node;
                    var limit = 0;
                    while (node && node !== document.body && limit++ < 7) {
                        var rect = node.getBoundingClientRect();
                        if (rect.height >= window.innerHeight * 0.55 &&
                            rect.height <= window.innerHeight * 1.8) {
                            best = node;
                        }
                        node = node.parentElement;
                    }
                    return best;
                }

                function decorate(video) {
                    if (!video) return;
                    video.classList.add('__thtv_tiktok_video');
                    video.setAttribute('playsinline', '');
                    video.setAttribute('webkit-playsinline', '');
                    video.playsInline = true;

                    var item = itemFor(video);
                    if (item) {
                        item.classList.add('__thtv_tiktok_item');
                        var node = video.parentElement;
                        var hops = 0;
                        while (node && node !== item && hops++ < 3) {
                            node.classList.add('__thtv_tiktok_video_wrap');
                            node = node.parentElement;
                        }
                    }
                }

                function visibleVideo() {
                    var list = videos();
                    var best = null;
                    var bestArea = 0;
                    var vw = window.innerWidth || document.documentElement.clientWidth || 1;
                    var vh = window.innerHeight || document.documentElement.clientHeight || 1;
                    for (var i = 0; i < list.length; i++) {
                        var r = list[i].getBoundingClientRect();
                        var w = Math.max(0, Math.min(vw, r.right) - Math.max(0, r.left));
                        var h = Math.max(0, Math.min(vh, r.bottom) - Math.max(0, r.top));
                        var area = w * h;
                        if (area > bestArea) {
                            bestArea = area;
                            best = list[i];
                        }
                    }
                    return best;
                }

                function findScroller() {
                    var current = visibleVideo();
                    var node = itemFor(current);
                    var hops = 0;
                    while (node && node !== document.body && hops++ < 9) {
                        try {
                            var css = getComputedStyle(node);
                            if ((css.overflowY === 'auto' || css.overflowY === 'scroll') &&
                                node.scrollHeight > node.clientHeight + 32) {
                                return node;
                            }
                        } catch (e) {}
                        node = node.parentElement;
                    }

                    var known = [
                        document.querySelector('[data-e2e="recommend-list"]'),
                        document.querySelector('[class*="DivVideoFeedContainer"]'),
                        document.querySelector('[class*="DivFeedContainer"]')
                    ];
                    for (var i = 0; i < known.length; i++) {
                        if (known[i] && known[i].scrollHeight > known[i].clientHeight + 32) {
                            return known[i];
                        }
                    }
                    return document.scrollingElement || document.documentElement;
                }

                function prepareScroller() {
                    var s = findScroller();
                    if (!s) return null;
                    try {
                        s.style.setProperty('scroll-snap-type', 'y mandatory', 'important');
                        s.style.setProperty('overscroll-behavior-y', 'contain', 'important');
                        s.style.setProperty('-webkit-overflow-scrolling', 'touch', 'important');
                        s.style.setProperty('scrollbar-width', 'none', 'important');
                    } catch (e) {}
                    state.scroller = s;
                    return s;
                }

                function managePlayback() {
                    var current = visibleVideo();
                    if (!current) return;
                    if (state.current !== current) {
                        state.current = current;
                        var list = videos();
                        for (var i = 0; i < list.length; i++) {
                            var v = list[i];
                            if (v !== current) {
                                try {
                                    var r = v.getBoundingClientRect();
                                    if (r.bottom < -window.innerHeight * 0.25 ||
                                        r.top > window.innerHeight * 1.25) {
                                        if (!v.paused) v.pause();
                                    }
                                } catch (e) {}
                            }
                        }
                        try {
                            var p = current.play();
                            if (p && typeof p.catch === 'function') p.catch(function() {});
                        } catch (e) {}
                    }
                }

                function scan() {
                    var list = videos();
                    for (var i = 0; i < list.length; i++) decorate(list[i]);
                    prepareScroller();
                    managePlayback();
                }

                state.scrollBy = function(dy) {
                    try {
                        var s = prepareScroller();
                        if (!s) return;
                        var amount = Number(dy) || 0;
                        if (s === document.documentElement || s === document.body ||
                            s === document.scrollingElement) {
                            window.scrollBy(0, amount);
                        } else {
                            s.scrollTop += amount;
                        }
                        if (!state.raf) {
                            state.raf = requestAnimationFrame(function() {
                                state.raf = 0;
                                managePlayback();
                            });
                        }
                    } catch (e) {}
                };

                state.fling = function(feedVelocity) {
                    try {
                        var s = prepareScroller();
                        if (!s) return;
                        var direction = Number(feedVelocity) >= 0 ? 1 : -1;
                        var amount = Math.max(320, (window.innerHeight || 480) * 0.94) * direction;
                        if (s === document.documentElement || s === document.body ||
                            s === document.scrollingElement) {
                            window.scrollBy({top: amount, left: 0, behavior: 'smooth'});
                        } else if (typeof s.scrollBy === 'function') {
                            s.scrollBy({top: amount, left: 0, behavior: 'smooth'});
                        } else {
                            s.scrollTop += amount;
                        }
                        setTimeout(managePlayback, 280);
                    } catch (e) {}
                };

                if (!state.bound) {
                    state.bound = true;
                    document.addEventListener('play', function(event) {
                        var target = event.target;
                        if (!target || target.tagName !== 'VIDEO') return;
                        state.current = target;
                        var list = videos();
                        for (var i = 0; i < list.length; i++) {
                            if (list[i] !== target) {
                                try {
                                    var r = list[i].getBoundingClientRect();
                                    if (r.bottom < 0 || r.top > window.innerHeight) {
                                        list[i].pause();
                                    }
                                } catch (e) {}
                            }
                        }
                    }, true);

                    window.addEventListener('resize', function() {
                        clearTimeout(state.resizeTimer);
                        state.resizeTimer = setTimeout(scan, 100);
                    }, {passive:true});
                }

                scan();
                clearInterval(state.scanTimer);
                state.scanTimer = setInterval(scan, 1400);
                return true;
            } catch (e) {
                return false;
            }
        })();
    """.trimIndent()
}
