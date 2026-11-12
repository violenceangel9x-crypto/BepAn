# Gemini trong app Bếp ăn

Xây ngay ở mốc đầu. Người dùng thấy một màn chat kiểu Messenger/Zalo. Màn này dùng cùng dữ liệu Room và quy tắc nghiệp vụ với các nút bấm thủ công.

## Cập nhật: nhập API key trên điện thoại

Theo yêu cầu mới nhất, Cài đặt → Gemini có ô API key với hiện/ẩn, lưu/thay thế và xóa. Lưu khóa bật Gemini thật và chế độ gọi trực tiếp tới `generativelanguage.googleapis.com` qua HTTPS; không cần gateway/USB. Không tự dùng lại khóa đã gửi trong cuộc trò chuyện.

Khóa do người dùng nhập được mã hóa AES-GCM bằng khóa Android Keystore, lưu riêng trong `noBackupFilesDir`; không vào Room, DataStore, SavedState, log hoặc báo cáo. Ô nhập trống sau lưu và không hiển thị lại khóa đã lưu. Khóa chỉ nằm trong header `x-goog-api-key` của yêu cầu tới Google, không nằm trong URL/nội dung yêu cầu; không theo redirect. Khai báo năm hàm nằm chung trong `app/src/main/assets/gemini-functions.v1.json`, dùng cho cả Android và gateway.

Kết nối gateway hiện có vẫn được giữ trong phần Kết nối nâng cao, dùng khóa trên máy chủ và không nhận khóa từ điện thoại. Phần giao thức bên dưới mô tả lựa chọn gateway này. Dù dùng kết nối nào, Kotlin vẫn kiểm tra và thực hiện cùng handler, transaction, Audit và receipt.

## Dòng xử lý một lượt

1. Người dùng gửi câu tiếng Việt hoặc văn bản nhận dạng từ micro. App ghi tin nhắn và cố định ngày hiện tại, ngày/kỳ đang xem và bữa/nhóm liên quan vào ngữ cảnh của lượt.
2. Client trực tiếp hoặc gateway gửi lời nhắn, chỉ dẫn và khai báo chức năng cho Gemini. Gemini trả đề xuất gọi hàm, câu hỏi làm rõ hoặc nội dung hội thoại. Client kiểm tra cấu trúc, giới hạn một lời gọi mỗi lượt rồi chuyển đến handler chung trong app.
3. App khớp lệnh theo danh sách cho phép, giải ngày, tìm người trong Room, kiểm tra phạm vi và quyền thao tác. App lấy số liệu hoặc dựng thẻ xem trước theo quy tắc nghiệp vụ.
4. Lệnh đọc trả kết quả hoặc mở màn đích. Lệnh đơn giản đã rõ có thể ghi ngay và cho hoàn tác. Lệnh ảnh hưởng thu chi, nhiều người hoặc ngày đã chốt chờ người dùng xác nhận.
5. Sau commit, thẻ trong chat hiển thị kết quả từ app, nút xem đối tượng hoặc hoàn tác nếu còn hợp lệ. Màn thủ công nhận dữ liệu mới từ Flow.

## Giao thức gateway

Dùng gateway qua HTTPS cho cấu hình sử dụng thực tế. Có thể tái dùng server hiện có. Với repo mới, dùng gateway nhỏ chạy trên Windows lúc phát triển, ví dụ Node.js, và chọn SDK/API Gemini chính thức đang được hỗ trợ. Từ Android debug, có thể thử gateway trên Windows qua `adb reverse`. Tên model để cấu hình ở gateway, chọn model hỗ trợ Function Calling và thử bằng tài khoản Gemini thực tế. Dùng [chỉ dẫn tiếng Việt cho Gemini](../data/gemini-system-prompt.vi.md) làm bản bắt đầu.

Ví dụ hợp đồng giữa app và gateway. Đây là schema nội bộ của app, Codex đối chiếu schema khai báo hàm với phiên bản API Gemini đã chọn.

```json
{
  "requestId": "msg-0192",
  "traceId": "trace-demo-0192",
  "attempt": 1,
  "message": "Ghi tiền mua rau hôm nay 85 nghìn",
  "context": {
    "timeZone": "Asia/Ho_Chi_Minh",
    "referenceDate": "2026-11-12",
    "selectedDate": "2026-11-12",
    "selectedPeriod": "2026-11",
    "route": "TODAY"
  }
}
```

```json
{
  "requestId": "msg-0192",
  "traceId": "trace-demo-0192",
  "kind": "PROPOSALS",
  "calls": [
    {
      "callId": "call-1",
      "name": "record_purchase",
      "args": {
        "dayRef": "TODAY",
        "description": "Mua rau",
        "amountVnd": 85000
      }
    }
  ]
}
```

