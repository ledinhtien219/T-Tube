# T Tube — Google Play Console checklist

## Build identity
- App name: T Tube
- Package: `com.ledinhtien.ttube`
- Version: `1.0.76`
- Version code: `272`
- minSdk: 29
- targetSdk / compileSdk: 36
- Artifact: Android App Bundle (`.aab`)
- Release build: non-debuggable
- Play Console package field: use exactly `com.ledinhtien.ttube`
- This Play package is intentionally separate from the GitHub stable package `com.tcar.auto`.

## Android Auto declaration
The Play build declares **Media only**:
```xml
<automotiveApp>
    <uses name="media" />
</automotiveApp>
```
The private projection host, CATEGORY_PROJECTION, ACCESS_SURFACE and NAVIGATION_TEMPLATES are removed from the Play branch.

## App access
Select **All functionality is available without special access** for the Play build.
The Play release bypasses the external T Tube activation/license flow.

## Ads
Choose **No** unless ads are added later.

## Target audience
Recommended starting point: adults / 18+ because the phone app can open third-party web/video content. Complete the questionnaire based on the final listing and content.

## Content rating
Answer from the actual Play build. Web content is third-party and may vary.

## Data safety
Use `play/DATA_SAFETY_DRAFT.md` as a checklist, then confirm against the final AAB before submitting.

## Privacy policy
Use the published public policy URL:\n`https://github.com/ledinhtien219/T-Tube/blob/main/PRIVACY_POLICY.md`\n\nA source copy is also kept in `docs/privacy-policy-play.md`.

## Testing track
1. Internal testing
2. Closed testing if required for the developer account
3. Production only after Play Console grants production access

## Release
Upload the signed bundle `ttube-1.0.76-272-play.aab`.
Do not upload the GitHub/full APK to Play Console.


## Upload key already prepared
- Alias: `ttube-play-upload`
- Public certificate: `play/upload_certificate.pem`
- SHA-256: `2E:F9:93:23:53:DB:CA:EA:27:41:24:68:2D:51:23:24:9A:10:E5:87:BD:A0:6D:62:BB:D2:35:FF:E0:EA:21:50`
- The private keystore/passwords are **not** stored in Git.
