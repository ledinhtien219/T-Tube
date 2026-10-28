# Native Google Maps + T-Tube: compatibility test (experimental)

**Goal:** Keep the **real Google Maps app on Android Auto** as the navigation provider while showing YouTube in the other pane, when the Android Auto host supports that layout. No Leaflet maps, no map capture, no changes to the Google Maps APK.

## What this build does

1. Adds a separate **T Tube Mirror (Test)** launcher activity **on the phone** with a lightweight YouTube WebView, fullscreen video support, and Back/Home/Reload/Close controls.
2. Works with an independent **ScreenOnAuto (Legacy)** mirror on a head unit **that actually offers a map + secondary pane**. ScreenOnAuto handles capture and touch forwarding; T-Tube does not request a screen recording permission.
3. Re-measures T-Tube's existing Android Auto projection when its actual host viewport changes, improving responsiveness during dashboard size changes.
4. Preserves the existing projection and Google Maps navigation path.

**This is NOT a native in-app Google Maps embedding API.** The host controls the pane sizes. On small or unsupported head units, opening Legacy mirroring may put Maps in the background; the Android Auto host may not give any app a second interactive pane. No software patch to T-Tube alone can guarantee split-screen on every head unit.

## Phone + car test procedure (parked vehicle)

1. Install the test APK from the GitHub Actions artifact on this branch. It uses the same package name and the repository's existing stable debug signer.
2. On the phone, confirm the new **T Tube Mirror (Test)** launcher icon appears. Open it, load a YouTube page, and check that video / Back / Home / Reload work.
3. Install and configure ScreenOnAuto according to its official documentation: https://screenonauto.lzn.idv.tw/docs/en/how-to-use/
4. Connect Android Auto and open **Google Maps** on the car first. Start an example route while parked.
5. Choose **ScreenOnAuto (Legacy)** from Android Auto's launcher. Open **T Tube Mirror (Test)** on the phone if not already open. The Legacy mirroring app should show the phone YouTube view in its pane **if the host supports it**.
6. Check: Google Maps remains native, turn-by-turn controls work; YouTube pane fits; YouTube fullscreen and landscape reflow; navigation audio stays audible.
7. Switch to existing **T Tube** Android Auto projection and back. Confirm no regressions.

## Compatibility observations

Record:
- Head-unit make/model, screen resolution and Android Auto version
- Android version and WebView version
- Whether Android Auto dashboard presents two panes with Legacy mirror
- Any black screen on Google Maps (Android 15+ capture protections may apply)
- Touch responsiveness, YouTube sign-in, audio focus, portrait/landscape changes

**Important:** Only test video when safely parked. Android Auto's supported media experience while driving focuses on audio controls. Do not disable sensitive-screen-capture protections merely to fix a black Maps pane.

## Recovery

The test uses the same package ID as the existing T-Tube, so installing over the previous build requires the same signing certificate; the experimental workflow refuses to build if that key is missing. To revert, reinstall your previously working T-Tube APK signed with the same certificate.

This branch does not change the OTA manifest, production version number, or stable release workflow.
