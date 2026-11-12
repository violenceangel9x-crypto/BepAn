# Bàn giao Bếp ăn — 13/11/2026

## Triển khai Mốc 2 — tiến độ theo từng phần

### 2.6 — Đạt kiểm tra JVM/build · 13/11/2026

- Bù gạo có Được cấp/Đã nhận/Còn chờ hoặc Nhận vượt; quyền lợi chỉ từ bữa chiến sĩ đã chốt và làm tròn một lần ở tổng kỳ. Nhận từng phần lưu một dòng thu CashEntry, kỳ quyền lợi độc lập ngày nhận.
- UI có form ngày/tiền thực nhận, preview tác động quỹ/kỳ và cảnh báo nhận vượt; khoản này không vào PaymentAllocation hay tiền nộp cá nhân. Điều chỉnh sau nhận chỉ đổi quyền lợi/chênh lệch, không tự ghi tiền mới.
- `scripts/build.ps1`: **BUILD SUCCESSFUL**, **53/53 JVM test đạt**, lint không lỗi. Đã kiểm tra 476₫; hai mức 700/900 ra 1.088₫; ba suất sáng làm tròn 286₫; nhận ngày 01/12 cho kỳ 11, nhận từng phần/vượt/replay và điều chỉnh sau nhận.
- Tiếp theo: **2.7** hợp đồng Excel, xuất SAF và mở lại file bằng Excel thật trên Windows khi có.

### 2.5 — Đạt kiểm tra JVM/build · 13/11/2026

- Schema **6**, migration 5→6 bổ sung liên kết cha của các lần hoàn từng phần, giữ ràng buộc FK và dữ liệu cũ.
- Chờ hoàn giữ phần dư nhưng không ghi chi; đã trả từng phần mới tạo CashEntry âm và Settlement con. Hủy chờ giải phóng phần còn lại, giữ các khoản đã trả. Cấn cùng người sang tháng kế tiếp ghi một liên kết hai vế nguyên tử, không sinh tiền mặt.
- Calculator phân biệt số đối chiếu, phần giữ và dư khả dụng; sau chốt/điều chỉnh nếu giữ vượt dư thì hiện cần đối soát và chặn ghi hoàn quá phạm vi. Audit lưu trước/sau số đối chiếu và phần giữ, log kỹ thuật không chứa các số này.
- `scripts/build.ps1`: **BUILD SUCCESSFUL**, **51/51 JVM test đạt**, lint không lỗi. Ca mới kiểm tra hoàn từng phần/replay/hủy, rollback hai vế cấn và hai preview tranh cùng phần dư; chốt sau giữ làm giảm dư không cho hoàn cũ.
- Tiếp theo: **2.6** thực nhận bù gạo theo kỳ quyền lợi và ngày tiền độc lập.

### 2.4 — Đạt kiểm tra JVM/build

- Thu cá nhân/nhóm từ Tài chính: chọn người, số tiền, kỳ và ngày thực nhận độc lập; có preview từng phần chia đồng lẻ theo ID cố định. Phân bổ lưu cả nhóm tại lúc xem trước, không tính lại theo nhóm hiện tại sau thu.
- Một CashEntry + các PaymentAllocation + Audit/receipt cùng transaction; bản nháp lưu Room. Có chứng từ sau commit, kiểm tra token tài chính khi xác nhận và đối chiếu receipt trước kiểm tra stale khi replay.
- `scripts/build.ps1`: **BUILD SUCCESSFUL**, **48/48 JVM test đạt**, lint không lỗi. Đã kiểm tra 1.000.001₫/3 người, 21.600.000₫/10 người, 4 confirm đồng thời cùng ID, payload khác ID cũ, nhóm đổi sau thu, kỳ ăn cũ/ngày tiền khác kỳ, rollback Audit và stale sau chốt.
- Tiếp theo: **2.5** giữ dư/chờ hoàn, hoàn từng phần, hủy và cấn sang kỳ sau.

### 2.3 — Đạt kiểm tra JVM/build

- FinanceMonth/FinanceCalculator là nguồn tính theo YearMonth: suất chốt có hiệu lực, dự kiến theo ngày, công nợ, nhóm lịch sử, thu/chi theo ngày tiền, quyền lợi bù và phiên chốt. Query ClosedMeal/Day/CloseRevision theo khoảng ngày; màn không tự cộng từ toàn bộ lịch sử.
- Tài chính có tháng riêng với Hôm nay, ba nhóm, lọc cắt/thiếu/dư, thẻ người, bảng S/T/C cuộn ngang giữ tên bên trái và hàng LazyColumn có khóa ID. Người đổi nhóm có suất ở từng nhóm lịch sử; công nợ là một số toàn kỳ, tổng nhóm tiền ăn không nhân đôi.
- Có ngày đã tới chưa chốt/tương lai, liên kết về Hôm nay, chi tiết bữa/nhóm và lý do các phiên chốt; mở quỹ có preview. Draft tài chính trong Room được phục hồi khi mở lại.
- `scripts/build.ps1`: **BUILD SUCCESSFUL**, **45/45 JVM test đạt**, lint không lỗi. Ca mới kiểm tra lịch 28/29/30/31, không tự tạo ngày khi đọc sổ, khách/người ngừng ăn, tổng nhóm, projection và điều chỉnh thay đóng góp ngày.
- Tiếp theo: **2.4** thu cá nhân/nhóm, đóng băng phân bổ và chứng từ thực lưu.

### 2.2 — Đạt kiểm tra JVM/build

