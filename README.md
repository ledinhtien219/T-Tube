# 🚘 T Tube - Android Auto Multimedia Dashboard & Waze/Vietmap HUD

[![Build APK](https://github.com/ledinhtien219/T-Tube/actions/workflows/build-apk.yml/badge.svg)](https://github.com/ledinhtien219/T-Tube/actions/workflows/build-apk.yml)
[![GitHub Release](https://img.shields.io/github/v/release/ledinhtien219/T-Tube?color=blue)](https://github.com/ledinhtien219/T-Tube/releases)
[![Android SDK](https://img.shields.io/badge/Android%20SDK-29%2B-brightgreen)](https://developer.android.com)

**T Tube** là ứng dụng dashboard đa phương tiện dành cho **Android Auto / màn hình xe**, kết hợp YouTube, IPTV, trình duyệt WebApp, điều khiển giọng nói và HUD cảnh báo giao thông từ Waze/Vietmap.

## ✅ Bản Google Play trên nhánh này

- **Play candidate:** `v1.0.88` / Build `286`
- **Branch:** `release/play`
- **Play Application ID:** `com.ledinhtien.ttube`
- **GitHub stable Application ID:** `com.tcar.auto` (package riêng)
- **Min SDK:** Android 10 / API 29
- **Target / Compile SDK:** API 36
- **Android Auto internal test:** full projection + media; restricted navigation permissions are not requested
- **Release:** non-debuggable, Android App Bundle (`.aab`)

> Package Google Play được tách khỏi package stable GitHub để tránh xung đột package đã tồn tại và để hai bản có thể cài song song.
>
> Play candidate 1.0.84 giữ **YouTube screen modes** ngay trong nút bánh răng: Tràn viền, Chuẩn 16:9, Phóng to/cắt viền, 4:3 và 21:9.
>
> 1.0.80 cũng sửa **Google Maps handoff**: khi chuyển khỏi T Tube, projection input và WebView nền được nhả/suspend để Maps nhận touch mượt hơn.
>
> 1.0.81 bổ sung **external audio handoff** cho SoundCloud/Spotify/Zing và **YouTube auto-next** khi video kết thúc.
>
> 1.0.82 bổ sung **TikTok/VTVgo/TV360 login compatibility**: login-route riêng, cookie/session subdomain và request filtering tách biệt khỏi YouTube.
>
> 1.0.83 chặn **TikTok Google OAuth black-screen** trong WebView và trả người dùng về login TikTok an toàn.
>
> 1.0.84 bổ sung **TikTok QR fallback hardening**: bắt click/popup Google ngay trong DOM, chuyển sang QR chính thức của TikTok, giữ cookie cùng WebView và tách hoàn toàn theme T Tube khỏi TikTok/VTVgo/TV360.
>
> 1.0.85 bổ sung **2 giao diện Waze HUD mới** từ nhánh `feature/waze-hud-reference-layouts`: #6 Waze Panel Lớn và #7 Waze Thanh Ngang.
>
> 1.0.86 tinh chỉnh #6/#7 theo đúng ảnh tham chiếu: lane guidance, nhiều cảnh báo, tỷ lệ panel và nút Báo cáo.
>
> 1.0.88 phóng lớn #7 ở mức 100% và sửa mapping/clear-state mũi tên Waze theo HLP/1.
>
> 1.0.88 chuẩn hóa kích thước HUD theo viewport Android Auto thật và reset vị trí cũ của #6/#7 để bám đúng ảnh mẫu.

---

## 🆕 Điểm mới trong v1.0.74

### 🔀 Chuyển app ổn định hơn

- Mỗi WebApp có **ownership** rõ ràng.
- Bấm app nào thì app đó nhận quyền điều khiển WebView và media.
- Không restore history của app cũ sang app mới.
- Không để callback trễ của YouTube/TikTok/IPTV/Browser giành lại màn hình sau khi đã chuyển app.
- IPTV luôn mở đúng `iptv_player.html`, không bị rơi sang YouTube/TikTok.
- Khi chuyển app, trang cũ được dừng và history cũ được làm sạch trước khi nạp app mới.
- Lớp chuyển tiếp dùng nền trung tính, không giữ ảnh app cũ nên không còn hiện chồng nội dung.

### ⌨️ Tìm kiếm nhập tay

- Keyboard/search session được gắn với đúng app đã mở.
- Nếu đang nhập rồi chuyển app, thao tác cũ sẽ bị hủy thay vì chạy trên app mới.
- YouTube search đi đúng YouTube.
- TikTok search đi đúng TikTok.
- Browser/address input không bị kéo sang YouTube.
- Hỗ trợ bàn phím Android Auto và bộ gõ tiếng Việt.

### 🎙️ Tìm kiếm bằng giọng nói

- Voice recognition được gắn với app owner tại thời điểm bắt đầu.
- Chuyển app sẽ hủy phiên voice cũ.
- Mở bàn phím nhập tay cũng hủy mic đang chờ để tránh kết quả giọng nói ghi đè.
- Lệnh phát nhạc YouTube và mở kênh IPTV giữ đúng ý định phát sau khi chuyển app.
- Kết quả voice YouTube chỉ auto-play sau khi trang kết quả mới đã render.

### 🎛️ Thanh điều khiển

- Sửa race giữa animation ẩn và thao tác chạm màn hình.
- Chạm màn hình sẽ hủy callback/animation ẩn cũ trước khi hiện toolbar.
- Ép toolbar về `VISIBLE + alpha=1` để tránh trạng thái “bấm không hiện, một lúc sau mới hiện”.
- Bắt touch cả trên lớp video fullscreen.

### 🔊 Audio coexistence

- T Tube không cố giữ/giành lại audio khi không thực sự phát.
- Giảm xung đột với các app nghe nhạc ngoài như Zing MP3, SoundCloud...
- Media callback của app cũ không được điều khiển app mới sau khi chuyển.

### 🌡️ Giảm tải / giảm nóng

- Dashboard video/GIF wallpaper dừng khi bị app fullscreen che.
- Clock/date/greeting chỉ redraw khi nội dung thực sự thay đổi.
- Marquee được giới hạn khoảng 30 fps và nghỉ giữa các vòng chạy.
- Bỏ `WebView.saveState()` nặng khỏi đường chuyển app khi không còn dùng cross-app restore.

### 🧹 Dashboard / Settings

- Bỏ ô **Ứng dụng** thừa khỏi Dashboard.
- Bỏ một số WebApp mặc định không cần thiết.
- App ghim trên Dashboard dùng kích thước adaptive và khoảng cách theo số lượng app để giảm đè/chồng.
- Dọn các setting/key không còn sử dụng.

---

## 📸 Giao diện T Tube v1.0.74

### 🏎️ Android Auto / màn hình xe

<div align="center">

<img src="docs/images/ttube_aa_dashboard.jpg" width="760" alt="T Tube v1.0.74 Android Auto Dashboard" />

*Dashboard T Tube v1.0.74: đồng hồ, lịch âm, thời tiết, HUD tốc độ, tìm kiếm, YouTube, IPTV, Trình duyệt và mini player.*

</div>

### 📱 Cài đặt trên điện thoại

<div align="center">

<img src="docs/images/ttube_phone_settings.jpg" width="420" alt="T Tube v1.0.74 phone dashboard pin settings" />

*Quản lý ứng dụng hiển thị trên Dashboard Android Auto: ghim/bỏ ghim YouTube Pro, IPTV Truyền hình và Trình duyệt Web.*

</div>

> Các ảnh trên là giao diện **T Tube v1.0.74** hiện tại. Phần ảnh THTV cũ đã được loại khỏi README để tránh nhầm với phiên bản hiện hành.

---

## 🌟 Tính năng nổi bật

### 📺 YouTube trên Android Auto

- Chặn nhiều nguồn quảng cáo và hỗ trợ SponsorBlock.
- Tìm kiếm bằng bàn phím hoặc giọng nói tiếng Việt.
- Hỗ trợ Phát/Dừng, Tiếp theo, seek và chọn chất lượng.
- Tối ưu giao diện mobile YouTube cho màn hình Android Auto.
- Callback media/search được khóa theo app owner để tránh lẫn trạng thái khi chuyển app.

### 📺 IPTV / M3U

- Phát danh sách M3U/M3U8 và HLS.
- Hỗ trợ chọn nhanh kênh từ Dashboard.
- Hỗ trợ điều chỉnh tỷ lệ video.
- Voice query có thể tìm/mở kênh IPTV.
- IPTV dùng local player riêng và không restore WebView history của app trước.

### 🌐 Trình duyệt & WebApp

- WebView dùng chung cho trải nghiệm trên màn hình xe nhưng routing được quản lý theo **app ownership**.
- App Grid cho phép mở WebApp tùy chỉnh và thêm URL mới.
- Hỗ trợ Desktop User-Agent cho WebApp cần giao diện máy tính.
- Generic Browser, YouTube, TikTok và IPTV được phân tách logic tìm kiếm/điều hướng.

### 🛡️ Waze / Vietmap HUD

- Nhận cảnh báo Waze HLP qua WebSocket thời gian thực.
- Fallback từ notification/broadcast khi HLP không có dữ liệu phù hợp.
- Priority nguồn dữ liệu: **HLP > broadcast > notification > GPS**.
- Harden reconnect, timestamp rollback, TTL stale alert, cảnh báo trùng và notification removal.
- HUD trên Android Auto không có nút đóng trực tiếp; bật/tắt từ app điện thoại.
- Hỗ trợ khóa vị trí HUD.

### 🎙️ Giọng nói & phím vô lăng

- Tìm kiếm giọng nói tiếng Việt.
- Điều khiển MediaSession / Next / Play-Pause.
- Bộ gõ tiếng Việt trên màn hình xe.
- Voice query có thể mở YouTube hoặc kênh IPTV phù hợp.
- Phiên voice cũ bị hủy khi đổi app để tránh chạy nhầm context.

### 🎛️ Dashboard buồng lái

- Đồng hồ lớn, ngày dương + âm lịch Việt Nam.
- Weather card với vector weather icon.
- Wallpaper tĩnh / ảnh tùy chỉnh / GIF / video.
- Chế độ ngày / đêm / tự động.
- Mini player điều khiển media ngay trên Dashboard.
- Hỗ trợ ghim WebApp ra Dashboard.

### 🧩 Ổn định & chẩn đoán

- `AppCrashHandler.kt` lưu crash report theo version/build.
- Stable signing giữ cùng certificate trong CI để hỗ trợ cài đè.
- Transition generation ngăn callback của app trước tác động lên app mới.
- Release stable chỉ được tạo sau khi regression, JVM, AndroidTest, signing check và emulator smoke đều đạt.

---

## 📂 Cấu trúc chính

```text
T-Tube/
├── .github/workflows/
│   ├── build-apk.yml
│   └── publish-v174-stable.yml
├── app/
│   ├── build.gradle.kts               # v1.0.74 / Build 269
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       ├── java/com/carhud/aaproxy/
│       │   ├── MainActivity.kt
│       │   ├── SettingsActivity.kt
│       │   ├── CarPresentation.kt
│       │   ├── CarDashboardView.kt
│       │   ├── CarHudProjectionHost.kt
│       │   ├── CarMediaBrowserService.kt
│       │   ├── CarMediaManager.kt
│       │   ├── WebAppModel.kt
│       │   ├── YouTubePlayerHelper.kt
│       │   ├── YouTubeAdBlocker.kt
│       │   ├── SponsorBlockManager.kt
│       │   ├── VoiceSearchManager.kt
│       │   ├── VoiceQueryResolver.kt
│       │   ├── WazeHlpWebSocketManager.kt
│       │   ├── WazeAlertPolicy.kt
│       │   ├── WazeHudManager.kt
│       │   ├── VietmapHudOverlay.kt
│       │   ├── WeatherManager.kt
│       │   ├── VietnameseLunarHelper.kt
│       │   ├── GpsSpeedManager.kt
│       │   └── AppCrashHandler.kt
│       └── res/
├── tests/                              # Static regression tests
├── docs/images/
├── update.json                         # Stable OTA manifest
└── settings.gradle.kts
```

---

## 🛠️ Build APK

Yêu cầu: **JDK 17**.

### Debug

```bash
./gradlew testDebugUnitTest assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/TTube_v1.0.74_debug.apk
```

### Release

```bash
./gradlew assembleRelease
```

APK:

```text
app/build/outputs/apk/release/TTube_v1.0.74_release.apk
```

### Cài đè qua ADB

```bash
adb install -r app/build/outputs/apk/debug/TTube_v1.0.74_debug.apk
```

> Để cài đè thành công, APK cũ và APK mới phải có cùng `applicationId`, cùng signing certificate và bản mới phải có `versionCode` cao hơn hoặc bằng điều kiện update của Android. Stable v1.0.74 dùng `com.tcar.auto` và build `269`.

---

## 🧪 CI & Stable Gate

### Build / regression

Các gate chính kiểm tra:

- static regression tests trong `tests/*.cjs`,
- JVM unit tests,
- `assembleDebug`,
- `assembleDebugAndroidTest`,
- certificate/signing compatibility,
- Android emulator runtime tests.

### Stable v1.0.74

Workflow stable kiểm tra trước khi publish:

1. đúng `versionName = 1.0.74` và `versionCode = 269`;
2. cùng stable signing certificate với bản public trước;
3. toàn bộ static regression + JVM tests;
4. build APK + AndroidTest;
5. runtime smoke cho app switching / projection density;
6. cable disconnect watchdog;
7. TikTok runtime smoke;
8. tạo immutable GitHub Release;
9. cập nhật `update.json` trên `main`.

Nếu bất kỳ gate nào fail thì release stable không được publish.

---

## 📦 Download / Release

- **Stable Release:** https://github.com/ledinhtien219/T-Tube/releases/tag/v1.0.74
- **APK:** https://github.com/ledinhtien219/T-Tube/releases/download/v1.0.74/TTube_v1.0.74_debug.apk
- **Source ZIP:** https://github.com/ledinhtien219/T-Tube/releases/download/v1.0.74/TTube_v1.0.74_source.zip
- **SHA256SUMS:** https://github.com/ledinhtien219/T-Tube/releases/download/v1.0.74/SHA256SUMS.txt
- **Stable OTA manifest:** `update.json`

---

## ⚠️ Lưu ý

- Khả năng tương thích có thể khác nhau giữa từng điện thoại, phiên bản Android Auto và head unit.
- Một số ROM/OEM có thể áp dụng thêm giới hạn với app projection hoặc app cài ngoài.
- Không nên thay signing key nếu muốn người dùng tiếp tục cài đè bản stable cũ.

---

## 📄 Bản quyền

Dự án được xây dựng và tối ưu cho hệ sinh thái Android / Android Auto trên màn hình ô tô. Mọi quyền được bảo lưu.
