# Kế hoạch triển khai Bếp ăn

Cập nhật sau rà soát toàn dự án: **12/11/2026**. Căn cứ: [báo cáo rà soát](PROJECT_AUDIT_2026-11-12.md), [nghiệp vụ](02_NGHIEP_VU.md), [nghiệm thu](07_LO_TRINH_VA_NGHIEM_THU.md) và [HANDOFF](HANDOFF.md). Đây là kế hoạch tiếp theo; các hạng mục chưa được đánh dấu hoàn thành.

## Mục tiêu lớn tiếp theo

**Mốc 2 — Người quản lý đối soát trọn một tháng: biết từng người phải nộp/đã nộp/còn thiếu hoặc dư; thu cá nhân/nhóm, hoàn hoặc cấn đúng một lần; theo dõi bù gạo được cấp/đã nhận; xuất Excel khớp số app.**

Mọi luồng thủ công chạy offline trên bản debug dữ liệu mẫu, giữ lịch sử khi nâng bản. App tính số và kiểm tra nghiệp vụ; Gemini dùng cùng handler khi công cụ tương ứng được mở. Giữ bốn tab và hệ thiết kế hiện tại.

Không xem Mốc 2 là cho phép nhập dữ liệu thật: trước vận hành thật cần hoàn thành cổng backup/restore của Mốc 3, cấu hình giá thật và nghiệm thu thiết bị.

## Trạng thái điểm xuất phát

| Mốc | Trạng thái có căn cứ |
|---|---|
| 0 · Nền, AI, chẩn đoán | Đã có triển khai và test transaction/Audit/receipt/log; còn các đầu việc phiên bản, kết nối và kiểm thử mở rộng trong báo cáo |
| 1 · Hôm nay | Luồng chính đã chạy và có bằng chứng cũ trên Samsung/emulator; còn nghiệm thu voice/TalkBack/app nhận ảnh và phạm vi hàng loạt/khách đoàn |
| 2 · Tài chính | Mới có tổng từ ngày chốt, Purchase, bù gạo tính toán và thẻ nhóm; chưa có thu/phân bổ/quyết toán/Excel |
| 3 · Vận hành và phục hồi | Chưa có backup/restore sử dụng được; UI người/giá tối thiểu được đưa sớm vào Mốc 2 |

Build debug/lint thành công; đã chạy lại **30/30 JVM test**, gateway **6/6**. Lint hiện **0 lỗi, 30 cảnh báo**. Package debug `vn.bepan.debug`, release `vn.bepan`; schema 2; Gradle thực tế `0.1.1-demo`/code 2. Git chưa có commit. Không thay package, không xóa DB để nâng schema.

## Hợp đồng số liệu phải giữ

| Số liệu | Nguồn và quy tắc |
|---|---|
| Tiền ăn chính thức | Chỉ ClosedMeal thuộc bản chốt đang hiệu lực, đúng giá và nhóm đã chụp; nháp/tương lai không tạo tiền ăn chính thức |
| Tiền dự kiến | Giá và hồ sơ có hiệu lực theo từng ngày thực của tháng; có nhãn riêng, không cộng vào khoản phải thu chính thức |
| Số đối chiếu người/kỳ | Đã nộp + cấn vào − tiền ăn đã chốt − đã hoàn thực tế − cấn ra; số âm thiếu, số dương dư |
| Dư khả dụng | Dư đối chiếu dương trừ phần đang giữ để hoàn/cấn; khoản giữ chưa làm giảm quỹ và chưa được trừ lần nữa như hoàn đã trả |
| Quỹ | Số dư đầu đã xác lập + thu tiền thực tế − chi tiền thực tế; mỗi Purchase chỉ là một nguồn chi. Nếu chưa xác lập đầu quỹ, chỉ gọi là thu/chi trong phạm vi |
| Thu nhóm | Một giao dịch quỹ, nhiều phân bổ đã lưu; tổng phân bổ đúng tổng thu, chia lẻ theo ID ổn định |
| Hoàn/cấn | Hoàn thực tế có chi quỹ; cấn cùng người giữa hai kỳ không đổi quỹ, hai vế lưu nguyên tử và liên kết duy nhất |
| Bù gạo | Chỉ chiến sĩ, lấy milli-VND trong ngày chốt; làm tròn nửa lên một lần ở tổng kỳ, độc lập số dư tiền ăn cá nhân |
| Thực nhận bù | Giao dịch thu tại ngày nhận, gắn kỳ quyền lợi; nhận tháng sau vẫn giảm chờ nhận kỳ trước, không tự tăng đã nộp tiền ăn |