- Schema **5**, migration 4→5 thêm CashEntry/PaymentAllocation/Settlement, phiên bản quỹ, số dư mở sổ và bản nháp xác nhận tài chính. Đường nâng từ v1/v2 vẫn giữ người, chat, suất chốt và Purchase/Audit/receipt.
- Mỗi Purchase mới ghi đúng một CashEntry cùng transaction. Migration sinh một dòng chi cho mỗi Purchase cũ, giữ ID/tham chiếu duy nhất; lấy ngày thực ghi từ receipt khi có, đánh dấu legacy khi thiếu, không tạo receipt thu/chi giả.
- FinanceRepository tách kỳ nghiệp vụ/ngày tiền thực tế khỏi ngày ăn; có token tài chính riêng (ghi chú không làm cũ preview tiền), preview lưu Room, hủy, replay và số dư mở quỹ. Số dư mở sổ không giả thành khoản vừa thu; chưa có đầu quỹ thì cashBalance trả null.
- `scripts/build.ps1`: **BUILD SUCCESSFUL**, **42/42 JVM test đạt**, lint không lỗi. Ca mới kiểm tra một chi duy nhất, mở quỹ, phục hồi preview sau mở lại DB, replay sau commit, rollback Audit và stale khi chốt ngày.
- Tiếp theo: **2.3** read model sổ tháng, bảng ngang/chi tiết và nguồn tính dùng chung.

### 2.1 — Đạt kiểm tra JVM/build, native tổng hợp ở 2.9

- Schema **4**, migration 3→4 tạo MembershipPeriod từ ngày vào/ngừng đã có. Nhóm/hoạt động được giải theo ngày, ngày chốt vẫn dùng snapshot; giữ ID và không xóa người có lịch sử.
- Cài đặt → Quản lý người ăn & định mức có form thêm/sửa hồ sơ, đổi nhóm/ngừng/ăn lại và giá/phí/gram/mức bù theo ngày hiệu lực. Có preview/xác nhận; trùng ngày hiệu lực bị từ chối để không ghi đè lịch sử. Bản form giữ khi preview bị hủy/lỗi.
- ConfigurationRepository và AuditedWriter lưu cấu hình + Audit + receipt cùng transaction; replay so fingerprint, kiểm tra token và làm cũ preview ngày nháp bị ảnh hưởng. Technical Log vẫn ngoài transaction.
- `scripts/build.ps1`: **BUILD SUCCESSFUL**, **38/38 JVM test đạt**, lint không có lỗi. Ca mới kiểm tra đổi nhóm/ngừng/ăn lại, đổi giá, snapshot chốt, stale, replay và rollback khi Audit lỗi.
- Commit baseline 2.0: `f33f7b6`. Tiếp theo: **2.2** một nguồn quỹ CashEntry, liên kết Purchase, phạm vi lệnh tài chính và migration.

### 2.0 — Đạt kiểm tra JVM/build, native tổng hợp ở 2.9

- Đã tái hiện lỗi khách bằng HistoricalMealsTest trước sửa (1/2 ca thất bại); sau sửa khách chỉ đăng ký trưa không bị mở sáng/tối sau chốt. Hủy bữa khách đã đăng ký vẫn giữ đúng trạng thái cắt.
- Schema **3**, migration **2→3** thêm registered nullable. Với sổ cũ, người thường xuyên hoặc bữa đang ăn có bằng chứng đăng ký; bữa khách bị tắt thiếu bằng chứng giữ NULL/chưa xác định, không sửa tiền cũ. CloseRevision mới giữ registered; bản revision cũ giữ nguyên.
- Serving giữ unitPrice của ClosedMeal; preview điều chỉnh dùng đúng giá này dù định mức hiện tại đổi. Finance tra tên từ toàn bộ hồ sơ ID và chỉ đếm bữa đã đăng ký bị cắt.
- Diagnostics lấy schema từ hằng dùng chung. Phiên bản hiện tại **0.2.0-demo/code 3**, package giữ nguyên.
- `scripts/build.ps1`: **BUILD SUCCESSFUL**, **34/34 JVM test đạt**, lint **0 lỗi/30 cảnh báo**. Có ca migration từ schema v2 giữ khoản mua 85.000₫ và tiền bữa khách, cùng ca nâng v1 qua v2 lên v3.
- Đã có commit baseline cục bộ `f33f7b6` từ nguồn/dữ liệu mẫu đã rà; không đẩy remote. Phần này kết thúc trước khi bắt đầu **2.1**.

## Rà toàn dự án và xác định mốc tiếp theo — 12/11/2026

Yêu cầu phiên này: quét dự án, lập mục tiêu lớn tiếp theo và kế hoạch cụ thể. Đã cập nhật tài liệu; chưa triển khai các nghiệp vụ tài chính mới.

