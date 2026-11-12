# Database, Audit Log và Technical Log

Dựng ba phần này cùng **AI Debug Center** và **Debug Handoff** ở mốc 0. Lấy một luồng chấm cơm bằng tay và một luồng qua Gemini làm bài thử tích hợp. Khi sửa repo đang có, đọc schema và cách ghi dữ liệu hiện tại trước khi thêm bảng hoặc migration.

| Thành phần | Dữ liệu và nơi lưu | Dùng để |
|---|---|---|
| **Database** | Room trên điện thoại. Suất ăn, thực đơn, hồ sơ, giao dịch, bản chốt, ghi chú và trạng thái xác nhận AI | Màn hình, báo cáo và Gemini đọc cùng dữ liệu đã lưu |
| **Audit Log** | Bảng `AuditEvent` cùng database nghiệp vụ | Xem ai làm, việc gì đã đổi và khi nào. Có lối vào **Ghi chú → Lịch sử hoạt động** |
| **Technical Log** | Các dòng sự kiện JSONL trong vùng lưu riêng của app. Có giới hạn dung lượng và thời gian | Xem bước diễn giải, kiểm tra lệnh, xác nhận, lỗi mạng hoặc kết quả transaction. Vẫn ghi được một số lỗi khi Room không mở được |
| **AI Debug Center** | Màn chẩn đoán đọc Technical Log và đối chiếu Audit/Room khi có | Tìm theo mã theo dõi, xem dòng thời gian và chuẩn bị báo cáo lỗi |
| **Debug Handoff** | Báo cáo đã lọc do app tạo và file `docs/HANDOFF.md` của repo | Người dùng gửi báo cáo cho GPT. Codex hoặc Antigravity dùng HANDOFF để tiếp tục sửa và kiểm thử |

## Database và biên transaction

- Dùng Room làm nguồn dữ liệu nghiệp vụ. UI thao tác tay và Gemini đi qua cùng use case/repository. Dùng khóa ổn định, ràng buộc duy nhất và migration giữ dữ liệu. Dữ liệu Debug dùng fixture riêng. Trạng thái khi đổi màn hoặc đóng app đọc lại từ Room theo [kiến trúc Android](04_KIEN_TRUC_ANDROID.md).
- Thao tác có ghi dùng `operationId` sinh tại app. Thao tác tay tạo mã khi bắt đầu một lần lưu. Thẻ AI giữ mã cùng nội dung xem trước. Bấm lại hoặc khôi phục sau khi app bị đóng dùng đúng mã cũ. Sửa nội dung xem trước tạo phiên bản thao tác mới.
- Khi lưu, kiểm tra `OperationReceipt` theo mã trước, rồi kiểm tra phiên bản dữ liệu và quyền thay đổi. Nếu mã đã có và nội dung khớp, trả kết quả đã lưu. Nếu mã trùng nhưng nội dung khác, báo lỗi `OPERATION_ID_CONFLICT`. Dùng bản tóm tắt tham số đã chuẩn hóa trong app để đối chiếu, giữ giá trị này trong dữ liệu nội bộ.
- Ghi thay đổi nghiệp vụ, `AuditEvent`, `OperationReceipt` và trạng thái thành công của thẻ AI nếu có **trong cùng transaction Room**. Biên này cũng áp dụng cho giao dịch và phân bổ thu nhóm, chốt sổ, hoàn/cấn hoặc thao tác hoàn tác. Khi lỗi, rollback toàn bộ. Thẻ chat đọc kết quả đã commit để hiện **Đã lưu**.
- Gọi Gemini, xuất file và ghi Technical Log bên ngoài transaction. Technical Log là thông tin chẩn đoán. Nếu bộ ghi log kỹ thuật lỗi, nghiệp vụ vẫn chạy khi Room và Audit có thể ghi. Nếu chưa thể ghi Audit cho thao tác nghiệp vụ, rollback và báo lưu chưa thành công.
- Sau khi khởi động lại, thẻ đang xử lý đối chiếu receipt trước khi cho tiếp tục. Nếu commit đã có nhưng thiếu dòng log kỹ thuật, hiển thị kết quả Room và ghi rõ dòng thời gian kỹ thuật bị thiếu. Khi khôi phục bản sao, giữ Audit và receipt cùng dữ liệu nghiệp vụ. Thẻ xác nhận đang chờ phải được kiểm tra lại với dữ liệu đã khôi phục.