- `POST /v1/assistant/interpret` là đường dẫn gợi ý. Response có một loại như `PROPOSALS`, `CLARIFY`, `TEXT` hoặc lỗi giao thức. Có mã lỗi rõ cho thiếu cấu hình, giới hạn yêu cầu, timeout và dữ liệu trả về sai. Giữ bản nháp và cho gửi lại lệnh khi phù hợp.
- `requestId` gắn lượt chat. `callId` liên kết đề xuất. App tạo mã thao tác nội bộ ổn định cho bản xem trước và dùng lại mã đó khi bấm lại xác nhận sau gián đoạn. Sửa tham số tạo bản xem trước mới. Chuyển bản cũ sang trạng thái hết hiệu lực.
- `traceId` đi từ chat qua gateway và kiểm tra lệnh tới transaction Room hoặc bước lỗi. Giữ `requestId` và tăng `attempt` khi gọi lại trong cùng lượt. Mã `operationId` được cố định cho thẻ ghi đã xác định và nối Audit/receipt với Technical Log. Dùng mã theo dõi để mở chẩn đoán từ thẻ lỗi. Xem [hợp đồng log](09_DATABASE_VA_LOG.md).
- `TODAY`, `TOMORROW`, `VIEW_DATE` và ngày cụ thể `YYYY-MM-DD` có ý nghĩa riêng. “Hôm nay/ngày mai” lấy từ `referenceDate` ở lúc gửi. “Ngày đang xem” lấy từ `selectedDate`. Khi nhắc “tháng này”, dùng tháng của ngày nghiệp vụ. Hỏi lại với ngày/bữa không xác định được.
- Số tiền nhận ở app là số nguyên VND. Hiện số đã chuẩn hóa trước xác nhận. Với “85 nghìn”, kết quả phải là 85.000₫. Đối chiếu các cách nói có dấu chấm, dấu phẩy, “triệu”, “nghìn”. Hỏi lại trường hợp hai cách hiểu đều hợp lý.
- Tên khớp qua ID sau bước xác minh trong Room. Ưu tiên tên đầy đủ và nhóm. Cho tìm có/không dấu, viết hoa/thường, dùng quy tắc rõ với chữ đ. Khi có nhiều ứng viên, cho chọn tại máy. Giữ đối tượng bằng ID khi người dùng chọn, dù tên hiển thị có thể trùng.

## Danh mục lệnh ban đầu

Tên bên dưới là tên đề xuất của Gemini. Kotlin quyết định quyền thực thi và loại xác nhận.

| Tên lệnh | Trường chính | Trải nghiệm |
|---|---|---|
| `get_meal_counts`, `get_shopping_budget` | Ngày, tùy bữa và nhóm | Đọc Room, trả số suất/ngân sách, nhãn nháp/chốt |
| `get_month_summary`, `get_person_balance` | Kỳ, tùy nhóm/người | Trả số liệu đã chốt và giao dịch đã ghi |
| `open_finance`, `open_attendance` | Kỳ/ngày, nhóm, bộ lọc | Mở màn và áp dụng phạm vi |
| `set_attendance` | Người, ngày, bữa, có ăn/cắt | Ghi nháp nếu một đối tượng đã rõ và còn sửa được, có Hoàn tác |
| `schedule_meal_skip` | Người/nhóm, bữa, ngày bắt đầu/kết thúc | Cho xem phạm vi. Hàng loạt, dài ngày hoặc có xung đột yêu cầu xác nhận |
| `add_guest`, `cancel_guest` | Tên hoặc số suất, ngày/bữa, thu sau | Hiện tác động suất và tiền, xác nhận khi phát sinh hoặc thay đổi tiền |
| `suggest_menu`, `set_menu` | Ngày, bữa, ràng buộc/giữ món | Magic Menu luôn xem trước và Áp dụng. Sửa một ô nháp đã rõ có Hoàn tác |
| `record_purchase`, `record_payment` | Ngày/kỳ, số tiền, nội dung, người/nhóm | Xác nhận, lưu cùng bộ quy tắc thủ công |
| `settle_balance`, `record_rice_subsidy` | Kỳ, số tiền, người hoặc nguồn cấp | Xác nhận. Phân biệt ghi nhận thực nhận, hoàn và cấn trừ |
| `create_note` | Tiêu đề/nội dung | Lưu, có Hoàn tác |
| `close_day`, `prepare_next_day` | Ngày, tùy mở ngày kế tiếp | Chốt cần tóm tắt và xác nhận. Mở ngày nháp kế tiếp chỉ tạo một bản theo ngày |
| `render_menu_image` | Ngày, xem/sao chép/chia sẻ | Gọi chức năng xuất ảnh. Khi chia sẻ, mở trình chọn app để người dùng gửi |