- Báo cáo có bằng chứng mã, phạm vi đã rà và giới hạn: [PROJECT_AUDIT_2026-11-12.md](PROJECT_AUDIT_2026-11-12.md).
- Kế hoạch tiếp theo: [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md), **Mốc 2 — chu trình tài chính tháng đối soát được**, chia 2.0–2.9 với phụ thuộc/đầu ra/tiêu chí đạt. Kéo hồ sơ và định mức có hiệu lực vào đầu mốc; backup/restore đầy đủ là cổng Mốc 3 trước dữ liệu thật.
- Điểm xuất phát thực tế: `:app`, package debug `vn.bepan.debug`, schema 2, Gradle **0.1.1-demo/code 2**. Tiêu đề “0.1.2-demo” ở phần bàn giao UI dưới là tên đã ghi ở phiên trước; không khớp phiên bản cấu hình hiện tại. Khi triển khai tiếp phải thống nhất metadata thay vì coi đó là APK 0.1.2 đã kiểm chứng.
- Git `master` chưa có commit; lúc bắt đầu có 329 mục ` A` trong working tree và 22 mục `??` theo cách đếm mặc định; diff đã staged rỗng. Giữ nguyên phần người dùng đang làm; phiên này không stage/commit/reset hoặc đẩy remote.
- **F01 cần làm đầu tiên:** ClosedMeal thiếu trạng thái đăng ký của khách, snapshot ngày chốt gán registered=true, Finance đếm mọi bữa không ăn là cắt. Khách chỉ đăng ký trưa có thể bị tính sáng/tối chưa đăng ký thành bữa cắt. Đây là phát hiện từ source, chưa sửa hoặc tái hiện bằng native trong lượt này.
- **F04/F05:** tên người lịch sử đang tra từ ngày Hôm nay; preview điều chỉnh đang dùng Rate hiện tại thay vì đúng giá snapshot bữa chốt. Cần ca hồi quy trước khi thêm UI giá và sổ tháng.
- Thiết kế CashEntry phải tránh trừ lại Purchase; lệnh tiền cần kỳ nghiệp vụ/ngày quỹ/phiên bản tài chính riêng, không dùng nguyên điều kiện ngày nháp. Có sổ chuyển tiếp/số dư đầu có căn cứ trước khi gọi thu−chi là quỹ thật.

Kết quả vừa chạy: `scripts/build.ps1` **BUILD SUCCESSFUL**; tiếp đó `scripts/build.ps1 -Tasks @(':app:testDebugUnitTest','--rerun-tasks')` **30 task thực thi, 30/30 test đạt**. `scripts/gateway.ps1 -Mode test` **6/6 đạt**. Lint hiện **0 lỗi, 30 cảnh báo**. Cảnh báo SDK XML không chặn build. Node chưa ở PATH; helper gateway tìm Node của Visual Studio và chạy được.

ADB nhận Samsung SM-N986U1 có quyền, nhưng phiên lập kế hoạch này chỉ kiểm tra kết nối: không cài app, không chạy connected tests, không dùng khóa hoặc gọi Gemini thật, không thay dữ liệu điện thoại. Bằng chứng native/Gemini bên dưới thuộc các phiên trước. Chưa build release trong lượt này.

**Bắt đầu phiên tiếp:** đọc báo cáo và kế hoạch → làm 2.0 (fixture khách/giá/tên lịch sử, sửa và migration giữ tiền) → 2.1 (hồ sơ/định mức có hiệu lực) → 2.2 (cash ledger và chuyển tiếp). Các lệnh build/gateway/thiết bị ở cuối kế hoạch và README còn áp dụng.

## Cập nhật 0.1.2-demo · Tái cấu trúc module UI và tinh chỉnh toàn diện UI/UX

Yêu cầu: Tự động hoàn thiện 3 giai đoạn UI/UX (Chuẩn hóa tokens & tách module; Tinh chỉnh thẩm mỹ & micro-interactions; Kiểm thử trên Samsung Note 20 Ultra SM-N986U1).

1. **Chuẩn hóa Design Tokens & Tách module UI**:
   - `Theme.kt`: Bổ sung toàn bộ semantic tokens theo `DESIGN.md` (`MutedOnLight`, `MutedOnDark`, `SuccessLight`, `SuccessDark`, `ErrorLight`, `ErrorDark`, `WarningLight`, `BorderLight`, `SlotCardBg`, `NextDayCardBg`, `DisabledContainer`, `DisabledContent`, `SunIconColor`, `UnfocusedBorder`). Loại bỏ hoàn toàn các mã màu hardcode cục bộ.
   - Tách tệp nguyên khối `KitchenApp.kt` (575 dòng) thành các module sạch sẽ, dễ bảo trì và hỗ trợ preview độc lập:
     - `vn.bepan.ui.components`: `CommonComponents.kt` (Section, ScreenHeader, DateHeader, LightAction, StatusBanner), `MenuComponents.kt` (MenuCardContent).
     - `vn.bepan.ui.dialogs`: `MenuEditorDialog.kt`, `PurchaseDialog.kt`, `GuestDialog.kt`, `ScheduleDialog.kt`, `AdjustAttendanceDialog.kt`, `ManualConfirmDialog.kt`.
     - `vn.bepan.ui.screens`: `TodayScreen.kt`, `FinanceScreen.kt`, `NotesScreen.kt`, `HistoryScreen.kt`, `SettingsScreen.kt`, `ChatScreen.kt`, `DiagnosticsScreen.kt`.
     - `KitchenApp.kt`: Thu gọn thành Navigation Shell điều hướng nhẹ nhàng, an toàn.

