# TTube 1.0.73 — chuyển app

Bản debug, versionCode 264, applicationId com.tcar.auto. Chữ ký APK giống bản 1.0.72 đã gửi trong cuộc trò chuyện.

Các thay đổi:
- Đánh dấu điều hướng đang chờ cả khi tải trang mới, không chỉ khi restoreState. Tránh việc kiểm tra URL cũ rồi tải thêm trang chủ YouTube trong cùng lần chuyển.
- Bấm lại app đang mở/đang tải không khởi động lại yêu cầu tải, không dựng lại giao diện không cần thiết.
- Dùng số thứ tự cho lần chuyển để loại callback trễ trong tình huống A -> B -> A; hủy lệnh phục hồi cũ khi chuyển hoặc đóng Presentation.
- Áp dụng viewport/theme theo trang đích trong lúc đang điều hướng, thay vì cấu hình theo URL cũ.
- Khôi phục vị trí phát một lần sau khi player sẵn sàng, thay cho ba lần tua định giờ. Dừng phục hồi khi người dùng đã chọn video khác. Trang chủ không kế thừa thời gian của video trước. Không ép phát nếu người dùng đã tạm dừng.
- Giữ thanh điều khiển đã dựng khi quay về cùng app. Không đọc lại ảnh nền tĩnh khi trở về Home nếu ảnh chưa đổi.
- Điện thoại: bấm lại TikTok giữ nguyên trang đang mở; ghi cookie ra ổ đĩa ở luồng nền thay vì chặn giao diện.
- Giữ các thay đổi DPI của 1.0.72.

Xác minh trên mã cuối cùng:
- 83/83 unit test đạt, gồm 5 bài mới kiểm tra phục hồi phiên/video và callback cũ.
- 2 instrumentation test đạt trên giả lập Android 16: một bài chuyển YouTube/trình duyệt bằng trang HTML được chặn và trả về cục bộ; một bài WebView chuyển qua nhiều display/DPI.
- Bài chuyển app: bấm lặp 12 lần không tải lại app; mở đích YouTube một lần không sinh tải trang chủ; chuyển A/B nhanh; trở về Home/app nhiều lần; giữ trạng thái tạm dừng.
- JavaScript: 40/42 tệp đạt. Hai kiểm tra cũ vẫn thất bại như trước: v162-media-session-tiktok-layout, v163-tiktok-popup-youtube-restore.
- APK đã kiểm tra chữ ký, package và phiên bản. Bộ kiểm thử giả lập dùng applicationId riêng .runtimetest để tránh thay thế app khác chữ ký đang cài. APK bàn giao dùng com.tcar.auto bình thường.

Giới hạn: chưa đo FPS trên đầu màn xe thật và chưa xác nhận tốc độ mạng/trang trực tuyến của từng nhà cung cấp. Một WebView vẫn phải điều hướng hoặc khôi phục tài liệu khi chuyển sang app khác, nên lần tải đầu vẫn phụ thuộc mạng. Không coi kết quả trang thử cục bộ là kiểm thử phát video trực tuyến hoặc chứng nhận mọi màn xe.

Dựng bình thường: dùng JDK 17, Android SDK và Gradle wrapper, chạy testDebugUnitTest assembleDebug.
Chạy kiểm thử giả lập độc lập: thêm -PisolatedRuntimeTest=true và chọn AppSwitchRuntimeTest,ProjectionDensityRuntimeTest. Không dùng APK .runtimetest làm bản cập nhật cho người dùng.
