# Quy tắc vận hành bếp ăn

## Ngày, người ăn và chấm cơm

- Ngày nghiệp vụ dùng `Asia/Ho_Chi_Minh`. Chọn ngày xem độc lập với ngày hiện tại. Ba bữa có mã `BREAKFAST`, `LUNCH`, `DINNER`.
- Cán bộ và chiến sĩ có hồ sơ và ngày bắt đầu/ngừng ăn. Người đang hoạt động được mặc định chọn cả ba bữa khi chuẩn bị một ngày. Khách có bữa khi có đăng ký theo lịch hoặc thêm trong ngày. Khách đoàn có thể nhập số suất theo tên đoàn, còn khách đích danh giữ ID riêng.
- Lịch cắt cơm có phạm vi bữa, ngày bắt đầu và ngày cuối cùng còn hiệu lực, hoặc để mở. **Ăn lại từ ngày X** có hiệu lực ngay ngày X. Cho phép **cắt cả ngày** và **cắt cùng bữa cho nhiều người**. Khi lịch trùng, tính mỗi suất một lần. Hai lựa chọn trái ngược cần hiện đối tượng và phạm vi để người quản lý xử lý.
- Khi xem ngày nháp, kết hợp hồ sơ có hiệu lực → lịch → điều chỉnh trực tiếp đã lưu. Thay đổi hồ sơ/lịch cập nhật phần mặc định cho các ngày nháp có liên quan. Giữ quyết định thủ công đã ghi, báo xung đột nếu phạm vi thay đổi. Lịch sử ngày đã chốt lấy từ bản chốt.
- Trong sổ, phân biệt **Có ăn**, **Cắt cơm**, **Chưa đăng ký** và lớp trạng thái **Ngày chưa chốt**. Khách được đánh dấu cắt khi hủy một bữa đã đăng ký. Một khách chưa từng đăng ký bữa đó có trạng thái Chưa đăng ký.
- Nếu có điểm danh, dùng trường riêng cho **Chưa điểm danh · Có mặt · Vắng**. Người vắng khi điểm danh có thể đưa vào danh sách cần đối soát. Thao tác **Cắt cơm** hoặc điều chỉnh đã xác nhận mới thay đổi suất tính tiền. Hạn cắt cơm và cách tính cho vắng không báo là quy tắc cần người quản lý xác nhận trước khi dùng thật.

## Trình tự trong ngày

| Bước | Điều app làm |
|---|---|
| Chuẩn bị ngày mai | Mở hoặc tạo đúng một bản nháp cho ngày tiếp theo. Áp dụng lịch, giữ thực đơn và điều chỉnh đã chuẩn bị |
| Cập nhật trong ngày | Sửa suất, thêm/hủy khách, sửa thực đơn và ghi khoản mua |
| Chốt vào buổi tối | Hiện số suất ba bữa, tiền ăn, ngân sách, tiền mua và thay đổi. Sau xác nhận, lưu bản chốt và đưa vào tổng hợp |
| Chuẩn bị tiếp | Chuyển đến ngày kế tiếp sau khi xác nhận chốt thành công. Bấm lại mở cùng ngày nháp |
| Điều chỉnh ngày đã chốt | Tạo phiên chỉnh có lý do. Tính chênh lệch, xác nhận và thay bản chốt đang có hiệu lực. Giữ bản cũ trong lịch sử |

Có thể giữ **Chốt suất** như dấu mốc dự trù riêng nếu repo đã có nhu cầu, với nhãn rõ ràng. Nút **Chốt sổ ngày** là bước đưa số liệu suất và tiền theo suất vào báo cáo. Khi đang chuẩn bị bản điều chỉnh, báo cáo vẫn đọc bản chốt đang có hiệu lực.

## Ngân sách và tiền mua

Giá theo mẫu ban đầu là **14.400₫ sáng, 28.800₫ trưa, 28.800₫ tối**, đủ ba bữa **72.000₫**. Cho phép chỉnh định mức có ngày hiệu lực. Phí gạo, gia vị và chất đốt dùng để tính ngân sách có đơn giá **theo suất, theo bữa**. Nếu sửa phí riêng một ngày còn nháp, hiện đó là mức áp dụng cho ngày đó và lưu mức này khi chốt. Các giá trong ví dụ dưới đây dùng cho kiểm thử. Thiết lập giá thực tế ở Cài đặt trước khi ghi dữ liệu vận hành.

| Chỉ số | Công thức và thời điểm |
|---|---|
| Tiền ăn theo định mức của bữa | Số suất được tính tiền × giá bữa |
| Ngân sách đi chợ của bữa | Số suất × (giá bữa − gạo − gia vị − chất đốt trên một suất) |
| Ngân sách đi chợ ngày | Tổng ngân sách ba bữa, có nhãn dự kiến/đã chốt |
| Đã mua | Tổng các giao dịch mua thực tế đang hiệu lực của ngày |
| Chênh lệch | Ngân sách đi chợ ngày − Đã mua. Số âm hiện **Vượt ngân sách** kèm số tiền |
| Tiền ăn phát sinh chính thức | Cộng từ các bản chốt đang hiệu lực trong kỳ |
| Quỹ thực tế | Giao dịch thu/chi đã lưu, ví dụ tiền đã nộp, đã mua, đã hoàn, bù gạo thực nhận |

Mỗi khoản mua có ngày, số tiền, nội dung, nguồn thao tác và ID. Có thể nhập một khoản tổng ngày hoặc nhiều dòng. Kiểm tra số nguyên VND và phân biệt 0₫ với ô chưa nhập. Nếu phí vượt giá bữa, hiện lỗi ở cấu hình hoặc bản xem trước để sửa. Khi bớt suất, ngân sách thay đổi và khoản mua đã ghi giữ giá trị theo giao dịch. Bản chốt lưu định mức đã dùng để đối soát.

