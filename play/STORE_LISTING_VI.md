# T Tube — Store listing (VI)

## Tên ứng dụng
T Tube

## Mô tả ngắn
Dashboard đa phương tiện, thời tiết, HUD và điều khiển media trên xe.

## Mô tả đầy đủ
T Tube là ứng dụng hỗ trợ trải nghiệm đa phương tiện và thông tin khi di chuyển.

Trên điện thoại, T Tube cung cấp:
- Trình phát và quản lý nội dung đa phương tiện.
- IPTV và danh sách kênh do người dùng cấu hình.
- Trình duyệt WebApp tích hợp.
- Tìm kiếm bằng bàn phím hoặc giọng nói.
- Dashboard, thời tiết, tốc độ GPS và thông tin hỗ trợ lái xe.
- Sao lưu và khôi phục cấu hình, mục yêu thích và giao diện.

Trên kênh thử nghiệm nội bộ, T Tube tiếp tục kiểm thử giao diện Android Auto, dashboard, HUD và điều khiển media. Cấu hình phát hành công khai sẽ được rà soát riêng theo chính sách Android Auto của Google Play.

Một số chức năng phụ thuộc vào quyền người dùng cấp, dịch vụ hệ thống hoặc dịch vụ bên thứ ba. T Tube không yêu cầu mã kích hoạt hoặc thanh toán ngoài ứng dụng trong bản phân phối Google Play.

## Ghi chú phát hành 1.0.78
- Nút bánh răng YouTube có thêm 5 kiểu màn hình: Tràn viền, Chuẩn 16:9, Phóng to/cắt viền, 4:3 và 21:9.
- Kiểu màn hình YouTube được lưu và tự áp dụng lại ở lần phát sau.
- Chất lượng video vẫn được chọn trong cùng menu Cài đặt YouTube.
- Chuẩn hóa bản Google Play với target API 36.
- Release build không debuggable.
- Bản Play không dùng luồng kích hoạt/bản quyền ngoài ứng dụng.
- Giữ các cải tiến chuyển app, tìm kiếm, voice, media và hiệu năng của 1.0.75.
- Tạo lại App Bundle cho kênh thử nghiệm nội bộ với versionCode 274.
- Khôi phục đầy đủ giao diện Android Auto/dashboard/HUD projection như bản trước để kiểm thử nội bộ.
- Gỡ 2 quyền AndroidX Navigation bị Google Play chặn (`ACCESS_SURFACE`, `NAVIGATION_TEMPLATES`) nhưng giữ projection host riêng.
