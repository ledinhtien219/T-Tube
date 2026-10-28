# T-Mirror 0.4 — independently written Android Auto mirroring experiment

**Copyright notice:** This implementation is authored independently. It is NOT a fork, derivative, repack, or patch of ScreenOnAuto's distributed APK. We used only publicly documented Android APIs and the existing AAuto SDK binding bundled in T-Tube.

## Current features

- Android 10+ MediaProjection screen capture with explicit permission per session
- Android 14+ one VirtualDisplay per session, resize existing display on captured content changes
- Screen sharing foreground service, persistent notification with Stop button
- Frame scale-to-fit according to available AA host Surface dimensions (no hard-coded head-unit size)
- Two *experimental* CarActivityService entries: T-Mirror / T-Mirror (Legacy)
- Installed-app picker, auto-launch of chosen app after user's capture consent
- Separate package com.tcar.tmirror, so T-Tube stays installed

## Still missing / limitations

- Android Auto host MAY HIDE either or both projection entries, even after Google Play installation.
- Legacy split-pane alongside real Google Maps is NOT GUARANTEED. Android Auto decides where to show an app.
- Optional tap and swipe forwarding is implemented via an opt-in Accessibility Service; privileged screen-off, Shizuku, brightness controls, media session proxy, and multi-touch are not yet implemented.
- Performance is a CPU-backed 10 fps prototype (up to 1280 long edge); later production versions should use hardware-accelerated GPU composition.
- No auto-bypass of MediaProjection consent. Screenshots of protected/DRM content may appear black. No anti-DRM measures.
- Google Play internal testing does NOT override Android Auto supported app category / quality restrictions. Android Auto video mirroring is not an officially supported video-app category for driving.
- Do not use video during driving; test when parked.

## Build

From repo root:

    ./gradlew :tmirror:assembleDebug :tmirror:bundleRelease

Debug APK:

    tmirror/build/outputs/apk/debug/tmirror-debug.apk

Release AAB (unsigned, **not upload-ready yet**):

    tmirror/build/outputs/bundle/release/tmirror-release.aab

The released AAB deliberately has no embedded private key. For your Play Console Internal Testing, generate a secure private upload keystore on YOUR machine, sign the AAB with jarsigner, verify it, and keep the key safe. Do not commit/upload your signing passwords or keystore to GitHub.

Example local signing (replace filenames and alias):

    keytool -genkeypair -v -keystore tmirror-upload.jks -alias tmirror-upload -keyalg RSA -keysize 3072 -validity 10000
    jarsigner -keystore tmirror-upload.jks -signedjar tmirror-play.aab tmirror/build/outputs/bundle/release/tmirror-release.aab tmirror-upload
    jarsigner -verify -verbose -certs tmirror-play.aab

Use signed tmirror-play.aab in Google Play Console Internal testing.

## Test checklist (vehicle parked)

1. Install the standalone test APK on Android 10+ phone; open T-Mirror.
2. Pick T-Tube (or another app) and tap Start capture; grant Android screen sharing.
3. Ensure screen capture notification appears and the selected app opens. Optionally tap **Bật điều khiển chạm** on the phone and manually enable **T-Mirror Touch** in Android Accessibility settings (only when parked).
4. Connect Android Auto. Open Google Maps first.
5. Look for T-Mirror (Legacy) in the car launcher and open it.
6. If the host gives a second pane, you should see the phone's screen next to genuine Google Maps. On small/unsupported screens host may hide Maps or omit the entry altogether.
7. Disconnect, confirm the service stops by tapping Stop. Check screen rotation, live frame pacing and audio.
8. Recheck the app against Android Auto Google Play quality criteria before attempting distribution.

The project relies on the AAuto SDK binary at `../app/libs/aauto.aar`, already bundled in this repo. Before redistributing a Play AAB, verify you have appropriate rights to redistribute that dependency and comply with all Google/Android Auto terms.


## v0.3 Android Auto launcher visibility diagnosis

When `T-Mirror` or `T-Mirror (Legacy)` is absent from Android Auto:

1. Open the T-Mirror phone app and read **Nguồn cài**. If it is not **Google Play Store**, Android Auto may reject the direct APK even when the services are registered.
2. Check the phone **Android Auto → Customize launcher** page. If the entry is not listed there, resizing/scaling fixes cannot help.
3. Check the phone app's `Projection service: Full=1 / Legacy=1` diagnostic. If zero, the installed manifest is not registering the entry correctly; reinstall the newly built APK.
4. To test a trusted install source use a **properly signed** AAB through Google Play Console Internal App Sharing or Internal Testing. Google Play distribution and Android Auto launcher approval are separate requirements; unsupported mirror/video categories may still be rejected.
5. Check on a parked vehicle. The Android Auto host controls whether Legacy actually has a second pane beside **Google Maps**.

**Explicit limitation:** This independent app's experimental `CarActivityService` + `CATEGORY_PROJECTION_OEM` mechanism is not a supported, publicly documented Play distribution API for arbitrary mirroring apps. No manifest combination guarantees appearing in every Android Auto launcher; this build is for compatibility diagnostics, not a claim of support.
