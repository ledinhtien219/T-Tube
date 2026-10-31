# T-Tube Hey Mimi Pro — 1.1.21 (Build 319)

**Status: signing blocked. Do not distribute the AAB as a signed update until the signing workflow is green.**

- Build source: `release/hey-mimi-pro-v121-build319`
- Version **1.1.21**, code **319** are applied **after** the complete v3–v18 Hey Mimi patch chain. The unpatched base `app/build.gradle.kts` intentionally remains 1.1.9 to support those patches.
- Build workflow: `.github/workflows/build-hey-mimi-pro-v121-signed.yml`.
- Previous regression CI: https://github.com/ledinhtien219/T-Tube/actions/runs/41165590205 (passed). The v1.1.21 workflow must run and pass independently.
- Scope: personalized wallpaper recovery after reverse camera; YouTube preroll/audio hardening; 4-row scrollable hidden PiP; Google Maps navigation state while notifications change. These still require in-car testing and cannot defeat Android locked-screen app-launch policies.

## Blocking prerequisite: registered Play upload key

Set **five repository secrets**, on GitHub Settings → Secrets and variables → Actions. The values must come from the **existing Play Console upload certificate**, not a newly generated debug key:

- `PLAY_UPLOAD_KEYSTORE_BASE64` — Base64 of the existing upload keystore (private).
- `PLAY_UPLOAD_STORE_PASSWORD` — Keystore password.
- `PLAY_UPLOAD_KEY_ALIAS` — Existing upload key alias.
- `PLAY_UPLOAD_KEY_PASSWORD` — Upload key password.
- `PLAY_UPLOAD_CERT_SHA256` — Expected SHA-256 fingerprint shown for the **upload key certificate** in Play Console.

Never post keys, passwords, keystore content or its Base64 in issues, commits or ChatGPT.

Once those secrets are available to this branch, use Actions → **Hey Mimi Pro 1.1.21 Build 319 - SIGNED AAB** → Run workflow → branch `release/hey-mimi-pro-v121-build319`.

The workflow **fails closed** if secrets are missing, the certificate mismatches or AAB/APK signature verification fails. A successful run uploads the artifact `TTube-HeyMimi-Pro-1.1.21-build319-SIGNED-AAB-APK` containing:
- `TTube-HeyMimi-Pro-1.1.21-build319-SIGNED-Play.aab`
- `TTube-HeyMimi-Pro-1.1.21-build319-SIGNED-upload-key.apk`
- `SHA256SUMS.txt`
- `SIGNING_STATUS.txt`

**AAB cannot be installed directly on Android phones**. Use Google Play to distribute the signed AAB, or test an APK whose certificate matches the APK already installed. The Play **upload certificate** may differ from the **app signing certificate** used to sign Play-delivered APKs, so the APK produced by this workflow may not install over an existing Play-installed app. Back up data and check signatures before changing installs.
