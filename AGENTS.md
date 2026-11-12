# Chỉ dẫn cho agent làm dự án Bếp ăn

## Khi nhận việc

- Đọc yêu cầu mới nhất, README, `PRODUCT.md`, `DESIGN.md`, `docs/HANDOFF.md`, rồi mở các tài liệu liên quan trong `docs/`. Dùng `PROMPT_CODEX.md` khi bắt đầu dự án hoặc khi người dùng giao lại toàn bộ mốc.
- Kiểm tra gốc repo, Git, module, Gradle Wrapper và mã hiện có trước khi sửa. Nếu đặc tả được giữ trong `docs/product-kit/`, đọc tài liệu ở đó và giữ chỉ dẫn của repo hiện có.
- Làm phần đã giao đến mức có thể chạy và kiểm tra. Ghi rõ bước cần môi trường, thiết bị hoặc thông tin còn thiếu, tiếp tục các phần độc lập.
- Khi quyết định UX, ưu tiên yêu cầu mới nhất, sau đó đến `PRODUCT.md`, đặc tả nghiệp vụ, `DESIGN.md` và đặc tả màn hình. Video, ảnh, bản HTML và gợi ý từ công cụ giúp triển khai và đối chiếu.

## Quy tắc sản phẩm

- Kotlin, Jetpack Compose và Room trên một thiết bị Android. Bốn tab theo thứ tự **Hôm nay · Tài chính · Ghi chú · Cài đặt**. Gemini mở bằng nút nổi và có màn chat riêng trong app.
- Người ăn thường xuyên đang hoạt động được chấm mặc định ba bữa. Lịch cắt cơm, điều chỉnh từng bữa và khách thay đổi suất cho đúng ngày. Tính tiền ăn chính thức theo các ngày đã chốt. Giao dịch tiền thực tế ghi quỹ tại thời điểm lưu.
- Bù giá gạo tính riêng cho **chiến sĩ** từ ngày đã chốt, theo mức có ngày hiệu lực và theo dõi để nhận cuối tháng. Ngân sách đi chợ dùng công thức trong `docs/02_NGHIEP_VU.md`.
- Thẻ thực đơn có bốn ô dọc. Ô cuối dùng dữ liệu của bữa sáng ngày kế tiếp. Xuất ảnh bằng nội dung riêng của thẻ, từ tiêu đề đến hết ô thứ tư.
- Màn hình và Gemini gọi chung các thao tác nghiệp vụ. Gemini đề xuất lệnh, app kiểm tra, tính số và thực hiện. Lệnh có tác động tài chính, hàng loạt hoặc chốt/điều chỉnh sổ cần thẻ xem trước và xác nhận. Kết quả thành công phải dựa trên dữ liệu đã lưu.
- Từ mốc 0, triển khai cùng nhau Database, Audit Log, Technical Log, AI Debug Center và Debug Handoff. Ghi dữ liệu, Audit và receipt cùng transaction Room, dùng `operationId` để chống ghi trùng. Technical Log dùng `traceId` để theo dõi bước xử lý và được ghi bên ngoài transaction.
- Dùng [quy tắc dữ liệu và log](docs/09_DATABASE_VA_LOG.md) và [hợp đồng chẩn đoán](docs/10_AI_DEBUG_CENTER_VA_HANDOFF.md). File gửi GPT chỉ gồm dữ liệu đã lọc và mô tả người dùng xem trước. Giữ khóa, tên, nội dung chat nguyên văn, dữ liệu bữa và giao dịch thật khỏi log kỹ thuật và file báo cáo.
- Mọi luồng thủ công chạy từ dữ liệu cục bộ. Giữ riêng bản thử với dữ liệu mẫu. Theo yêu cầu mới nhất, cho người dùng nhập API key trong Cài đặt: mã hóa bằng Android Keystore, lưu trong vùng không sao lưu, chỉ gửi đến HTTPS của Google. Gateway vẫn là lựa chọn nâng cao và giữ khóa riêng trên máy chủ. Tách dữ liệu thật, sổ cá nhân, khóa và tệp sao lưu khỏi Git/log/báo cáo.

## Làm và bàn giao

- Với UI, dùng `docs/01_QUY_TRINH_VIDEO.md` cùng `DESIGN.md`. Có thể dùng UI/UX Pro Max và Impeccable khi cài được. Với Android native, kiểm tra Compose và ảnh/chạy thử trên thiết bị.
- Ưu tiên sửa theo từng luồng nhỏ. Giữ định danh app đang dùng, viết migration Room giữ dữ liệu, và duy trì lệnh build thực tế trong README.
- Chạy build và kiểm tra phù hợp. Khi có Android được cấp quyền USB, dùng bản debug có dữ liệu mẫu. Ghi rõ phần đã thử qua JVM, emulator hoặc máy thật.
- Kết thúc mỗi mốc, cập nhật `docs/HANDOFF.md` với thay đổi, kết quả thật, lệnh còn chạy được và bước tiếp theo. Khi xử lý lỗi, ghi ca tái hiện, phiên bản, mã theo dõi đã lọc và bằng chứng Room/Audit. Khi tiếp tục trên Codex hoặc Antigravity, đọc HANDOFF trước rồi làm tiếp từ trạng thái repo hiện tại.
