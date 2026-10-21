# T Tube Landing Page + Email Collector

Landing page nằm trong `docs/` và có thể deploy bằng GitHub Pages. Form email dùng Google Apps Script để ghi dữ liệu vào Google Sheet; trang `docs/admin.html` cho phép đọc danh sách bằng admin token và xuất file Excel `.xlsx`.

## 1. Google Sheet đích

Backend đã được cấu hình sẵn cho file Google Sheet hiện có:

`14vfUJwL33kXJ7ck6mJpT2Pnstp1FeR729UJQ0ktpaco`

Apps Script sẽ chỉ tạo/ghi vào tab riêng **T Tube Emails**. Các tab hiện có trong spreadsheet không bị sửa.

Không cần tự tạo header; Apps Script sẽ tạo tab và header ở lần chạy đầu.

## 2. Tạo Apps Script

1. Trong Google Sheet: **Extensions → Apps Script**.
2. Dán nội dung file `email_collector_apps_script.gs` vào project.
3. Mở **Project Settings → Script Properties** và tạo:
   - `ADMIN_TOKEN`: một chuỗi bí mật dài, ngẫu nhiên (khuyến nghị từ 32 ký tự trở lên).
4. Không cần tạo `SPREADSHEET_ID`; file đích đã được cố định trong backend.
5. Tuyệt đối không commit `ADMIN_TOKEN` vào GitHub.

## 3. Deploy Web App

1. Trong Apps Script chọn **Deploy → New deployment**.
2. Type: **Web app**.
3. Execute as: **Me**.
4. Who has access: **Anyone**.
5. Deploy và copy URL kết thúc bằng `/exec`.

Ví dụ:

```text
https://script.google.com/macros/s/XXXXXXXXXXXX/exec
```

## 4. Kết nối landing page

Mở `docs/config.js` và thay:

```js
window.TTUBE_CONFIG = {
  emailApiUrl: "YOUR_GOOGLE_APPS_SCRIPT_WEB_APP_URL"
};
```

bằng URL Web App thật.

Endpoint này là public để nhận email, nhưng danh sách email chỉ trả về khi có đúng `ADMIN_TOKEN`.

## 5. Trang quản trị / xuất Excel

Sau khi deploy website, mở:

```text
https://<username>.github.io/<repo>/admin.html
```

Nhập `ADMIN_TOKEN` để:

- xem tổng số đăng ký,
- xem đăng ký hôm nay,
- xem danh sách email,
- tải `T-Tube-emails-YYYY-MM-DD.xlsx`.

Nếu thư viện XLSX CDN không tải được, trang sẽ tự fallback sang CSV có BOM để mở tốt bằng Excel.

## 6. Bật GitHub Pages

Repo hiện có workflow `.github/workflows/deploy-pages.yml`.

Nếu Pages chưa bật:

1. Vào **Settings → Pages**.
2. Ở **Build and deployment → Source**, chọn **GitHub Actions**.
3. Push/merge thay đổi vào `main` hoặc chạy workflow thủ công.

Website mặc định sẽ có dạng:

```text
https://ledinhtien219.github.io/T-Tube/
```

## 7. Kiểm tra nhanh

- Mở landing page trên desktop và mobile.
- Submit một email test.
- Kiểm tra Google Sheet có dòng mới.
- Submit lại cùng email: backend sẽ không tạo dòng trùng.
- Mở `admin.html`, nhập token và export Excel.
- Xóa email test nếu không cần giữ.

## Quyền riêng tư

Landing page chỉ gửi các trường: email, nguồn form và trạng thái đồng ý nhận tin. Backend mẫu không lưu IP. Hãy cập nhật Privacy Policy nếu mục đích sử dụng email thay đổi.
