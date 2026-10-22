package com.carhud.aaproxy

import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * TikTok web policy.
 *
 * v16 stays close to the real desktop website and throttles popup sweeps so
 * TikTok feed mutations do not cause unnecessary layout work. Previous builds
 * rewrote feed/video layout and maintained a synthetic guest queue; that made
 * TikTok's SPA fragile and could leave a black/empty surface after search.
 *
 * We now let www.tiktok.com own feed, search, scrolling and video navigation.
 * T Tube only keeps the signed-in cookie session, blocks native-app deep links,
 * and hides app-only promotion/challenge overlays.
 */
internal object TikTokWebPolicy {
    private const val POLICY_VERSION = 18

    private const val DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    private const val PHONE_UA =
        "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    private const val QR_LOGIN_URL = "https://in.tiktok.com/login/qrcode"
    private const val LOGIN_URL = "https://in.tiktok.com/login/"

    fun isTikTok(url: String?): Boolean {
        val host = runCatching { Uri.parse(url.orEmpty()).host.orEmpty().lowercase() }
            .getOrDefault("")
        return host == "tiktok.com" || host.endsWith(".tiktok.com")
    }

    fun isLoginRoute(url: String?): Boolean {
        if (!isTikTok(url)) return false
        val path = runCatching { Uri.parse(url.orEmpty()).path.orEmpty().lowercase() }.getOrDefault("")
        return path.startsWith("/login") ||
            path.startsWith("/signup") ||
            path.startsWith("/passport")
    }

    fun isGoogleOAuth(url: String?): Boolean {
        val parsed = runCatching { Uri.parse(url.orEmpty()) }.getOrNull() ?: return false
        val host = parsed.host.orEmpty().lowercase()
        val path = parsed.path.orEmpty().lowercase()
        return host == "accounts.google.com" ||
            host.endsWith(".accounts.google.com") ||
            host == "oauth2.googleapis.com" ||
            host.endsWith(".googleusercontent.com") ||
            (host == "google.com" && path.contains("oauth"))
    }

    fun safeLoginUrl(): String = LOGIN_URL

    fun qrLoginUrl(): String = QR_LOGIN_URL

    fun isSessionUrl(url: String?): Boolean {
        if (isTikTok(url)) return true
        val host = runCatching { Uri.parse(url.orEmpty()).host.orEmpty().lowercase() }.getOrDefault("")
        return host == "accounts.google.com" ||
            host.endsWith(".googleusercontent.com") ||
            host == "appleid.apple.com" ||
            host == "facebook.com" ||
            host.endsWith(".facebook.com") ||
            host == "x.com" ||
            host.endsWith(".x.com") ||
            host == "twitter.com" ||
            host.endsWith(".twitter.com")
    }

    fun isSearch(url: String?): Boolean = isTikTok(url) &&
        runCatching { Uri.parse(url.orEmpty()).path.orEmpty() }.getOrDefault("")
            .let { it == "/search" || it.startsWith("/search/") }

    /**
     * Use TikTok's normal desktop search route. No /search/video rewrite and
     * no Google fallback: the user stays on tiktok.com.
     */
    fun searchUrl(query: String): String =
        "https://www.tiktok.com/search?q=" + Uri.encode(query.trim())

    fun isAppRedirect(url: String?): Boolean {
        val raw = url.orEmpty().trim()
        val lower = raw.lowercase()
        if (lower.startsWith("intent:") ||
            lower.startsWith("snssdk") ||
            lower.startsWith("tiktok:") ||
            lower.startsWith("tiktoklite:") ||
            lower.startsWith("market:") ||
            lower.contains("play.google.com/store")
        ) return true

        if (!isTikTok(raw)) return false
        val path = runCatching { Uri.parse(raw).path.orEmpty().lowercase() }.getOrDefault("")
        return path.startsWith("/download") || path.startsWith("/open-app")
    }

