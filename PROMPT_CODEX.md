# Prompt giao Codex trong VS Code xây app Bếp ăn với Gemini

Bạn là kỹ sư Android kiêm nhà thiết kế sản phẩm. Hãy trực tiếp thiết kế, viết mã, build và kiểm thử app quản lý bếp ăn tiếng Việt. Tôi dùng Windows với VS Code và thử trên Samsung Galaxy Note 20 Ultra qua USB. App chạy trên một điện thoại Android cho một người quản lý.

## 1. Bắt đầu từ workspace

- Kiểm tra `git status`, README, AGENTS, Gradle Wrapper, module Android, application ID, phiên bản SDK/JDK và phần mã hiện có. Nếu thư mục mới, khởi tạo app Android native ở thư mục đang mở. Dùng Kotlin, Jetpack Compose, Material 3, Navigation Compose, ViewModel, Room, Coroutines/Flow và DataStore cho thiết lập phù hợp. Chọn bộ công cụ tương thích với máy rồi ghi phiên bản cụ thể.
- Đọc `AGENTS.md`, `PRODUCT.md`, `DESIGN.md`, các file đánh số 01 đến 10 trong `docs/`, `docs/HANDOFF.md`, `data/README.md` và `references/README.md`. Trong repo đã có, các file bàn giao có thể ở `docs/product-kit/`. Hợp nhất ba file bối cảnh ở gốc để Codex và Impeccable dùng được. Giữ các quy tắc và mã nguồn còn hiệu lực, chỉnh liên kết khi đổi vị trí.
- Viết kế hoạch ngắn theo từng mốc, rồi bắt đầu mốc 0 và mốc 1 trong phiên này. Với phần nghiệp vụ có mặc định thử trong tài liệu, ghi giá trị đã dùng trong HANDOFF. Khi gặp thiếu cấu hình, thiết bị hoặc khóa, vẫn triển khai và kiểm tra phần độc lập.
- Dùng dữ liệu giả trong debug. Với app đã tồn tại, giữ application ID bản đang dùng, schema và dữ liệu, viết migration khi cần. Chỉ chạy cài bản thử trên thiết bị được xác định rõ.

## 2. Triển khai đồng thời dữ liệu và chẩn đoán ở mốc 0

Làm đủ **Database, Audit Log, Technical Log, AI Debug Center và Debug Handoff** trong cùng mốc. Kiểm chứng bằng một thay đổi chấm cơm từ Hôm nay, một lệnh Gemini ghi dữ liệu và một ca Gemini gặp lỗi.

1. **Database.** Dùng Room để lưu dữ liệu nghiệp vụ và trạng thái thao tác AI. UI và Gemini gọi chung use case/repository. Có schema export, migration giữ dữ liệu, bản ghi kết quả lưu theo `operationId` và kiểm tra trước khi ghi lại.
2. **Audit Log.** Lưu ai thao tác, thời gian, loại và thay đổi trước/sau có ý nghĩa. Ghi Audit, kết quả chống trùng, thay đổi nghiệp vụ và trạng thái thẻ thành công cùng transaction Room. Hiển thị ở **Ghi chú → Lịch sử hoạt động**. Hoàn tác ghi lịch sử mới.
3. **Technical Log.** Ghi sự kiện có mã, bước xử lý, kết quả và `traceId` từ UI/Gemini đến Room. Dùng file JSONL riêng, bộ ghi nền và xoay log có giới hạn. Log kỹ thuật ghi bên ngoài transaction. Khi file log lỗi, app vẫn ghi nghiệp vụ được nếu Room và Audit hoạt động. Đọc quy tắc trong `docs/09_DATABASE_VA_LOG.md`.
4. **AI Debug Center.** Đặt ở **Cài đặt → Chẩn đoán**, mở đúng lượt từ nút **Xem chẩn đoán** trên thẻ lỗi chat. Hiện chế độ Gemini thật/mô phỏng, dòng thời gian, bước lỗi và trạng thái ghi lấy từ Room khi kiểm tra được. Hỗ trợ tìm theo mã và xem log khi offline.
5. **Debug Handoff.** Cho xem trước rồi xuất báo cáo Markdown đã lọc, có phiên bản, mã theo dõi, bước tái hiện, lỗi và trạng thái nghiệp vụ để tôi gửi GPT. Giữ khóa, token, tên thật, nội dung chat nguyên văn và dữ liệu sổ cá nhân khỏi log kỹ thuật và file xuất. Đồng thời cập nhật `docs/HANDOFF.md` cho agent tiếp nhận. Dùng `docs/10_AI_DEBUG_CENTER_VA_HANDOFF.md` và `data/debug-report-mau.md`.

