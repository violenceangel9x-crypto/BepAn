# Dữ liệu phát triển

| File | Nội dung |
|---|---|
| [menu-templates.v1.json](menu-templates.v1.json) | 165 mẫu bữa khác nhau từ bốn sheet của **Bảng menu cho app.xlsx**, ghi tên món, vai trò theo vị trí và ô nguồn. Có 4 ghi nhận bữa đặc biệt |
| [cham-com-fixtures.v1.json](cham-com-fixtures.v1.json) | Ca số liệu giả cho phép tính ngân sách, thu nhóm, hoàn/cấn và bù gạo |
| [gemini-cases.v1.json](gemini-cases.v1.json) | Lệnh tiếng Việt và kết quả mong đợi cho test bộ xử lý lệnh và chat |
| [gemini-system-prompt.vi.md](gemini-system-prompt.vi.md) | Chỉ dẫn gợi ý cho Gemini ở gateway, dùng kèm khai báo hàm thực tế |
| [logging-cases.v1.json](logging-cases.v1.json) | Ca thử Room/Audit, lỗi Gemini, chống ghi trùng, log thiếu và lọc báo cáo |
| [debug-report-mau.md](debug-report-mau.md) | Báo cáo lỗi mẫu đã lọc để GPT hoặc Codex đọc, minh họa một thẻ xác nhận hết hiệu lực |

Các file JSON có `schemaVersion = 1`. Mã bữa là `BREAKFAST`, `LUNCH`, `DINNER`, ngày là ISO `YYYY-MM-DD`. Tiền VND trong fixture là số nguyên, trừ giá trị phần nghìn đồng ghi rõ cho bù giá gạo. Kho mẫu thực đơn là nguồn gợi ý có thể dùng offline. Dữ liệu fixture chấm cơm và Gemini dùng cho debug/test.

Tên món giữ dấu và thứ tự trong mẫu. `sourceOccurrences` là số lần tổ hợp xuất hiện. `source` của mỗi mẫu thường ghi lần xuất hiện đầu tiên. `specialMeals` ghi riêng “Có KH riêng”. Các slot phân loại theo vị trí món trong bảng để chọn bữa hoặc thay một vị trí. Xem [06_EXCEL_VA_MAGIC_MENU.md](../docs/06_EXCEL_VA_MAGIC_MENU.md) để xử lý lịch, ngoại lệ, ràng buộc nguyên liệu và giá.

Khi nạp vào app, giữ ID mẫu ổn định, kiểm tra schema và dùng Room làm nơi lưu thực đơn đã chọn. Seed mới hoặc cập nhật chỉ thay kho mẫu theo phiên bản, giữ dữ liệu nghiệp vụ đã lưu.