    /**
     * TikTok desktop web performs best with the normal hardware compositor.
     * Build 252 forced software rendering on search; remove that workaround.
     */
    fun applyRenderingPolicy(view: WebView, url: String?) {
        if (!isTikTok(url)) return
        if (view.layerType != View.LAYER_TYPE_HARDWARE) {
            runCatching { view.setLayerType(View.LAYER_TYPE_HARDWARE, null) }
        }
    }

    /**
     * Kept as a compatibility hook for existing page callbacks. Desktop
     * tiktok.com owns search rendering now, so this intentionally does nothing.
     */
    fun recoverSearchIfBlank(view: WebView, url: String?) {
        if (!isTikTok(url)) return
    }

    fun userAgent(defaultUa: String, url: String? = null): String {
        // TikTok's own QR/phone-email web login is designed for a full browser.
        // Keep a stable desktop browser identity for the entire TikTok session;
        // Google OAuth itself is never opened in this WebView.
        return DESKTOP_UA
    }

    /**
     * Phone TikTok must use the responsive mobile site. Reusing the Android Auto
     * desktop identity makes TikTok render a desktop rail plus a narrow video
     * column in the middle of a portrait phone WebView.
     */
    fun configurePhone(view: WebView, url: String?) {
        if (!isTikTok(url)) return
        applyRenderingPolicy(view, url)

        view.settings.apply {
            if (userAgentString != PHONE_UA) userAgentString = PHONE_UA
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            // Do not shrink a desktop canvas to fit the phone. TikTok mobile
            // owns the viewport and should render at the real WebView width.
            loadWithOverviewMode = false
            textZoom = 100
            cacheMode = WebSettings.LOAD_DEFAULT
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                view.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
        }

        runCatching {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAcceptThirdPartyCookies(view, true)
                }
            }
        }
    }

    /**
     * Restore a real phone viewport and undo any desktop/AA zoom left on the
     * shared WebView. Safe to call repeatedly during TikTok SPA navigation.
     */
    fun fitPhoneLayout(view: WebView, url: String?) {
        if (!isTikTok(url)) return
        view.evaluateJavascript(
            """
            (function() {
                try {
                    var host = (location.hostname || '').toLowerCase();
                    if (!(host === 'tiktok.com' || host.endsWith('.tiktok.com'))) return;

                    var viewport = document.querySelector('meta[name="viewport"]');
                    if (!viewport) {
                        viewport = document.createElement('meta');
                        viewport.name = 'viewport';
                        (document.head || document.documentElement).appendChild(viewport);
                    }
                    viewport.setAttribute(
                        'content',
                        'width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1, viewport-fit=cover'
                    );

                    var body = document.body;
                    var html = document.documentElement;
                    if (body) {
                        body.style.removeProperty('zoom');
                        body.style.removeProperty('transform');
                        body.style.removeProperty('width');
                        body.style.removeProperty('min-width');
                        body.style.removeProperty('max-width');
                    }
                    if (html) {
                        html.style.removeProperty('zoom');
                        html.style.removeProperty('transform');
                        html.style.removeProperty('width');
                        html.style.removeProperty('min-width');
                        html.style.removeProperty('max-width');
                    }

                    var style = document.getElementById('ttube-tiktok-phone-layout');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'ttube-tiktok-phone-layout';
                        (document.head || document.documentElement).appendChild(style);
                    }
                    style.textContent = [
                        'html,body{',
                        'width:100% !important;',
                        'min-width:0 !important;',
                        'max-width:100vw !important;',
                        'margin:0 !important;',
                        'overflow-x:hidden !important;',
                        'overscroll-behavior-x:none !important;',
                        '}',
                        'video,img{max-width:100% !important;}'
                    ].join('');

                    window.__ttubePhoneViewport = {
                        width: Math.max(1, document.documentElement.clientWidth || window.innerWidth || 1),
                        dpr: window.devicePixelRatio || 1
                    };
                } catch (_) {}
            })();
            """.trimIndent(),
            null
        )
    }

    /** Apply a stable, browser-like configuration before TikTok navigation. */
    fun configure(view: WebView, url: String?) {
        if (!isTikTok(url)) return
        applyRenderingPolicy(view, url)

        val targetUa = userAgent(WebSettings.getDefaultUserAgent(view.context), url)
        view.settings.apply {
            if (userAgentString != targetUa) userAgentString = targetUa
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            // Desktop tiktok.com is wider than phone/AA WebViews. Let WebView
            // calculate an overview scale so the full site fits horizontally.
            loadWithOverviewMode = true
            textZoom = 100
            cacheMode = WebSettings.LOAD_DEFAULT
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                view.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
        }

        runCatching {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAcceptThirdPartyCookies(view, true)
                }
            }
        }
    }

    /**
     * Do not reshape TikTok's feed or individual videos. Only suppress overlays
     * that explicitly force the native TikTok/TikTok Lite app.
     */
    fun fitLayout(view: WebView, url: String?) {
        if (!isTikTok(url)) return

        view.evaluateJavascript(
            """
            (function() {
                try {
                    var host = (location.hostname || '').toLowerCase();
                    if (!(host === 'tiktok.com' || host.endsWith('.tiktok.com'))) return;

                    var VERSION = $POLICY_VERSION;
                    var state = window.__ttubeTikTokWebOnly ||
                        (window.__ttubeTikTokWebOnly = {});
                    state.version = VERSION;
                    state.interval = 900;
                    state.loginRoute =
                        (location.pathname || '').indexOf('/login') === 0 ||
                        (location.pathname || '').indexOf('/signup') === 0 ||
                        (location.pathname || '').indexOf('/passport') === 0;

                    state.qrLoginUrl = "https://in.tiktok.com/login/qrcode";
                    state.loginUrl = "https://in.tiktok.com/login/";
                    state.qrRoute = (location.pathname || '').indexOf('/login/qrcode') === 0;

                    function looksLikeGoogleOAuth(url) {
                        var u = String(url || '').toLowerCase();
                        return u.indexOf('accounts.google.com') >= 0 ||
                            u.indexOf('oauth2.googleapis.com') >= 0 ||
                            u.indexOf('googleusercontent.com') >= 0;
                    }

                    function loginText(el) {
                        return (((el && (el.innerText || el.textContent)) || '') + '')
                            .toLowerCase().replace(/\s+/g, ' ').trim().slice(0, 280);
                    }

                    function installLoginHelper() {
                        if (!state.loginRoute || !document.body) return;

                        var helper = document.getElementById('ttube-tiktok-login-helper');
                        if (!helper) {
                            helper = document.createElement('div');
                            helper.id = 'ttube-tiktok-login-helper';
                            helper.setAttribute('style',
                                'position:fixed;right:12px;bottom:12px;z-index:2147483647;' +
                                'display:flex;gap:8px;padding:8px;border-radius:12px;' +
                                'background:rgba(10,15,24,.92);box-shadow:0 4px 18px rgba(0,0,0,.35);'
                            );

                            var primary = document.createElement('button');
                            primary.type = 'button';
                            primary.textContent = state.qrRoute ? 'Cách đăng nhập khác' : 'Đăng nhập QR';
                            primary.setAttribute('style',
                                'border:0;border-radius:9px;padding:10px 14px;' +
                                'font:600 14px sans-serif;color:#fff;background:#fe2c55;'
                            );
                            primary.onclick = function(ev) {
                                try {
                                    ev.preventDefault();
                                    ev.stopPropagation();
                                } catch (_) {}
                                location.assign(state.qrRoute ? state.loginUrl : state.qrLoginUrl);
                                return false;
                            };
                            helper.appendChild(primary);

                            if (!state.qrRoute) {
                                var hint = document.createElement('div');
                                hint.textContent = 'Google bị Google chặn trong WebView · dùng QR hoặc SĐT/email';
                                hint.setAttribute('style',
                                    'max-width:220px;align-self:center;color:#e2e8f0;' +
                                    'font:12px/1.25 sans-serif;'
                                );
                                helper.appendChild(hint);
                            }
                            document.body.appendChild(helper);
                        }
                    }

                    if (state.loginRoute && !state.loginBridgeInstalled) {
                        state.loginBridgeInstalled = true;

                        // Capture Google before TikTok can open an OAuth popup/iframe.
                        // QR keeps authentication on TikTok's own web origin and the
                        // resulting cookies remain inside this exact WebView session.
                        document.addEventListener('click', function(e) {
                            var target = e.target;
                            var action = target && target.closest
                                ? target.closest('a,button,[role="button"]')
                                : null;
                            if (!action) return;

                            var text = loginText(action);
                            var href = action.href ||
                                (action.getAttribute ? action.getAttribute('href') : '') || '';

                            if (text.indexOf('google') >= 0 || looksLikeGoogleOAuth(href)) {
                                try {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    if (e.stopImmediatePropagation) e.stopImmediatePropagation();
                                } catch (_) {}
                                location.assign(state.qrLoginUrl);
                                return false;
                            }

                            if (action.getAttribute && action.getAttribute('target') === '_blank') {
                                action.setAttribute('target', '_self');
                            }
                        }, true);

                        var nativeOpen = window.open;
                        window.open = function(url, target, features) {
                            try {
                                if (looksLikeGoogleOAuth(url)) {
                                    location.assign(state.qrLoginUrl);
                                    return window;
                                }
                                if (url && typeof url === 'string') {
                                    location.assign(url);
                                    return window;
                                }
                            } catch (_) {}
                            return nativeOpen
                                ? nativeOpen.call(window, url, '_self', features)
                                : window;
                        };
                    }

                    function ensureStyle() {
                        var style = document.getElementById('ttube-tiktok-web-only');
                        if (!style) {
                            style = document.createElement('style');
                            style.id = 'ttube-tiktok-web-only';
                            (document.head || document.documentElement).appendChild(style);
                        }
                        style.textContent = [
                            'html,body{',
                            'max-width:100vw !important;',
                            'overflow-x:hidden !important;',
                            'overscroll-behavior-x:none !important;',
                            '}',
                            'video,img{max-width:100% !important;}',
                            '[data-ttube-app-promo-wall="1"],',
                            '[data-ttube-app-promo-backdrop="1"] {',
                            'display:none !important;',
                            'visibility:hidden !important;',
                            'opacity:0 !important;',
                            'pointer-events:none !important;',
                            '}'
                        ].join('');
                    }

                    function fitPageWidth() {
                        if (!document.body || state.fitPending) return;

                        var viewport = Math.max(
                            1,
                            document.documentElement.clientWidth || window.innerWidth || 1
                        );
                        var routeKey = (location.pathname || '') + (location.search || '') +
                            '|' + Math.round(viewport);

                        if (state.fitRouteKey !== routeKey) {
                            state.fitRouteKey = routeKey;
                            state.fitPasses = 0;
                            state.fitScale = 1;
                            document.body.style.removeProperty('zoom');
                            document.body.style.removeProperty('width');
                        }
                        if ((state.fitPasses || 0) >= 2) return;

                        state.fitPending = true;
                        requestAnimationFrame(function() {
                            try {
                                var body = document.body;
                                var de = document.documentElement;
                                if (!body || !de) return;

                                // Measure once at natural desktop-web size, then
                                // shrink only as much as this WebView requires.
                                body.style.removeProperty('zoom');
                                body.style.removeProperty('width');

                                var rawWidth = Math.max(
                                    de.scrollWidth || 0,
                                    body.scrollWidth || 0,
                                    de.clientWidth || 0
                                );
                                var currentViewport = Math.max(
                                    1,
                                    de.clientWidth || window.innerWidth || viewport
                                );
                                var scale = rawWidth > currentViewport * 1.015
                                    ? currentViewport / rawWidth
                                    : 1;

                                // Avoid making the real TikTok UI unreadably tiny.
                                scale = Math.max(0.76, Math.min(1, scale));

                                if (scale < 0.995) {
                                    body.style.setProperty('zoom', scale.toFixed(4), 'important');
                                    body.style.setProperty(
                                        'width',
                                        (100 / scale).toFixed(3) + '%',
                                        'important'
                                    );
                                }

                                state.fitScale = scale;
                                state.fitRawWidth = rawWidth;
                                state.fitViewport = currentViewport;
                                state.fitPasses = (state.fitPasses || 0) + 1;
                            } catch (_) {
                            } finally {
                                state.fitPending = false;
                            }
                        });
                    }

                    function normalizedText(el) {
                        return (((el && (el.innerText || el.textContent)) || '') + '')
                            .toLowerCase().replace(/\s+/g, ' ').trim().slice(0, 2600);
                    }

                    // textContent avoids layout work (innerText may force layout). Used for
                    // simple buttons/links; only overlay candidates use normalizedText().
                    function cheapText(el) {
                        var t = (el && el.textContent) || '';
                        if (t.length > 160) return '';
                        return t.toLowerCase().replace(/\s+/g, ' ').trim();
                    }

                    function markerText(el) {
                        if (!el || !el.getAttribute) return '';
                        return (
                            (el.getAttribute('data-e2e') || '') + ' ' +
                            (el.getAttribute('class') || '') + ' ' +
                            (el.getAttribute('id') || '') + ' ' +
                            (el.getAttribute('role') || '')
                        ).toLowerCase();
                    }

                    function isLargeOverlay(el) {
                        if (!el || !el.getBoundingClientRect) return false;
                        var r = el.getBoundingClientRect();
                        var area = Math.max(0, r.width) * Math.max(0, r.height);
                        var screen = Math.max(1, innerWidth * innerHeight);
                        var cs = getComputedStyle(el);
                        return area > screen * 0.18 &&
                            (cs.position === 'fixed' || cs.position === 'absolute');
                    }

                    function isAppOnlyWall(el) {
                        if (!el || ((el.textContent || '').length > 6000)) return false;
                        var text = normalizedText(el);
                        if (!text) return false;

                        var appCta =
                            text.indexOf('continue with tiktok app') >= 0 ||
                            text.indexOf('continue in tiktok app') >= 0 ||
                            text.indexOf('open tiktok') >= 0 ||
                            text.indexOf('mở tiktok') >= 0 ||
                            text.indexOf('open app') >= 0 ||
                            text.indexOf('mở ứng dụng') >= 0 ||
                            text.indexOf('open tiktok lite') >= 0 ||
                            text.indexOf('tiktok lite') >= 0 ||
                            text.indexOf('full experience in the app') >= 0 ||
                            text.indexOf('enjoy the full experience') >= 0 ||
                            text.indexOf('trải nghiệm đầy đủ trong ứng dụng') >= 0 ||
                            text.indexOf('tận hưởng trải nghiệm đầy đủ') >= 0;

                        var appBotChallenge =
                            (
                                text.indexOf("confirm you're not a bot") >= 0 ||
                                text.indexOf('confirm you are not a bot') >= 0 ||
                                text.indexOf('xác nhận bạn không phải') >= 0
                            ) &&
                            (
                                text.indexOf('tiktok app') >= 0 ||
                                text.indexOf('ứng dụng tiktok') >= 0
                            );

                        if (!appCta && !appBotChallenge) return false;

                        var marker = markerText(el);
                        var modalLike =
                            marker.indexOf('dialog') >= 0 ||
                            marker.indexOf('modal') >= 0 ||
                            marker.indexOf('popup') >= 0 ||
                            marker.indexOf('bottomsheet') >= 0 ||
                            marker.indexOf('download') >= 0 ||
                            marker.indexOf('app-promo') >= 0 ||
                            marker.indexOf('lite') >= 0;

                        return modalLike || isLargeOverlay(el);
                    }

                    function markBackdropNear(el) {
                        var node = el && el.parentElement;
                        for (var depth = 0; node && depth < 4; depth++, node = node.parentElement) {
                            if (node === document.body || node === document.documentElement) break;
                            if (isLargeOverlay(node)) {
                                node.setAttribute('data-ttube-app-promo-backdrop', '1');
                                return;
                            }
                        }
                    }

                    function unlockPage() {
                        [document.documentElement, document.body].forEach(function(el) {
                            if (!el) return;
                            el.style.removeProperty('overflow');
                            el.style.removeProperty('overflow-y');
                            el.style.removeProperty('pointer-events');
                            el.style.removeProperty('touch-action');
                        });
                        ['#app', '#root', '[data-e2e="app-root"]'].forEach(function(sel) {
                            var el = document.querySelector(sel);
                            if (!el) return;
                            el.removeAttribute('inert');
                            if (el.getAttribute('aria-hidden') === 'true') {
                                el.removeAttribute('aria-hidden');
                            }
                        });
                    }

                    function sweepAppWalls() {
                        state.pending = false;
                        state.lastSweepAt = Date.now();
                        ensureStyle();

                        // Login/signup is real TikTok web UI. Never hide a large
                        // authentication modal merely because it also advertises
                        // the native app.
                        if (state.loginRoute) {
                            unlockPage();
                            // Never scale the QR/auth UI with the feed-fit algorithm.
                            // A large, native TikTok QR is easier for a phone to scan.
                            try {
                                if (document.body) {
                                    document.body.style.removeProperty('zoom');
                                    document.body.style.removeProperty('width');
                                }
                            } catch (_) {}
                            installLoginHelper();
                            state.interval = 1800;
                            return;
                        }

                        var candidates = Array.prototype.slice.call(
                            document.querySelectorAll(
                                '[role="dialog"], [class*="Modal"], [class*="modal"], ' +
                                '[class*="Popup"], [class*="popup"], ' +
                                '[class*="BottomSheet"], [class*="bottom-sheet"], ' +
                                '[class*="AppPromo"], [class*="app-promo"], ' +
                                '[class*="TikTokLite"], [class*="tiktok-lite"], ' +
                                '[class*="Download"], [class*="download"]'
                            )
                        );

                        document.querySelectorAll('button, a, [role="button"]').forEach(function(trigger) {
                            var text = cheapText(trigger);
                            if (!text) return;
                            if (
                                text.indexOf('open tiktok') < 0 &&
                                text.indexOf('continue with tiktok app') < 0 &&
                                text.indexOf('continue in tiktok app') < 0 &&
                                text.indexOf('open app') < 0 &&
                                text.indexOf('tiktok lite') < 0
                            ) return;

                            var node = trigger;
                            for (var depth = 0; node && depth < 9; depth++, node = node.parentElement) {
                                if (node === document.body || node === document.documentElement) break;
                                if (isAppOnlyWall(node) || isLargeOverlay(node)) {
                                    if (candidates.indexOf(node) < 0) candidates.push(node);
                                    break;
                                }
                            }
                        });

                        var hidden = false;
                        candidates.forEach(function(el) {
                            if (el.getAttribute('data-ttube-app-promo-wall') === '1') return;
                            if (!isAppOnlyWall(el)) return;
                            hidden = true;
                            el.setAttribute('data-ttube-app-promo-wall', '1');
                            markBackdropNear(el);
                        });

                        if (hidden) unlockPage();
                        fitPageWidth();
                        // Back off when nothing needs hiding. TikTok's feed mutates often;
                        // sweeping the full page for every mutation makes scrolling janky.
                        state.interval = hidden
                            ? 900
                            : Math.min(3000, Math.round((state.interval || 900) * 1.4));
                    }

                    function scheduleSweep() {
                        if (state.pending) return;
                        state.pending = true;
                        var wait = Math.max(
                            0,
                            (state.interval || 900) - (Date.now() - (state.lastSweepAt || 0))
                        );
                        setTimeout(function() {
                            requestAnimationFrame(sweepAppWalls);
                        }, wait);
                    }

                    if (!state.observerInstalled) {
                        state.observerInstalled = true;
                        state.observer = new MutationObserver(function(mutations) {
                            for (var i = 0; i < mutations.length; i++) {
                                if (mutations[i].addedNodes && mutations[i].addedNodes.length) {
                                    scheduleSweep();
                                    return;
                                }
                            }
                        });
                        state.observer.observe(document.documentElement, {
                            childList: true,
                            subtree: true
                        });
                    }

                    scheduleSweep();
                } catch (_) {}
            })();
            """.trimIndent(),
            null
        )
    }
}
