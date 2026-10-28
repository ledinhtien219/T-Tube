const fs = require('node:fs');
const assert = require('node:assert/strict');

const kotlin = fs.readFileSync('app/src/main/java/com/carhud/aaproxy/YouTubePlayerHelper.kt', 'utf8');
const match = kotlin.match(/val JS_CLEANUP_AND_ADBLOCK: String = """([\s\S]*?)"""\.trimIndent\(\)/);
assert.ok(match, 'Missing injected YouTube JavaScript');
const js = match[1];
new Function(js);

assert.match(js, /html\.carhud-youtube-pip \[data-carhud-rec-scroll="1"\] \{ position:fixed!important/);
assert.match(js, /width:100vw!important;max-width:100vw!important/);
assert.match(js, /overflow-y:auto!important/);
assert.match(js, /padding:18px 16px 108px max\(110px,8vw\)/);
assert.match(js, /pointer-events:auto!important/);
assert.match(js, /html\.carhud-youtube-pip \[data-carhud-rec-scroll="1"\] #items/);
assert.match(js, /function findRecommendationRoot\(\)/);
assert.match(js, /ytd-watch-flexy #secondary/);
assert.match(js, /ytd-watch-grid #secondary/);
assert.match(js, /ytm-watch \.watch-below-the-fold/);
assert.match(js, /function syncRecommendationPane\(enabled\)/);
assert.match(js, /syncRecommendationPane\(pip\)/);
assert.match(js, /carhudRecommendationRoot\.removeAttribute\('data-carhud-rec-scroll'\)/);

const first = js.indexOf('var carhudRecommendationRoot = null;');
const last = js.indexOf('function updateWatchLayout()', first);
assert.ok(first > -1 && last > first, 'Cannot isolate recommendation scroll logic');
const section = js.slice(first, last);

function setup(selector) {
    const listeners = {};
    const card = { closest() { return null; } };
    const pane = {
        attributes: {},
        isConnected: true,
        scrollHeight: 2600,
        clientHeight: 540,
        _top: 0,
        get scrollTop() { return this._top; },
        set scrollTop(v) { this._top = Math.max(0, Math.min(v, this.scrollHeight - this.clientHeight)); },
        getBoundingClientRect() { return { top: 80, left: 850, width: 430, height: 560 }; },
        querySelector(q) { return q.includes('watch') ? card : null; },
        contains(t) { return t === card; },
        setAttribute(k, v) { this.attributes[k] = v; },
        removeAttribute(k) { delete this.attributes[k]; }
    };
    const document = {
        querySelector(q) { return q === selector ? pane : null; },
        addEventListener(type, fn) { (listeners[type] ||= []).push(fn); }
    };
    const window = {
        __carhudPipMode: true,
        __carhudMiniPlayerHost: { contains() { return false; } }
    };
    const api = new Function('document', 'window',
        section + '\nreturn {findRecommendationRoot, syncRecommendationPane};'
    )(document, window);
    return { pane, card, listeners, api, window };
}

for (const selector of [
    'ytd-watch-flexy #secondary',
    'ytm-watch .watch-below-the-fold'
]) {
    const o = setup(selector);
    assert.equal(o.api.findRecommendationRoot(), o.pane);
    o.api.syncRecommendationPane(true);
    assert.equal(o.pane.attributes['data-carhud-rec-scroll'], '1');

    let prevented = false;
    const blank = { closest() { return null; } };
    o.listeners.touchstart[0]({
        touches: [{ clientX: 250, clientY: 490 }],
        target: blank
    });
    o.listeners.touchmove[0]({
        touches: [{ clientX: 250, clientY: 265 }],
        cancelable: true, preventDefault() { prevented = true; }
    });
    assert.equal(o.pane.scrollTop, 225, 'Swipe on noninteractive blank area must scroll recommendations');
    assert.equal(prevented, true);

    // Inside the native related list, don't hijack the original YouTube
    // touch handling. The browser will scroll and allow link clicks.
    o.listeners.touchend[0]();
    prevented = false;
    o.listeners.touchstart[0]({
        touches: [{ clientX: 1100, clientY: 340 }],
        target: o.card
    });
    o.listeners.touchmove[0]({
        touches: [{ clientX: 1100, clientY: 120 }],
        cancelable: true, preventDefault() { prevented = true; }
    });
    assert.equal(prevented, false, 'Native card gestures must remain unmodified');

    o.api.syncRecommendationPane(false);
    assert.equal(o.pane.attributes['data-carhud-rec-scroll'], undefined);
}

console.log('PASS: desktop/mobile scroll panes, blank-area swipe fallback, native card touch, cleanup');
