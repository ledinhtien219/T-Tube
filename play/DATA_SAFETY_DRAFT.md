# Data Safety draft — T Tube Play

> Đây là checklist kỹ thuật dựa trên source `release/play-v1.0.75`. Cần đối chiếu lại với AAB cuối và câu hỏi thực tế trong Play Console trước khi gửi.

## Vị trí
**Có thể rời thiết bị khi người dùng bật thời tiết.**
- Quyền: ACCESS_COARSE_LOCATION / ACCESS_FINE_LOCATION.
- Mục đích: xác định vị trí để lấy thời tiết.
- WeatherManager gửi latitude/longitude đến Open-Meteo để lấy dữ liệu thời tiết.
- Không dùng cho quảng cáo trong source hiện tại.

## Micro / giọng nói
- RECORD_AUDIO dùng cho tìm kiếm giọng nói.
- Nhận dạng được thực hiện qua Android SpeechRecognizer / dịch vụ nhận dạng được cài trên thiết bị.
- T Tube không lưu bản ghi âm vào máy chủ của T Tube trong source Play hiện tại.
- Nhà cung cấp dịch vụ nhận dạng giọng nói có thể xử lý âm thanh theo chính sách của họ.

## Notification access
- Dịch vụ NotificationListener đọc thông báo Waze/Google Maps để trích xuất dữ liệu dẫn đường/cảnh báo.
- Dữ liệu này được xử lý trong ứng dụng.
- Bản Play không đồng bộ dữ liệu notification lên hệ thống license T Tube.

## Device / screen information
- Ứng dụng có thể tạo mã thiết bị cục bộ và lưu thông tin kích thước/DPI màn hình.
- Bản Play không dùng license activation ngoài Play.
- ScreenProfileReporter chỉ đồng bộ khi LicenseManager báo đã kích hoạt; Play build không kích hoạt luồng này.

## Email / thông tin cá nhân
- Play release không yêu cầu người dùng nhập email để kích hoạt.
- ActivationActivity không được khai báo trong Play manifest.

## Crash logs
- AppCrashHandler lưu log lỗi cục bộ để người dùng xem/chẩn đoán.
- Không thấy luồng tự động upload crash log trong Play branch hiện tại.

## WebView và website bên thứ ba
- Người dùng có thể mở YouTube, TikTok hoặc website khác trong phần điện thoại.
- Các website đó có thể tự thu thập dữ liệu theo chính sách riêng của họ.
- Cần mô tả rõ trong privacy policy rằng nội dung web bên thứ ba không do T Tube kiểm soát.

## Cần xác nhận trong Play Console
- Có/không thu thập Precise location: nên khai báo theo luồng Open-Meteo thực tế.
- Audio data: đối chiếu định nghĩa của Play đối với SpeechRecognizer bên thứ ba.
- App activity / Web browsing: đối chiếu câu hỏi Data Safety hiện tại.
- Data encrypted in transit: các API internet bên ngoài dùng HTTPS, ngoại trừ WebView có thể mở URL do người dùng nhập và app có hỗ trợ cleartext cho một số nguồn IPTV.
