# Báo cáo lỗi Bếp ăn

Ví dụ giả lập cho ca L05. App cần tạo báo cáo tương tự từ dữ liệu đã lọc.

| Trường | Giá trị mẫu |
|---|---|
| Schema báo cáo | 1 |
| Mã báo cáo | REPORT-DEMO-035 |
| Mã theo dõi | TRACE-DEMO-035 |
| Mã thao tác | OP-DEMO-035 |
| Phạm vi | Một lượt đã chọn, 5 sự kiện, đủ dữ liệu |
| Xuất lúc | 2026-11-12T12:36:40Z |
| App | 0.1.0-demo, versionCode 1, debug |
| Môi trường | Android API 33, Room schema 2, Gemini mô phỏng |

## Tái hiện

1. Mở chat Gemini và tạo thẻ xem trước cho khoản mua.
2. Chốt ngày bằng thao tác tay ở màn Hôm nay.
3. Quay lại chat và xác nhận thẻ trước đó.

| | Mô tả |
|---|---|
| Mong đợi | Hiện lý do thẻ hết hiệu lực và cách mở lại để xem dữ liệu hiện tại |
| Thực tế trong ca mẫu | App từ chối lưu và hiện thông báo chung. Người dùng cần biết vì sao thẻ đã cũ |

## Dòng thời gian

| Giờ UTC | Bước | Sự kiện | Chi tiết |
|---|---|---|---|
| 12:34:21.000 | Chat | `REQUEST_STARTED` | Lượt AI được tạo |
| 12:34:21.091 | Gateway | `GATEWAY_RETURNED` | Gemini mô phỏng trả đề xuất |
| 12:34:21.120 | Kiểm tra | `PREVIEW_READY` | Loại thao tác ghi khoản mua |
| 12:36:33.000 | Xác nhận | `CONFIRMATION_RECEIVED` | Kiểm tra lại thẻ đã xem trước |
| 12:36:33.006 | Kiểm tra | `PREVIEW_EXPIRED` | Ngày đã chốt sau lúc xem trước |

## Đối chiếu nghiệp vụ

Room đọc được. Với mã thao tác ghi khoản mua ở trên, chưa có receipt và chưa có Audit. Báo cáo chỉ nhận định về lượt đang xem. Thao tác chốt ngày được thực hiện trong một lượt khác.
