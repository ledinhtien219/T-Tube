package com.carhud.aaproxy

import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * TikTok-specific WebView policy for Android Auto.
 *
 * The page remains TikTok's public web UI. The policy only removes client-side
 * login overlays, preserves the browsing session, and keeps a small queue of
 * public video links already exposed by TikTok so a guest can continue browsing
 * when the web feed itself becomes login-gated.
 */
internal object TikTokWebPolicy {
    private const val POLICY_VERSION = 9

    fun isTikTok(url: String?): Boolean {
        val host = runCatching { Uri.parse(url.orEmpty()).host.orEmpty().lowercase() }.getOrDefault("")
        return host == "tiktok.com" || host.endsWith(".tiktok.com")
    }

    fun isSearch(url: String?): Boolean = isTikTok(url) &&
        runCatching { Uri.parse(url.orEmpty()).path.orEmpty() }.getOrDefault("")
            .let { it == "/search" || it.startsWith("/search/") }

    fun searchUrl(query: String): String =
        "https://www.tiktok.com/search/video?q=" + Uri.encode(query.trim())

    fun userAgent(defaultUa: String): String {
        // Keep Android/Chrome identity internally consistent. Pretending to be
        // Linux desktop while exposing Android touch/WebView characteristics can
        // increase TikTok risk/challenge scoring on guest sessions.
        return defaultUa
            .replace("; wv", "")
            .replace(Regex("""Version/[\d.]+\s?"""), "")
    }

