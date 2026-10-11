package com.carhud.aaproxy

import android.graphics.Color
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * TikTok tablet mode for the projected car surface.
 *
 * Keep TikTok's own responsive layout intact instead of rewriting the feed into
 * artificial full-screen cards. Chromium's real UA is retained, with only the
 * "Mobile" marker removed so TikTok can choose its tablet layout. This is much
 * closer to browsing TikTok on a large tablet and avoids the aggressive DOM
 * automation that made the previous layout unstable.
 */
object TikTokPlayerHelper {

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

            // Stay consistent with the actual Chromium engine. Removing only the
            // phone "Mobile" marker gives TikTok a tablet-sized responsive layout
            // without pretending WebView is Safari or a different browser engine.
            val baseUa = WebSettings.getDefaultUserAgent(view.context)
            val tabletUa = baseUa
                .replace("; wv", "")
                .replace(" Mobile ", " ")
                .replace(" Mobile", "")
                .replace(Regex("\\s+"), " ")
                .trim()
            view.settings.userAgentString = tabletUa

            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(view, true)
                flush()
            }
        } catch (_: Throwable) {
        }
    }

    fun install(view: WebView?) {
        view ?: return
        view.post {
            try {
                view.evaluateJavascript(JS_TABLET_LAYOUT, null)
            } catch (_: Throwable) {
            }
        }
    }

    fun scrollBy(view: WebView?, distanceY: Float) {
        view ?: return
        val dy = (distanceY * 1.45f).coerceIn(-720f, 720f)
        try {
            view.evaluateJavascript(
                "if(window.__thtvTikTokTablet&&window.__thtvTikTokTablet.scrollBy){window.__thtvTikTokTablet.scrollBy($dy);}",
                null
            )
        } catch (_: Throwable) {
        }
    }

    fun fling(view: WebView?, velocityY: Float) {
        view ?: return
        val feedVelocity = (-velocityY).coerceIn(-10000f, 10000f)
        try {
            view.evaluateJavascript(
                "if(window.__thtvTikTokTablet&&window.__thtvTikTokTablet.fling){window.__thtvTikTokTablet.fling($feedVelocity);}",
                null
            )
        } catch (_: Throwable) {
        }
    }

    private val JS_TABLET_LAYOUT = """
        (function() {
            try {
                var host = (location.hostname || '').toLowerCase();
                if (host !== 'tiktok.com' && host.indexOf('.tiktok.com') < 0) return false;

                // Never alter verification/challenge UI. If TikTok requires a
                // verification step, leave the page exactly as delivered.
                var challengeUrl = /captcha|verify|verification|challenge/i.test(location.href);
                var challengeDom = document.querySelector(
                    'iframe[src*="captcha"],iframe[src*="verify"],' +
                    '[id*="captcha"],[class*="captcha"],' +
                    '[data-e2e*="captcha"],[data-e2e*="verify"]'
                );
                if (challengeUrl || challengeDom) {
                    var oldStyle = document.getElementById('thtv-tiktok-tablet-style');
                    if (oldStyle) oldStyle.remove();
                    return false;
                }

                var viewport = document.querySelector('meta[name="viewport"]');
                if (!viewport) {
                    viewport = document.createElement('meta');
                    viewport.name = 'viewport';
                    (document.head || document.documentElement).appendChild(viewport);
                }
                viewport.content =
                    'width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover';

                var style = document.getElementById('thtv-tiktok-tablet-style');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'thtv-tiktok-tablet-style';
                    (document.head || document.documentElement).appendChild(style);
                }

                // Minimal tablet polish only. Do not hide TikTok navigation,
                // captions, creator controls or rewrite its internal feed tree.
                style.textContent = [
                    'html,body{margin:0!important;background:#000!important;overscroll-behavior-y:contain!important;}',
                    'body{width:100%!important;max-width:100%!important;}',
                    '[data-e2e="download-app"],[data-e2e="open-app"],' +
                    '[class*="DivDownloadContainer"]{display:none!important;}',
                    'video{max-width:100%!important;max-height:calc(100vh - 8px)!important;object-fit:contain!important;background:#000!important;}',
                    '[data-e2e="recommend-list-item-container"],[data-e2e="feed-item"]{' +
                    'scroll-snap-align:center!important;scroll-snap-stop:normal!important;}'
                ].join('');

                var api = window.__thtvTikTokTablet || {};
                window.__thtvTikTokTablet = api;

                function findScroller() {
                    var known = [
                        document.querySelector('[data-e2e="recommend-list"]'),
                        document.querySelector('[class*="DivVideoFeedContainer"]'),
                        document.querySelector('[class*="DivFeedContainer"]'),
                        document.querySelector('main')
                    ];
                    for (var i = 0; i < known.length; i++) {
                        var el = known[i];
                        if (!el) continue;
                        try {
                            var css = getComputedStyle(el);
                            if ((css.overflowY === 'auto' || css.overflowY === 'scroll') &&
                                el.scrollHeight > el.clientHeight + 24) {
                                return el;
                            }
                        } catch (e) {}
                    }

                    var all = document.querySelectorAll('div,main,section');
                    for (var j = 0; j < all.length && j < 180; j++) {
                        var node = all[j];
                        try {
                            var s = getComputedStyle(node);
                            if ((s.overflowY === 'auto' || s.overflowY === 'scroll') &&
                                node.scrollHeight > node.clientHeight + 80 &&
                                node.clientHeight > window.innerHeight * 0.55) {
                                return node;
                            }
                        } catch (e) {}
                    }
                    return document.scrollingElement || document.documentElement;
                }

                api.scrollBy = function(dy) {
                    try {
                        var s = findScroller();
                        if (!s) return;
                        var amount = Number(dy) || 0;
                        if (s === document.documentElement || s === document.body ||
                            s === document.scrollingElement) {
                            window.scrollBy(0, amount);
                        } else {
                            s.scrollTop += amount;
                        }
                    } catch (e) {}
                };

                api.fling = function(feedVelocity) {
                    try {
                        var s = findScroller();
                        if (!s) return;
                        var direction = Number(feedVelocity) >= 0 ? 1 : -1;
                        var amount = Math.max(280, (window.innerHeight || 480) * 0.88) * direction;
                        if (s === document.documentElement || s === document.body ||
                            s === document.scrollingElement) {
                            window.scrollBy({top: amount, left: 0, behavior: 'smooth'});
                        } else if (typeof s.scrollBy === 'function') {
                            s.scrollBy({top: amount, left: 0, behavior: 'smooth'});
                        } else {
                            s.scrollTop += amount;
                        }
                    } catch (e) {}
                };

                return true;
            } catch (e) {
                return false;
            }
        })();
    """.trimIndent()
}