Tách `mealDate`/kỳ nghiệp vụ, `cashDate`/thời điểm tiền thực nhận hoặc chi và `recordedAt` ghi nhận. Mặc định giao dịch mới dùng ngày thực tế tại lúc lưu theo múi giờ nghiệp vụ; khoản tiền của tháng cũ không tự lùi ngày ghi quỹ theo tháng đang xem. Dữ liệu chuyển tiếp không được bịa thời điểm nhận tiền hoặc tự coi số dư đầu là khoản vừa thu.

## Thứ tự triển khai và phụ thuộc

Chuỗi chính: **2.0 → 2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 2.6 → 2.7 → 2.8 → 2.9**. Mỗi phần kết thúc bằng build/check liên quan và cập nhật HANDOFF; không triển khai tất cả rồi mới kiểm tra số liệu.

### 2.0 · Khóa nền kiểm chứng và sửa lịch sử hiện hữu — P0 — Đã đạt JVM/build

- Đối chiếu F01–F12 trong báo cáo. Viết ca hồi quy khách chỉ đăng ký trưa, khách hủy một bữa, người ngừng ăn và preview điều chỉnh theo giá đã chốt.
- Bổ sung trạng thái đăng ký vào dữ liệu chốt; chặn điều chỉnh “ăn lại” bữa khách chưa từng đăng ký. Với dữ liệu cũ không đủ bằng chứng phân loại, giữ dấu chưa xác định và giữ nguyên tiền đã chốt, không gán mọi bữa thành đã đăng ký.
- Tên người trong tài chính tra theo hồ sơ ID, độc lập ngày Hôm nay. Preview chênh lệch lấy cùng snapshot giá với commit.
- Thống nhất phiên bản thực tế trong tài liệu; đưa schema diagnostics về nguồn chung khi nâng DB. Phân loại lint, sửa cảnh báo có ảnh hưởng trực tiếp tới màn đang làm.
- Chuẩn bị baseline Git để review: kiểm tra phần đang thêm/untracked, schema/assets/test, loại output/bí mật; tạo commit nền khi thực hiện mốc triển khai. Không tự đẩy remote.

**Đầu ra:** các ca sai lịch sử được chặn, thiết kế migration có nguồn chuyển tiếp, danh sách file baseline đủ.

**Đạt khi:** khách chưa đăng ký không thành “Có cắt”; khách chỉ trưa không bật sáng/tối sau chốt; người cũ có tên; preview tiền bằng chênh lệch thực ghi; charge/chi/bù gạo cũ giữ nguyên.

### 2.1 · Hồ sơ và định mức có hiệu lực — P0

- Thêm lịch sử nhóm/trạng thái người ăn với ID cố định; có ngày vào/ngừng/ăn lại, khoảng hiệu lực không mâu thuẫn. Không xóa vật lý người đã có phát sinh.
- Làm Cài đặt → Người ăn và Định mức: giá ba bữa, phí gạo/gia vị/chất đốt, gram và mức bù, ngày hiệu lực. Kiểm tra số nguyên/giới hạn/phí không vượt giá bữa.
- Thay cấu hình cập nhật mặc định ngày nháp bị ảnh hưởng và version của chúng; giữ override thủ công. Ngày đã chốt đọc snapshot cũ; sửa quá khứ phải qua điều chỉnh có preview.
- Giữ nhóm đã chốt và phân bổ cũ khi người đổi nhóm. Với lịch sử trước schema mới, giữ ClosedMeal làm bằng chứng của ngày đã chốt; không suy diễn đầy đủ lịch sử nhóm chỉ từ Person hiện tại.

**Đầu ra:** thao tác cấu hình thật từ UI, domain dùng cùng logic hiệu lực; migration/schema export tương ứng.

**Đạt khi:** thêm người trên DB trống, đổi nhóm/ngừng/ăn lại giữa tháng, đổi giá 700→900 bù giữa kỳ đúng ngày; lịch sử không đổi và preview cũ bị từ chối.

### 2.2 · Sổ quỹ, phạm vi lệnh tài chính và migration — P0

