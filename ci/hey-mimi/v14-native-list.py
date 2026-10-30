from pathlib import Path
source = Path("app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt")
s = source.read_text(encoding="utf8")
start = s.index("        function findRecommendationRoot() {", s.index("var carhudRecommendationRoot = null;"))
end = s.index("        // Independent native-link chooser", start)
s = s[:start] + """        function findRecommendationRoot() {
            // Pick the YouTube related list with the most actual video links,
            // not a stale one-item #secondary desktop shell.
            var selectors = [
                'ytd-watch-flexy #secondary', 'ytd-watch-grid #secondary',
                'ytd-watch-flexy #related', 'ytd-watch-next-secondary-results-renderer',
                'ytm-watch .watch-below-the-fold', 'ytm-watch ytm-item-section-renderer',
                '.watch-below-the-fold', '#secondary', 'ytd-rich-grid-renderer'
            ];
            var best = null, score = -1;
            for (var i = 0; i < selectors.length; i++) {
                var node = document.querySelector(selectors[i]);
                if (!node || !node.getBoundingClientRect) continue;
                var count = 0;
                try {
                    if (node.querySelectorAll) {
                        var links = node.querySelectorAll('a[href*="/watch"]');
                        var seen = {};
                        for (var j = 0; j < links.length; j++) {
                            var href = links[j].getAttribute('href') || '';
                            if (/\\/watch[?]/.test(href)) seen[href] = true;
                        }
                        count = Object.keys(seen).length;
                    } else if (node.querySelector && node.querySelector('a[href*="/watch"]')) {
                        count = 1;
                    }
                } catch(e) {}
                if (count > score) { best = node; score = count; }
                if (count >= 8) break;
            }
            return best;
        }

""" + s[end:]
start = s.index("        // Independent native-link chooser")
end = s.index("        // In some car touch stacks", start)
s = s[:start] + """        // Pro v14: no shadow-DOM video chooser, search bar or fabricated cards.
        // Render YouTube's own scrollable recommendations with their original links.
        function syncRecommendationPane(enabled) {
            // Clean up a chooser left by a restored older YouTube session.
            var stale = document.getElementById &&
                document.getElementById('carhud-pip-video-chooser');
            if (stale) stale.remove();
            if (!enabled) {
                if (carhudRecommendationRoot)
                    carhudRecommendationRoot.removeAttribute('data-carhud-rec-scroll');
                carhudRecommendationRoot = null;
                return;
            }
            var node = findRecommendationRoot();
            if (carhudRecommendationRoot !== node) {
                if (carhudRecommendationRoot)
                    carhudRecommendationRoot.removeAttribute('data-carhud-rec-scroll');
                carhudRecommendationRoot = node;
                if (node) node.setAttribute('data-carhud-rec-scroll', '1');
            }
        }

""" + s[end:]
old = "html.carhud-youtube-pip body, html.carhud-youtube-pip #app, html.carhud-youtube-pip ytm-app, html.carhud-youtube-pip .page-container, html.carhud-youtube-pip ytm-watch, html.carhud-youtube-pip ytm-player, html.carhud-youtube-pip .player-container, html.carhud-youtube-pip .player-size"
assert old in s
s = s.replace(old, old + ", html.carhud-youtube-pip ytd-app, html.carhud-youtube-pip ytd-watch-flexy, html.carhud-youtube-pip ytd-watch-grid, html.carhud-youtube-pip #page-manager, html.carhud-youtube-pip #columns", 1)
start = s.index("        // Cinema card grid only during Mini PiP:")
end = s.index("        '.video-ads,", start)
s = s[:start] + """        // One native vertical, scrollable list; leave video renderer links clickable.
        'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] ytd-watch-next-secondary-results-renderer #items, html.carhud-youtube-pip [data-carhud-rec-scroll="1"] #related #items, html.carhud-youtube-pip [data-carhud-rec-scroll="1"] ytm-item-section-renderer lazy-list { display:flex!important;flex-direction:column!important;align-items:stretch!important;gap:10px!important;width:100%!important; } ' +
        'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] ytd-compact-video-renderer, html.carhud-youtube-pip [data-carhud-rec-scroll="1"] ytm-compact-video-renderer, html.carhud-youtube-pip [data-carhud-rec-scroll="1"] ytm-video-with-context-renderer { display:block!important;width:100%!important;max-width:100%!important;height:auto!important;margin:0 0 8px!important;padding:8px!important;box-sizing:border-box!important;border-radius:9px!important;background:#161e2a!important;pointer-events:auto!important; } ' +
        'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] ytd-compact-video-renderer #dismissible, html.carhud-youtube-pip [data-carhud-rec-scroll="1"] ytm-compact-video-renderer .compact-media-item, html.carhud-youtube-pip [data-carhud-rec-scroll="1"] ytm-video-with-context-renderer .media-item { display:flex!important;flex-direction:row!important;align-items:flex-start!important;gap:12px!important;min-width:0!important; } ' +
        'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] ytd-thumbnail, html.carhud-youtube-pip [data-carhud-rec-scroll="1"] a#thumbnail, html.carhud-youtube-pip [data-carhud-rec-scroll="1"] .media-item-thumbnail-container { display:block!important;flex:0 0 clamp(135px,25vw,235px)!important;width:clamp(135px,25vw,235px)!important;max-width:clamp(135px,25vw,235px)!important;aspect-ratio:16/9!important;overflow:hidden!important;border-radius:8px!important;pointer-events:auto!important; } ' +
        'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] #details, html.carhud-youtube-pip [data-carhud-rec-scroll="1"] .details { min-width:0!important;flex:1 1 auto!important;max-width:100%!important; } ' +
        'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] a { pointer-events:auto!important;touch-action:manipulation!important; } ' +
        'html.carhud-youtube-pip [data-carhud-rec-scroll="1"] { background:#0c1018!important;padding:12px clamp(195px,35vw,360px) 90px 14px!important; } ' +
        'html.carhud-youtube-pip [data-carhud-rec-scroll="1"]::-webkit-scrollbar { width:7px!important; } ' +
        'html.carhud-youtube-pip [data-carhud-rec-scroll="1"]::-webkit-scrollbar-thumb { background:#607086!important;border-radius:8px!important; } ' +
""" + s[end:]
source.write_text(s, encoding="utf8")
g = Path("app/build.gradle.kts")
b = g.read_text(encoding="utf8")
assert b.count("versionCode = 314") == 1 and b.count('versionName = "1.1.16"') == 1
g.write_text(b.replace("versionCode = 314","versionCode = 315").replace('versionName = "1.1.16"','versionName = "1.1.17"'),encoding="utf8")
# The v9 chooser regression is no longer valid for v14; test the native list instead.
Path("tests/youtube-pip-chooser-pro-regression.test.cjs").write_text(
    "require('./hey-mimi-pro-v14-native-list.test.cjs');\n",encoding="utf8")
print("Mimi Pro v14 native scroll list ready")
