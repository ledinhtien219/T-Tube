# T Tube — Google Play Console checklist

## Build identity
- App name: T Tube
- Package: `com.ledinhtien.ttube`
- Version: `1.0.89`
- Version code: `287`
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

## 1.0.89 internal-test change
- Style #7: 100% giờ là kích thước gốc thật, bỏ auto-shrink; tăng rõ kích thước biển tốc độ, tốc độ, ô rẽ, lane và cảnh báo.
- Fix mũi tên Waze: dùng `trn` HLP/1 làm nguồn chính, không tự fallback `↑` khi maneuver đã hết/không xác định.
- Khi HLP gửi `nav=0`, `dst=-1`, `st2=""` hoặc lane rỗng, app xóa state dẫn đường cũ thay vì giữ lại.
- Sửa kích thước #6/#7 theo viewport Android Auto thật, không còn phóng theo DPI điện thoại.
- #6 neo góc trên phải theo tỷ lệ ảnh mẫu; #7 neo sát mép trên bên trái như ảnh mẫu.
- Reset vị trí lưu cũ của #6/#7 để bản cập nhật không giữ tọa độ sai từ build trước.
- Waze HUD photo v2: scale #6/#7 theo ảnh test thực tế trên màn hình xe.
- Tách vị trí #6/#7 khỏi vị trí HUD cũ và reset sang vị trí mặc định mới khi cập nhật.
- Giữ kích thước các ô lane/cảnh báo ổn định khi Waze chưa có dữ liệu, tránh HUD co thành thanh sai bố cục.
- Làm lại HUD #6/#7 bám sát 2 ảnh mẫu người dùng cung cấp.
- #6: biển giới hạn lớn + tốc độ lớn + ô rẽ/đường/làn + nhiều cảnh báo xếp dọc + nút Báo cáo.
- #7: thanh ngang thấp một hàng với giới hạn, tốc độ, rẽ, lane guidance, nhiều cảnh báo và nút Báo cáo nhỏ.
- Bỏ nút khóa trực tiếp khỏi #6/#7 để đúng mẫu; khóa vị trí vẫn chỉnh được trong Cài đặt HUD.
- Đồng bộ nhánh `feature/waze-hud-reference-layouts` vào Play build.
- Thêm HUD #6 **Waze Panel Lớn**: biển giới hạn + tốc độ lớn + hướng rẽ + cảnh báo xếp bên phải.
- Thêm HUD #7 **Waze Thanh Ngang**: thanh thấp một hàng cho màn hình Android Auto hẹp.
- TikTok Google sign-in: capture the Google button/popup before OAuth opens and redirect to TikTok's official QR login page so the authenticated TikTok cookies stay in the same WebView.
- Add an always-visible TikTok login helper button for QR / alternate login methods; QR pages are not scaled by feed CSS.
- Disable T Tube universal theme/CSS injection on TikTok, VTVgo and TV360 pages to avoid black auth/player surfaces.
- Keep all 1.0.83 and earlier Maps/audio/auto-next fixes.
- TikTok: block unsupported Google OAuth inside Android WebView before navigation, preventing the black-screen/disallowed-useragent path.
- Return to TikTok's normal login page and tell the user to use phone/email/QR, which can keep a WebView session.
- Keep 1.0.82 VTVgo/TV360 compatibility, 1.0.81 audio handoff + YouTube auto-next, and 1.0.80 Maps handoff.
- TikTok login: preserve the real login/signup UI, use a browser-like mobile UA on auth routes, and keep Google/Apple/Facebook/X auth redirects in the TikTok session.
- VTVgo / TV360: treat service subdomains as the same app, persist first/third-party cookies, use browser-like WebView identity, and keep login popup navigation in the same WebView.
- YouTube request blocking is scoped to YouTube only, so it cannot block TikTok/VTVgo/TV360 auth, CDN or playback requests.
- Keeps the 1.0.81 external-audio handoff + YouTube auto-next and the 1.0.80 Google Maps handoff fixes.
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
Upload the signed bundle `TTube_v1.0.89_build287_WazeTurnScaleFix_signed.aab`.
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
