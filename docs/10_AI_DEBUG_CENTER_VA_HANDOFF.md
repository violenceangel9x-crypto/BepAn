# AI Debug Center và Debug Handoff

Mục tiêu là khi một lệnh Gemini có vấn đề, người quản lý mở được đúng lần thao tác, xem lỗi xảy ra ở bước nào và xuất báo cáo để GPT hoặc Codex đọc. Dựng luồng tối thiểu này ngay trong mốc 0, song song với database, Audit Log và Technical Log.

## 1. Màn chẩn đoán trong app

Vào **Cài đặt → Chẩn đoán → AI Debug Center**. Thẻ lỗi trong chat có nút **Xem chẩn đoán** mở đúng lần thao tác. Khi lỗi xảy ra ở màn thủ công, có thể dùng cùng màn để tra mã nếu lệnh có `traceId`.

| Vùng | Nội dung |
|---|---|
| Tóm tắt | Phiên bản app, schema Room nếu đọc được, chế độ **Gemini thật / Mô phỏng**, mạng lúc thao tác, tình trạng bộ ghi log |
| Danh sách | Các lượt gần đây. Lọc **Tất cả · Lỗi · Đã xử lý** và theo nguồn Bạn/Gemini, tìm bằng mã theo dõi |
| Chi tiết một lượt | Mã theo dõi, hành động, thời gian, trạng thái, các bước theo thứ tự và mã lỗi. Tóm tắt kết quả Room/Audit khi truy vấn được |
| Thao tác | **Sao chép mã**, **Xuất báo cáo lỗi**, **Xem thao tác trong chat** nếu còn tham chiếu |
| Trạng thái đặc biệt | Chưa có lượt, log hết hạn, thiếu một số bước, không đọc được Room, hoặc xuất file thất bại. Cho biết phần nào vẫn có thể xem |

Ví dụ các bước hiển thị **Đã nhận lệnh → Gemini trả đề xuất → Đã kiểm tra → Chờ xác nhận → Room đã lưu**. Khi timeout, hiển thị **Lỗi khi kết nối Gemini**, mã `GATEWAY_TIMEOUT` và tình trạng nghiệp vụ đọc được. Khi mất một phần log, trình bày **Đã lưu theo Room** nếu tìm được receipt và báo dòng thời gian kỹ thuật chưa đủ. Màn chỉ quan sát và xuất, đường sửa hoặc thử lại mở lại luồng thao tác thích hợp trong app.

## 2. Báo cáo lỗi để gửi GPT

1. Chọn một lượt. App gom sự kiện cùng `traceId` và một tóm tắt Audit/receipt đã lọc nếu có. Mặc định xuất lượt đang xem. Khi lỗi cần thêm bối cảnh, cho chọn một số lượt liên quan, hiện phạm vi trước khi xuất.
2. Mở bản xem trước. Cho người dùng nhập ngắn **Các bước đã làm · Mong đợi · Thực tế**, giữ các ô này trống mặc định. Cho xem và sửa phần tự nhập trước khi xuất, với gợi ý dùng mô tả chung. Các trường do app tạo dùng danh sách cho phép và không đưa nội dung gốc của sổ hoặc chat.
3. Xuất file Markdown UTF-8, ví dụ `bep-an-debug-<ma-bao-cao>.md`. Có thể thêm JSON cùng schema khi cần phân tích tự động. Cho **Lưu file**, **Chia sẻ** qua Android Sharesheet hoặc **Sao chép nội dung** đã xem trước. Người dùng có thể đính kèm file hoặc dán nội dung báo cáo vào GPT.
4. Tạo báo cáo khi người dùng bấm xuất. Giữ file tạm trong cache riêng và cấp quyền đọc tạm thời cho app nhận. Dùng Storage Access Framework cho **Lưu file**. Dọn bản xem trước/cache sau thời gian phù hợp. Khi xuất lỗi, giữ nội dung xem trước và cho thử lại.

### Dữ liệu báo cáo v1

