# Hệ thiết kế Bếp ăn

Dùng Jetpack Compose và Material 3. Hướng hình ảnh lấy từ [bảng màu SaveSmart](references/bang-mau-savesmart.jpg) và [ảnh bố cục thực đơn](references/bo-cuc-the-thuc-don.jpg). Màn tham chiếu đầu tiên là **Hôm nay**, sau đó dùng lại token và component trong **Tài chính**, **Ghi chú**, **Cài đặt** và chat **Gemini**.

## Màu và bề mặt

| Token | HEX | Dùng ở |
|---|---|---|
| Background | `#1C2632` | Nền xanh đen |
| SurfaceDark | `#202D3D` | Bề mặt tối, nền thanh dưới và sheet |
| SurfaceLight | `#F6F7F9` | Thẻ ngân sách, món ăn, bảng đối chiếu, biểu mẫu |
| OnDark | `#F6F7F9` | Chữ chính trên tối |
| MutedOnDark | `#B9C5D1` | Chữ phụ trên tối |
| OnLight, OnPrimary | `#17212D` | Chữ trên trắng/vàng |
| MutedOnLight | `#506071` | Chữ phụ trên sáng |
| Primary | `#FFCB15` | Nút hành động chính, điểm nhấn tab đang chọn |
| Gemini | `#6A3BEB` | Nút Gemini, tin người dùng, nhấn thẻ thao tác |
| OnGemini | `#FFFFFF` | Chữ trên tím đậm |
| Cyan | `#4CC0E5` | Điểm nhấn thông tin trên nền tối |
| SuccessDark, ErrorDark | `#4ADE80`, `#FF806E` | Trạng thái trên nền tối |
| SuccessLight, ErrorLight | `#14663D`, `#9F2F32` | Trạng thái trên nền sáng |
| WarningLight | `#7A4B00` | Chữ cảnh báo trên nền sáng |

- Nền có thể dùng hai vệt tím/cyan nhẹ sau cụm thẻ. Thẻ kính tối lấy `SurfaceDark` độ đục khoảng 82–92% và viền trắng 1dp khoảng 12–18%. Thẻ kính sáng dùng nền trắng lạnh gần đặc. Giảm trong suốt nếu chữ hoặc số đọc kém trên nền đã phối.
- Dùng nền đặc cho bảng nhiều hàng, ô nhập, composer chat, nội dung xác nhận và ảnh thực đơn. Với màu động hoặc hiệu ứng kính tắt, giữ phân cấp bằng màu nền, viền và khoảng cách.
- Giữ chữ, icon và số sắc nét. Nếu thử blur, làm trên lớp nền trang trí tách khỏi nội dung và đo độ mượt. `Modifier.blur` làm mờ lớp được gắn. Dùng bề mặt đặc nếu phần cứng hoặc phiên bản Android chưa phù hợp.
- Một hành động chính nổi bật trong mỗi vùng thao tác. Màu trạng thái đi kèm nhãn như **Nháp**, **Đã chốt**, **Đã cắt**. Các token trên là điểm bắt đầu. Kiểm tra cặp màu sau khi ghép alpha, mục tiêu 4,5:1 với chữ thường và 3:1 với chữ lớn.

## Kiểu chữ, khoảng cách và cử chỉ

- Dùng Roboto/sans-serif của Android, bảo đảm đủ dấu tiếng Việt. Tiêu đề màn 24–28sp, tổng tiền 32–36sp, thân 14–16sp, thông tin phụ 12–14sp. Số tiền theo `vi-VN`, ví dụ **240.000₫**, dùng số cùng độ rộng ở bảng khi font hỗ trợ.
- Lề ngang 16dp ở điện thoại. Nhịp cách 4/8/12/16/24/32dp. Góc bo 24–28dp cho thẻ lớn và 16–20dp cho thẻ con. Vùng chạm tối thiểu 48dp, gồm cả icon và chip. Tôn trọng font scale, system bars, IME và gesture inset.
- Màn hẹp và chữ lớn cho phép các hàng tiền/nút xuống dòng. Không gắn chiều cao cố định cho món ăn, tên người, tin nhắn hoặc thẻ hành động. Giữ khoảng trống khi cuộn tới hàng cuối và khi bong bóng ở gần thanh điều hướng.
- Animation mở/đóng và trạng thái thao tác ngắn, theo cài đặt chuyển động của hệ thống. Dùng nhấn và phản hồi dễ nhận thấy khi lưu, xác nhận, hoàn tác. Không kéo cuộn chat về cuối khi người dùng đang đọc tin cũ, có nút về tin mới khi cần.

## Component Compose

