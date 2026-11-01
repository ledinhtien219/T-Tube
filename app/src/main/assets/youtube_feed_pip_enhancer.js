/*
 * T-Tube YouTube feed ad cleanup + four-column native recommendation grid.
 * Runs only on YouTube; does not create a second player or intercept playback.
 * Keep original video anchors and listeners for native click + scroll.
 */
(function() {
  'use strict';
  if (!/(^|\.)youtube\.com$/i.test(location.hostname)) return;
  if (window.__ttubeFeedPipV2) {
    window.__ttubeFeedPipV2.refresh();
    return;
  }

  var STYLE_ID = 'ttube-feed-pip-v2';
  var AD_RENDERERS = [
    'ytd-ad-slot-renderer',
    'ytd-in-feed-ad-layout-renderer',
    'ytd-promoted-sparkles-web-renderer',
    'ytd-promoted-video-renderer',
    'ytd-compact-promoted-video-renderer',
    'ytd-display-ad-renderer',
    'ytd-action-companion-ad-renderer',
    'ytd-banner-promo-renderer',
    'ytd-masthead-ad-v3-renderer',
    'ytd-search-pyv-renderer',
    'ytd-promoted-sparkles-text-search-renderer',
    'ytm-ad-slot-renderer',
    'ytm-promoted-sparkles-web-renderer',
    'ytm-promoted-video-renderer',
    'ytm-compact-promoted-item-renderer',
    'ytm-display-ad-renderer',
    'ytm-companion-ad-renderer'
  ];
  var AD_QUERY = AD_RENDERERS.join(',');
  var CARD_QUERY = [
    'ytd-compact-video-renderer',
    'ytd-video-renderer',
    'ytd-rich-item-renderer',
    'ytm-compact-video-renderer',
    'ytm-video-with-context-renderer',
    'ytm-rich-item-renderer',
    'yt-lockup-view-model',
    'a[href*="/watch?v="]'
  ].join(',');
  var GRID_CANDIDATES = [
    'ytd-watch-next-secondary-results-renderer #items',
    '#related #items',
    'ytd-watch-next-secondary-results-renderer #contents',
    'ytm-item-section-renderer > lazy-list',
    'ytm-item-section-renderer lazy-list',
    'ytm-section-list-renderer > lazy-list',
    'ytd-item-section-renderer #contents',
    '.rich-grid-renderer-contents'
  ];
  var styleText = [
    // Dedicated advertising renderers, not generic video containers.
    AD_QUERY + '{display:none!important;visibility:hidden!important;' +
      'height:0!important;min-height:0!important;margin:0!important;' +
      'padding:0!important;pointer-events:none!important;}',
    // Modern Chromium/WebView supports :has; remove the *ad-only* parent
    // slot to avoid leaving an empty position in a YouTube grid.
    'ytd-rich-item-renderer:has(ytd-ad-slot-renderer),' +
    'ytd-rich-item-renderer:has(ytd-in-feed-ad-layout-renderer),' +
    'ytd-rich-item-renderer:has(ytd-promoted-sparkles-web-renderer),' +
    'ytd-rich-section-renderer:has(> ytd-ad-slot-renderer),' +
    'ytm-rich-item-renderer:has(ytm-ad-slot-renderer)' +
      '{display:none!important;}',
    '[data-ttube-sponsored="1"]{display:none!important;}',
    // The original YouTube related-video list, never cloned into a second
    // WebView, becomes the scrollable FOUR-column PiP browser on the car.
    // Override the legacy YouTubePlayerHelper #items selector by matching its
    // ID specificity plus one extra PiP state attribute. Otherwise the old
    // auto-fill/minmax(205px) rule keeps rendering 2-3 columns.
    'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] #items[data-ttube-four-col="1"],' +
    'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] lazy-list[data-ttube-four-col="1"],' +
    'html.carhud-youtube-pip [data-ttube-four-col="1"] {' +
      'display:grid!important;grid-template-columns:repeat(4,minmax(0,1fr))!important;' +
      'grid-auto-flow:row!important;grid-auto-rows:auto!important;' +
      'column-gap:clamp(6px,0.9vw,12px)!important;row-gap:12px!important;' +
      'align-items:start!important;width:100%!important;max-width:100%!important;' +
      'box-sizing:border-box!important;overflow:visible!important;' +
      'padding:4px 4px 96px!important;}',
    'html.carhud-youtube-pip [data-ttube-four-col="1"] > * {' +
      'min-width:0!important;width:100%!important;max-width:100%!important;' +
      'box-sizing:border-box!important;grid-column:auto!important;' +
      'overflow:hidden!important;pointer-events:auto!important;}',
    'html.carhud-youtube-pip [data-ttube-four-col="1"] ' +
      'ytd-compact-video-renderer,html.carhud-youtube-pip [data-ttube-four-col="1"] ' +
      'ytm-compact-video-renderer,html.carhud-youtube-pip [data-ttube-four-col="1"] ' +
      'ytm-video-with-context-renderer {' +
      'min-width:0!important;width:100%!important;max-width:100%!important;' +
      'display:flex!important;flex-direction:column!important;}',
    'html.carhud-youtube-pip [data-ttube-four-col="1"] ' +
      'a#thumbnail,html.carhud-youtube-pip [data-ttube-four-col="1"] ' +
      'ytd-thumbnail,html.carhud-youtube-pip [data-ttube-four-col="1"] ' +
      'ytm-thumbnail-cover {' +
      'width:100%!important;max-width:100%!important;' +
      'aspect-ratio:16/9!important;height:auto!important;}',
    'html.carhud-youtube-pip [data-ttube-four-col="1"] ' +
      'h3,html.carhud-youtube-pip [data-ttube-four-col="1"] .video-title {' +
      'font-size:clamp(10px,1.15vw,14px)!important;line-height:1.3!important;' +
      'overflow:hidden!important;display:-webkit-box!important;' +
      '-webkit-line-clamp:2!important;-webkit-box-orient:vertical!important;}',
    // Make suggestions usable even on an otherwise fullscreen watch page.
    'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] {' +
      'overflow-y:auto!important;overflow-x:hidden!important;' +
      'touch-action:pan-y!important;overscroll-behavior-y:contain!important;' +
      '-webkit-overflow-scrolling:touch!important;pointer-events:auto!important;}',
    // Keep the existing video as a floating overlay without moving its node.
    'html.carhud-youtube-pip:not(.carhud-youtube-pip-hidden) ' +
      '[data-carhud-pip-host="1"] {' +
      'right:12px!important;bottom:12px!important;' +
      'width:clamp(156px,25vw,284px)!important;z-index:9900!important;' +
      'pointer-events:auto!important;}'
  ].join('\n');

  function mountStyle() {
    var st = document.getElementById(STYLE_ID);
    if (!st) {
      st = document.createElement('style');
      st.id = STYLE_ID;
      (document.head || document.documentElement).appendChild(st);
    }
    if (st.textContent !== styleText) st.textContent = styleText;
  }

  function isPip() {
    return document.documentElement.classList.contains('carhud-youtube-pip') &&
      location.pathname.indexOf('/watch') === 0;
  }

  function markAdOnlySlots(container) {
    if (!container || !container.querySelectorAll) return;
    var ads = container.querySelectorAll(AD_QUERY);
    // Bound work per frame. CSS still hides every ad renderer instantly.
    var cap = Math.min(ads.length, 80);
    for (var i = 0; i < cap; i++) {
      var ad = ads[i];
      var host = ad.closest(
        'ytd-rich-item-renderer,ytm-rich-item-renderer,' +
        'ytd-compact-video-renderer,ytm-video-with-context-renderer,' +
        'ytd-video-renderer,ytm-compact-video-renderer'
      );
      if (host && !host.hasAttribute('data-ttube-sponsored')) {
        host.setAttribute('data-ttube-sponsored', '1');
      }
    }
  }

  function updateGrid() {
    var previous = document.querySelectorAll('[data-ttube-four-col="1"]');
    if (!isPip()) {
      for (var i = 0; i < previous.length; i++)
        previous[i].removeAttribute('data-ttube-four-col');
      return;
    }
    // The older player helper marks the original related-video pane.
    // If it is not ready yet, request its native layout refresh.
    var pane = document.querySelector('[data-carhud-rec-scroll="1"]');
    if (!pane && typeof window.__carhudUpdateWatchLayout === 'function') {
      try { window.__carhudUpdateWatchLayout(); } catch (_) {}
      pane = document.querySelector('[data-carhud-rec-scroll="1"]');
    }
    if (!pane) return;
    var chosen = null;
    for (var k = 0; k < GRID_CANDIDATES.length; k++) {
      var candidates = pane.querySelectorAll(GRID_CANDIDATES[k]);
      for (var j = 0; j < candidates.length; j++) {
        if (candidates[j].querySelector(CARD_QUERY)) {
          chosen = candidates[j];
          break;
        }
      }
      if (chosen) break;
    }
    if (!chosen) {
      // Fallback only if the pane directly holds multiple real video cards.
      var direct = 0;
      for (var c = pane.firstElementChild; c && direct < 2; c = c.nextElementSibling)
        if (c.matches && c.matches(CARD_QUERY)) direct++;
      if (direct >= 2) chosen = pane;
    }
    for (var m = 0; m < previous.length; m++)
      if (previous[m] !== chosen) previous[m].removeAttribute('data-ttube-four-col');
    if (chosen && !chosen.hasAttribute('data-ttube-four-col'))
      chosen.setAttribute('data-ttube-four-col', '1');
  }

  var timer = 0;
  function refresh() {
    timer = 0;
    mountStyle();
    updateGrid();
    // Prefer the active native feed/related pane: avoid rescanning the
    // entire YouTube document during frequent recommendation updates.
    markAdOnlySlots(target || document);
  }
  function schedule() {
    if (timer) return;
    timer = setTimeout(refresh, 180);
  }
  var observer = null;
  var target = null;
  function watchFeed() {
    var root = document.querySelector(
      'ytd-rich-grid-renderer #contents, ytd-watch-flexy #secondary, ' +
      'ytm-section-list-renderer, ytm-watch .watch-below-the-fold, ' +
      'ytd-search #contents'
    );
    if (root === target) return;
    if (observer) observer.disconnect();
    target = root;
    if (!root) return;
    observer = new MutationObserver(function(mutations) {
      // Ignore style-only updates and debounce YouTube's frequent DOM batches.
      for (var n = 0; n < mutations.length; n++) {
        if (mutations[n].addedNodes && mutations[n].addedNodes.length) {
          schedule();
          break;
        }
      }
    });
    observer.observe(root, {childList:true,subtree:true});
  }
  function sync() {
    watchFeed();
    schedule();
  }
  window.__ttubeFeedPipV2 = {refresh:sync};
  window.addEventListener('yt-navigate-finish', sync, {passive:true});
  window.addEventListener('yt-page-data-updated', sync, {passive:true});
  window.addEventListener('popstate', sync, {passive:true});
  document.addEventListener('ytm-navigate-finish', sync, {passive:true});
  if (document.readyState === 'loading')
    document.addEventListener('DOMContentLoaded', sync, {once:true});
  sync();
  // Cheap recovery for native player pane created after YouTube loads.
  setInterval(function() {
    if (!document.getElementById(STYLE_ID) ||
        (isPip() && !document.querySelector('[data-ttube-four-col="1"]')))
      sync();
  }, 4000);
})();
