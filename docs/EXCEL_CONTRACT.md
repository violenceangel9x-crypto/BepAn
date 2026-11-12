# Hợp đồng Excel đối soát Mốc 2

File XLSX nội bộ lấy một snapshot FinanceMonth từ Room, độc lập cuộn màn hình/bộ lọc hiện tại. Không tự thay đổi dữ liệu khi xuất. Kỳ ăn và tháng phát sinh tiền thực tế có nhãn riêng.

| Sheet | Nội dung và cơ sở tính |
|---|---|
| Đối soát | Kỳ, ngày xuất, ngày chốt/chưa chốt/tương lai; tổng tiền ăn, nộp, thiếu/dư, bù chính xác và thực nhận; mỗi người tính một lần |
| Cán bộ / Chiến sĩ / Khách | Chỉ nhóm có người trong kỳ; cột tên cố định, ngày 1..cuối tháng, ba bữa Sáng/Trưa/Tối; tổng bữa và tiền ăn đúng nhóm snapshot |
| Quỹ tháng | Các dòng CashEntry theo ngày tiền thực tế trong tháng đang chọn; không cộng thêm Purchase lần thứ hai |
| Chứng từ kỳ | CashEntry gắn kỳ nghiệp vụ, kể cả nhận tiền vào tháng khác; có ngày thực nhận/chi và tham chiếu |
| Phân bổ | Người, nhóm và tiền đã phân bổ tại lúc thu; tổng theo chứng từ bằng khoản thu cha |
| Quyết toán | Nguồn/kỳ đến, trạng thái chờ/đã trả/hủy/cấn, số tiền, chứng từ và liên kết khoản giữ |

Tiền ăn nhóm và phân bổ nhóm cộng được theo nhóm. Cột số đối chiếu/hoàn/cấn của cá nhân là **toàn kỳ**, chỉ tham khảo ở sheet nhóm; người đổi nhóm có thể xuất hiện ở nhiều sheet, vì vậy không cộng lặp các cột này. Sheet Đối soát tổng hợp mỗi ID một lần. Bù gạo giữ milli-VND, chỉ làm tròn tổng kỳ; đóng góp mỗi người ở sheet chiến sĩ giữ phần lẻ, không cộng các số làm tròn riêng.

Ô bữa phân biệt Có ăn, Cắt, Chưa đăng ký, Chưa xác định (sổ cũ), Nháp và Tương lai. Ngày chưa chốt không tạo tiền ăn chính thức. Giá lịch sử lấy ClosedMeal, không dùng giá hiện tại thay thế.

Tiền là ô số; tên/nội dung là inline string để giữ dấu và không thực thi chuỗi bắt đầu bằng dấu '=' như công thức. File không có khóa API, chat hoặc Technical Log. File sổ chứa dữ liệu riêng người dùng chủ động xuất; không đưa file thật vào Git/báo cáo lỗi.

Định dạng dùng ZIP/SpreadsheetML theo [cấu trúc chính thức của Microsoft](https://learn.microsoft.com/en-us/office/open-xml/spreadsheet/structure-of-a-spreadsheetml-document) và [working with sheets](https://learn.microsoft.com/en-us/office/open-xml/spreadsheet/working-with-sheets). Writer nhỏ dùng Java ZIP sẵn có, không thêm thư viện desktop vào APK. Test kiểm tra các part/relationships/XML/giá trị và mở lại file mẫu bằng Excel khi có môi trường.

Đây là mẫu đối soát nội bộ theo docs/06; chưa tuyên bố khớp mẫu in/nộp chính thức chưa được duyệt.