2. **Tinh chỉnh Thẩm mỹ & Trải nghiệm (Visual & UX Polish)**:
   - **Hôm nay**: Thẻ Ngân sách bổ sung thanh tiến trình trực quan (`LinearProgressIndicator`), badge phân biệt rõ ràng trạng thái Còn lại (xanh lá) / Vượt ngân sách (đỏ); thẻ Thực đơn 4 ô nổi bật bữa sáng ngày kế tiếp (`NextDayCardBg` viền tím); thẻ Người ăn thêm avatar tròn chữ cái đầu và badge phân nhóm rõ nét; các nút hành động tối ưu cho thao tác 1 tay.
   - **Tài chính**: Bộ chọn tháng dạng card nổi trực quan; các thẻ thống kê tổng tiền ăn, tiền mua thực tế, bù giá gạo có icon và khối màu phân biệt; danh sách người ăn có badge cắt cơm nổi bật.
   - **Chat Gemini**: Bong bóng chat bo góc tin nhắn hiện đại; thẻ hành động (`AssistantAction`) phân biệt rõ trạng thái Chờ xác nhận (vàng), Đã lưu (xanh kèm nút Hoàn tác); gợi ý lệnh dạng chip tối; composer nhập liệu cải tiến nút gửi tin nhắn.
   - **Ghi chú & Lịch sử**: Thẻ ghi chú đã ghim có viền tím nhận diện; lịch sử audit dạng card timeline phân biệt rõ nguồn thao tác (Bạn vs Gemini).
   - **Cài đặt & Chẩn đoán**: Giao diện cài đặt tinh gọn, hiển thị badge trạng thái API key (Đã lưu / Chưa lưu), nút truy cập AI Debug Center nổi bật.

3. **Kiểm thử trên Samsung Note 20 Ultra SM-N986U1**:
   - **30 test JVM đạt** 100%.
   - **Lint**: 0 lỗi.
   - Đã cài đặt APK debug thành công lên thiết bị thật `R5CN70V0BED` qua `scripts/install-debug.ps1`, bảo toàn toàn bộ dữ liệu.
   - Đã chụp ảnh màn hình thực tế từ thiết bị:
     - `docs/evidence/note20/screen-today.png`
     - `docs/evidence/note20/screen-finance.png`
     - `docs/evidence/note20/screen-notes.png`
     - `docs/evidence/note20/screen-settings.png`
     - `docs/evidence/note20/screen-chat.png`


## Cập nhật 0.1.1-demo · Nhập API key trong Cài đặt

Yêu cầu mới nhất: người dùng tự nhập API key ngay trong app. Đã thêm ô mật khẩu **API key Gemini**, hiện/ẩn, lưu/thay thế và xóa. Lưu khóa bật Gemini thật, dùng kết nối trực tiếp tới Google qua HTTPS; gateway cũ còn trong **Kết nối nâng cao**. Không tự dùng lại khóa trong cuộc trò chuyện. Samsung đã được cập nhật bằng `install -r`, không gỡ app/xóa dữ liệu, và đã kiểm tra ô nhập xuất hiện.

- `assistant/ApiKeyStore.kt`: AES-256-GCM bằng Android Keystore, ghi AtomicFile tại `noBackupFilesDir/credentials/gemini.bin`. Không có khóa trong Room, DataStore, SavedState, Technical Log hoặc báo cáo. Ô nhập trống sau lưu/mở lại, không đọc khóa đã lưu vào UI. Xóa khóa xóa cả file và alias Keystore.
- `assistant/DirectGeminiGateway.kt`: header `x-goog-api-key` chỉ gửi tới endpoint HTTPS cố định của Google, không theo redirect; giới hạn response, chỉ cho năm hàm, lọc lỗi thành mã an toàn và không hiển thị nội dung thought. Chỉ diễn giải; cùng handler/repository/Audit/receipt với gateway và thao tác tay. Model mặc định `gemini-3.6-flash`, theo tài liệu Google thay thế 3.5 Flash.
- Năm Function Declaration dùng chung từ `app/src/main/assets/gemini-functions.v1.json`; server đọc cùng asset. Cấu hình gateway cũ tự giữ nguyên cho cài đặt đã có; lưu API key mới chuyển sang trực tiếp. Khóa trên điện thoại không được gửi sang gateway.
- Phiên bản `0.1.1-demo` (code 2), package và Room schema 2 giữ nguyên. Không có migration dữ liệu nghiệp vụ trong thay đổi này.
- **30 test JVM đạt**, gồm 6 ca transport trực tiếp/khóa trong header/cấm redirect/lỗi đã lọc/response quá lớn và hàm lạ; **6 test gateway đạt**, lint **0 lỗi, 28 cảnh báo**. Ca native mới đã kiểm tra Keystore thật, ciphertext không chứa khóa mẫu, đọc lại qua Activity recreate, xóa khóa và báo thiếu khóa khi chat.
- Lượt native tổng hợp đầu bị gián đoạn bởi hộp thoại emulator **Process system isn't responding**; ảnh bằng chứng ở `.tools/emulator-system-anr.png` (không vào Git). Đã khởi động lại emulator và giải phóng daemon Gradle đang idle trước khi chạy lại.
- Chưa gọi Gemini trực tiếp bằng khóa thật trong phiên sửa này: người dùng sẽ tự nhập khóa. Các bằng chứng Gemini thật ở phần dưới thuộc kết nối gateway của bản 0.1.0, không dùng để tuyên bố client trực tiếp đã được thử bằng tài khoản thật.

Lệnh kiểm tra: `scripts/build.ps1`; native trên emulator: `scripts/build.ps1 -Tasks @(':app:connectedDebugAndroidTest', '-Pandroid.testInstrumentationRunnerArguments.class=vn.bepan.ApiKeySettingsTest,vn.bepan.NativeFlowTest')`. Test khóa chỉ dùng dữ liệu giả và bỏ qua nếu thiết bị đã có khóa. Không chạy connected test có thể gỡ target app trên điện thoại người dùng; cập nhật APK bằng `scripts/install-debug.ps1 -Serial <serial>`.

