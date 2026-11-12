# Rà soát dự án Bếp ăn — 12/11/2026

## Kết luận

Mục tiêu lớn tiếp theo là **Mốc 2 — Hoàn thiện chu trình tài chính tháng và đối soát được đến từng giao dịch**: hồ sơ/định mức có hiệu lực → sổ tháng → thu cá nhân/nhóm → hoàn/cấn → nhận bù gạo → xuất Excel. Nghiệm thu trên bản debug với dữ liệu mẫu, chạy được offline.

Nền vận hành ngày và chẩn đoán đã có triển khai đáng kể cùng kiểm thử. Khoảng trống lớn nhất nằm ở việc chuyển các số ngày đã chốt thành công nợ, tiền thực thu/chi và quyết toán tháng. Cần sửa những sai lệch lịch sử bên dưới trước khi dùng các số đó để thu hoặc hoàn tiền. Kế hoạch thực hiện ở [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

## Phạm vi và cách kiểm tra

- Đối chiếu AGENTS, README, PRODUCT, DESIGN, PROMPT_CODEX, HANDOFF, tài liệu nghiệp vụ/kiến trúc/AI/log/xuất sổ và fixture trong `data/`.
- Kiểm kê `:app`: **29 file Kotlin chính, 3.984 dòng**, **6 file JVM test**, **3 file Android test**; rà domain, Room/schema 1–2, repository, ViewModel, màn Compose, dialog, renderer, assistant, Keystore và diagnostics.
- Rà gateway Node, test gateway, Gradle Wrapper, build settings, manifest debug/release, ba script PowerShell, Git/ignore, assets và danh mục bằng chứng native. Hai bản JSON mẫu menu trong app/data có SHA-256 giống nhau.
- Dùng Quick Reference của UI/UX Pro Max cho yêu cầu native: vùng chạm, chữ lớn, trạng thái, khả năng tiếp cận và bảng dữ liệu. Không tạo hướng thiết kế mới hoặc chạy CLI thiết kế.
- Không kiểm toán nội bộ toàn bộ thư viện skill đã vendored; chúng là công cụ hỗ trợ. Không đọc DB/khóa/tệp riêng trên điện thoại, không kiểm tra từng khung hình video hoặc coi toàn bộ ảnh lịch sử là kiểm thử vừa chạy.
- Các phát hiện source dưới đây là phân tích tĩnh; chưa thêm ca hồi quy hoặc sửa logic trong lượt lập kế hoạch này.

## Kiểm chứng trong lượt rà soát

| Kiểm tra | Kết quả thật |
|---|---|
| `scripts/build.ps1` | Thành công: assembleDebug, testDebugUnitTest, lintDebug; phần lớn task dùng kết quả còn hợp lệ từ Gradle |
| `scripts/build.ps1 -Tasks @(':app:testDebugUnitTest','--rerun-tasks')` | Thành công; **30 task thực thi, 30 test đạt**, 0 failed/error/skipped |
| `scripts/gateway.ps1 -Mode test` | **6/6 test đạt**, dùng upstream giả, không dùng API key thật |
| Báo cáo lint hiện tại | **0 lỗi, 30 cảnh báo**: 20 về dependency/AGP/target, 3 về kích thước màn, 2 UseKtx, 2 ModifierParameter, 1 icon, 1 đọc state, 1 data extraction; chi tiết XML là nguồn kiểm tra chính |
| ADB | Có Samsung `SM-N986U1` ở trạng thái `device`; chỉ kiểm tra kết nối |
| Native/Gemini thật trong lượt này | **Không chạy lại**, không cài app hoặc thay dữ liệu điện thoại. Bằng chứng cũ được giữ trong HANDOFF/evidence |
| Git lúc bắt đầu | Nhánh `master`, **chưa có commit**; 329 mục trạng thái ` A` trong working tree và 22 mục `??` theo porcelain mặc định; diff đã staged rỗng. Không tạo commit/stage/reset |

Lệnh `node` trực tiếp không chạy được vì chưa nằm trong PATH. Helper gateway tìm được Node của Visual Studio và chạy đủ sáu test; vì vậy tiếp tục dùng helper trong README. Khi build có cảnh báo lệch phiên bản trình đọc SDK XML, nhưng build không thất bại.

Phân loại lint ở bảng trên có nhóm tổng hợp; không hiểu cảnh báo phiên bản thư viện là yêu cầu nâng đồng loạt dependency trong mốc tài chính. Lượt này chưa build/nghiệm thu release.

## Độ phủ hiện có

| Phần | Đã có trong mã | Phần còn lại |
|---|---|---|
| Nền dữ liệu | Room v2, schema export, migration 1→2, Audit/receipt/trạng thái AI cùng transaction, chống trùng theo hash | Migration tài chính; ràng buộc quan hệ, phiên bản phạm vi kỳ và truy vấn theo kỳ |
| Hôm nay | Chấm bữa, cắt cả ngày cho một người, lịch cắt/ăn lại, khách đích danh, ngân sách, mua, chốt/điều chỉnh | Hàng loạt nhiều người, khách đoàn, các ca nghiệm thu thiết bị còn tồn |
| Thực đơn | Bốn ô, sáng hôm sau dùng khóa chung, sửa/Magic Menu offline, PNG độc lập vùng cuộn | Hạn chế lặp mẫu gần đây, giữ món/ràng buộc nguyên liệu; thử app nhận ảnh thực tế |
| Tài chính | Tổng tiền ngày đã chốt, tiền mua, bù gạo tính toán, ba nhóm, số bữa từng người | Thu/chi thống nhất, phân bổ, công nợ, bảng tháng/bộ lọc, hoàn/cấn, thực nhận, Excel |
| Ghi chú/lịch sử | Tạo/sửa/tìm/ghim, Audit theo nguồn | Lọc ngày/nghiệp vụ; form giữ nháp khi lưu lỗi; hoàn thiện nguồn SYSTEM |
| Cài đặt | API key, direct/gateway, mock debug, chẩn đoán | Quản lý người/nhóm có hiệu lực, chỉnh giá/phí/bù, sao lưu/khôi phục |
| Gemini | Đúng năm hàm Today; trùng tên, preview khoản mua/chốt, receipt/retry/undo | Ngữ cảnh màn/kỳ tài chính; công cụ tài chính chỉ thêm sau handler cục bộ tương ứng |
| Chẩn đoán/khóa | Keystore + AES-GCM/noBackup; JSONL có giới hạn và whitelist; preview báo cáo | Phiên bản schema dùng chung, ca khóa hỏng/mất hiệu lực, direct thật, redirect gateway |

## Phát hiện cần đưa vào kế hoạch

**P0**: xử lý trước phần thu/quyết toán dựa trên số liệu; **P1**: điều kiện nghiệm thu chức năng liên quan; **P2**: hoàn thiện sau hoặc song song khi không chặn số liệu.

| ID | Mức | Bằng chứng source | Tác động và hướng xử lý |
|---|---|---|---|
| F01 | P0 | `Database.kt:17` ClosedMeal không có registered; `KitchenRepository.kt:37` gán true khi đọc ngày chốt; `FinanceScreen.kt:225` đếm mọi enabled=false là cắt | Khách chỉ đăng ký trưa có thể bị tính sáng/tối chưa đăng ký thành hai bữa cắt, và ngày đã chốt cho phép điều chỉnh các bữa đó. Lưu trạng thái đăng ký khi chốt, giữ phân biệt khi đọc/điều chỉnh/xuất; migration không tự bịa lịch sử thiếu bằng chứng |
| F02 | P0 | `KitchenRepository.kt:65` chặn mọi lệnh trên ngày chốt trừ adjust/note; `Models.kt:29` Command gắn date/expectedVersion | Thu tháng cũ và nhận bù gạo cần phạm vi tài chính, ngày tiền thực tế và kỳ nghiệp vụ riêng. Không mở các lệnh tiền mới bằng cách bỏ chung kiểm tra ngày đã chốt |
| F03 | P0 | `Database.kt:15` Purchase là dữ liệu chi hiện có; chưa có CashEntry/PaymentAllocation/Settlement | Khi thêm quỹ phải chọn một nguồn chi duy nhất và chuyển tiếp có kiểm tra; tránh trừ Purchase một lần nữa. Chưa có căn cứ để gọi thu−chi là số dư quỹ thật nếu chưa xác lập số dư đầu |
| F04 | P1 | `FinanceScreen.kt:195` lấy tên từ snapshot ngày đang xem | Người đã ngừng ăn hoặc khách tháng cũ vẫn có hàng theo ClosedMeal, nhưng tên có thể thành “Người ăn <ID>”. Truy vấn hồ sơ theo ID độc lập ngày đang xem; bổ sung người chỉ có giao dịch khi có sổ thu |
| F05 | P1 | `ManualConfirmDialog.kt:59` dùng snapshot.rate; `KitchenRepository.kt:81` điều chỉnh theo ClosedMeal cũ | Khi thêm sửa giá có hiệu lực, preview điều chỉnh có thể dùng giá khác số thực ghi. Preview phải dùng đúng giá snapshot của bữa đã chốt, cùng phép tính với commit |
| F06 | P1 | `Database.kt:9` Person chỉ có một group/starts/ends; `SettingsScreen.kt:257` còn thông báo chưa làm | Cần lịch sử nhóm/hoạt động và UI định mức trước nghiệm thu tháng đổi nhóm/giá. Không thay nhóm lịch sử đã lưu trong ClosedMeal hoặc allocation |
| F07 | P1 | `KitchenViewModel.kt:75` đọc toàn bộ ClosedMeal/Purchase mỗi refresh; `FinanceScreen.kt:37` tổng hợp trong composition | Nguồn tính tháng cần read model và query theo khoảng ngày/kỳ, dùng chung cho UI/Excel/Gemini; tránh tải cả lịch sử sau mỗi Audit |
| F08 | P1 | `Gateway.kt:70` luôn route TODAY/period từ selectedDate; ViewModel chỉ giữ ngày | Mở chat từ Tài chính chưa truyền tháng/nhóm/bộ lọc thật. Lưu ngữ cảnh khi gửi và retry trước khi mở công cụ tài chính |
| F09 | P1 | `DebugReport.kt:21`, `DiagnosticsScreen.kt:54` ghi cứng schema 2 | Nâng schema dễ tạo báo cáo sai phiên bản. Đưa phiên bản schema về nguồn dùng chung, giữ UNKNOWN khi chưa đọc được Room |
| F10 | P1 | `HANDOFF.md` phần UI đặt tên 0.1.2-demo, `app/build.gradle.kts:15` thực tế code 2/name 0.1.1-demo | Đồng bộ phiên bản bàn giao theo metadata APK; giữ mục cũ như lịch sử, không coi tên mốc tài liệu là phiên bản đã build |
| F11 | P1 | `Gateway.kt:62` HttpURLConnection chưa tắt redirect; DirectGeminiGateway đã tắt | Bổ sung từ chối 3xx và test không gửi tiếp nội dung chat sang đích khác cho gateway. Đây là thiếu biện pháp phòng vệ, chưa có bằng chứng lộ dữ liệu |
| F12 | P2 | Finance/Today/Chat/History còn Color literal; `HistoryScreen.kt:52` coi mọi nguồn không MANUAL là Gemini | Không đúng hoàn toàn với tuyên bố “loại bỏ hoàn toàn màu hardcode”; gom khi sửa màn liên quan. Hiển thị SYSTEM riêng khi thêm lịch sử hệ thống |

Các dòng source tính ở thời điểm rà soát; xem tên hàm nếu file đã thay đổi. F01/F04 là hành vi suy ra trực tiếp từ mã; F05 là lỗi tiềm ẩn khi thêm quản lý giá. Chưa tuyên bố đã tái hiện trên máy thật.

## Chất lượng và giới hạn

- 30 test hiện có kiểm tra tốt ranh giới transaction/retry/log và phép tính nền. `Accounting.allocate` đã có test chia lẻ; **chưa có luồng thu nhóm lưu Room**. Test tháng nhuận hiện là phép tính thuần, chưa phải nghiệm thu bảng tháng và công nợ.
- Migration test hiện chủ yếu giữ người/chat/Audit/receipt 1→2; mốc mới phải thêm Purchase, ClosedMeal, revision, menu và dữ liệu phân bổ/quyết toán vào ca bảo toàn.
- Direct transport đã có test giả; bằng chứng Gemini thật cũ thuộc gateway. Không dùng bằng chứng đó để công bố direct đã chạy tài khoản thật.
- Các ca voice vi-VN thành công, chọn tệp, TalkBack, dán PNG/app nhận, Sharesheet, process death rộng và mọi form font 2× vẫn cần nghiệm thu. Thiết bị kết nối không chứng minh các ca này đã đạt.
- `.gitignore` đã loại khóa/DB/backup/Excel riêng. Kiểm tra chuỗi dạng khóa Google phổ biến trong nguồn/tài liệu được rà không tìm thấy khớp; đây không phải chứng nhận quét mọi loại bí mật. Chưa có commit/CI nên cần chuẩn bị baseline review trước mốc ghi dữ liệu mới.

## Quyết định phạm vi

Làm Mốc 2 trên dữ liệu mẫu. Kéo hồ sơ và định mức có hiệu lực từ phần Cài đặt vào đầu mốc vì chúng là đầu vào của tài chính. Hoàn thiện bảng sổ và giao dịch trước khi thêm AI ghi tiền. Giữ backup/restore đầy đủ ở Mốc 3, với điều kiện bắt buộc phải nghiệm thu phục hồi trước khi nhận dữ liệu vận hành thật.

Khách đoàn, nhiều lệnh AI một lượt, đọc tệp bằng Gemini, Magic Menu theo nguyên liệu, tablet và phân phối store ở backlog riêng; không dùng chúng để trì hoãn chu trình tháng đã chọn. Mẫu xuất nội bộ có thể thực hiện từ đặc tả hiện có; khớp mẫu nộp chính thức cần file mẫu đã được duyệt.