Dùng các ca trong `data/logging-cases.v1.json` để kiểm thử. Kiểm tra mốc 0 gồm lưu qua khởi động lại, rollback khi Audit ghi lỗi, xác nhận lặp chỉ tạo một giao dịch, mở chẩn đoán khi Gemini timeout và xuất báo cáo đã lọc. Khi chưa có khóa Gemini, dùng gateway mô phỏng có nhãn để hoàn thành ca tích hợp và ghi rõ kết quả. Tiếp tục mở rộng công cụ chẩn đoán cùng các mốc nghiệp vụ.

## 3. Hoàn thiện bốn tab và quy trình ngày

- **Hôm nay.** Header chọn ngày, trạng thái sổ. Thẻ ngân sách đi chợ, đã mua, chênh lệch. Thẻ Thực đơn ngay bên dưới gồm bốn ô dọc theo ảnh, lần lượt sáng, trưa, tối của ngày đang xem và sáng của ngày kế tiếp. Mỗi ô có icon, ngày, tên bữa và danh sách món xuống dòng. Nút **Chỉnh sửa** mở sửa từng ô. **Magic Menu** chọn bữa hoặc món cần đổi, tạo từ mẫu, cho xem trước và bấm **Áp dụng**. Ô sáng hôm sau và ô sáng trên màn ngày kế tiếp cùng truy cập một bản ghi.
- Dưới ô cuối, đặt **Sao chép ảnh thực đơn** và **Chia sẻ**. Tạo PNG từ nội dung riêng của thẻ, gồm tiêu đề và đầy đủ bốn ô, chiều cao theo nội dung. Thử nội dung dài và phần ngoài vùng cuộn. Dùng clipboard ảnh và Android Sharesheet với quyền URI phù hợp.
- Chấm mặc định ba bữa cho cán bộ và chiến sĩ đang hoạt động. Khách có suất theo đăng ký. Cho tìm người, cắt một hoặc nhiều bữa, đặt lịch cắt/ăn lại, thêm hoặc hủy khách và rà ngoại lệ. Điểm danh, nếu có, là trạng thái riêng. Ghi tiền mua và xem trước khi **Chốt sổ và chuẩn bị ngày mai** vào buổi tối. Chốt một lần cho mỗi ngày, đóng góp ngày đó đúng một lần vào sổ tháng, hỗ trợ điều chỉnh có lý do và thay bản chốt đang dùng.
- **Tài chính.** Chọn tháng. Thẻ tổng tiền ăn, đã thu/còn phải thu, tiền mua, bù giá gạo của chiến sĩ. Có số ngày đã chốt và lối xem ngày chưa chốt. Sổ chấm cơm chia **Cán bộ · Chiến sĩ · Khách**, lọc **Có cắt cơm · Còn thiếu · Có dư**, xem thẻ người, chi tiết ngày và bảng toàn nhóm có tên cố định khi cuộn ngang. Hỗ trợ thu cá nhân, thu nhóm chiến sĩ một giao dịch quỹ với các dòng phân bổ, hoàn tiền hoặc cấn trừ, và xuất sổ theo đặc tả.
- **Ghi chú.** Tạo/sửa/tìm/ghim, lối vào **Lịch sử hoạt động** với nguồn Bạn hoặc Gemini, ngày và thay đổi. **Cài đặt.** Hồ sơ người ăn, lịch và định mức, mức bù có hiệu lực theo ngày, cấu hình/trạng thái AI, sao lưu/khôi phục và thiết lập hiển thị phù hợp.
- Dùng đúng công thức trong `docs/02_NGHIEP_VU.md`. Tiền ăn phát sinh chính thức lấy từ ngày đã chốt. Giao dịch thu/chi thực tế vào quỹ khi lưu. Bù giá gạo tính riêng cho chiến sĩ theo số bữa đã chốt, theo mức áp dụng của từng ngày. Chỉ ghi thu quỹ bù giá gạo lúc thực nhận. Giữ khoản này riêng trong quyết toán cuối tháng.