Các phần bên dưới ghi lại mốc nền và kết quả bản 0.1.0; dùng trạng thái 0.1.1 ở trên cho thay đổi mới nhất.

## Trạng thái tiếp nhận

Workspace ban đầu chỉ có bộ đặc tả và dữ liệu mẫu, chưa có Git/module Android. Đã tạo app native và Git cục bộ. Nhánh `master`, chưa có commit hay remote; các file mới đã được đánh dấu intent-to-add để xem diff, chưa commit/push. Đọc AGENTS, PRODUCT, DESIGN và file này trước khi sửa tiếp. Kế hoạch ở [IMPLEMENTATION_PLAN](IMPLEMENTATION_PLAN.md).

**Mốc 0 đã có luồng chạy được bằng mô phỏng, với đủ năm phần nền tảng; đã gọi Gemini thật và đối chiếu bốn câu mẫu. Mốc 1 đã triển khai các luồng Hôm nay và bốn ca native đạt trên Samsung; còn các mục nghiệm thu bên dưới. Mốc 2–3 chưa hoàn thành.** Bản này dùng dữ liệu giả, chưa coi là bản vận hành thật. Kết nối Gemini có lỗi 503/timeout thực tế, cần giữ riêng kết quả chức năng và độ ổn định dịch vụ.

## Công cụ và lệnh đã chạy

| Mục | Giá trị |
|---|---|
| App | `vn.bepan`; debug `vn.bepan.debug`, tên Bếp ăn · Dữ liệu mẫu |
| Phiên bản | `0.1.0-demo` (code 1), Room schema 2 |
| JDK | Microsoft OpenJDK 21.0.13, portable trong `.tools/jdk/`, không vào Git |
| Gradle / AGP | 8.13 / 8.11.1; Wrapper có SHA-256 distribution |
| Kotlin / KSP | 2.1.20 / 2.1.20-1.0.32 |
| Compose BOM / Room | 2025.05.01 / 2.7.1 |
| Android SDK | min 26, compile/target 35, build-tools 36.0.0 |
| Gateway | Node 24.17.0 có trên máy dưới Visual Studio; helper tìm được dù Node chưa có trong PATH |
| Thiết bị đã thử | Emulator riêng `BepAn_API34`, Android 14/API 34, x86_64, WHPX/SwiftShader |
| Điện thoại | Samsung Note 20 Ultra `SM-N986U1`, Android 13/API 33; cỡ chữ 1,3×, giữ nguyên mật độ hiển thị của máy |

```powershell
.\scripts\build.ps1
# Tương đương: .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
$env:ANDROID_SERIAL = 'emulator-5554'
.\scripts\build.ps1 -Tasks @(':app:connectedDebugAndroidTest')
.\scripts\gateway.ps1 -Mode test
.\scripts\install-debug.ps1 -Serial 'emulator-5554'
```

- APK: `app/build/outputs/apk/debug/app-debug.apk` (khoảng 19 MB).
- SHA-256 APK cuối: `DCC8B8BCABABCD9058ADF16E83FC953053D4DD9CCBB3B250FD814280BDC2C4D1`.
- JVM: **24 test đạt**, gồm Room thật dưới Robolectric/SQLite, kế toán thuần, migration, parser mô phỏng và renderer.
- Native: **4 luồng đạt trên Samsung/API 33 và cùng 4 luồng đạt trên emulator/API 34**, sau thay đổi UI cuối: giữ vị trí lịch sử chat; mua–chốt–điều chỉnh/ảnh dài; từ chối micro vẫn nhập chữ; trùng tên–cắt–hoàn tác–timeout–báo cáo. Thêm **1 test Gemini thật đạt trên Samsung**, bao gồm 4 lệnh và retry. Bản sao JUnit ở `docs/evidence/note20/{native,gemini}-results.xml` và `docs/evidence/native/native-results.xml`.
- Gateway: **6 test đạt**, gồm HTTP cục bộ với upstream giả, cấu trúc Function Calling, timeout, thiếu cấu hình và lọc lỗi.
- Lint: **0 lỗi, 28 cảnh báo**, chủ yếu dependency/SDK; còn icon app, đọc layout state trong composition, insets/configuration và gợi ý Kotlin. Không dùng baseline để che lỗi; chưa profile hiệu năng.
- Connected test có thể gỡ app thử khi kết thúc; chỉ chạy trên emulator/thiết bị thử được chỉ định, cài lại bằng helper khi muốn mở app.
- Script PowerShell dùng chuỗi ASCII trong mã để chạy được trên Windows PowerShell 5.1 không cần BOM. `local.properties` đã escape dấu `:` và được bỏ khỏi Git.
- Lượt UI cuối: thu gọn header khi bàn phím mở, ẩn hàng câu gợi ý lúc nhập và consume Scaffold padding để không đệm IME hai lần. Đã chạy lại build/JVM/lint và bốn ca native Samsung sau sửa. Luồng Gemini thật đã đạt trước thay đổi bố cục này; handler/protocol không đổi.
- Cuối phiên đã cài lại APK mới trên Samsung và emulator sau connected tests. Samsung đã lưu `http://127.0.0.1:8787`, tắt mô phỏng, adb reverse 8787 và gateway foreground đang hoạt động. Force-stop/mở lại app vẫn tải ngân sách mẫu và giữ chế độ Gemini thật; [ảnh sẵn sàng](evidence/note20/gateway-ready.png). Không coi đây là kiểm thử đầy đủ việc khôi phục tiến trình cho mọi luồng.

