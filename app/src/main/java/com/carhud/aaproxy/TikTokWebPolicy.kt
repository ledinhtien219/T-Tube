package com.carhud.aaproxy

import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * TikTok-specific WebView policy for Android Auto.
 *
 * The page remains TikTok's public web UI. Explicit login is allowed and kept
 * in the same WebView/cookie session. Automatic login gates may still fall back
 * to public browsing, and a small queue of public video links lets playback
 * continue when TikTok temporarily guest-gates the feed.
 */
internal object TikTokWebPolicy {
    private const val POLICY_VERSION = 13

    fun isTikTok(url: String?): Boolean {
        val host = runCatching { Uri.parse(url.orEmpty()).host.orEmpty().lowercase() }.getOrDefault("")
        return host == "tiktok.com" || host.endsWith(".tiktok.com")
    }

    fun isSearch(url: String?): Boolean = isTikTok(url) &&
        runCatching { Uri.parse(url.orEmpty()).path.orEmpty() }.getOrDefault("")
            .let { it == "/search" || it.startsWith("/search/") }

    fun searchUrl(query: String): String =
        // The plain public search route is stable in projected Android WebView.
        // The old video-specific route can render an empty mobile shell.
        "https://www.tiktok.com/search?q=" + Uri.encode(query.trim())

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
            // Keep OAuth/login links in this WebView instead of silently dropping them.
            javaScriptCanOpenWindowsAutomatically = true
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
                            html:not(.ttube-tiktok-search) [data-e2e="browse-sidebar"],
                            html:not(.ttube-tiktok-search) [data-e2e="left-nav"],
                            html:not(.ttube-tiktok-search) [class*="SideNavContainer"],
                            html:not(.ttube-tiktok-search) [class*="DivSideNav"],
                            html:not(.ttube-tiktok-search) [class*="DivMainNavContainer"],
                            html:not(.ttube-tiktok-search) [class*="DivActionItemContainer"],
                            html:not(.ttube-tiktok-search) [class*="ActionItemContainer"],
                            html:not(.ttube-tiktok-search) [class*="DivVideoActionContainer"],
                            html:not(.ttube-tiktok-search) [data-e2e="browse-like"],
                            html:not(.ttube-tiktok-search) [data-e2e="browse-comment"],
                            html:not(.ttube-tiktok-search) [data-e2e="browse-share"],
                            html:not(.ttube-tiktok-search) [data-e2e="browse-save"] {
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

                    function isLoginRoute() {
                        var path = (location.pathname || '').toLowerCase();
                        return path === '/login' || path.indexOf('/login/') === 0 ||
                            path === '/signup' || path.indexOf('/signup/') === 0;
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
                        document.documentElement.classList.add('ttube-tiktok-search');
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

                        // Do not hide the entire TikTok page until the selected video
                        // has real media metadata. The old eager fullscreen class could
                        // leave a permanent black canvas when TikTok had only mounted
                        // an empty <video> shell after a search-result click.
                        var ready = video.readyState >= 1 && (video.videoWidth > 0 || video.videoHeight > 0);
                        if (!ready) {
                            if (!video.__ttubeReadyHook) {
                                video.__ttubeReadyHook = true;
                                ['loadedmetadata', 'canplay', 'playing'].forEach(function(name) {
                                    video.addEventListener(name, schedule, { once: true, passive: true });
                                });
                            }
                            return;
                        }

                        document.documentElement.classList.add('ttube-tiktok-video-only');
                        video.setAttribute('data-ttube-video-only', '1');
                        var node = video.parentElement;
                        var depth = 0;
                        while (node && node !== document.body && node !== document.documentElement && depth++ < 12) {
                            node.classList.add('ttube-tiktok-video-chain');
                            node = node.parentElement;
                        }

                        // Android Auto allows autoplay for this WebView. Ask the
                        // selected result to start once it is actually playable.
                        try {
                            video.playsInline = true;
                            if (video.paused) {
                                var playPromise = video.play();
                                if (playPromise && typeof playPromise.catch === 'function') {
                                    playPromise.catch(function() {});
                                }
                            }
                        } catch (_) {}
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
                        var text = ((el.innerText || el.textContent || '') + '').toLowerCase().replace(/\s+/g, ' ').slice(0, 2200);
                        if (!text) return false;

                        var tiktokLitePromo =
                            text.indexOf('tiktok lite') >= 0 &&
                            (
                                text.indexOf('open tiktok lite') >= 0 ||
                                text.indexOf('try out more features') >= 0 ||
                                text.indexOf('watch your favorite videos with less data') >= 0 ||
                                text.indexOf('faster, smoother experience') >= 0
                            );

                        var asksOpenApp = tiktokLitePromo ||
                            text.indexOf('mở tiktok') >= 0 || text.indexOf('open tiktok') >= 0 ||
                            text.indexOf('open app') >= 0 || text.indexOf('mở ứng dụng') >= 0 ||
                            text.indexOf('trải nghiệm đầy đủ trong ứng dụng') >= 0 ||
                            text.indexOf('full experience in the app') >= 0 ||
                            text.indexOf('enjoy the full experience') >= 0 ||
                            text.indexOf('tận hưởng trải nghiệm đầy đủ') >= 0;
                        if (!asksOpenApp) return false;

                        var promoMarker =
                            marker.indexOf('dialog') >= 0 ||
                            marker.indexOf('modal') >= 0 ||
                            marker.indexOf('popup') >= 0 ||
                            marker.indexOf('bottomsheet') >= 0 ||
                            marker.indexOf('app-promo') >= 0 ||
                            marker.indexOf('apppromo') >= 0 ||
                            marker.indexOf('download') >= 0 ||
                            marker.indexOf('lite') >= 0;

                        return promoMarker || isLargeFixedOverlay(el);
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


                    function installLoginIntentTracking() {
                        if (state.loginIntentHandlerVersion === VERSION && state.loginIntentHandler) return;
                        if (state.loginIntentHandler) {
                            try { document.removeEventListener('click', state.loginIntentHandler, true); } catch (_) {}
                        }
                        state.loginIntentHandlerVersion = VERSION;
                        state.loginIntentHandler = function(event) {
                            var target = event.target;
                            if (target && target.nodeType !== 1) target = target.parentElement;
                            var clickable = target && target.closest ?
                                target.closest('button, a, [role="button"], [data-e2e]') : null;
                            if (!clickable) return;

                            var marker = (
                                (clickable.getAttribute('data-e2e') || '') + ' ' +
                                (clickable.getAttribute('class') || '') + ' ' +
                                (clickable.getAttribute('id') || '')
                            ).toLowerCase();
                            var text = ((clickable.innerText || clickable.textContent || '') + '')
                                .toLowerCase().replace(/\s+/g, ' ').trim();

                            var loginIntent =
                                marker.indexOf('login') >= 0 || marker.indexOf('sign-in') >= 0 ||
                                text === 'log in' || text === 'login' || text.indexOf('đăng nhập') >= 0 ||
                                text.indexOf('sign in') >= 0 || text.indexOf('use phone') >= 0 ||
                                text.indexOf('use email') >= 0;
                            if (!loginIntent) return;

                            state.loginRequestedAt = Date.now();
                            state.guestFallback = false;
                            document.querySelectorAll(
                                '[data-ttube-login-wall="1"], [data-ttube-login-backdrop="1"]'
                            ).forEach(function(el) {
                                el.removeAttribute('data-ttube-login-wall');
                                el.removeAttribute('data-ttube-login-backdrop');
                            });
                        };
                        document.addEventListener('click', state.loginIntentHandler, true);
                    }

                    function dismissLoginWalls(force) {
                        var now = Date.now();
                        if (!force && state.lastLoginSweepAt && now - state.lastLoginSweepAt < 250) return;
                        state.lastLoginSweepAt = now;

                        var candidateList = Array.prototype.slice.call(document.querySelectorAll(
                            '[data-e2e="login-modal"], [data-e2e="modal-inner-wrapper"], ' +
                            '[data-e2e="bottom-login-entry"], [data-e2e="browse-login"], ' +
                            '[role="dialog"], [class*="LoginModal"], [class*="login-modal"], ' +
                            '[class*="DivLoginGuide"], [class*="login-guide"], [class*="LoginContainer"], ' +
                            '[class*="Modal"], [class*="modal"], [class*="Popup"], [class*="popup"], ' +
                            '[class*="BottomSheet"], [class*="bottom-sheet"], [class*="AppPromo"], ' +
                            '[class*="app-promo"], [class*="TikTokLite"], [class*="tiktok-lite"]'
                        ));

                        // TikTok Lite promos are sometimes rendered in an anonymous portal
                        // without dialog/modal classes. Find their CTA text, then walk up to
                        // the large fixed/absolute container that owns the overlay.
                        var triggers = document.querySelectorAll('button, a, [role="button"]');
                        for (var i = 0; i < triggers.length; i++) {
                            var trigger = triggers[i];
                            var triggerText = ((trigger.innerText || trigger.textContent || '') + '')
                                .toLowerCase().replace(/\s+/g, ' ').trim();
                            if (
                                triggerText.indexOf('open tiktok lite') < 0 &&
                                triggerText.indexOf('tiktok lite') < 0 &&
                                triggerText.indexOf('open tiktok') < 0
                            ) continue;

                            var node = trigger;
                            for (var depth = 0; node && depth < 9; depth++, node = node.parentElement) {
                                if (node === document.body || node === document.documentElement) break;
                                if (looksLikeAppPromoWall(node) || isLargeFixedOverlay(node)) {
                                    if (candidateList.indexOf(node) < 0) candidateList.push(node);
                                    break;
                                }
                            }
                        }

                        var hidden = false;
                        var allowLogin = isLoginRoute() ||
                            (state.loginRequestedAt && now - state.loginRequestedAt < 180000);
                        candidateList.forEach(function(el) {
                            var loginWall = looksLikeLoginWall(el);
                            if (loginWall && allowLogin) {
                                el.removeAttribute('data-ttube-login-wall');
                                document.querySelectorAll('[data-ttube-login-backdrop="1"]').forEach(function(bg) {
                                    bg.removeAttribute('data-ttube-login-backdrop');
                                });
                                return;
                            }

                            var appPromo = !loginWall && looksLikeAppPromoWall(el);
                            if (!loginWall && !appPromo) return;
                            hidden = true;
                            el.setAttribute(appPromo ? 'data-ttube-app-promo-wall' : 'data-ttube-login-wall', '1');
                            // Automatic gates can still use guest fallback. A login
                            // explicitly opened by the user is never hidden.
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
                        if (state.searchClickHandlerVersion === VERSION && state.searchClickHandler) return;

                        if (state.searchClickHandler) {
                            try { document.removeEventListener('click', state.searchClickHandler, true); } catch (_) {}
                        }

                        state.searchClickHandlerVersion = VERSION;
                        state.searchClickHandler = function(event) {
                            if (!isSearchRoute()) return;

                            var target = event.target;
                            if (target && target.nodeType !== 1) target = target.parentElement;
                            var anchor = target && target.closest ? target.closest('a[href*="/video/"]') : null;
                            if (!anchor) return;

                            var canonical = normalizeVideoUrl(anchor.href || anchor.getAttribute('href'));
                            if (!canonical) return;

                            // TikTok sometimes rewrites a result click to /player/v1/<id>
                            // in WebView. That player shell can paint black forever.
                            // Navigate to the canonical public /@user/video/<id> route.
                            collectPublicVideos(document);
                            if (state.guestQueue.indexOf(canonical) < 0) state.guestQueue.push(canonical);
                            saveGuestQueue();

                            try {
                                event.preventDefault();
                                event.stopPropagation();
                                if (event.stopImmediatePropagation) event.stopImmediatePropagation();
                            } catch (_) {}

                            state.lastGuestNavAt = Date.now();
                            try { location.assign(canonical); } catch (_) { location.href = canonical; }
                        };

                        document.addEventListener('click', state.searchClickHandler, true);
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
                        state.lastApplyAt = Date.now();
                        ensureViewport();
                        ensureStyle();
                        document.documentElement.classList.toggle('ttube-tiktok-search', isSearchRoute());
                        if (recoverLegacyPlayerRoute()) return;

                        if (!isVideoRoute()) {
                            var collectNow = Date.now();
                            var collectGap = isSearchRoute() ? 420 : 900;
                            if (!state.lastCollectAt || collectNow - state.lastCollectAt >= collectGap) {
                                state.lastCollectAt = collectNow;
                                collectPublicVideos(document);
                            }
                        } else {
                            var current = normalizeVideoUrl(location.href);
                            if (current && state.guestQueue.indexOf(current) < 0) state.guestQueue.push(current);
                        }

                        installLoginIntentTracking();
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
                        var run = function() {
                            if (!state.lastApplyAt) {
                                setTimeout(applyAll, 72);
                                return;
                            }
                            var elapsed = Date.now() - state.lastApplyAt;
                            var delay = Math.max(72, 160 - elapsed);
                            setTimeout(applyAll, delay);
                        };
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