| Khối | Trường được đưa vào |
|---|---|
| Nhận diện báo cáo | `schemaVersion`, `reportId`, thời điểm xuất, `traceId`, phạm vi, số sự kiện, cờ rút gọn/thiếu |
| Môi trường | Tên và mã phiên bản app, debug/release, commit build nếu có, Android API level, phiên bản schema Room nếu đọc được, chế độ Gemini thật hoặc mô phỏng, mã model hoặc loại lỗi mạng khi có |
| Vấn đề | Các bước tái hiện, kết quả mong đợi và hiện tượng do người dùng điền. Tóm tắt trạng thái từ app |
| Dòng thời gian | Thời điểm, mã sự kiện, bước xử lý, thời lượng, kết quả, mã lỗi, mã thao tác không chứa dữ liệu cá nhân |
| Kết quả nghiệp vụ | Có/không có receipt và có/không có Audit nếu kiểm tra được. Loại thao tác và trạng thái trước/sau nằm trong danh sách cho phép. Hiện **Chưa kiểm tra được** khi Room lỗi |

Mặc định đề xuất tối đa **200 sự kiện hoặc 512 KiB mỗi báo cáo**. Khi rút gọn, giữ bước đầu, các bước lỗi, kết quả cuối và nêu số sự kiện bỏ qua. Tóm tắt nghiệp vụ xuất ra chỉ dùng trạng thái, ví dụ **Có ăn → Cắt cơm**, hoặc alias như **Đối tượng 1** trong riêng file đó. Dùng kiểm tra ở cả lúc ghi log và trước xuất. Tệp báo cáo không kèm database, file Excel, token, prompt/chat nguyên văn, thông tin định danh, số tiền thật hoặc đường dẫn riêng của thiết bị. Với phần tự nhập, người dùng duyệt lại nội dung trước khi gửi.

Xem [báo cáo lỗi mẫu](../data/debug-report-mau.md). Khi đính kèm cho GPT, có thể dùng lời nhắn sau.

> Đọc báo cáo và cho tôi biết lỗi xảy ra ở bước nào. Nêu bằng chứng theo mã sự kiện, điều còn thiếu để kết luận, cách tái hiện ngắn nhất, chỗ nên kiểm tra trong mã và một ca test để xác nhận đã sửa. Nếu app chưa ghi nghiệp vụ, ghi rõ bằng chứng hoặc trạng thái chưa kiểm tra được.

## 3. Handoff cho người tiếp tục viết mã

Dùng [HANDOFF.md](HANDOFF.md) trong repo sau mỗi mốc hoặc khi đổi máy/agent. File này ghi phiên bản app, schema, commit, cách build/test, nơi triển khai, mã theo dõi của ca tái hiện, bước đã chạy, kết quả, nguyên nhân đã xác minh hoặc giả thuyết và bước tiếp theo. Có thể đính kèm báo cáo đã lọc từ bản debug dùng dữ liệu mẫu. Dữ liệu thật và báo cáo người dùng cần được giữ ngoài Git.

Khi Codex nhận một báo cáo, yêu cầu đọc HANDOFF cùng phiên bản/commit, đối chiếu mã lỗi và trace với handler, xác minh trạng thái Room/Audit, tái hiện trên fixture nếu có và chạy lại ca sau khi sửa. [Bộ ca nghiệm thu](../data/logging-cases.v1.json) có các tình huống ghi trùng, lỗi transaction, mất log và lọc dữ liệu.

## 4. Điều kiện hoàn thành mốc đầu

- Thực hiện cùng thao tác cắt một bữa bằng tay hoặc qua Gemini ở ngày nháp. Mở lại app thấy kết quả từ Room, Audit có đúng nguồn và có thể đối chiếu một luồng trong AI Debug Center.
- Mô phỏng một lỗi Gemini, chẳng hạn timeout hoặc bản xem trước hết hiệu lực. Mở chẩn đoán từ chat và xuất một báo cáo. Báo cáo cho thấy bước lỗi, phiên bản và kết quả kiểm tra ghi dữ liệu.
- Gửi lại cùng mã xác nhận sau lỗi hiển thị. Kiểm tra database có đúng một kết quả commit và một lịch sử cho thao tác mẫu. Thử lỗi ghi Audit và kiểm tra rollback. Thử tắt bộ ghi Technical Log và kiểm tra nghiệp vụ vẫn hoạt động.
- Mở báo cáo xuất bằng trình đọc Markdown hoặc VS Code. Đối chiếu các trường và thử dữ liệu giả có tên, khóa, nội dung chat, HTTP body, số tiền. Các dữ liệu đó chỉ ở vị trí nghiệp vụ cần dùng và được loại khỏi báo cáo. Ghi kết quả vào HANDOFF.

Nguồn Android. [Chia sẻ file an toàn](https://developer.android.com/training/secure-file-sharing) · [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files) · [Rủi ro lộ dữ liệu qua log](https://developer.android.com/privacy-and-security/risks/log-info-disclosure)
