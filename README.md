# Bộ hướng dẫn xây app Bếp ăn với Gemini

## App Android đang triển khai

Đã có module native `:app`, dữ liệu Room và gateway tại workspace này. Bản debug có tên **Bếp ăn · Dữ liệu mẫu**, package **`vn.bepan.debug`**, seed 10 người giả (2 cán bộ, 8 chiến sĩ; có hai người tên Nam). Release giữ `vn.bepan`, không seed người và không bật mô phỏng. Tiến độ và kết quả kiểm tra thật ở [HANDOFF](docs/HANDOFF.md); kế hoạch theo mốc ở [IMPLEMENTATION_PLAN](docs/IMPLEMENTATION_PLAN.md).

Sau [rà soát toàn dự án ngày 12/11/2026](docs/PROJECT_AUDIT_2026-11-12.md), **Mốc 2** đã triển khai sổ tháng, thu cá nhân/nhóm, giữ dư/hoàn/cấn, nhận bù gạo và Excel đối soát; hồ sơ/định mức có ngày hiệu lực và dữ liệu khách đã chốt giữ đúng lịch sử. [Kế hoạch 2.0–2.9](docs/IMPLEMENTATION_PLAN.md) và [HANDOFF](docs/HANDOFF.md) ghi riêng kết quả kiểm tra sau từng phần. Sao lưu/khôi phục của Mốc 3 vẫn là điều kiện phải đạt trước nhập dữ liệu vận hành thật.

### Công cụ và build

| Công cụ | Phiên bản của bản đầu |
|---|---|
| JDK đã chạy | Microsoft OpenJDK 21.0.13; AGP yêu cầu tối thiểu JDK 17 |
| Gradle Wrapper / AGP | 8.13 / 8.11.1 |
| Kotlin / Compose compiler | 2.1.20 / 2.1.20 |
| KSP / Room | 2.1.20-1.0.32 / 2.7.1 |
| Compose BOM | 2025.05.01 |
| min / compile / target SDK | 26 / 35 / 35 |
| SDK Build Tools | 36.0.0 |
| Gateway | Node.js ≥22; đã chạy Node 24.17.0, không cần npm dependency |