- Tách lệnh tiền khỏi điều kiện ngày bữa nháp; dùng kiểu lệnh rõ phạm vi người/kỳ/chứng từ. Giữ cơ chế receipt/hash/Audit của repository.
- Thêm CashEntry, PaymentAllocation, Settlement và phiên bản tài chính liên quan; unique/FK/index theo nguồn giao dịch, kỳ, người và trạng thái. Ràng buộc tổng phân bổ được kiểm tra trong transaction.
- Chọn **CashEntry là nguồn quỹ**; Purchase liên kết duy nhất đến dòng chi. Backfill một dòng chi xác định cho mỗi Purchase cũ, lưu dấu nguồn và không tạo lại receipt thu/chi của người dùng. Tổng “đã mua” vẫn truy ra cùng khoản gốc.
- Chuyển tiếp Purchase dựa trên ngày đã lưu và receipt/timestamp có căn cứ; đánh dấu dữ liệu legacy thiếu thời điểm, không giả ngày chuyển schema là ngày chi mới.
- Xác định cách mở sổ: nhập lịch sử còn thiếu hoặc số dư đầu có mốc. Không tự giả định dữ liệu v2 đã chứa toàn bộ các khoản thu trước đây. Chặn cộng trùng phần lịch sử đã gộp vào số dư mở sổ.
- Mỗi lần thay schema tăng tuần tự từ v2, export schema và kiểm tra đường nâng 1→2→mới; không bắt buộc gộp mọi thay đổi của mốc vào một migration.

**Đầu ra:** repository/use case tài chính, hợp đồng preview, migration giữ dữ liệu và một nguồn chi duy nhất.

**Đạt khi:** nâng DB mẫu v2 có người/khách, menu, Purchase, ngày chốt, revision, chat/Audit/receipt; tổng trước/sau bằng nhau; một khoản mua 85.000₫ chỉ giảm quỹ một lần; mở lại/replay không nhân đôi.

### 2.3 · Sổ tháng chỉ đọc và chi tiết người — P0

- Tạo FinanceRepository/FinanceCalculator/read model theo YearMonth, query theo phạm vi thay vì nạp toàn bộ lịch sử vào composition. UI, file xuất và AI đọc chung nguồn tính.
- Hiện tổng tiền ăn, ngày đã chốt, ngày đã tới chưa chốt và ngày tương lai. Tập ngày từ lịch tháng, không chỉ dựa vào Day đã được tạo khi người dùng xem màn hình.
- Danh sách theo ba nhóm lịch sử, có người phát sinh bữa hoặc giao dịch trong kỳ, kể cả đã ngừng hoạt động. Làm thẻ người → chi tiết ngày/bữa → bản chốt/điều chỉnh.
- Làm bảng nhóm tên cố định, ngày 1..cuối tháng và ba bữa; ô phân biệt Có ăn/Cắt/Chưa đăng ký/Chưa chốt. Chạm ô mở được nguồn và lý do.
- Bộ lọc Có cắt/Còn thiếu/Có dư dùng calculator chung; giữ tháng/nhóm/bộ lọc/vị trí khi trở lại. Phần tiền thu/số dư hoàn chỉnh khi 2.4–2.5 được nối.

**Đầu ra:** màn Tài chính tháng/chi tiết/bảng ngang thực dùng, state/ViewModel tài chính riêng khi cần.

**Đạt khi:** tháng 28/29/30/31 ngày, nhóm đổi giữa kỳ, khách lịch sử, tháng trống/chỉ nháp; tổng người = tổng nhóm = tổng tháng; thay ngày Hôm nay không làm mất tên hoặc đổi tháng tự chọn.

### 2.4 · Thu tiền cá nhân và nhóm — P0

- Luồng chọn người/kỳ → nhập tiền → preview → xác nhận → mở chứng từ đã lưu. Thu nhóm đóng băng danh sách ID, nhóm tại lúc thu và phân bổ; cho thấy đồng lẻ trước lưu.
- Lưu cash entry + allocations + Audit + receipt cùng transaction. Khóa nút đang lưu; thay nội dung tạo operation mới; retry/thẻ sau khởi động lại đối chiếu receipt.
- Preview kiểm tra phiên bản tập người/kỳ/phân bổ liên quan thay vì chỉ dayVersion. Thay đổi ảnh hưởng tiền hoặc danh sách yêu cầu xem lại.
- Cập nhật đã nộp/còn thiếu/dư và các bộ lọc; cho truy từ tổng tới chứng từ. Nếu sửa giao dịch đã lưu, dùng thao tác đảo/đối ứng có lý do và liên kết, không xóa mất lịch sử.

