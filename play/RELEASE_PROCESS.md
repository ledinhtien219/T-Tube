# T Tube — quy trình phát hành Google Play

Nhánh phát hành Play lâu dài: `release/play`.

## Quy tắc không được đổi

- Package: `com.ledinhtien.ttube`
- Upload/signing key: giữ nguyên upload key hiện tại
- `targetSdk = 36`
- Mỗi bản mới phải có `versionCode` lớn hơn bản đã phát hành trên Google Play
- Không thêm lại `androidx.car.app.ACCESS_SURFACE` hoặc `androidx.car.app.NAVIGATION_TEMPLATES`; Play Console sẽ chặn nếu app không phải app chỉ đường được chấp thuận

## Tạo bản mới

Ví dụ từ 1.0.77 / 273 lên 1.0.78 / 274:

```bash
python3 scripts/bump_play_version.py 1.0.78 274
git diff
git add app/build.gradle.kts play/ scripts/bump_play_version.py
git commit -m "play: bump to 1.0.78 build 274"
git push origin release/play
```

Workflow `.github/workflows/build-play-aab.yml` tự đọc `versionName`, `versionCode` và package từ Gradle, nên không phải sửa workflow theo từng bản nữa.

## Các gate workflow tự kiểm tra

- package đúng `com.ledinhtien.ttube`
- API 36
- release non-debuggable
- Android Auto projection host vẫn tồn tại
- `CATEGORY_PROJECTION` và `CarHudProjectionService` vẫn có trong merged manifest
- hai quyền Navigation bị Play chặn không xuất hiện
- JVM tests
- release lint
- build AAB
- signature verification nếu GitHub Secrets cho upload key đã được cấu hình

## Đưa lên Internal testing

1. Tạo bản phát hành mới trong **Thử nghiệm nội bộ**.
2. Upload AAB mới.
3. Xem trước và xác nhận.
4. **Lưu và xuất bản**.
5. Tester hiện có không cần link mời mới; Google Play sẽ cung cấp bản cập nhật.

## Lưu ý signing

Nếu workflow báo:

`Unsigned validation AAB: configure Play upload secrets before Play Console upload.`

thì artifact GitHub chỉ dùng để kiểm tra build. Cần ký AAB bằng đúng upload key hiện tại trước khi upload Play Console.

Không tạo upload key mới cho mỗi phiên bản.
