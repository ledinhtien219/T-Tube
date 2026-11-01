# Brave Shields experimental integration (T-Tube)

Branch: `experiment/brave-shields-youtube-rust`. This is NOT the Play release branch.

## What is integrated

- The actual open-source Brave `adblock-rust` engine via the community Android JNI wrapper `io.github.mlm-games:adblock:0.1.3` (MPL-2.0). Source: https://github.com/mlm-games/adblock-rs-kt-wrapper and https://github.com/brave/adblock-rust
- Rule sources: EasyList, uBlock Origin filters, quick fixes and unbreak; downloaded over HTTPS, limited in size, cached in the app cache and refreshed at most once every 72 hours per app process. An offline baseline covers known dedicated ad hosts.
- Rule parsing and refresh occur on one background worker. Request checks use the precompiled native engine only; a mutex protects replacement.
- Early cosmetic scriptlets run only on YouTube origins through AndroidX WebKit document-start support. Existing auto-Skip and SponsorBlock remain fallback layers.
- Fail-open behavior: if native library, network, filter download or scriptlet support fails, player resources remain available.

## Safety/performance limits

- No OS-wide DNS, VPN, certificate interception or traffic redirection.
- Only applies when the shared WebView displays a YouTube document. Main-frame navigation, non-GET calls, video media, googlevideo.com, youtubei player/next/browse/search and embedded media playlists always bypass the new network blocker.
- Java 21 is required for the JNI wrapper bytecode on this experimental CI; main app code still targets Java 17. The Rust library brings native ABIs to the AAB and must pass an actual car/device smoke test before promotion.
- Android WebView is not Brave's Chromium fork. Filter coverage and scriptlet execution cannot be guaranteed identical to Brave; server-side or unskippable ads may persist.
- A rebuild/CI success proves compilation and limited emulator behavior, not YouTube ad-free playback on the car.

## Device acceptance tests before promotion

1. On a parked vehicle, run ten different YouTube videos (including multiple mid-roll ads) and log observed ads and playback interruptions.
2. Check pause/resume, navigation between videos, music audio, Bluetooth/Android Auto routing and SponsorBlock transitions.
3. Check 4×4 hidden PiP video chooser, swipe scrolling and thermal behavior over a long session.
4. Repeat with the internet offline after rules have been cached; repeat with unsupported WebView document-start capabilities.
5. If any black frames, missing video/audio or stalled navigation appear, keep this branch experimental and roll back the JNI integration.

No Play upload key or credentials are stored in this repository.
