# T Tube — Google Play Console checklist

## Build identity
- App name: T Tube
- Package: `com.ledinhtien.ttube`
- Version: `1.0.81`
- Version code: `277`
- minSdk: 29
- targetSdk / compileSdk: 36
- Artifact: Android App Bundle (`.aab`)
- Release build: non-debuggable
- Play Console package field: use exactly `com.ledinhtien.ttube`
- This Play package is intentionally separate from the GitHub stable package `com.tcar.auto`.

## Android Auto declaration — INTERNAL TEST FULL BUILD
This internal-test build restores the full Android Auto host used by the previous T Tube build:
- MediaBrowser/MediaSession
- private projection host (`CarHudProjectionService`)
- `CATEGORY_PROJECTION`
- dashboard / WebView / HUD projection path
- does **not** request `androidx.car.app.ACCESS_SURFACE` or `androidx.car.app.NAVIGATION_TEMPLATES` because Google Play restricts those to navigation apps

This build is intended for **Internal testing**. Before Production submission, re-check current Google Play / Android Auto policy because the private projection path is not the same as a conventional Play-approved media-only app.

## 1.0.81 internal-test change
- External audio handoff: when T Tube is backgrounded and SoundCloud/Spotify/Zing takes audio, T Tube no longer fights the focus or auto-resumes.
- Returning to T Tube does not steal audio back; playback resumes only after an explicit Play/user selection.
- YouTube auto-next: when a video truly ends, T Tube advances to the next video with player API/button/recommendation fallbacks and bounded retries.
- Fix Google Maps handoff: T Tube releases projection touch/input when it loses foreground.
- Phone/background WebViews are suspended instead of being forcibly resumed behind Maps.
- Projection surface teardown no longer forces Chromium timers or audio focus back on while Maps is active.
- YouTube gear menu now includes screen modes: fill, 16:9 contain, cover/crop, 4:3 and 21:9.
- Selected screen mode is persisted and restored on later YouTube playback.
- Video quality selection remains in the same gear menu.

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
Upload the signed bundle `TTube_v1.0.81_build277_FullAA_Internal_signed.aab`.
Do not upload the GitHub/full APK to Play Console.

### Không giữ bundle cũ trong cùng bản phát hành
- Chỉ để **versionCode mới nhất** trong bản phát hành đang tạo.
- Nếu Play Console hiện mục **Gói ứng dụng được giữ lại / Retained app bundles**, hãy xóa các versionCode cũ khỏi bản phát hành hiện tại trước khi **Xem trước và xác nhận**.
- Không dùng **Thêm từ thư viện** để kéo lại AAB cũ khi đang tạo bản cập nhật mới.
- Nếu Play báo kiểu: “APK này sẽ không được phân phối ... vì APK khác có mã phiên bản cao hơn đã thay thế hoàn toàn”, nghĩa là bundle cũ đang bị giữ chung với bundle mới; xóa bundle cũ khỏi draft rồi tiếp tục.


## Upload key already prepared
- Alias: `ttube-play-upload`
- Public certificate: `play/upload_certificate.pem`
- SHA-256: `2E:F9:93:23:53:DB:CA:EA:27:41:24:68:2D:51:23:24:9A:10:E5:87:BD:A0:6D:62:BB:D2:35:FF:E0:EA:21:50`
- The private keystore/passwords are **not** stored in Git.