## Phần đã triển khai

### Nền dữ liệu và chẩn đoán

- `data/Database.kt`: Person, Day, MealOverride, Schedule, Menu, Purchase, ClosedMeal, CloseRevision, Rate, Audit, Receipt, AssistantAction, ChatMessage, Note. Export cả schema 1 và 2; migration 1→2 thêm chế độ/ngữ cảnh/lần thử của chat, giữ người ăn, Audit, receipt và tin cũ. Không có destructive fallback.
- `data/KitchenRepository.kt`: thao tác tay và Gemini cùng `commit`. Trong transaction kiểm tra receipt/hash tham số trước, sau đó trạng thái thẻ, trạng thái ngày và version. Ghi nghiệp vụ, Audit, receipt và trạng thái SAVED cùng transaction. Hoàn tác là thao tác mới liên kết gốc; bản cũ/stale bị chặn.
- `diagnostics/TechnicalLog.kt`: JSONL riêng trong `noBackupFilesDir/technical`, một writer IO, queue 256, chunk khoảng 256 KiB, giữ tối đa 7 ngày/5 MiB. Lỗi logger không chặn Room/Audit. Snapshot đợi writer và lọc theo kiểu/enum/UUID; không nhận chuỗi exception/body/tên/số tiền/chat.
- AI Debug Center độc lập với việc mở Room. Từ thẻ lỗi mở đúng trace; chi tiết được đặt trước danh sách lượt. Có lọc lượt, tìm UUID, tra receipt ngay cả khi log mất, hiện UNKNOWN khi chưa đọc được Room, dọn log kỹ thuật.
- Debug Handoff: preview Markdown, mô tả trống mặc định, giới hạn 200 sự kiện/512 KiB, cho Lưu qua SAF, chia sẻ FileProvider hoặc sao chép sau khi xem. Không đưa dữ liệu nghiệp vụ/Audit nguyên văn vào báo cáo.

### Hôm nay và chat

- Bốn tab đúng thứ tự. Theme Material 3 xanh đen/trắng/vàng/tím; bubble Gemini kéo được trong vùng an toàn, chat riêng và giữ draft qua DataStore.
- Debug seed 10 người giả: 2 cán bộ, 8 chiến sĩ, hai người tên Nam. Người thường xuyên mặc định ba bữa; khách chỉ có bữa đăng ký. Chạm đổi bữa nháp, cắt cả ngày có preview, lịch cắt/ăn lại, thêm khách đích danh thu sau và hủy bữa khách đã đăng ký.
- Lịch bắt đầu từ ngày đang chọn, kết thúc bao gồm ngày cuối. Ăn lại từ X đóng lịch trái ngược trước đó ở X−1; lịch tương lai xung đột yêu cầu xử lý. Override trực tiếp giữ ưu tiên; thay lịch làm cũ các preview ngày nháp liên quan.
- Ngân sách = suất × (giá − gạo − gia vị − chất đốt). Purchase là giao dịch chi thực tế hiện tại, cùng bản ghi dùng cho tổng ngày và tháng. Mốc 2 cần thống nhất thêm thu/quỹ mà không đếm lại Purchase.
- Thực đơn bốn ô, sáng ngày kế tiếp dùng khóa ngày/bữa chung. Editor có Magic Menu offline theo weekday/weekend, chọn cả bữa hoặc một món; gợi ý → áp dụng vào draft → lưu. Hủy giữ bản đã lưu. Preview menu có version; lỗi lưu giữ chữ đang sửa.
- PNG render từ dữ liệu đã lưu, độc lập scroll và có chiều cao theo nội dung. Đã đọc lại URI clipboard trên Android và kiểm tra ảnh dài **1080×3125**, đủ bốn ô và món cuối. Chia sẻ đi qua Sharesheet/FileProvider; chưa thử dán ảnh trong một ứng dụng bên thứ ba trên Samsung.
- Ghi mua xem trước; chốt giữ snapshot suất/nhóm/giá/ngân sách/bù gạo/menu và chuẩn bị đúng một ngày kế tiếp. Tổng tháng chỉ đọc các dòng chốt đang hiệu lực. Điều chỉnh suất ngày đã chốt cần lý do và preview chênh lệch; giữ CloseRevision cũ, không cộng trùng ngày. Khoản mua đã ghi giữ nguyên.
- Gemini hỗ trợ đúng 5 hàm đã khai báo: hỏi suất, ngân sách, đổi một bữa, đề xuất ghi mua, đề xuất chốt. Trùng tên chọn tại máy; thu chi/chốt cần xác nhận. Thẻ thành công đọc từ trạng thái Room. Retry giữ request/trace/ngày tham chiếu, tăng attempt, kiểm tra action/receipt đã có.
- Micro dùng SpeechRecognizer vi-VN, nút dừng, chữ đưa vào composer để sửa rồi mới gửi. Nút + dùng OpenDocument, chỉ xem lựa chọn tại máy, chưa gửi nội dung tệp cho AI.
- Tài chính hiện xem tổng ngày đã chốt, tiền mua, bù gạo và thẻ nhóm. Ghi chú có tạo/sửa/tìm/ghim; lịch sử có nguồn Bạn/Gemini/Mô phỏng. Cài đặt hiện cấu hình gateway và chẩn đoán.

## Kết quả kiểm tra và bằng chứng

