# Hey Mimi v6 — ký AAB để tải lên Play Console

Google Play từ chối AAB 1.1.9 build 307 trước đây vì **unsigned**, đúng như file `SIGNING_STATUS.txt` của artifact cũ. Không đổi tên file sẽ làm AAB được ký.

## Dùng khóa ký hiện tại (khuyến nghị)

Trong Play Console, mở ứng dụng T-Tube > **Play app signing / Chữ ký ứng dụng trên Play**. Xem mục **Upload key certificate / Chứng chỉ khóa tải lên**, ghi lại SHA-256. Đây là SHA-256 **chứng chỉ tải lên**, không phải SHA-256 của AAB, không phải chứng chỉ app-signing do Google giữ.

Trên máy đã giữ khóa ký upload của ứng dụng, tìm file `.jks` hoặc `.keystore`, tên alias, mật khẩu kho khóa và mật khẩu khóa. **Không** gửi file khóa hoặc mật khẩu trong issue, commit, email hay ChatGPT.

Mở GitHub repository **Settings > Secrets and variables > Actions > New repository secret** và tạo:

| Secret | Nội dung |
|---|---|
| `PLAY_UPLOAD_KEYSTORE_BASE64` | Nội dung Base64 của file JKS/keystore **hiện tại** |
| `PLAY_UPLOAD_STORE_PASSWORD` | Mật khẩu keystore |
| `PLAY_UPLOAD_KEY_ALIAS` | Alias của key upload |
| `PLAY_UPLOAD_KEY_PASSWORD` | Mật khẩu key upload |
| `PLAY_UPLOAD_CERT_SHA256` | **Khuyến nghị**: fingerprint SHA-256 của chứng chỉ upload ở Play Console |

Tạo Base64 không xuống dòng bằng PowerShell (thay đường dẫn bằng đường dẫn file khóa):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\\path\\upload-key.jks")) | Set-Clipboard
```

Sau khi lưu các secrets, chạy workflow [Build SIGNED Hey Mimi v6 Play AAB](https://github.com/ledinhtien219/T-Tube/actions/workflows/build-hey-mimi-v6-play-aab.yml) tại nhánh `test/hey-mimi-v6-play-aab-build307` bằng **Run workflow**. Artifact mới có tên `TTube-Hey-Mimi-v6-1.1.9-build307-SIGNED-Play-AAB`; tệp `SIGNING_STATUS.txt` phải ghi `SIGNED_WITH_PLAY_UPLOAD_KEY`.

Nếu trước đây app đã có phiên bản trên Play, phải dùng đúng **upload key được đăng ký** hoặc thực hiện **Request upload key reset** tại Play Console. **Không tự tạo khóa mới để thay thế** rồi tải lên như bản cập nhật. Cũng phải bảo đảm versionCode 307 lớn hơn những versionCode đã tải lên Play trước đó.

## Ký trực tiếp AAB đã build trên máy dùng Windows

Có thể dùng script `tools/sign-aab-with-existing-key.ps1` với JDK 17+ đã cài. Script sử dụng keystore hiện tại, yêu cầu so sánh fingerprint với Play Console trước khi ký:

```powershell
powershell -ExecutionPolicy Bypass -File tools/sign-aab-with-existing-key.ps1 -InputAab "C:\\path\\HeyMimi_v6_build307.aab" -Keystore "C:\\path\\upload-key.jks" -Alias "upload"
```

Script tạo `HeyMimi_v6_build307-signed.aab`. Không cần Android SDK để ký AAB đã có; chỉ cần Java JDK. Không dùng debug.keystore để ký tải lên Play trừ phi chính chứng chỉ đó đã được đăng ký làm upload key.

Google Play Help: https://support.google.com/googleplay/android-developer/answer/9842756