Tổ hợp AGP/Gradle/JDK đối chiếu [tài liệu Android chính thức](https://developer.android.com/build/releases/agp-8-11-0-release-notes). Chưa nâng target SDK để phân phối store trong mốc này.

Trong PowerShell, đặt `JAVA_HOME` tới JDK có sẵn. Android SDK mặc định ở `%LOCALAPPDATA%\Android\Sdk`; nếu khác, tạo `local.properties` với đường dẫn đã escape. Script nhận JDK portable cục bộ trong `.tools/jdk/` nếu có và tự tạo `local.properties` cho SDK mặc định. `.tools`, cấu hình máy và output build không vào Git.

```powershell
.\scripts\build.ps1
# Hoặc khi JAVA_HOME và SDK đã cấu hình:
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: **`app/build/outputs/apk/debug/app-debug.apk`**. Kết quả JVM: `app/build/reports/tests/testDebugUnitTest/index.html`; lint: `app/build/reports/lint-results-debug.html`. Bản triển khai Mốc 2: **0.2.0-demo/code 3**, schema **7**. Schema ở `app/schemas/vn.bepan.data.KitchenDatabase/`; có migration tuần tự **1 → 2 → 3 → 4 → 5 → 6 → 7**, không dùng destructive fallback.

### Thử luồng đầu

1. Mở Hôm nay, xem 10 suất mỗi bữa và ngân sách mẫu 576.000₫. Chọn từng bữa để cắt/ăn lại; cắt cả ngày có xem trước. Lịch bắt đầu ở ngày đang chọn; ăn lại từ X kết thúc lịch cắt trước đó ở X−1. Điều chỉnh trực tiếp giữ ưu tiên trên lịch.
2. Thực đơn có Sáng/Trưa/Tối và Sáng ngày kế tiếp. Mở Chỉnh sửa, nhập mỗi món một dòng hoặc dùng Magic Menu offline cho cả bữa/món chọn. Gợi ý chỉ vào bản nháp sau Áp dụng; Lưu mới ghi Room. Hủy giữ dữ liệu cũ. Bản sửa cũ bị chặn nếu ngày đã thay đổi.
3. Nút tím mở Gemini; kéo nút trong vùng an toàn. Bản debug mặc định ghi rõ **Mô phỏng AI**. Thử bốn câu gợi ý, chọn đúng Nam, hoàn tác, xem trước khoản mua rồi xác nhận. Thu nhỏ/Back giữ chat và bản nháp.
4. Nhắn `timeout`, mở **Xem chẩn đoán**, xem receipt/Audit, bấm **Xuất báo cáo lỗi · xem trước**. Phần mô tả trống mặc định; duyệt lại trước Lưu/Chia sẻ/Sao chép. Báo cáo không tự chứa tên, số tiền hay chat. Tự kiểm tra nội dung do mình nhập.
5. Chốt ngày chỉ sau xem trước. Sổ tháng lấy bản chốt đang hiệu lực. Bấm bữa của ngày đã chốt để nhập lý do và xem trước điều chỉnh; các phiên cũ được giữ ở `CloseRevision`. Khoản mua đã lưu không đổi theo suất.
6. Tài chính có tháng riêng, nhóm/bộ lọc, bảng cuộn ngang giữ tên và chi tiết người/ngày/phiên chốt. Thu cá nhân/nhóm có phân bổ từng đồng và xác nhận. Chờ hoàn giữ dư; chỉ ghi đã trả mới giảm quỹ. Cấn sang tháng kế tiếp không tạo tiền mặt. Bù gạo tách kỳ được cấp khỏi ngày thực nhận. Quỹ chỉ có số dư khi đã xác lập mốc đầu.
7. Xuất Excel qua trình chọn tệp Android; file gồm đối soát, nhóm, quỹ theo ngày tiền, chứng từ theo kỳ, phân bổ và quyết toán. [Hợp đồng Excel](docs/EXCEL_CONTRACT.md) giải thích tổng nhóm khi có người đổi nhóm. Cài đặt → Quản lý người ăn & định mức lưu lịch sử theo ngày hiệu lực; sổ đã chốt giữ snapshot.

Micro xin quyền khi bấm, bấm lại để dừng; chữ nhận dạng đưa vào ô nhập để sửa trước khi gửi. Nút + chọn ảnh/tệp tại máy; bản này chưa gửi nội dung tệp cho Gemini. Nhập chữ vẫn dùng khi không có micro/quyền.

### Điện thoại USB và kiểm thử native

```powershell
$BepAdb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
& $BepAdb devices -l
# Thay bằng đúng serial đã nhận diện:
.\scripts\install-debug.ps1 -Serial 'SERIAL_CUA_MAY_THU'

# Chỉ chạy trên bản debug dữ liệu giả; các suite ghi chứng từ mẫu:
.\scripts\test-finance-native.ps1 -Serial 'SERIAL_CUA_MAY_THU' -Suite core
.\scripts\test-finance-native.ps1 -Serial 'SERIAL_CUA_MAY_THU' -Suite presentation -SkipBuild
.\scripts\test-finance-native.ps1 -Serial 'SERIAL_CUA_MAY_THU' -Suite recovery -SkipBuild
# Suite recovery force-stop debug giữa 3 phase, giữ toàn bộ Room/khóa.
```

Đã cài và thử bản dữ liệu mẫu trên Note 20 Ultra **SM-N986U1, Android 13/API 33**, cùng emulator riêng `BepAn_API34`. Giữ nguyên cỡ chữ 1,3× và mật độ hiển thị của điện thoại. Kết quả và ảnh native ở [HANDOFF](docs/HANDOFF.md). Không gỡ package để xử lý lỗi cài hoặc nâng schema.

### Nhập API key trong app

Từ bản **0.1.1-demo**, mở **Cài đặt → Gemini → API key Gemini**, dán khóa rồi bấm **Lưu API key**. App tự bật Gemini thật và gọi Google qua HTTPS, không cần chạy gateway trên Windows hoặc cắm USB. Ô nhập có hiện/ẩn; lưu xong ô trở về trống. Dùng **Xóa API key** để xóa khóa trên điện thoại.

Khóa được mã hóa bằng Android Keystore và lưu trong vùng không sao lưu. Không đưa khóa vào Room, DataStore, Git, log hay báo cáo. App chỉ gửi khóa trong header tới Google, không gửi khóa thiết bị sang gateway. Màn hình và Gemini tiếp tục dùng chung kiểm tra nghiệp vụ, xác nhận, Audit và receipt. Kết nối gateway cũ nằm trong **Kết nối nâng cao → Qua gateway**.

Client trực tiếp mặc định dùng `gemini-3.6-flash`; Google ghi nhận [3.5 Flash đã được thay thế và tự chuyển sang 3.6 Flash](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash). Không nhúng hoặc tự nhập lại khóa từ cuộc trò chuyện; người dùng sẽ tự nhập trên điện thoại.

### Gateway tùy chọn

Gateway chỉ diễn giải qua [Gemini Function Calling](https://ai.google.dev/gemini-api/docs/function-calling); app tự tính và xác nhận. Có **9 hàm chung cho direct/gateway**: hỏi suất, ngân sách, đổi một bữa, đề xuất mua/chốt; đọc sổ tháng, công nợ người, bù gạo và đề xuất thu tiền cá nhân/toàn nhóm. Lệnh thu luôn cần preview/xác nhận; hoàn/cấn/nhận bù hiện thao tác tay. Nhiều lệnh cùng lượt được yêu cầu tách riêng. Mô phỏng không thay thế cho thử tài khoản Gemini thật.

```powershell
.\scripts\gateway.ps1 -Mode test
.\scripts\gateway.ps1
# Nhập khóa tại lời nhắc ẩn. Khóa chỉ ở bộ nhớ tiến trình; Ctrl+C để dừng.
# Hoặc cấp GEMINI_API_KEY trong môi trường tiến trình; không ghi vào repo.
# Mặc định gemini-3.5-flash; đổi model bằng GEMINI_MODEL nếu cần.
# Gateway lắng nghe 127.0.0.1:8787, có GET /health chỉ trả trạng thái cấu hình.
& $BepAdb -s 'SERIAL_CUA_MAY_THU' reverse tcp:8787 tcp:8787
```

Trong Cài đặt → Gemini → Kết nối nâng cao → Qua gateway, nhập `http://127.0.0.1:8787`, lưu và tắt mô phỏng. HTTP chỉ được cho phép với loopback ở debug; release dùng HTTPS. Server hiện chỉ phục vụ phát triển qua USB/loopback; chưa triển khai dịch vụ HTTPS/xác thực từ xa. Khi thiếu cấu hình/lỗi mạng, giữ bản nháp và dùng thao tác tay. Retry giữ mã lượt/ngày tham chiếu; receipt đã có sẽ được đối chiếu trước khi tạo thêm thao tác.

Ngày 12/11/2026 đã gọi Gemini thật với khóa người dùng cấp: bốn câu mẫu trả đúng hàm/ngày và số tiền. `gemini-3.8-flash` gặp 503/timeout trong phiên thử; `gemini-3.5-flash` trả kết quả thành công. Bằng chứng đã lọc ở [gemini-check.json](docs/evidence/gemini-check.json). Không lưu khóa trong mã nguồn, APK hay báo cáo.

[Luồng Gemini thật trên Samsung đã đạt](docs/evidence/note20/gemini-results.xml), gồm hỏi suất, trùng tên/cắt/hoàn tác, xác nhận mua và chốt. Có timeout thật rồi retry thành công; khoản mua chỉ ghi một lần theo Room/Audit/receipt. Kết nối vẫn có thể chậm, chưa xem là dịch vụ ổn định để vận hành thật.

Kiểm thử thiết bị với Gemini thật chỉ chạy khi chủ động bật, có gateway và USB reverse. Test dùng ngày riêng và dữ liệu mẫu; kiểm tra xem trước, Room/Audit/receipt và chống ghi trùng:

```powershell
$env:ANDROID_SERIAL = 'SERIAL_CUA_MAY_THU'
.\scripts\build.ps1 -Tasks @(':app:connectedDebugAndroidTest', '-Pandroid.testInstrumentationRunnerArguments.class=vn.bepan.RealGatewayTest', '-Pandroid.testInstrumentationRunnerArguments.realGateway=true')
```

### Phạm vi còn lại

Mốc 3: sao lưu/khôi phục SAF có version/checksum, preview, thử phục hồi DB riêng và giữ Audit/receipt/phân bổ/quyết toán. Chưa dùng dữ liệu vận hành thật trước khi đạt cổng này. Voice thành công, chia sẻ sang app nhận, AI nhiều bước/tệp, khách đoàn và mẫu in chính thức nằm ngoài chuỗi tài chính Mốc 2. Công cụ tài chính Gemini mới đã thử bằng mock/gateway test; bằng chứng Gemini thật phía trên thuộc phiên bản trước. Đọc [HANDOFF](docs/HANDOFF.md) trước khi tiếp tục.

Bộ này dùng để giao việc cho Codex trong VS Code trên Windows. App đích là Android native, viết bằng Kotlin và Jetpack Compose, dùng trên một điện thoại. Mốc 0 dựng đồng thời **Room, Audit Log, Technical Log, AI Debug Center và Debug Handoff**. Dùng luồng Hôm nay → Gemini → cắt bữa và hoàn tác để kiểm tra cả dữ liệu đã lưu và báo cáo lỗi có thể gửi cho GPT.

## Mở và giao việc

1. Giải nén rồi mở **thư mục chứa `AGENTS.md`** bằng **File → Open Folder** trong VS Code. Với dự án mới, mở chính thư mục `BepAn_Gemini_Codex_VSCode`.
2. Mở khung chat Codex. Sao chép toàn bộ [PROMPT_CODEX.md](PROMPT_CODEX.md) vào chat. Có thể bắt đầu bằng câu ngắn bên dưới khi Codex đọc được file trong workspace.
3. Để Codex kiểm tra dự án và bắt đầu mốc đầu tiên. Khi có bản debug, thử trên Note 20 Ultra theo [hướng dẫn Windows và USB](docs/08_WINDOWS_VS_CODE_GITHUB.md).

> Đọc AGENTS.md, PRODUCT.md, DESIGN.md và PROMPT_CODEX.md ở thư mục đang mở. Triển khai đủ năm phần nền tảng trong mốc 0. Thử luồng Hôm nay → Gemini → cắt bữa → kiểm tra → hoàn tác, rồi mở AI Debug Center và xuất một báo cáo lỗi đã lọc. Ghi kết quả vào docs/HANDOFF.md.

**Nếu đã có repo Android**, mở gốc repo đó. Đổi tên thư mục của bộ này thành `product-kit` và đặt trực tiếp trong `docs/`. Trong Codex, yêu cầu đọc `docs/product-kit/PROMPT_CODEX.md` và hợp nhất chỉ dẫn ở gốc theo tài liệu. Hướng dẫn chi tiết có ở [08_WINDOWS_VS_CODE_GITHUB.md](docs/08_WINDOWS_VS_CODE_GITHUB.md).

## Tài liệu trong bộ

| File | Dùng khi |
|---|---|
| [PROMPT_CODEX.md](PROMPT_CODEX.md) | Giao toàn bộ nhiệm vụ cho Codex trong VS Code |
| [AGENTS.md](AGENTS.md) | Đặt quy tắc làm việc lâu dài cho Codex và agent tiếp nhận |
| [PRODUCT.md](PRODUCT.md) | Cung cấp bối cảnh sản phẩm cho người làm app và Impeccable |
| [DESIGN.md](DESIGN.md) | Xây hệ màu, chữ, bề mặt kính và component Compose |
| [01_QUY_TRINH_VIDEO.md](docs/01_QUY_TRINH_VIDEO.md) | Áp dụng từng bước trong video, cài công cụ tùy môi trường và rà UI native |
| [02_NGHIEP_VU.md](docs/02_NGHIEP_VU.md) | Chấm cơm, chốt ngày, ngân sách, thu nhóm, quyết toán và bù giá gạo |
| [03_MAN_HINH_VA_TUONG_TAC.md](docs/03_MAN_HINH_VA_TUONG_TAC.md) | Bố cục bốn tab, thẻ thực đơn và chat kiểu Messenger/Zalo |
| [04_KIEN_TRUC_ANDROID.md](docs/04_KIEN_TRUC_ANDROID.md) | Phân lớp Kotlin, mô hình Room, lưu lịch sử, sao lưu và xuất ảnh |
| [05_GEMINI.md](docs/05_GEMINI.md) | Tích hợp Gemini, cấu trúc lệnh, xác nhận, hoàn tác, giọng nói và mạng |
| [06_EXCEL_VA_MAGIC_MENU.md](docs/06_EXCEL_VA_MAGIC_MENU.md) | Đối chiếu hai mẫu Excel, dữ liệu món và cách tạo thực đơn |
| [07_LO_TRINH_VA_NGHIEM_THU.md](docs/07_LO_TRINH_VA_NGHIEM_THU.md) | Chia mốc, test nghiệp vụ và tiêu chí bàn giao từng mốc |
| [08_WINDOWS_VS_CODE_GITHUB.md](docs/08_WINDOWS_VS_CODE_GITHUB.md) | Chuẩn bị máy Windows, build, cài USB và tiếp tục trên máy khác |
| [09_DATABASE_VA_LOG.md](docs/09_DATABASE_VA_LOG.md) | Ghi Room và Audit cùng transaction, lưu Technical Log có mã theo dõi và giới hạn |
| [10_AI_DEBUG_CENTER_VA_HANDOFF.md](docs/10_AI_DEBUG_CENTER_VA_HANDOFF.md) | Tra cứu lệnh Gemini, xuất báo cáo đã lọc và giao ca lỗi cho GPT/Codex |
| [HANDOFF.md](docs/HANDOFF.md) | Ghi tiến độ, phần đã kiểm chứng và bước tiếp theo |
| [data/README.md](data/README.md) | Dữ liệu Magic Menu trích từ mẫu và các ca thử có kết quả mong đợi |
| [references/README.md](references/README.md) | Video, ảnh tham chiếu, HTML mẫu và cách dùng Excel gốc |

Tên app dùng trong tài liệu là **Bếp ăn**. Bộ này cung cấp đặc tả, dữ liệu mẫu và nguồn tham chiếu để Codex triển khai. Trạng thái mã nguồn, build APK và thử Gemini thật sẽ được ghi trong HANDOFF sau khi triển khai trên repo Android.
