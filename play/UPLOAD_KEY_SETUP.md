# Play upload key setup

Package Google Play: `com.ledinhtien.ttube`

Upload key đang dùng cho bản Play:
- Alias: `ttube-play-upload`
- SHA-256: `2E:F9:93:23:53:DB:CA:EA:27:41:24:68:2D:51:23:24:9A:10:E5:87:BD:A0:6D:62:BB:D2:35:FF:E0:EA:21:50`
- Public certificate: `play/upload_certificate.pem`

Private `.jks`, password và private key **không được commit vào Git**. Giữ bản keystore tương ứng với fingerprint trên ở nơi an toàn và sao lưu offline. Không tạo key khác cho các bản cập nhật sau trừ khi chủ động thực hiện quy trình reset/rotate upload key với Google Play.

## GitHub Secrets

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

Alias:
`ttube-play-upload`

## Build

Workflow:
`.github/workflows/build-play-aab.yml`

Khi secrets chưa có, workflow tạo AAB unsigned để kiểm tra kỹ thuật.
Khi đủ secrets, AAB được ký bằng upload key và có thể upload vào Play Console.

## Play App Signing

Bật Google Play App Signing trong Play Console. Upload key dùng để xác thực các bundle gửi lên; app signing key do Google Play quản lý theo cấu hình của ứng dụng.

## Cài song song với bản GitHub

Bản Google Play dùng `com.ledinhtien.ttube`, còn bản stable GitHub hiện dùng `com.tcar.auto`. Hai bản là hai package khác nhau nên có thể cùng tồn tại trên một thiết bị; chúng không cài đè lên nhau.