**Ca chuẩn.** Trưa có 10 suất giá 28.800₫, phí 3.000₫ gạo + 1.000₫ gia vị + 800₫ chất đốt. Tiền ăn là 288.000₫, ngân sách là 240.000₫. Cắt hợp lệ một suất trước chốt thì còn 9 suất, tiền ăn 259.200₫ và ngân sách 216.000₫. Khoản mua rau 85.000₫ vẫn là 85.000₫.

## Sổ tháng, thu và quyết toán

- Chọn tháng và nhóm **Cán bộ · Chiến sĩ · Khách**. **Có cắt cơm** là bộ lọc trong từng nhóm, tính từ bản chốt có hiệu lực. Ngày nháp hiện riêng. Số ngày chưa chốt cần phân biệt ngày đã tới với ngày tương lai.
- Sổ người có tiền tạm thu dự kiến, tiền ăn đã chốt, đã nộp, cấn vào, đã hoàn, cấn ra, còn thiếu/dư. Tiền dự kiến dùng để tham khảo, ví dụ 30 ngày × 72.000₫ = 2.160.000₫. Lấy số ngày đúng của tháng, kể cả năm nhuận. Người bắt đầu/ngừng giữa kỳ hiển thị thời gian có hiệu lực để đối chiếu.
- Khách dùng đơn giá bữa, thường mặc định **Thu sau**. Hỗ trợ thu cá nhân hoặc gắn với đăng ký khách đoàn. Khi chưa chốt kỳ, ghi rõ số dư là tạm tính theo những ngày đã chốt.
- Thu nhóm chiến sĩ lưu **một giao dịch thu quỹ** và các dòng phân bổ cho từng người tại thời điểm thu. Xem trước danh sách, kỳ và số tiền. Với chia đều, phân phần đồng lẻ theo thứ tự ID ổn định và hiện trước khi lưu. Tổng phân bổ bằng tổng giao dịch. Đổi nhóm sau này giữ nguyên lịch sử phân bổ.
- Số đối chiếu một người = đã nộp tiền ăn + cấn vào − tiền ăn đã chốt − đã hoàn thực tế − cấn ra. Số âm là còn thiếu, số dương là dư. Khoản chờ hoàn giữ phần dư tương ứng để tránh dùng lại. Ghi chi quỹ khi xác nhận đã hoàn thực tế. Hủy chờ hoàn giải phóng phần đã giữ.
- Cấn trừ liên kết hai kỳ, giảm số phải nộp của kỳ đến, đồng thời trừ số dư còn dùng của kỳ đi. Ghi một liên kết có ID duy nhất. Phần dư đã dành để hoàn/cấn cần được đối soát nếu một ngày đã quyết toán thay đổi.
- Đối chiếu tháng có thể chứa khoản thu đầu kỳ, mua thực tế và khoản hoàn dù ngày ăn còn nháp. Thẻ số ngày đã chốt và nhãn cơ sở của từng số phải đọc được ngay.

## Bù giá gạo

- Đây là **khoản được cấp thêm cho chiến sĩ**, để cuối tháng nhận và quyết toán. Cán bộ và khách được loại khỏi phép tính. Bù giá gạo tách khỏi ngân sách đi chợ và khỏi tiền ăn phải nộp/hoàn của từng người.
- Mức tham chiếu trong mẫu là **700₫/kg**. Lượng gạo tham chiếu **136 g sáng, 272 g trưa, 272 g tối**. Mức tiền và định lượng theo bữa có ngày hiệu lực. Bản chốt giữ nhóm, số bữa, định lượng và mức áp dụng của ngày.
- Tổng chính xác của tháng là `sum(suất chiến sĩ × gram mỗi suất × VND mỗi kg) / 1000`. Lưu phần nguyên theo một phần nghìn đồng hoặc dùng số thập phân chính xác. Mặc định triển khai **làm tròn nửa lên đến đồng một lần ở tổng tháng**, ghi trong HANDOFF để người dùng đối chiếu trước khi dùng thật.
- Đủ ba bữa của một chiến sĩ ở mức 700₫/kg là **476₫**. Ba suất sáng ở mức đó là 285,6₫, tổng hiển thị **286₫**. Tính theo ngày áp dụng khi đổi mức giữa tháng.
- Thẻ bù gạo có **Được cấp theo tính toán**, **Đã nhận**, **Còn chờ nhận**. Khi đã nhận thực tế, ghi một giao dịch thu quỹ loại **Bù giá gạo**, gắn kỳ bù và ngày nhận. Kỳ bù có thể khác tháng hạch toán tiền nhận. Hiện chênh lệch để đối soát nếu số nhận vượt hoặc điều chỉnh làm giảm số được cấp.

## Mặc định cần đối chiếu trước khi nhập thật

| Nội dung | Phương án để tiếp tục phát triển |
|---|---|
| Vắng không báo, báo muộn | Ghi trạng thái cần đối soát. Người quản lý chọn điều chỉnh suất nếu có căn cứ |
| Giá suất, phí, mức bù hiện hành | Dùng dữ liệu thử trong debug và cấu hình có hiệu lực. Đối chiếu mức thực tế trước khi vận hành |
| Làm tròn khoản cấp | Giữ giá trị chính xác theo kỳ, dùng quy tắc ở mục Bù giá gạo làm mặc định thử |
| Mẫu in/xuất sổ | Dựng bảng đối chiếu theo nhóm trước. Đối chiếu mẫu nộp chính thức ở giai đoạn xuất Excel |