**Đầu ra:** thu cá nhân/nhóm và công nợ có dữ liệu Room thực.

**Đạt khi:** 21.600.000₫/10 người tạo đúng một thu quỹ; 1.000.001₫ chia ba ID a/b/c thành 333.334/333.334/333.333₫; đổi nhóm sau thu giữ phân bổ; thu hôm nay cho kỳ cũ đúng hai phạm vi; lỗi Audit rollback và confirm/retry chỉ ghi một lần.

### 2.5 · Giữ dư, hoàn và cấn sang kỳ sau — P0

- Tạo khoản chờ hoàn giữ phần dư, xác nhận đã trả mới ghi chi; hủy chờ hoàn giải phóng giữ. Hỗ trợ hoàn một phần với số dư khả dụng tính trong transaction.
- Cấn **cùng người giữa hai kỳ** với hai vế nguyên tử, một liên kết duy nhất, không tạo thu/chi tiền mặt. Không mở rộng sang chuyển tiền giữa người khi chưa có quy tắc.
- Điều chỉnh ngày đã chốt ảnh hưởng khoản đã giữ/đã quyết toán phải preview chênh lệch và đánh dấu cần đối soát. Không tự sửa tiền đã thu/hoàn/cấn.
- Kiểm tra đồng thời hai thao tác dùng cùng phần dư; chỉ chấp nhận phạm vi còn hợp lệ. Duy trì Audit/receipt và kết quả có thể mở lại.

**Đầu ra:** chu trình quyết toán từng người, có trạng thái và lịch sử đối ứng.

**Đạt khi:** dư 28.800₫ → chờ hoàn chưa giảm quỹ → đã trả giảm đúng 28.800₫; hoặc cấn sang kỳ sau đúng hai vế và không đổi quỹ; không dùng lại phần đã giữ; điều chỉnh tăng charge sau giữ dư không cho hoàn quá số khả dụng một cách âm thầm.

### 2.6 · Bù giá gạo tính toán và thực nhận — P0

- Dùng ClosedMeal.subsidyMilli làm nguồn quyền lợi; hiển thị Được cấp/Đã nhận/Chờ nhận theo kỳ. Tổng kỳ làm tròn một lần, không làm tròn từng bữa/ngày hoặc cộng các số đã làm tròn từng người.
- Luồng nhận tiền có kỳ bù, ngày nhận, số tiền, preview và xác nhận; liên kết dòng thu CashEntry. Cho đối soát nhận từng phần/nhận vượt mà không sửa quyền lợi đã tính.
- Điều chỉnh ngày chốt tính lại quyền lợi, hiện chênh lệch sau nhận; không tự sinh thêm giao dịch quỹ. Không trừ bù gạo khỏi tiền ăn phải nộp của cá nhân.

**Đầu ra:** thẻ bù gạo và chứng từ thực nhận có đường truy nguồn.

**Đạt khi:** một chiến sĩ đủ ba bữa ở 700₫/kg = 476₫; hai ngày mức 700/900 = 1.088₫; ba suất sáng = 285,6₫ → tổng 286₫; cán bộ/khách bằng 0; bù tháng 11 nhận 01/12 nằm đúng kỳ quyền lợi 11 và quỹ tháng 12; retry chỉ một thu.

### 2.7 · Excel đối soát từ cùng nguồn dữ liệu — P1

- Chốt hợp đồng cột từ docs/06 trước khi chọn thư viện Android: ngày/bữa, tổng suất, tiền ăn, đã nộp, hoàn/cấn, thiếu/dư và bù chiến sĩ. Không ghi đè giá lịch sử bằng mức hiện tại.
- Xuất XLSX offline qua SAF, từng nhóm có dữ liệu; thêm nguồn/cơ sở tổng hợp và phân biệt nháp. Có tổng đối chiếu giao dịch quỹ theo ngày tiền thực tế, tách khỏi kỳ ăn.
- File chỉ chứa dữ liệu nghiệp vụ người dùng chủ động xuất; không gồm API key, chat hoặc Technical Log. Đây là file sổ có dữ liệu riêng, giữ ngoài Git/báo cáo chẩn đoán.
- Xuất theo hợp đồng nội bộ trước. Bố cục in khớp mẫu nộp chính thức là bước đối chiếu riêng khi có mẫu đã duyệt; không chặn kiểm tra số học hoặc yêu cầu lại file ở giai đoạn này.

