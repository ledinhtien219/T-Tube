# Play upload key setup

Không commit file `.jks`, password hoặc private key vào Git.

## 1. Tạo upload key trên máy cá nhân
```bash
keytool -genkeypair \
  -keystore ttube-play-upload.jks \
  -alias ttube-play-upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Lưu file này ở nơi an toàn và sao lưu offline.

## 2. Tạo GitHub Secrets
Base64 file keystore:
```bash
base64 -w 0 ttube-play-upload.jks
```
Trên macOS:
```bash
base64 < ttube-play-upload.jks | tr -d '\n'
```

Thêm 4 secrets vào repo:
- `PLAY_UPLOAD_KEYSTORE_BASE64`
- `PLAY_UPLOAD_STORE_PASSWORD`
- `PLAY_UPLOAD_KEY_ALIAS`
- `PLAY_UPLOAD_KEY_PASSWORD`

Alias mặc định nếu dùng lệnh trên:
`ttube-play-upload`

## 3. Build
Workflow:
`.github/workflows/build-play-aab.yml`

Khi secrets chưa có, workflow tạo AAB unsigned để kiểm tra kỹ thuật.
Khi đủ secrets, AAB được ký bằng upload key và có thể upload vào Play Console.

## 4. Play App Signing
Nên bật Google Play App Signing trong Play Console. Upload key dùng để xác thực các bản bạn gửi lên; app signing key do Play quản lý theo cấu hình bạn chọn.

## Lưu ý cài đè
Bản GitHub hiện dùng certificate stable/debug cũ. Nếu Play App Signing dùng certificate khác, người đang cài bản GitHub sẽ không thể cài đè trực tiếp bản Play trên cùng package mà không gỡ bản cũ.