## 4. Tích hợp Gemini từ mốc đầu

- Dựng màn **Gemini — Trợ lý bếp ăn** toàn màn hình với nút/bong bóng nổi ở bốn tab. Chat theo cách dùng quen thuộc của Messenger/Zalo. Tin của tôi bên phải, Gemini bên trái, thanh tiêu đề có avatar và thu nhỏ, dưới là ngày/bữa/kỳ đang xem. Giữ lịch sử và tin đang soạn khi thu nhỏ. Thanh nhập có **+ · Ô nhắn · Micro · Gửi**, nằm trên bàn phím, kèm gợi ý lệnh theo màn.
- Viết bộ lệnh Kotlin theo `docs/05_GEMINI.md` và cho UI thủ công gọi cùng thao tác nghiệp vụ. Gemini Function Calling qua gateway trả yêu cầu có cấu trúc. Kotlin kiểm tra phạm vi, trạng thái, ID, ngày, tiền, số lượng và tính từ Room. Giữ khóa Gemini ở gateway. Dựng đường kết nối thật ở mốc đầu và thử với cấu hình hợp lệ khi có. Cung cấp mô phỏng có nhãn riêng cho debug/test.
- Lệnh đọc, điều hướng, lọc có thể thực hiện ngay. Thao tác nháp đơn giản đã xác định rõ có nút **Hoàn tác**. Thu/chi, thêm khách phát sinh tiền, hàng loạt, quyết toán, chốt sổ, điều chỉnh ngày đã chốt hoặc sửa định mức có thẻ xem trước và xác nhận. Tên trùng hiện lựa chọn ngay trong chat. Lệnh nhiều bước có xem trước chung, báo mục thiếu thông tin và áp dụng phần đã xác nhận theo một giao dịch nếu cùng phạm vi.
- Khi bấm **Xác nhận lưu**, khóa thẻ trong lúc xử lý. Sau khi lưu thành công, thẻ cập nhật thành kết quả có đường dẫn tới mục đã thay đổi. Dùng mã yêu cầu/mã thao tác và kiểm tra phiên bản để chống lưu trùng và chống dùng bản xem trước đã cũ. Hủy hoặc lỗi giữ trạng thái đúng. Ghi lịch sử với nguồn Gemini.
- Nhận giọng nói tiếng Việt theo luồng bấm micro, dừng, kiểm tra/sửa chữ nhận dạng rồi gửi. Nút **+** mở trình chọn ảnh/tệp theo quyền hệ thống. Kiểm thử nhập chữ khi từ chối micro. Với mất mạng hoặc thiếu cấu hình, giữ bản nháp và lối thao tác thủ công. Chế độ mô phỏng cần được nhận biết trong UI.
- Thử lệnh **“Hôm nay có bao nhiêu suất trưa?”**, **“Cắt bữa tối hôm nay cho Nam”**, **“Ghi tiền mua rau hôm nay 85 nghìn”** và **“Chốt hôm nay và chuẩn bị ngày mai”**. Kết quả phải thể hiện đúng số Room, có ca hoàn tác, ca trùng tên, ca xác nhận, thử gửi lại và ca ngày đã chốt.