| Component | Hành vi cần dùng lại |
|---|---|
| `DayHeader` / `PeriodHeader` | Chọn ngày hoặc tháng, nhãn trạng thái, khôi phục vị trí khi quay lại |
| `GlassCard`, `MoneySummaryCard` | Bề mặt, tiền lớn, tiền phụ, mô tả trạng thái đã mua/chưa ghi |
| `MenuCardContent` | Tiêu đề + bốn ô dọc, dữ liệu và bố cục chung cho màn hình và ảnh PNG |
| `MealToggle`, `StatusChip` | Tên bữa, icon và nhãn, trạng thái selected/disabled/TalkBack |
| `FinanceGroupTable` | Tiêu đề ngày hai tầng, tên cố định, ô trạng thái đủ đọc, xem chi tiết khi chạm |
| `GeminiLauncher` | Chạm mở, kéo trong vùng an toàn, có lối mở truy cập bằng TalkBack |
| `ChatBubble`, `ActionCard` | Tin trái/phải, giờ, trạng thái, xác nhận/sửa/hủy/hoàn tác tùy trường hợp |
| `TraceStepRow`, `DebugReportPreview` | Dòng thời gian chẩn đoán, nhãn bước lỗi và trạng thái ghi. Mã theo dõi có thể sao chép. Bản xem trước báo cáo dễ đọc và cuộn khi chữ lớn |
| `FormSheet`, `ConfirmSummary` | Phạm vi thao tác, ô số tiền dễ đọc, hành động hiện trên bàn phím |
| `InlineStatus`, `EmptyState` | Tải, lỗi, trống, ngoại tuyến và đường xử lý rõ ràng |

Tên component gợi ý để tìm và dùng lại theo quy ước repo. Chuẩn bị Compose Preview có dữ liệu cho ngày nháp, ngày đã chốt, bảng dày, chat chờ xác nhận và menu dài. Rà ít nhất màn 320–360dp, Note 20 Ultra và cỡ chữ khoảng 1,3× đến 2× khi có thể. Kiểm tra nhãn hành động và thứ tự đọc khi TalkBack bật.

## Quyết định triển khai 12/11/2026

- Cài đặt Gemini có ô API key dạng mật khẩu, hiện/ẩn, lưu/thay thế và xóa; trạng thái có khóa được thể hiện riêng. Không khôi phục draft khóa qua SavedState hoặc hiện lại khóa đã lưu. Lưu khóa bật kết nối trực tiếp, giữ gateway cũ trong phần nâng cao để việc nhập khóa không cần thao tác trên máy tính.

- Native Android, chế độ tác vụ (Operate). Giữ bảng màu đã chốt; dùng Material 3 cho navigation, chip, dialog, switch và vùng chạm. Bề mặt tiền/thực đơn gần đặc để đọc ngoài bếp; chưa thêm blur trang trí.
- Đã cài Impeccable 4.8.2 và UI/UX Pro Max ở `.agents/skills/`, phạm vi project. Đã đọc hướng dẫn native Android, audit và polish. Python chưa có nên không chạy CLI tra thiết kế; dùng Quick Reference và đối chiếu trực tiếp dữ liệu `jetpack-compose.csv`, không coi đó là kết quả CLI.
- Áp dụng hướng dẫn đã đối chiếu: StateFlow từ ViewModel, khóa ổn định trong LazyColumn, DisposableEffect dọn SpeechRecognizer, nhãn cho icon tương tác, font `sp`, nút tối thiểu 48dp, giữ trạng thái tab bằng SaveableStateHolder.
- Khi mở bàn phím, chat dùng header một hàng, giữ ngày/chế độ và ẩn hàng gợi ý để tăng vùng đọc. Consume padding của Scaffold trước IME padding theo [hướng dẫn Material 3](https://developer.android.com/develop/ui/compose/system/material-insets). Kéo lên đọc lịch sử dừng tự cuộn, nút Tin mới nhất bật lại; đã kiểm thử trên Samsung/API 33 và emulator/API 34.
- Thẻ thực đơn dùng bốn hàng, ô cuối có màu nền nhẹ và ngày cụ thể. Renderer PNG dùng cùng khóa ngày/bữa, đo chiều cao theo toàn bộ nội dung, không chụp vùng cuộn. Magic Menu chỉ thay bữa đang chọn, cho xem gợi ý rồi áp dụng vào bản nháp trước khi lưu.
- Bản đầu tối ưu điện thoại. Đã thử các luồng native trên Samsung SM-N986U1/API 33 với cỡ chữ hệ thống 1,3×; giữ nguyên cài đặt hiển thị của máy. Chưa tuyên bố hỗ trợ tablet hay đã thử TalkBack. Bằng chứng và giới hạn ghi ở HANDOFF/UI_REVIEW.

Đặc tả theo màn ở [03_MAN_HINH_VA_TUONG_TAC.md](docs/03_MAN_HINH_VA_TUONG_TAC.md). Quy trình rà và tinh chỉnh ở [01_QUY_TRINH_VIDEO.md](docs/01_QUY_TRINH_VIDEO.md).