**Đầu ra:** một file mở được bằng ứng dụng bảng tính, số và tên tiếng Việt đúng.

**Đạt khi:** tổng từng sheet/người/bữa bằng UI và Room; đủ tháng nhuận; dữ liệu điều chỉnh/thu nhóm/hoàn/cấn/nhận khác kỳ đúng; hủy SAF không tạo trạng thái thành công giả; mở lại tệp thật trên thiết bị/máy tính.

### 2.8 · Nối ngữ cảnh Gemini và chẩn đoán tài chính — P1

- Khi mở chat, truyền route/tháng/nhóm/ngày đang xem đúng thực tế; giữ ngữ cảnh gốc khi retry. Bổ sung query sổ tháng/số dư/bù từ read model đã kiểm chứng.
- Công cụ ghi tài chính chỉ được khai báo sau khi có handler dùng chung và UI thủ công đã đạt. Ưu tiên thu cá nhân rồi thu nhóm; hoàn/cấn và nhiều bước AI có thể tách mốc sau. Không lấy số mô hình tự tính làm kết quả chính thức.
- Với mọi công cụ tiền được mở: preview hiển thị người, kỳ, ngày quỹ, phân bổ, thay đổi số dư; app xác nhận lại dependency version và lưu transaction; chat đọc receipt đã commit.
- Tắt redirect gateway và thêm ca 3xx. Mở rộng enum sự kiện theo whitelist; báo cáo vẫn không tự chứa tên/số tiền/chat/bữa/giao dịch. Schema/version trong báo cáo phản ánh bản đang chạy.

**Đầu ra:** đọc tài chính đúng kỳ từ chat, timeline/chẩn đoán cho nghiệp vụ mới; danh sách công cụ ghi đã mở được ghi rõ trong HANDOFF.

**Đạt khi:** đang xem tháng cũ rồi hỏi “tháng đang xem”, đổi ngày/màn trước retry vẫn giữ kỳ gốc; câu đọc trả đúng Room; công cụ chưa có handler không được khai báo; confirm lặp và redaction tiếp tục đạt.

### 2.9 · Nghiệm thu cả tháng và gói bàn giao — P0

- Tạo fixture tháng cố định đi qua mọi luồng 2.0–2.8, so số theo ngày/người/nhóm/kỳ/quỹ/file; không chỉ kiểm tra helper tính tiền.
- Chạy JVM/Room transaction/migration, gateway khi protocol đổi, Compose/native trên emulator mẫu và Samsung đúng serial với bản debug. Dữ liệu nghiệm thu có cả người ngừng ăn, hai Nam, khách và thay giá/nhóm giữa kỳ.
- Kiểm tra offline, khởi động lại, Activity recreation, mất tiến trình lúc trước/sau commit, thao tác đồng thời, preview cũ và lỗi ghi giữa transaction.
- Rà 320–360dp, Samsung font 1,3× và form 2× trên môi trường thử; TalkBack đọc đúng người/ngày/bữa/thiếu-dư, cuộn bảng giữ tên và bong bóng không che nút. Không thay thiết lập điện thoại cá nhân chỉ để lấy ảnh.
- Cập nhật README/HANDOFF, phiên bản APK/hash/schema, báo cáo kết quả thực và ca chưa đạt. Chỉ đánh dấu Mốc 2 hoàn thành khi các tiêu chí bắt buộc dưới đây đạt.

**Đầu ra:** APK debug tái lập được, dữ liệu mẫu nghiệm thu, Excel mẫu, kết quả tự động/thiết bị và bàn giao đầy đủ.

## Bảng kiểm kết thúc Mốc 2