## 5. Dựng UI/UX theo quy trình video

1. Đọc `PRODUCT.md` và dùng Impeccable `init` để bổ sung chỗ còn thiếu nếu công cụ đã sẵn sàng. Đọc `AGENTS.md` trước mọi lượt. Theo [hướng dẫn chuyển quy trình video sang Codex](docs/01_QUY_TRINH_VIDEO.md) để cài vào scope dự án và chọn cú pháp đúng phiên bản.
2. Dùng UI/UX Pro Max tra hệ thiết kế mobile Android/Jetpack Compose cho **dashboard vận hành bếp ăn, Glassmorphism, số tiền dễ đọc, tiếng Việt, thao tác một tay**. Ghi quyết định dùng được vào `DESIGN.md`. Dùng ảnh SaveSmart lấy hướng màu xanh đen, trắng lạnh, vàng, tím/cyan. Dùng ảnh Thực đơn để chốt bố cục bốn ô dọc.
3. Làm Compose theme và component chung trước, rồi dựng một luồng **Hôm nay → Chat Gemini → cập nhật Chấm cơm** có dữ liệu Room. Gắn dữ liệu từ Preview/debug cho trạng thái mẫu, ngày đã chốt, danh sách dài, chữ lớn và lỗi.
4. Chụp màn hình native hoặc dùng Compose Preview để chạy lượt nhận xét bố cục, rà lỗi và chỉnh lại với Impeccable `critique`, `audit`, `polish` khi có. Rà độ tương phản, vùng bấm, chữ tiếng Việt, thanh nhập khi bàn phím mở, vị trí bong bóng AI, phần cuộn và ảnh thực đơn. Ghi lại vấn đề, thay đổi, ảnh trước/sau nếu có và kết quả kiểm tra.
5. Áp dụng cùng hệ thiết kế lên Tài chính, Ghi chú và Cài đặt. Dùng danh sách, biểu mẫu và bảng số liệu có bề mặt rõ, giữ hiệu ứng kính cho lớp phù hợp. Kiểm thử ở cỡ chữ hệ thống lớn và với TalkBack.

## 6. Kiểm tra và bàn giao

- Theo [lộ trình và bảng nghiệm thu](docs/07_LO_TRINH_VA_NGHIEM_THU.md). Ưu tiên build debug, unit test nghiệp vụ và lint, rồi kiểm tra thiết bị nếu sẵn sàng. Kiểm tra riêng ngày chuyển tháng, phân bổ thu nhóm, bù giá gạo đổi mức giữa kỳ, ngày chưa chốt, chống trùng và khôi phục dữ liệu.
- Trên PowerShell, dùng Gradle Wrapper của repo, kiểm tra `adb devices -l`, cài APK debug đúng serial và chạy theo [08_WINDOWS_VS_CODE_GITHUB.md](docs/08_WINDOWS_VS_CODE_GITHUB.md). Thử offline, nhập liệu và quay lại sau khi đóng app. Với Gemini thật, ghi cấu hình thử ở mức tên biến/môi trường và kết quả, giữ bí mật trong môi trường chạy gateway.
- Cập nhật README của dự án với phiên bản công cụ, lệnh build, vị trí APK và cách dùng Gemini. Cập nhật `docs/HANDOFF.md` sau mỗi mốc với nhánh/commit nếu có, phần đã làm, kết quả thực tế, lỗi, quyết định mở và bước tiếp theo đủ cụ thể để Codex hoặc Antigravity tiếp tục. Khi có lỗi tái hiện được, thêm mã theo dõi, trạng thái Room/Audit, báo cáo mẫu đã lọc và ca kiểm tra lại.
- Báo cáo kết thúc phiên gồm luồng đã chạy được, vị trí mã/APK nếu đã build, bằng chứng kiểm thử và phần đang chờ. Nếu còn việc có thể thực hiện trong phiên, tiếp tục triển khai.