### Bảng cần có

| Bảng | Trường chính và ràng buộc |
|---|---|
| `AuditEvent` | `eventId`, `operationId`, `eventIndex`, `traceId`, thời điểm UTC, nguồn/chế độ Gemini, loại hành động, tham chiếu đối tượng, thay đổi trước/sau theo trường, liên kết hành động trước nếu hoàn tác. Khóa duy nhất `(operationId, eventIndex)`, chỉ mục theo thời điểm và `traceId` |
| `OperationReceipt` | `operationId` là khóa chính, `traceId`, loại hành động, phiên bản/tham số đã chuẩn hóa để phát hiện trùng sai, kết quả đã commit hoặc tham chiếu đủ để trả lại kết quả, thời điểm UTC |
| `AssistantAction` | ID thẻ xác nhận, `requestId`, `traceId`, `operationId` khi có lệnh ghi, đề xuất đã kiểm tra, phiên bản dữ liệu lúc xem trước, trạng thái, liên kết receipt nếu đã lưu |

Giữ thuật ngữ cũ của repo nếu có bảng tương đương. Hạn chế sao chép hàng dữ liệu trong log. Giao dịch và lịch sử nghiệp vụ tham chiếu bản ghi nguồn khi đủ để đối soát.

## Audit Log

- Ghi cho các thay đổi có ý nghĩa đã commit. Ví dụ đổi suất, cắt bữa, sửa thực đơn, tạo/hủy khách, ghi tiền, chốt hoặc điều chỉnh sổ, hoàn/cấn và khôi phục dữ liệu. Nguồn là **Bạn**, **Gemini**, hoặc **Gemini (mô phỏng)** trong debug. Chọn trường trước/sau cần đối soát cho từng loại hành động.
- Một thao tác thay đổi một đối tượng có một sự kiện chính. Nếu thao tác hàng loạt, có thể ghi các sự kiện theo đối tượng hoặc một bản ghi tóm tắt có danh sách tham chiếu, với thứ tự ổn định trong cùng `operationId`. Lệnh đọc, hủy xác nhận hoặc lỗi chưa ghi nghiệp vụ có thể có Technical Log và trạng thái chat tương ứng.
- Audit chỉ thêm sự kiện trong thao tác thường. Sửa hoặc hoàn tác tạo hành động mới và liên kết với hành động gốc. **Ghi chú → Lịch sử hoạt động** lọc theo ngày, nghiệp vụ và nguồn. Màn chi tiết ở máy có thể đọc thông tin nghiệp vụ cần thiết để đối soát.
- Audit đi theo vòng đời dữ liệu nghiệp vụ và bản sao lưu. Thao tác dọn Technical Log giữ nguyên Audit. Khi dùng dữ liệu xuất ra ngoài máy, chỉ đưa vào báo cáo phần đối chiếu đã lọc theo [hợp đồng Debug Handoff](10_AI_DEBUG_CENTER_VA_HANDOFF.md).

## Technical Log

Một sự kiện có `schemaVersion`, `eventId`, `timestampUtc`, `level`, `component`, `eventCode`, `source`, `provider` và các trường liên kết phù hợp. Dùng mã sự kiện ổn định, cùng từ điển trong app, gateway và file báo cáo.

| Mã liên kết | Thời gian sống |
|---|---|
| `traceId` | Một luồng từ thao tác UI/tin nhắn đến kết quả. Dùng cùng mã qua gateway và khi thử lại trong cùng luồng. Hoàn tác là luồng mới có tham chiếu thao tác gốc. Gán mã cho lỗi hệ thống có thể chọn để xuất báo cáo, ví dụ lỗi mở Room |
| `requestId` | Một lượt tin nhắn gửi để diễn giải. Lần gọi lại cùng lượt giữ mã này và tăng `attempt` |
| `operationId` | Một thao tác ghi đã xác định. Giữ qua thử lại xác nhận, liên kết Technical Log, Audit và receipt |

Các mã sự kiện đầu tiên.

