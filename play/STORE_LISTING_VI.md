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

## Ghi chú phát hành 1.0.89
- Phóng lớn giao diện Waze Thanh Ngang (#7); mức 100% giờ hiển thị đúng kích thước chuẩn thay vì bị tự co nhỏ.
- Sửa chỉ dẫn rẽ Waze bị kẹt ở mũi tên đi thẳng; trái/phải/chếch/quay đầu/vòng xuyến dùng đúng mã HLP/1.
- Xóa trạng thái chỉ đường cũ ngay khi Waze kết thúc hoặc đổi tuyến.
- Sửa tỷ lệ HUD Waze theo viewport Android Auto thật để không còn bị phóng quá lớn.
- Neo Panel Lớn ở góc trên phải và Thanh Ngang ở mép trên trái theo ảnh mẫu.
- Căn lại kích thước HUD Waze #6/#7 theo ảnh test thực tế trên màn hình xe.
- Reset vị trí riêng cho 2 layout mới và giữ khung cảnh báo ổn định khi chưa có dữ liệu Waze.
- Làm lại 2 giao diện Waze HUD mới để bám sát ảnh mẫu: Panel Lớn và Thanh Ngang.
- Bổ sung lane guidance, nhiều cảnh báo Waze cùng lúc và nút Báo cáo theo đúng bố cục tham chiếu.
- Thêm 2 giao diện HUD Waze mới: **Waze Panel Lớn** và **Waze Thanh Ngang**.
- Hai giao diện dùng chung dữ liệu Waze/HUD hiện có, chỉ thay đổi cách trình bày để dễ nhìn hơn trên màn hình xe.
- Sửa sâu lỗi TikTok đăng nhập bằng Google bị màn đen: bắt nút/popup Google trước khi OAuth mở và chuyển sang trang QR chính thức của TikTok trong cùng WebView.
- Thêm nút hỗ trợ Đăng nhập QR ngay trên trang đăng nhập TikTok; QR không bị co/zoom bởi CSS của T Tube.
- Tắt theme/CSS chung của T Tube trên TikTok, VTVgo và TV360 để tránh che/đen trang đăng nhập hoặc trình phát.
- Giữ các sửa lỗi Google Maps, nhường audio ngoài và YouTube auto-next.
- Sửa màn đen khi bấm đăng nhập TikTok bằng Google trong WebView.
- Google OAuth không hỗ trợ WebView nhúng, nên T Tube chặn luồng này trước khi mở và đưa về trang đăng nhập TikTok an toàn (số điện thoại/email/QR).
- Giữ các sửa lỗi VTVgo/TV360, Google Maps, nhường audio ngoài và YouTube auto-next.
- Sửa TikTok đăng nhập: giữ nguyên giao diện đăng nhập web thật, tương thích các luồng Google/Apple/Facebook/X và lưu cookie phiên.
- Tăng tương thích VTVgo/TV360: hỗ trợ redirect qua subdomain, cookie đăng nhập và popup đăng nhập trong cùng WebView.
- Không để bộ lọc request của YouTube can thiệp vào đăng nhập/phát nội dung của TikTok, VTVgo hoặc TV360.
- Giữ sửa lỗi Google Maps, nhường audio cho SoundCloud/Spotify/Zing và YouTube tự chuyển bài.
- Nhường âm thanh sạch cho SoundCloud/Spotify/Zing khi chuyển khỏi T Tube, không tự giành lại audio.
- Khi quay lại T Tube, chỉ phát lại khi người dùng chủ động bấm Play hoặc chọn video.
- YouTube tự chuyển sang bài/video tiếp theo khi video hiện tại kết thúc, có cơ chế retry nếu nút Next tải chậm.
- Sửa lỗi chuyển từ T Tube sang Google Maps bị lag hoặc khó kéo bản đồ.
- Nhả touch/projection và giảm tải WebView nền khi T Tube không còn ở foreground.
- Không cưỡng bức giành lại audio focus khi Android Auto chuyển sang ứng dụng khác.
- Nút bánh răng YouTube có thêm 5 kiểu màn hình: Tràn viền, Chuẩn 16:9, Phóng to/cắt viền, 4:3 và 21:9.
- Kiểu màn hình YouTube được lưu và tự áp dụng lại ở lần phát sau.
- Chất lượng video vẫn được chọn trong cùng menu Cài đặt YouTube.
- Chuẩn hóa bản Google Play với target API 36.
- Release build không debuggable.
- Bản Play không dùng luồng kích hoạt/bản quyền ngoài ứng dụng.
- Giữ các cải tiến chuyển app, tìm kiếm, voice, media và hiệu năng của 1.0.75.
- Tạo lại App Bundle cho kênh thử nghiệm nội bộ với versionCode 287.
- Khôi phục đầy đủ giao diện Android Auto/dashboard/HUD projection như bản trước để kiểm thử nội bộ.
- Gỡ 2 quyền AndroidX Navigation bị Google Play chặn (`ACCESS_SURFACE`, `NAVIGATION_TEMPLATES`) nhưng giữ projection host riêng.
