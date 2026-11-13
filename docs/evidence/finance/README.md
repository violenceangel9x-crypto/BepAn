# Bằng chứng Mốc 2 · 13/11/2026

Tất cả số và ảnh ở thư mục này dùng dữ liệu mẫu. XLSX, Room, khóa và output instrumentation thô nằm ngoài Git. APK kiểm tra: `vn.bepan.debug`, **0.2.0-demo/code 3**, schema **7**; SHA-256 **10105A88AF0C266E639411C85B2C26FA3CB3FF013266334DFB630D6125E97605**.

- [build-check.json](build-check.json): 64 JVM, gateway 7, lint 0 lỗi/30 cảnh báo. HTML/XML gốc trong `app/build/reports` và `app/build/test-results`.
- [excel-full-month-check.json](excel-full-month-check.json): Excel 16.0 mở fixture tháng 02/2028, 8 sheet/99 cột nhóm. Tiền ăn 5.472.000₫, thu 6.000.001₫, bù 22.780₫, còn thiếu 30.000₫, dư 362.000₫, quỹ 6.897.380₫; tổng ba nhóm khớp. Fixture có thay giá/nhóm/ngừng/ăn lại, khách hủy/ăn, điều chỉnh hai phiên, hoàn từng phần và cấn.
- [excel-check.json](excel-check.json): fixture nhỏ kiểm tra Unicode/XML, ô số, tên không thành công thức và tháng nhuận.
- [native-excel-check.json](native-excel-check.json): hai file thực sự lưu qua SAF đã mở lại read-only bằng Excel; số kỳ khác nhau vì mỗi ca dùng trạng thái mẫu trên thiết bị đó.
- Các file `SERIAL-SUITE.json` được `scripts/test-finance-native.ps1` sinh từ kết quả instrumentation thật, kèm hash app và test APK. Các lần sửa độ ổn định của mã test có hash test APK khác nhau; **hash app giữ nguyên**. `tests` là số ca runner ghi nhận; `skipped` phải trừ ra khi đếm ca đã chạy.

| Suite | Phạm vi |
|---|---|
| core | Thu nhóm/đồng lẻ → giữ/hoàn một phần/hủy → cấn → nhận bù; kiểm tra Room/Audit/receipt; bảng giữ tên; AI chọn hai Nam/xác nhận/retry/kỳ gốc; Activity recreation |
| configuration | Sửa người hiện có và định mức qua UI, ngày hiệu lực riêng ở 2140 để không thay lịch sử mẫu; không thêm/xóa người |
| presentation | Thu cá nhân rồi hủy không ghi; hủy SAF không báo thành công; lưu XLSX và đọc lại ZIP. Emulator 320dp/font2×/máy bay; Samsung giữ font1,3× |
| recovery | Ba lần instrumentation riêng, `am force-stop` giữa trước/sau commit; draft, kỳ/nhóm/bộ lọc, receipt/phân bổ và chống ghi trùng. Kết thúc đưa tháng về hiện tại |
| regression | Hôm nay: chấm/mua/thực đơn dài/chốt/điều chỉnh; chat: hai Nam/hoàn tác/timeout/báo cáo lọc/giữ vị trí đọc; từ chối micro rồi nhập chữ nếu máy chưa cấp quyền |
| talkback | TalkBack 14.2 bật thật trên emulator 360dp/font1,3×; dịch vụ đọc và touch exploration hoạt động, focus thiếu/dư và ô có tên/ngày/bữa/trạng thái |

Samsung `R5CN70V0BED` là SM-N986U1/API33. Emulator `emulator-5554` là BepAn_API34/API34. Samsung đã có quyền micro nên ca từ chối quyền được bỏ qua, giữ quyền người dùng; không thay khóa đã lưu hay cài đặt hiển thị máy thật. Mỗi kết quả phải đọc cùng file JSON của suite; ảnh là điểm chụp giữa luồng, không thay cho assertion Room.

Ảnh chính: [Samsung bảng giữ tên](samsung/finance-pinned-name.png), [Samsung AI đã lưu](samsung/finance-assistant-saved.png), [320dp chữ 2×](emulator/finance-large-preview.png), [TalkBack focus](emulator/finance-talkback-focus.png). [talkback-check.json](emulator/talkback-check.json) giữ các cờ đo thực tế.

Lệnh tái lập ở gốc repo:

```powershell
.\scripts\build.ps1
.\scripts\gateway.ps1 -Mode test
.\scripts\verify-excel.ps1
.\scripts\verify-excel.ps1 -Workbook app/build/outputs/finance-month-fixture.xlsx -FullMonth
.\scripts\test-finance-native.ps1 -Serial SERIAL -Suite core
.\scripts\test-finance-native.ps1 -Serial SERIAL -Suite presentation -SkipBuild
.\scripts\test-finance-native.ps1 -Serial SERIAL -Suite configuration -SkipBuild
.\scripts\test-finance-native.ps1 -Serial SERIAL -Suite regression -SkipBuild
.\scripts\test-finance-native.ps1 -Serial SERIAL -Suite recovery -SkipBuild
# Chỉ emulator, bật TalkBack trước khi chạy:
.\scripts\test-finance-native.ps1 -Serial emulator-5554 -Suite talkback -SkipBuild
```

Native chỉ dùng bản dữ liệu mẫu; script cài `-r`, không uninstall/clear. Bản thử để lại chứng từ giả và các mốc cấu hình xa trong tương lai. JVM tạo fixture từ đầu trong DB thử riêng. Gemini mới kiểm tra bằng mock/transport giả, không gọi khóa thật trong chuỗi tài chính này. Backup/restore và mẫu in nộp chính thức vẫn thuộc mốc sau.