Ngày đã chốt dùng luồng điều chỉnh có lý do. Lệnh thay mức tiền hoặc xóa dữ liệu nếu triển khai về sau dùng màn xác nhận chuyên biệt. Chỉ khai báo các lệnh đã có handler trong bản hiện tại để chat phản ánh đúng khả năng. Với nhiều lệnh ảnh hưởng dữ liệu, dựng bản xem trước chung. Làm rõ trường thiếu trước khi ghi. Các ghi cùng phạm vi chạy trong một transaction, và báo kết quả theo commit. Điều hướng/chia sẻ thực hiện sau khi có kết quả thích hợp.

## Xác nhận và hoàn tác

| Trạng thái | Ý nghĩa |
|---|---|
| Đang xử lý | Đã gửi, đang chờ diễn giải hoặc truy vấn |
| Cần làm rõ | Thiếu thông tin, trùng người hoặc có xung đột. Chọn ứng viên hoặc trả lời trong chat |
| Chờ xác nhận | Hiện người/ngày/bữa/kỳ, tác động và nút Xác nhận, Chỉnh sửa, Hủy |
| Đã lưu | Room xác nhận commit. Có liên kết đến dữ liệu và mã thao tác trong lịch sử |
| Hết hiệu lực | Đối tượng/giá/ngày thay đổi sau khi xem trước. Tạo bản xem trước mới |
| Đã hủy / Đã hoàn tác / Lỗi | Hiện kết quả thực tế. Thử lại dùng đúng mã thao tác nếu cần đối chiếu trạng thái trước |

Cất phiên bản dữ liệu mà bản xem trước đã đọc. Khi xác nhận, tra receipt theo `operationId` rồi kiểm tra phiên bản nếu thao tác chưa commit. Dùng khóa duy nhất chống lưu trùng. Nếu app đóng sau commit và trước khi cập nhật giao diện, mở lại đọc kết quả đã lưu. Hoàn tác qua thao tác đối ứng có Audit mới liên kết hành động gốc và kiểm tra phiên bản. Khi ngày đã chốt hoặc đối tượng đã thay đổi, hướng người dùng tới điều chỉnh phù hợp.

Trường kỹ thuật của lệnh gồm mã sự kiện, nguồn, chế độ thật/mô phỏng và kết quả theo từng bước. Nội dung chat được giữ trong màn chat theo chức năng. Báo cáo chẩn đoán xuất từ [trường cho phép](10_AI_DEBUG_CENTER_VA_HANDOFF.md).

## Micro, tệp và kết nối

- Dùng `SpeechRecognizer` hoặc luồng nhận dạng hệ thống phù hợp với API level. Bấm micro mới xin quyền, hiển thị đang nghe, bấm dừng rồi nhận chữ `vi-VN` để xem/sửa/gửi. Kiểm tra hỗ trợ nhận dạng trên thiết bị. Khi mạng, quyền hoặc ngôn ngữ không phù hợp, giữ nhập bằng chữ.
- Nút **+** dùng Photo Picker hoặc Storage Access Framework để chọn ảnh/tệp và hiện bản xem trước tại máy. Bản đầu ưu tiên lệnh chữ. Nếu bổ sung đọc ảnh hoặc tệp qua Gemini, hiển thị nội dung sắp gửi, để người dùng chủ động gửi và áp dụng cùng quy tắc xử lý lệnh.
- Giữ `GEMINI_API_KEY` trong môi trường chạy gateway, giới hạn quyền và log. App gửi lời nhắn và ngữ cảnh vừa đủ. Đối soát danh sách người, tiền, tên trùng và hồ sơ tại thiết bị. Với endpoint sử dụng thật, dùng TLS, xác thực thiết bị/người dùng phù hợp và giới hạn yêu cầu.
- Duy trì hai nguồn cung cấp tách rõ. **Gemini thật** gọi gateway. **Mô phỏng AI** dành cho debug/test, cho kết quả cố định để thử xác nhận, lỗi, trùng tên và hoàn tác. Ghi kết quả hai chế độ riêng. Khi thiếu cấu hình hoặc mất mạng, chat hiện trạng thái và các chức năng thủ công vẫn làm việc.

Nguồn chính thức. [Gemini Function Calling](https://ai.google.dev/gemini-api/docs/function-calling) · [Google Gen AI SDK cho Node.js](https://github.com/googleapis/js-genai) · [Khóa Gemini API](https://ai.google.dev/gemini-api/docs/api-key) · [SpeechRecognizer Android](https://developer.android.com/reference/android/speech/SpeechRecognizer)
