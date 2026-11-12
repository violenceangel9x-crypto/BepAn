# Bếp ăn

## Platform

android

## Stack

Kotlin, Jetpack Compose Material 3, Navigation Compose, ViewModel/Flow, Room và DataStore. Theo yêu cầu mới nhất, người dùng nhập API key trong Cài đặt; app mã hóa khóa bằng Android Keystore và gọi Gemini trực tiếp qua HTTPS. Gateway chạy riêng vẫn là kết nối nâng cao, dùng khóa riêng ở máy chủ.

## Người dùng và công việc

Một người quản lý bếp ăn dùng một điện thoại Android. Buổi tối, họ chốt sổ ngày vừa qua rồi chuẩn bị ngày kế tiếp. Trong ngày họ xem ngân sách đi chợ, sửa các trường hợp cắt cơm, thêm khách, theo dõi bữa ăn, cập nhật thực đơn và ghi tiền mua. Cuối tháng họ đối chiếu sổ, thu hoặc quyết toán tiền ăn, nhận khoản bù giá gạo được cấp cho chiến sĩ.

App dùng tiếng Việt. Ngữ cảnh thật là điện thoại cầm một tay khi đi chợ hoặc đang làm ở bếp. Mở app phải thấy ngày, trạng thái sổ và số tiền có thể đi chợ. Khi đối chiếu tháng, cần xem thẻ tóm tắt và bảng có cấu trúc quen thuộc với sổ Excel.

## Kết quả người dùng cần

| Nhu cầu | Kết quả trong app |
|---|---|
| Chuẩn bị bữa | Cán bộ và chiến sĩ đang ăn được chọn mặc định Sáng, Trưa, Tối. Xử lý ngoại lệ theo ngày/bữa, khách thêm theo lịch hoặc trong ngày |
| Lên thực đơn | Xem bốn ô dọc, sửa từng ô, dùng Magic Menu theo mẫu. Bữa sáng hôm sau chuyển đúng sang màn ngày kế tiếp |
| Chia sẻ thực đơn | Sao chép hoặc chia sẻ PNG gồm đúng tiêu đề và bốn ô, kể cả phần nằm ngoài vùng đang cuộn |
| Đi chợ và chốt ngày | Phân biệt ngân sách, tiền đã mua, chênh lệch. Xem trước rồi chốt buổi tối. Ngày đã chốt đi vào sổ tháng |
| Đối soát tài chính | Có nhóm Cán bộ, Chiến sĩ, Khách và bộ lọc Có cắt cơm. Xem từng người, bảng tháng, khoản đã nộp, thiếu/dư, bù giá gạo và giao dịch thực nhận |
| Giao việc cho AI | Nhắn tiếng Việt hoặc đọc lệnh trong chat kiểu Messenger/Zalo. Đọc kết quả, chọn người trùng tên, xem trước/xác nhận hoặc hoàn tác ngay trong chat |
| Tìm nguyên nhân khi app gặp lỗi | Mở AI Debug Center từ Cài đặt hoặc thẻ lỗi trong chat. Xem bước nào xảy ra lỗi và xuất báo cáo đã lọc để gửi GPT/Codex |
| Dùng khi mạng chập chờn | Chấm cơm, thực đơn, ghi tiền, ghi chú và chốt sổ hoạt động trên thiết bị. Chat hiện trạng thái Gemini và giữ bản nháp khi mạng gián đoạn |

## Điều hướng và ưu tiên

1. **Hôm nay** mở mặc định. Chọn ngày, xem ngân sách, thực đơn, chấm cơm, thay đổi, tiền mua và chốt sổ.
2. **Tài chính** mở kỳ tháng. Xem tổng, bù giá gạo, sổ chấm cơm, thu và quyết toán.
3. **Ghi chú** có tìm kiếm, ghim, ghi chú và lối vào Lịch sử hoạt động.
4. **Cài đặt** quản lý người ăn, định mức, bù giá gạo, Gemini, sao lưu, giao diện và Chẩn đoán.
5. **Gemini** dùng nút nổi trong bốn tab. Chat toàn màn hình, thu nhỏ trở về đúng tab và vị trí.

## Nguyên tắc trải nghiệm

- Thao tác hằng ngày ưu tiên ngoại lệ và kết quả cần hành động. Ngày, bữa, kỳ và trạng thái hiển thị bằng chữ.
- Số tiền có thể đọc nhanh trên nền ổn định. Nền xanh đen, thẻ trắng lạnh, vàng cho hành động chính, tím nhận diện Gemini, cyan cho thông tin phụ.
- Chấm cơm bằng tay và qua Gemini luôn nhìn thấy cùng kết quả. Thẻ xác nhận ghi rõ người, ngày/bữa, số tiền hoặc phạm vi. Tác vụ không rõ đối tượng cần làm rõ ngay trong chat.
- Mốc đầu có luồng thật từ **Hôm nay → hỏi suất trong chat → cắt một bữa → thẻ cập nhật → hoàn tác**. Dựng đồng thời Database (Room), Audit Log, Technical Log, AI Debug Center và Debug Handoff để xem và xuất báo cáo lỗi. Chạy thử gateway thật khi có cấu hình, dùng chế độ mô phỏng có nhãn rõ trong debug khi cần.

Nghiệp vụ chi tiết ở [02_NGHIEP_VU.md](docs/02_NGHIEP_VU.md). Bố cục và hệ thiết kế ở [03_MAN_HINH_VA_TUONG_TAC.md](docs/03_MAN_HINH_VA_TUONG_TAC.md) và [DESIGN.md](DESIGN.md).