| Ca | Kết quả |
|---|---|
| L01 | Cắt bữa, đóng/mở lại file Room dưới Robolectric: 9 suất, 1 Audit, 1 receipt |
| L02 | 4 confirm đồng thời cùng operationId: đúng 1 Purchase/Audit/receipt, cùng kết quả và thẻ SAVED; khác tham số bị chặn |
| L03–L05 | Timeout không ghi; tiền âm rollback; preview cũ/ngày đã chốt bị từ chối |
| L06 | Fault sau ghi nghiệp vụ, trước Audit: rollback nghiệp vụ, receipt, version và thẻ |
| L07 | Logger lỗi: vẫn ghi nghiệp vụ + Audit + receipt, báo incomplete |
| L08 | Marker khóa/tên/chat/header/số tiền giả không xuất hiện trong technical event/report; lỗi gateway cũng chỉ trả mã an toàn |
| L09 | Test xoay theo dung lượng, quá 7 ngày và dọn log, giữ nguyên nghiệp vụ/Audit |
| L10 | Mở lại Room khi không có WRITE_COMMITTED: đọc được receipt, replay không nhân đôi, báo dòng thời gian thiếu |
| L11 | Room không dùng được: báo UNKNOWN, vẫn render báo cáo từ log riêng |
| Migration | Tạo DB theo schema 1 export, thêm người/chat/Audit/receipt, nâng lên 2 và kiểm tra còn đủ dữ liệu |
| Native chat | Hỏi suất → hai Nam → chọn người → cắt tối → hoàn tác → timeout → đúng trace → preview → sao chép Markdown |
| Gemini thật trên Samsung | `RealGatewayTest` đạt: đọc 10 suất, hỏi chọn hai Nam, cắt/hoàn tác, khoản mua chỉ lưu sau xác nhận, replay giữ đúng một Purchase/Audit/receipt, chốt có preview và tạo ngày nháp kế tiếp. Một lượt timeout, bấm retry giữ cùng trace/request, attempt 2 thành công |
| Native Hôm nay | Cắt/ăn lại bằng chip → menu dài ngoài viewport → clipboard URI/PNG → mua 85.000₫ → chốt → điều chỉnh có lý do; tổng tiền ăn 691.200₫, tiền mua vẫn 85.000₫ |
| Samsung | Bốn ca native đạt trên API 33/cỡ chữ 1,3×; đọc lại URI ảnh dài, từ chối quyền micro và tiếp tục nhập lệnh chữ |
| Chat có lịch sử | Tái hiện trên Samsung: gửi tin khi bàn phím mở, phản hồi nhanh ngắt animation cuộn nên nằm ngoài viewport. Giữ trạng thái theo tin mới qua lần hủy animation; thao tác kéo lên dừng theo, nút Tin mới nhất bật lại. Ca hồi quy giữ tin đang đọc khi phản hồi mới đến đã đạt |
| UI | Ảnh native ở màn khoảng 411dp; Hôm nay ở 320dp với font 1,3× và 2×. Đã sửa date header/tab label sau ảnh trước/sau |

Ảnh và báo cáo chỉ dùng dữ liệu giả, ở [evidence](evidence/). [Báo cáo timeout mô phỏng trên Android](evidence/native/timeout-report.md): REQUEST_STARTED → GATEWAY_TIMEOUT, Room NOT_COMMITTED, 0 receipt/Audit. Mã theo dõi trong file là nguồn đúng sau mỗi lần chạy lại test; lỗi Gemini thật được ghi riêng bên dưới.

Ảnh và kết quả máy thật ở [evidence/note20](evidence/note20/), gồm [timeout đã lọc](evidence/note20/timeout-report.md). Chi tiết rà UI ở [UI_REVIEW](UI_REVIEW.md). Ảnh là native Android. Không tuyên bố đã thử TalkBack, giọng nói thành công, dán ảnh trong Zalo hay đã profile hiệu năng Samsung.

## Gateway thật và giới hạn môi trường

- Cập nhật theo yêu cầu mới nhất: người dùng sẽ tự nhập API key. Gateway phiên trước đã dừng; đã kiểm tra không còn listener tại `127.0.0.1:8787`. Khi cần, chạy `scripts/gateway.ps1` và nhập khóa tại lời nhắc ẩn. Không tự dùng lại khóa từ cuộc trò chuyện.