    /** Apply stable settings once before TikTok navigation. */
    fun configure(view: WebView, url: String?) {
        if (!isTikTok(url)) return

        val ua = userAgent(WebSettings.getDefaultUserAgent(view.context))
        view.settings.apply {
            if (userAgentString != ua) userAgentString = ua
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = false
            textZoom = 100
            cacheMode = WebSettings.LOAD_DEFAULT
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { view.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false) }
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
     * Install/update a route-aware, low-overhead layout policy.
     * Safe to call from page-start/commit/history/finish callbacks.
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
                    var state = window.__ttubeTikTokPolicy || (window.__ttubeTikTokPolicy = {});
                    state.version = VERSION;
                    state.guestQueue = state.guestQueue || [];
                    state.guestVisited = state.guestVisited || {};
                    state.guestFallback = !!state.guestFallback;
                    state.lastGuestNavAt = state.lastGuestNavAt || 0;

                    function ensureViewport() {
                        var meta = document.querySelector('meta[name="viewport"]');
                        if (!meta) {
                            meta = document.createElement('meta');
                            meta.name = 'viewport';
                            (document.head || document.documentElement).appendChild(meta);
                        }
                        meta.content = 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no';
                    }

                    function ensureStyle() {
                        var style = document.getElementById('ttube-tiktok-policy');
                        if (!style) {
                            style = document.createElement('style');
                            style.id = 'ttube-tiktok-policy';
                            (document.head || document.documentElement).appendChild(style);
                        }
                        style.textContent = `
                            html, body {
                                overscroll-behavior-x: none !important;
                                overscroll-behavior-y: contain !important;
                                -webkit-text-size-adjust: 100% !important;
                            }
                            body { touch-action: pan-x pan-y pinch-zoom; }
                            video { background: #000 !important; }

                            [data-ttube-login-wall="1"],
                            [data-ttube-app-promo-wall="1"],
                            [data-ttube-login-backdrop="1"] {
                                display: none !important;
                                visibility: hidden !important;
                                opacity: 0 !important;
                                pointer-events: none !important;
                            }

                            /* T Tube owns navigation/search on Android Auto. Hide
                               TikTok desktop rails/action columns that waste space. */
                            [data-e2e="browse-sidebar"],
                            [data-e2e="left-nav"],
                            [class*="SideNavContainer"],
                            [class*="DivSideNav"],
                            [class*="DivMainNavContainer"],
                            [class*="DivActionItemContainer"],
                            [class*="ActionItemContainer"],
                            [class*="DivVideoActionContainer"],
                            [data-e2e="browse-like"],
                            [data-e2e="browse-comment"],
                            [data-e2e="browse-share"],
                            [data-e2e="browse-save"],
                            [data-e2e="top-login-button"],
                            [class*="DivHeaderRightContainer"] {
                                display: none !important;
                                visibility: hidden !important;
                                pointer-events: none !important;
                            }

                            html.ttube-tiktok-video-only,
                            html.ttube-tiktok-video-only body {
                                margin: 0 !important;
                                padding: 0 !important;
                                width: 100% !important;
                                height: 100% !important;
                                overflow: hidden !important;
                                background: #000 !important;
                            }
                            html.ttube-tiktok-video-only [data-e2e="browse-video-desc"],
                            html.ttube-tiktok-video-only [data-e2e="search-comment-container"],
                            html.ttube-tiktok-video-only [data-e2e="comment-input"],
                            html.ttube-tiktok-video-only [class*="CommentListContainer"],
                            html.ttube-tiktok-video-only [class*="DivCommentContentContainer"],
                            html.ttube-tiktok-video-only [class*="DivVideoInfoContainer"] {
                                display: none !important;
                            }
                            html.ttube-tiktok-video-only .ttube-tiktok-video-chain {
                                overflow: visible !important;
                                transform: none !important;
                                clip: auto !important;
                                contain: none !important;
                            }
                            html.ttube-tiktok-video-only video[data-ttube-video-only="1"] {
                                position: fixed !important;
                                inset: 0 !important;
                                width: 100vw !important;
                                height: 100vh !important;
                                max-width: 100vw !important;
                                max-height: 100vh !important;
                                min-width: 0 !important;
                                min-height: 0 !important;
                                margin: 0 !important;
                                object-fit: contain !important;
                                background: #000 !important;
                                z-index: 2147483646 !important;
                            }
                        `;
                    }

                    function isVideoRoute() {
                        return /\/video\/\d+/.test(location.pathname || '');
                    }

                    function isSearchRoute() {
                        var path = location.pathname || '';
                        return path === '/search' || path.indexOf('/search/') === 0;
                    }

                    function clearVideoMode() {
                        document.documentElement.classList.remove('ttube-tiktok-video-only');
                        document.querySelectorAll('[data-ttube-video-only="1"]').forEach(function(v) {
                            v.removeAttribute('data-ttube-video-only');
                        });
                        document.querySelectorAll('.ttube-tiktok-video-chain').forEach(function(n) {
                            n.classList.remove('ttube-tiktok-video-chain');
                        });
                    }

                    function recoverSearchShell() {
                        if (!isSearchRoute()) return;
                        clearVideoMode();
                        try {
                            document.documentElement.classList.remove('ttube-tiktok-video-only');
                            [document.documentElement, document.body].forEach(function(el) {
                                if (!el) return;
                                el.style.setProperty('visibility', 'visible', 'important');
                                el.style.setProperty('opacity', '1', 'important');
                                el.style.setProperty('min-height', '100%', 'important');
                                el.style.setProperty('overflow-y', 'auto', 'important');
                                el.style.removeProperty('pointer-events');
                            });
                            ['#app', '#root', '[data-e2e="app-root"]', 'main'].forEach(function(sel) {
                                var el = document.querySelector(sel);
                                if (!el) return;
                                el.removeAttribute('inert');
                                if (el.getAttribute('aria-hidden') === 'true') el.removeAttribute('aria-hidden');
                                el.style.setProperty('display', '', 'important');
                                el.style.setProperty('visibility', 'visible', 'important');
                                el.style.setProperty('opacity', '1', 'important');
                                el.style.removeProperty('pointer-events');
                            });
                            document.querySelectorAll('[data-ttube-login-backdrop="1"]').forEach(function(el) {
                                if (el.querySelector('main, [data-e2e*="search"], a[href*="/video/"]')) {
                                    el.removeAttribute('data-ttube-login-backdrop');
                                }
                            });
                        } catch (_) {}
                    }

                    function primaryVideo() {
                        var videos = Array.prototype.slice.call(document.querySelectorAll('video'));
                        if (!videos.length) return null;
                        videos.sort(function(a, b) {
                            var ar = a.getBoundingClientRect();
                            var br = b.getBoundingClientRect();
                            var aa = Math.max(0, ar.width) * Math.max(0, ar.height);
                            var ba = Math.max(0, br.width) * Math.max(0, br.height);
                            if (!a.paused) aa += 10000000;
                            if (!b.paused) ba += 10000000;
                            return ba - aa;
                        });
                        return videos[0] || null;
                    }

                    function applyVideoMode() {
                        clearVideoMode();
                        if (!isVideoRoute()) return;
                        var video = primaryVideo();
                        if (!video) return;

                        document.documentElement.classList.add('ttube-tiktok-video-only');
                        video.setAttribute('data-ttube-video-only', '1');
                        var node = video.parentElement;
                        var depth = 0;
                        while (node && node !== document.body && node !== document.documentElement && depth++ < 12) {
                            node.classList.add('ttube-tiktok-video-chain');
                            node = node.parentElement;
                        }
                    }

                    function normalizeVideoUrl(raw) {
                        try {
                            var u = new URL(raw || '', location.href);
                            var h = (u.hostname || '').toLowerCase();
                            if (!(h === 'tiktok.com' || h.endsWith('.tiktok.com'))) return '';
                            if (!/\/@[^/]+\/video\/\d+/.test(u.pathname || '')) return '';
                            return 'https://www.tiktok.com' + u.pathname;
                        } catch (_) { return ''; }
                    }

                    function restoreGuestQueue() {
                        if (state.queueRestored) return;
                        state.queueRestored = true;
                        try {
                            var saved = JSON.parse(sessionStorage.getItem('ttube_tiktok_guest_queue') || '[]');
                            if (Array.isArray(saved)) {
                                saved.slice(-80).forEach(function(u) {
                                    if (u && state.guestQueue.indexOf(u) < 0) state.guestQueue.push(u);
                                });
                            }
                        } catch (_) {}
                    }

                    function saveGuestQueue() {
                        try {
                            sessionStorage.setItem('ttube_tiktok_guest_queue', JSON.stringify(state.guestQueue.slice(-80)));
                        } catch (_) {}
                    }

                    function collectPublicVideos(root) {
                        restoreGuestQueue();
                        var scope = root && root.querySelectorAll ? root : document;
                        var links = scope.querySelectorAll('a[href*="/video/"]');
                        var limit = Math.min(links.length, 48);
                        for (var i = 0; i < limit; i++) {
                            var u = normalizeVideoUrl(links[i].href || links[i].getAttribute('href'));
                            if (u && state.guestQueue.indexOf(u) < 0) state.guestQueue.push(u);
                        }
                        var current = normalizeVideoUrl(location.href);
                        if (current && state.guestQueue.indexOf(current) < 0) state.guestQueue.push(current);
                        if (state.guestQueue.length > 80) state.guestQueue = state.guestQueue.slice(-80);
                        saveGuestQueue();
                    }

                    function isLargeFixedOverlay(el) {
                        if (!el || !el.getBoundingClientRect) return false;
                        var r = el.getBoundingClientRect();
                        var area = Math.max(0, r.width) * Math.max(0, r.height);
                        var screen = Math.max(1, innerWidth * innerHeight);
                        var cs = getComputedStyle(el);
                        return area > screen * 0.22 && (cs.position === 'fixed' || cs.position === 'absolute');
                    }

                    function looksLikeLoginWall(el) {
                        if (!el || !el.querySelectorAll) return false;
                        var marker = ((el.getAttribute && (
                            (el.getAttribute('data-e2e') || '') + ' ' +
                            (el.getAttribute('class') || '') + ' ' +
                            (el.getAttribute('id') || '') + ' ' +
                            (el.getAttribute('role') || '')
                        )) || '').toLowerCase();

                        if (marker.indexOf('login-modal') >= 0 || marker.indexOf('loginmodal') >= 0 ||
                            marker.indexOf('login-guide') >= 0 || marker.indexOf('loginguide') >= 0 ||
                            marker.indexOf('bottom-login-entry') >= 0 || marker.indexOf('browse-login') >= 0) return true;

                        var text = ((el.innerText || el.textContent || '') + '').toLowerCase().replace(/\s+/g, ' ').slice(0, 1600);
                        if (!text) return false;
                        var asksLogin = text.indexOf('log in') >= 0 || text.indexOf('login') >= 0 ||
                            text.indexOf('sign up') >= 0 || text.indexOf('đăng nhập') >= 0 ||
                            text.indexOf('đăng ký') >= 0;
                        if (!asksLogin) return false;

                        var modalLike = marker.indexOf('dialog') >= 0 || marker.indexOf('modal') >= 0 ||
                            marker.indexOf('logincontainer') >= 0 || isLargeFixedOverlay(el);
                        return modalLike;
                    }

                    function looksLikeAppPromoWall(el) {
                        if (!el || !el.querySelectorAll) return false;
                        var marker = ((el.getAttribute && (
                            (el.getAttribute('data-e2e') || '') + ' ' +
                            (el.getAttribute('class') || '') + ' ' +
                            (el.getAttribute('id') || '') + ' ' +
                            (el.getAttribute('role') || '')
                        )) || '').toLowerCase();
                        var text = ((el.innerText || el.textContent || '') + '').toLowerCase().replace(/\s+/g, ' ').slice(0, 1800);
                        if (!text) return false;
                        var asksOpenApp = text.indexOf('mở tiktok') >= 0 || text.indexOf('open tiktok') >= 0 ||
                            text.indexOf('open app') >= 0 || text.indexOf('mở ứng dụng') >= 0 ||
                            text.indexOf('trải nghiệm đầy đủ trong ứng dụng') >= 0 ||
                            text.indexOf('full experience in the app') >= 0 ||
                            text.indexOf('enjoy the full experience') >= 0 ||
                            text.indexOf('tận hưởng trải nghiệm đầy đủ') >= 0;
                        if (!asksOpenApp) return false;
                        return marker.indexOf('dialog') >= 0 || marker.indexOf('modal') >= 0 || isLargeFixedOverlay(el);
                    }

                    function unlockDocumentAfterModal() {
                        try {
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
                                if (el.getAttribute('aria-hidden') === 'true') el.removeAttribute('aria-hidden');
                            });
                        } catch (_) {}
                    }

                    function markBackdropNear(el) {
                        try {
                            var node = el && el.parentElement;
                            for (var depth = 0; node && depth < 3; depth++, node = node.parentElement) {
                                if (node === document.body || node === document.documentElement) break;
                                if (isLargeFixedOverlay(node)) {
                                    var containsContent = !!node.querySelector('main, [data-e2e*="search"], a[href*="/video/"]');
                                    if (!containsContent) node.setAttribute('data-ttube-login-backdrop', '1');
                                    break;
                                }
                            }
                        } catch (_) {}
                    }

                    function dismissLoginWalls(force) {
                        var now = Date.now();
                        if (!force && state.lastLoginSweepAt && now - state.lastLoginSweepAt < 350) return;
                        state.lastLoginSweepAt = now;
                        var candidates = document.querySelectorAll(
                            '[data-e2e="login-modal"], [data-e2e="modal-inner-wrapper"], ' +
                            '[data-e2e="bottom-login-entry"], [data-e2e="browse-login"], ' +
                            '[role="dialog"], [class*="LoginModal"], [class*="login-modal"], ' +
                            '[class*="DivLoginGuide"], [class*="login-guide"], [class*="LoginContainer"]'
                        );
                        var hidden = false;
                        candidates.forEach(function(el) {
                            var loginWall = looksLikeLoginWall(el);
                            var appPromo = !loginWall && looksLikeAppPromoWall(el);
                            if (!loginWall && !appPromo) return;
                            hidden = true;
                            el.setAttribute(appPromo ? 'data-ttube-app-promo-wall' : 'data-ttube-login-wall', '1');
                            // Avoid synthetic close clicks: CSS removal is enough and
                            // looks less automated to TikTok's guest risk checks.
                            markBackdropNear(el);
                        });
                        if (hidden) {
                            state.guestFallback = true;
                            state.loginBlockedAt = Date.now();
                            collectPublicVideos(document);
                            unlockDocumentAfterModal();
                        }
                    }

                    function guestNavigate(direction) {
                        if (!state.guestFallback) return false;
                        var now = Date.now();
                        if (now - state.lastGuestNavAt < 650) return false;
                        collectPublicVideos(document);
                        if (!state.guestQueue.length) return false;

                        var current = normalizeVideoUrl(location.href);
                        var index = current ? state.guestQueue.indexOf(current) : -1;
                        if (index < 0) index = typeof state.guestIndex === 'number' ? state.guestIndex : -1;
                        var targetIndex = direction > 0 ? index + 1 : index - 1;
                        if (targetIndex < 0) targetIndex = 0;
                        if (targetIndex >= state.guestQueue.length) targetIndex = state.guestQueue.length - 1;
                        if (targetIndex === index || !state.guestQueue[targetIndex]) return false;

                        var target = state.guestQueue[targetIndex];
                        state.guestIndex = targetIndex;
                        state.guestVisited[target] = 1;
                        state.lastGuestNavAt = now;
                        try {
                            location.assign(target);
                            return true;
                        } catch (_) { return false; }
                    }

                    function installGuestGestures() {
                        if (state.gesturePatched) return;
                        state.gesturePatched = true;
                        var startY = 0;
                        document.addEventListener('touchstart', function(e) {
                            if (e.touches && e.touches.length) startY = e.touches[0].clientY;
                        }, {capture: true, passive: true});
                        document.addEventListener('touchend', function(e) {
                            if (!state.guestFallback || !e.changedTouches || !e.changedTouches.length) return;
                            var dy = e.changedTouches[0].clientY - startY;
                            if (Math.abs(dy) < 64) return;
                            if (guestNavigate(dy < 0 ? 1 : -1)) {
                                try { e.preventDefault(); } catch (_) {}
                            }
                        }, {capture: true, passive: false});
                        document.addEventListener('wheel', function(e) {
                            if (!state.guestFallback || Math.abs(e.deltaY || 0) < 70) return;
                            if (guestNavigate(e.deltaY > 0 ? 1 : -1)) {
                                try { e.preventDefault(); } catch (_) {}
                            }
                        }, {capture: true, passive: false});
                    }

                    function installFastSearchResultClicks() {
                        // Leave search-result anchor navigation native. Forced JS
                        // location.assign() produced extra guest risk/CAPTCHA events.
                        state.searchClicksPatched = true;
                    }

                    function recoverLegacyPlayerRoute() {
                        var m = (location.pathname || '').match(/^\/player\/v1\/(\d+)/);
                        if (!m) return false;
                        restoreGuestQueue();
                        var id = m[1], target = '';
                        for (var i = state.guestQueue.length - 1; i >= 0; i--) {
                            if (state.guestQueue[i].indexOf('/video/' + id) >= 0) { target = state.guestQueue[i]; break; }
                        }
                        if (target) {
                            try { location.replace(target); return true; } catch (_) {}
                        }
                        try { if (history.length > 1) { history.back(); return true; } } catch (_) {}
                        return false;
                    }

                    function applyThemeHint() {
                        var isDay = window.__carhudCurrentIsDay !== false;
                        var scheme = isDay ? 'light' : 'dark';
                        document.documentElement.style.setProperty('color-scheme', scheme, 'important');
                        if (document.body) document.body.style.setProperty('color-scheme', scheme, 'important');
                    }

                    function applyAll() {
                        state.pending = false;
                        ensureViewport();
                        ensureStyle();
                        if (recoverLegacyPlayerRoute()) return;
                        if (!isVideoRoute()) collectPublicVideos(document);
                        else {
                            var current = normalizeVideoUrl(location.href);
                            if (current && state.guestQueue.indexOf(current) < 0) state.guestQueue.push(current);
                        }
                        dismissLoginWalls(false);
                        recoverSearchShell();
                        installGuestGestures();
                        installFastSearchResultClicks();
                        applyVideoMode();
                        applyThemeHint();
                    }

                    function schedule() {
                        if (state.pending) return;
                        state.pending = true;
                        var run = function() { setTimeout(applyAll, 72); };
                        if (window.requestAnimationFrame) requestAnimationFrame(run); else run();
                    }

                    if (!state.historyPatched) {
                        state.historyPatched = true;
                        ['pushState', 'replaceState'].forEach(function(name) {
                            var original = history[name];
                            if (typeof original !== 'function') return;
                            history[name] = function() {
                                var result = original.apply(this, arguments);
                                schedule();
                                return result;
                            };
                        });
                        window.addEventListener('popstate', schedule, true);
                        window.addEventListener('pageshow', schedule, true);
                        document.addEventListener('visibilitychange', function() {
                            if (!document.hidden) schedule();
                        }, true);
                    }

                    if (!state.observer) {
                        try {
                            state.observer = new MutationObserver(function(mutations) {
                                for (var i = 0; i < mutations.length; i++) {
                                    if (mutations[i].addedNodes && mutations[i].addedNodes.length) {
                                        schedule();
                                        return;
                                    }
                                }
                            });
                            state.observer.observe(document.documentElement, { childList: true, subtree: true });
                        } catch (_) {}
                    }

                    applyAll();
                } catch (_) {}
            })();
            """.trimIndent(),
            null
        )
    }
}