| Bước | Mã sự kiện |
|---|---|
| Nhận yêu cầu và gọi Gemini | `REQUEST_STARTED`, `GATEWAY_RETURNED`, `GATEWAY_TIMEOUT`, `GATEWAY_UNAVAILABLE` |
| Kiểm tra và xem trước | `VALIDATION_REJECTED`, `CLARIFICATION_REQUIRED`, `PREVIEW_READY`, `CONFIRMATION_RECEIVED`, `PREVIEW_EXPIRED`, `USER_CANCELLED` |
| Truy vấn và giao dịch | `READ_COMPLETED`, `ROOM_OPEN_FAILED`, `WRITE_STARTED`, `WRITE_COMMITTED`, `WRITE_ROLLED_BACK`, `IDEMPOTENT_REPLAY`, `OPERATION_ID_CONFLICT` |

Giữ `source` là `MANUAL`, `ASSISTANT` hoặc `SYSTEM` và `provider` là `NONE`, `GEMINI` hoặc `MOCK`. Có thể thêm `stage`, `actionType`, `route`, thời lượng ms, mã lỗi đã chuẩn hóa, HTTP status, loại ngoại lệ và vị trí lớp/hàm của app đã lọc. Dùng đồng hồ monotonic cho thời lượng. Khi retry, tăng `attempt` cho lần gọi gateway và giữ mã thao tác ghi đã xem trước. Truyền mã theo dõi trong request gateway, dùng cho chẩn đoán của gateway và app. App ghi `WRITE_COMMITTED` hoặc `WRITE_ROLLED_BACK` theo kết quả của Room.

Ví dụ một sự kiện. Lưu mỗi sự kiện trên một dòng JSONL.

```json
{
  "schemaVersion": 1,
  "eventId": "EVENT-DEMO-005",
  "timestampUtc": "2026-11-12T12:36:33.006Z",
  "level": "WARN",
  "component": "COMMAND",
  "eventCode": "PREVIEW_EXPIRED",
  "source": "ASSISTANT",
  "provider": "MOCK",
  "traceId": "TRACE-DEMO-035",
  "requestId": "MSG-DEMO-035",
  "operationId": "OP-DEMO-035",
  "actionType": "record_purchase",
  "reasonCode": "DAY_CLOSED_AFTER_PREVIEW",
  "durationMs": 6
}
```

### Lưu, lọc và giới hạn

- Có một bộ ghi nền cho file JSONL, hàng đợi giới hạn, xoay file theo dung lượng và dọn theo thời gian. Có thể dùng `filesDir` hoặc `noBackupFilesDir` riêng, cấu hình để file log không vào bản sao nghiệp vụ và bản sao hệ thống. Log phải vẫn xem được khi lỗi mở Room. Khi hàng đợi đầy hoặc ổ đĩa lỗi, báo tình trạng thiếu log khi có thể. Đọc snapshot cho xuất báo cáo để tránh kẹt luồng chính hoặc đọc dòng ghi dở.
- Mặc định đề xuất **giữ tối đa 7 ngày và 5 MiB trên máy**, vượt giới hạn nào dọn theo giới hạn đó. Debug có thêm bước chi tiết. Bản dùng thật giữ lỗi, cảnh báo và các mốc cần chẩn đoán đã chọn của lệnh AI. Tùy chọn **Dọn log kỹ thuật** cho phép xóa log trên máy từ Cài đặt.
- Giới hạn trường ngay lúc ghi và lọc lại khi xuất. Chỉ đưa mã, số đếm, trạng thái, thông tin phiên bản và tham chiếu an toàn. Loại khóa API, token, tên người, ghi chú, số tiền thật, bản ghi chấm cơm, câu lệnh/chat nguyên văn, SQL có tham số, HTTP body/header và thông tin định danh thiết bị. Dùng mã lỗi đã chuẩn hóa thay cho nội dung exception có thể chứa dữ liệu. Với gateway, áp dụng cùng quy tắc cho thông tin được gửi vào log.
- Dùng `logcat` như lối kiểm tra bổ sung trên bản debug, với dữ liệu đã lọc. Màn chẩn đoán và file báo cáo đọc dữ liệu có cấu trúc, nên có thể lọc theo mã và hoạt động offline.

Tài liệu Android. [Room transaction](https://developer.android.com/reference/androidx/room/RoomDatabaseKt) · [Room migration](https://developer.android.com/training/data-storage/room/migrating-db-versions) · [Tránh lộ dữ liệu qua log](https://developer.android.com/privacy-and-security/risks/log-info-disclosure)