- Có `gateway/server.mjs`, POST `/v1/assistant/interpret`, Function Calling REST, chỉ trả đề xuất và không có quyền truy cập Room. Client HTTP đọc mã lỗi trong danh sách cho phép, giới hạn response, giữ draft.
- Người dùng đã cấp khóa và cho phép dùng trong phiên này. Đọc khóa bằng stdin ẩn trong terminal, chỉ giữ trong bộ nhớ Node; không ghi vào repo, file cấu hình, log hay APK. Không lưu khóa để dùng lại sau khi tiến trình kết thúc.
- `scripts/gateway.ps1` chạy `gateway/start.mjs`: nhận `GEMINI_API_KEY` trong môi trường hoặc hỏi ẩn nếu chưa có; `GEMINI_MODEL` tùy chọn, mặc định **gemini-3.5-flash**. `npm start` dùng cùng starter. Ctrl+C dừng gateway.
- **Đã gọi thật thành công bốn câu mẫu**: đúng hàm/ngày, khoản mua đúng 85.000₫, chỉ trả đề xuất. Bằng chứng không chứa khóa/chat ở [gemini-check.json](evidence/gemini-check.json). Model 3.8-flash gặp HTTP 503 UNAVAILABLE và timeout; 3.5-flash trả đủ bốn kết quả.
- Lượt bổ sung qua HTTP gateway vẫn gặp timeout và upstream từ chối. Thử thinking low chưa cải thiện và đã trả về cấu hình từng đạt; kết quả thử được giữ ở [gemini-transient-check.json](evidence/gemini-transient-check.json). Không suy diễn rằng khóa sai hoặc hết quota từ các lỗi này. Test `RealGatewayTest` chỉ bật bằng tham số rõ ràng, có một lần bấm retry qua UI, giữ request/trace và kiểm tra chưa có receipt trước retry.
- **Kiểm thử Gemini thật từ Samsung đã đạt** sau khi trả về cấu hình trước đó: [JUnit](evidence/note20/gemini-results.xml), [kết quả đã lọc](evidence/note20/gemini-result.txt), [ảnh chốt](evidence/note20/gemini-close-saved.png). [Báo cáo timeout thật](evidence/note20/gemini-transient-2cb7b697-c1c6-4db1-8b93-a58bd17454b1.md) có trace `be4c6637-8cc6-4dde-9aac-88adb78e24cd`, REQUEST_STARTED → GATEWAY_TIMEOUT, Room NOT_COMMITTED/0 receipt/0 Audit; lần retry cùng lượt thành công. Đây là bằng chứng lỗi và phục hồi thực tế, không phải mô phỏng.
- Server hiện bind loopback 127.0.0.1:8787. Debug Android cho HTTP đúng loopback; release yêu cầu HTTPS. Chưa triển khai dịch vụ gateway/xác thực từ xa.
- Lệnh khởi động gateway nền ở lượt trước bị kiểm soát tự động từ chối (`blocked by policy`, không có lý do chi tiết). Không đổi chính sách hay chạy lại lệnh đó. Sau khi người dùng cấp khóa, đã dùng tiến trình foreground trong terminal với lời nhắc ẩn; gateway và adb reverse đã hoạt động. Đây là tiến trình tạm, không phải dịch vụ khởi động cùng Windows.

## Mặc định nghiệp vụ của bản thử

- Giá 14.400/28.800/28.800₫; phí/suất/bữa 3.000₫ gạo + 1.000₫ gia vị + 800₫ chất đốt. 10 người đủ ba bữa: tiền ăn dự kiến 720.000₫, ngân sách 576.000₫.
- Bù gạo chỉ chiến sĩ, 700₫/kg, 136/272/272g. Lưu milli-VND; làm tròn nửa lên một lần ở tổng tháng. Đủ 3 bữa = 476₫/chiến sĩ; chưa ghi thành thu quỹ khi chỉ là khoản được cấp theo tính toán.
- Timezone nghiệp vụ Asia/Ho_Chi_Minh, timestamp log UTC, date/kỳ dùng LocalDate/YearMonth. Quy tắc ngày hiện tại lấy lúc gửi, khác ngày đang xem.
- Hạn cắt và vắng không báo chưa tự động tính tiền/phạt. Khách bản đầu đích danh, thu sau. Chưa có điểm danh riêng hay khách đoàn nhập số lượng.

## Việc tiếp theo

1. Hoàn tất nghiệm thu mốc 1 trên Note 20 Ultra: nhận giọng nói vi-VN thành công, chọn tệp, TalkBack, kéo bubble, dán PNG và Sharesheet sang ứng dụng nhận. Micro bị từ chối, nhập chữ, IME/cỡ chữ 1,3× và URI ảnh đã thử. Rà offline/force-stop và toàn bộ form ở chữ 2×. Giữ package/schema/dữ liệu, không uninstall để sửa lỗi chữ ký.
2. Bổ sung phạm vi mốc 1 còn thiếu: chọn nhiều bữa/người trong một thao tác có preview chung, khách đoàn, xử lý ngoại lệ/điểm danh nếu chốt quy tắc; Magic Menu giữ/ràng buộc nguyên liệu cần danh mục đã rà, chưa tự đoán “tránh cá”. Chưa hỗ trợ AI nhiều bước; hiện yêu cầu tách từng lệnh.
3. Mốc 2: UI hồ sơ/nhóm có hiệu lực, UI rate có hiệu lực; bảng sổ tháng cố định tên, bộ lọc và ngày chưa chốt; CashEntry/PaymentAllocation, thu cá nhân/nhóm, hoàn/cấn/giữ dư; thực nhận bù gạo khác kỳ; Excel. Accounting.allocate đã có test nhưng **chưa có luồng thu nhóm lưu Room**. Khi đổi schema viết migration và test bảo toàn Purchase/Audit/receipt.
4. Mốc 3: sao lưu/khôi phục SAF nhất quán, preview phạm vi, rollback lỗi và đối chiếu Audit/receipt; cấu hình đầy đủ, audit filter theo ngày/loại, tách file UI lớn và rà tất cả trạng thái. Chưa có backup/restore vận hành.
5. Kiểm tra thêm độ ổn định Gemini theo thời điểm/latency và tài khoản; giữ riêng 4 câu gateway đã đạt với kết quả native thật mới nhất. Chỉ thêm Function Declaration khi có handler dùng chung trong Kotlin. Giữ log/báo cáo theo whitelist hiện tại; không tự lưu khóa từ cuộc trò chuyện vào file.

Mở `KitchenRepository.kt`, `Database.kt`, các test và [02_NGHIEP_VU](02_NGHIEP_VU.md) trước khi làm Tài chính. Không coi các bảng/placeholder UI của mốc sau là tính năng đã hoàn thành.