| Mã | Điều kiện bắt buộc | Trạng thái hiện tại |
|---|---|---|
| M2-01 | Sổ cũ/Purchase/Audit/receipt/CloseRevision giữ nguyên qua toàn bộ đường migration | Chưa triển khai |
| M2-02 | Khách phân biệt chưa đăng ký/cắt; người và nhóm lịch sử đúng | Chưa đạt đầy đủ — F01/F04 |
| M2-03 | Tháng 28/29/30/31, giá/nhóm có hiệu lực, nháp/tương lai tách số chính thức | Chưa triển khai đầy đủ |
| M2-04 | Một nguồn chi, thu cá nhân/nhóm khớp phân bổ và công nợ | Chưa triển khai |
| M2-05 | Hoàn/cấn/giữ dư đúng, không dùng phần dư hai lần | Chưa triển khai |
| M2-06 | Quyền lợi bù và tiền nhận khác kỳ đúng, làm tròn một lần | Mới có phần tính nền |
| M2-07 | Bảng tháng, bộ lọc, chi tiết người và truy chứng từ chạy offline | Chưa triển khai |
| M2-08 | Excel mở được và đối chiếu toàn bộ tổng với app/Room | Chưa triển khai |
| M2-09 | Retry/concurrency/stale/rollback và chẩn đoán nghiệp vụ mới đạt | Chỉ có kiểm thử nền cũ |
| M2-10 | Ngữ cảnh chat tài chính đúng; mọi công cụ ghi đã mở có handler/preview/receipt | Chưa triển khai |
| M2-11 | Native/font lớn/TalkBack/process death và kết quả đúng APK được ghi lại | Chưa nghiệm thu mốc mới |

## Các luồng làm song song và cổng vận hành thật

- **Bổ sung Mốc 1:** voice vi-VN thành công, chọn tệp tại máy, thử dán PNG và Sharesheet sang ứng dụng nhận, kéo bubble, offline/force-stop toàn luồng. Ghi kết quả riêng; không coi test cũ là bằng chứng cho UI vừa thay.
- **Mốc 3 bắt buộc trước dữ liệu thật:** backup/restore SAF có version/checksum/phạm vi, preview và xác nhận; phục hồi thử sang DB thử riêng, giữ dữ liệu cũ khi lỗi, giữ Audit/receipt/phân bổ/quyết toán; bỏ khóa API và Technical Log khỏi bản sao; làm hết hiệu lực thẻ AI chờ sau khôi phục. Kiểm tra nâng bản/khôi phục vòng kín bằng fixture của Mốc 2.
- **Khả năng tiếp tục từ sổ thật cũ:** chỉ chốt số dư mở sổ khi biết mốc quản lý và dữ liệu thu/chi đã có. Phần này cần dữ liệu/chính sách của người quản lý trước dùng thật, không chặn lập trình và kiểm tra bằng fixture.
- **Kết nối thật:** direct cần người dùng tự nhập khóa trên điện thoại và thử riêng; gateway từ xa cần HTTPS/xác thực nếu được chọn. Thiếu mạng/khóa không chặn kiểm thử luồng tài chính thủ công.
- **Backlog:** khách đoàn, thao tác nhiều người/bữa có preview chung, AI nhiều bước/đọc tệp, nguyên liệu Magic Menu, tablet, phát hành store. Không mở rộng các phần này trong chuỗi chính khi chưa nghiệm thu số tháng.

## Gói việc đầu tiên cho phiên triển khai

Bắt đầu **2.0**, sau đó **2.1**: tái hiện F01 bằng fixture khách chỉ đăng ký trưa → sửa trạng thái chốt/migration → sửa tên lịch sử và preview giá → kiểm tra bảo toàn số liệu → thêm UI hồ sơ/định mức có hiệu lực. File bắt đầu: `Database.kt`, `KitchenRepository.kt`, `FinanceScreen.kt`, `ManualConfirmDialog.kt` và các test Room/migration hiện có.

Chỉ mở nhánh thu/quyết toán sau khi hợp đồng quỹ/Purchase/số dư chuyển tiếp ở 2.2 đã được ghi thành code và kiểm thử. Không dùng `Accounting.allocate` đã đạt để thay cho test lưu thu nhóm thật.

## Lệnh kiểm tra dùng tiếp

```powershell
.\scripts\build.ps1
.\scripts\gateway.ps1 -Mode test
# Buộc thực thi lại kiểm thử JVM khi cần bằng chứng mới:
.\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', '--rerun-tasks')
# Chỉ trên thiết bị thử chứa dữ liệu mẫu đã xác định:
$env:ANDROID_SERIAL = 'SERIAL_MAY_THU'
.\scripts\build.ps1 -Tasks @(':app:connectedDebugAndroidTest')
```

Test native có thể thay đổi/gỡ package thử, nên phải chỉ định đúng thiết bị và phạm vi ca; không tự chạy trên sổ người dùng đang nhập. Test Gemini thật opt-in theo README, không lấy khóa từ cuộc trò chuyện hoặc file repo.
