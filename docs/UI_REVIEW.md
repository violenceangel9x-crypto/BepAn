# Rà UI native — 12/11/2026

Phạm vi: Hôm nay → chat → thẻ lưu/hoàn tác → lỗi → chẩn đoán → preview báo cáo, cùng form mua/chốt/điều chỉnh và PNG dài. Dùng Impeccable hướng dẫn Android/audit/polish và Quick Reference/UI stack Compose của UI/UX Pro Max. Không dùng detector HTML để kết luận cho Android. Python chưa có nên không chạy CLI tìm hệ thiết kế, không cài Python thay người dùng.

## Bằng chứng

- Emulator Android 14/API 34, khung 1080×2400 (khoảng 411dp), thêm override 840×1680 (320dp).
- Samsung Note 20 Ultra SM-N986U1/API 33, cỡ chữ 1,3× và mật độ do người dùng đặt, không đổi cài đặt máy. [Bốn ca native đạt](evidence/note20/native-results.xml), [chat](evidence/note20/chat-cut.png), [giữ lịch sử](evidence/note20/chat-history-kept.png), [từ chối micro vẫn nhập chữ](evidence/note20/micro-denied-text-works.png).
- [Gemini thật trên Samsung](evidence/note20/gemini-results.xml) đã đạt bốn lệnh và một lần retry sau timeout. [Emulator cũng đạt bốn ca native](evidence/native/native-results.xml) sau thay đổi bố cục cuối.
- [Hôm nay](evidence/today.png), [trước tinh chỉnh](evidence/today-before-polish.png).
- [320dp chữ 1,3×](evidence/today-320dp-font13.png), [320dp chữ 2×](evidence/today-320dp-font2.png), [trước sửa chữ 2×](evidence/today-320dp-font2-before.png).
- [Chat](evidence/native/chat-cut.png), [lỗi timeout](evidence/native/diagnostics-timeout.png), [preview báo cáo](evidence/native/report-preview.png).
- [Khoản mua](evidence/native/purchase-preview.png), [ngày đã điều chỉnh](evidence/native/closed-day-adjusted.png), [PNG đầy đủ 1080×3125](evidence/native/menu-long.png).
- Các override font/size đã được trả về trạng thái emulator ban đầu sau chụp. Chưa thử tablet; sản phẩm hiện nhắm điện thoại.

## Đã sửa qua các lượt

| Mức | Phát hiện và tác động | Thay đổi / kiểm chứng |
|---|---|---|
| P1 | Icon status bar tối trên nền navy khó đọc | SystemBarStyle.dark; ảnh native sau sửa có icon trắng |
| P1 | Extended FAB che nhiều nội dung/nút | Bubble tròn 56dp, kéo được trong vùng an toàn; đáy danh sách có khoảng trống |
| P1 | Xuất ảnh dài cần tách công việc nền và tương tác hệ thống | Đo/render/file ở IO, clipboard/intent trên Main; phản hồi qua state thay Toast gọi từ continuation nền; native test đọc được URI và PNG |
| P1 | Ngày bị tách thành `2026-1 / 1-12`, label tab sát/cắt cạnh ở 320dp/2× | Ngày dùng hết hàng, trạng thái ở hàng riêng; giới hạn bề rộng label và tăng chiều cao navigation khi font lớn; chụp lại 2× |
| P1 | Phản hồi nhanh hủy animation của tin gửi, làm kết quả ngoài viewport khi Samsung mở IME | Giữ ý định theo tin mới qua cập nhật/đổi viewport; kéo tay tạm dừng, Tin mới nhất khôi phục. Ca giữ lịch sử khi nhận phản hồi và ca gửi nhiều lượt đều đạt |
| P2 | Header và hàng gợi ý chiếm phần lớn vùng chat khi Samsung mở bàn phím/cỡ chữ 1,3× | Header một hàng khi IME mở, giữ ngày/chế độ rõ ràng, ẩn hàng gợi ý trong lúc nhập. Consume padding của Scaffold để tránh cộng lặp với IME; [ảnh sau sửa](evidence/note20/chat-read-state.png), [ảnh trước](evidence/note20/gemini-counts.png). Bốn ca Samsung chạy lại đạt |
| P2 | Chọn trace từ lỗi nhưng chi tiết nằm sau danh sách dài | Đưa chi tiết trace lên trước; test native chạy sau nhiều thao tác vẫn mở và xuất đúng lượt |
| P2 | Lỗi lưu menu có thể đóng editor và mất chữ | Chỉ đóng sau commit; khóa nút trong lúc lưu, giữ nội dung và hiện lỗi nếu stale |
| P2 | Màu mặc định một số dialog lệch hệ navy | Đặt surface container roles theo theme; giữ Material 3 controls |
| P2 | Chip đọc riêng thiếu tên người | Thêm contentDescription tên/bữa và stateDescription Có ăn/Đã cắt/Chưa đăng ký; minimum 48dp |

## Đánh giá kỹ thuật tạm thời

| Chiều | Điểm /4 | Căn cứ và giới hạn |
|---|---:|---|
| Khả năng tiếp cận | 3 | Có tên, trạng thái, sp và ảnh 1,3×/2×; chưa chạy TalkBack thật |
| Hiệu năng | 3 | LazyColumn có key; Room/log/PNG/đọc mẫu ngoài main; chưa profile trên Samsung |
| Theme | 3 | Token chung, bề mặt số tiền ổn định; vẫn còn màu cục bộ nên tiếp tục gom token |
| Quy ước Android | 4 | Material navigation/dialog/chip/switch, system Back, insets và composer trên IME; ảnh native |
| Thích ứng | 3 | Có chứng cứ 320–411dp; chưa nghiệm thu landscape/multi-window |
| Tổng | **16/20** | Đánh giá từ source + các trạng thái đã chụp, không thay nghiệm thu thiết bị |

Giao diện theo đúng bối cảnh bếp: ngân sách đi trước, thẻ menu bốn bữa, ngoại lệ suất sau menu. Thao tác tài chính có preview, trạng thái chữ đi cùng màu, nhãn mô phỏng/dữ liệu mẫu nhìn thấy được. UI tài chính hiện còn phạm vi mốc sau, chưa tính là đã hoàn thiện.

## Cần kiểm tra tiếp

- P1 trước dùng thật: TalkBack trên toàn luồng; focus khi mở/đóng dialog; ASR vi-VN thành công; dán ảnh clipboard trong app nhận. Đã thử Samsung/IME, từ chối micro và đọc lại URI clipboard.
- P2: Rà chữ 2× toàn bộ form/chat/diagnostics, landscape; giảm mật độ chip Magic Menu khi có nhiều món; tách KitchenApp.kt theo màn; gom các màu cục bộ về token.
- P2: Thử chuỗi quay lại chat/tab dài hơn, giữ vị trí và draft khi tiến trình bị hệ thống thu hồi; thử thiết bị API thấp hơn 34.

Questions skipped: sản phẩm, bảng màu, platform và luồng ưu tiên đã được xác định trong yêu cầu/tài liệu; người dùng đã yêu cầu tự triển khai và tiếp tục. Các quy tắc nghiệp vụ chưa chốt được ghi trong HANDOFF thay vì tự coi là dữ liệu vận hành.
