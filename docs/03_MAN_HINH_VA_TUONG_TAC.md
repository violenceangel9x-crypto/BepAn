# Bố cục và tương tác

Màn chính có thanh dưới **Hôm nay · Tài chính · Ghi chú · Cài đặt**. Chuyển tab giữ vị trí, bộ lọc và trạng thái hợp lý. Nút tròn **Gemini** nằm trên thanh dưới và có thể kéo đến cạnh an toàn. Bố trí nút tránh che hàng cuối, Chốt sổ, Lưu, bàn phím và các nút xác nhận.

## Hôm nay

| Từ trên xuống | Nội dung và thao tác |
|---|---|
| Header | **Hôm nay**, nút ngày trước/sau hoặc chọn ngày, nhãn **Nháp / Đã chốt / Đang điều chỉnh** |
| Thẻ Ngân sách đi chợ | Số tiền lớn, **Dự kiến** hoặc **Đã chốt**, hai số **Đã mua** và **Chênh lệch**. Bấm mở chi tiết ba bữa, số suất và phí |
| Thẻ Thực đơn | Tiêu đề, **Chỉnh sửa**, bốn ô dọc, **Sao chép ảnh thực đơn** và **Chia sẻ** dưới ô cuối |
| Thẻ Chấm cơm | Số suất Sáng/Trưa/Tối, tìm tên, ba nhóm, các nút chọn bữa, cắt cả ngày, thêm khách |
| Thẻ Thay đổi trong ngày | Ngoại lệ như cắt bữa, khách thêm/hủy và trường hợp cần đối soát |
| Thẻ Tiền mua | Danh sách khoản mua của ngày, tổng, **Ghi tiền mua**, chi tiết giao dịch |
| Cuối màn | **Chốt sổ và chuẩn bị ngày mai**. Sau chốt, **Chuẩn bị ngày mai** hoặc **Điều chỉnh ngày** |

Chạm suất trên thẻ Chấm cơm mở nhóm/bữa đang chọn. Với danh sách nhiều người, ưu tiên vùng **Có thay đổi hôm nay**, có thể mở danh sách toàn bộ. Mỗi hàng có tên, nhóm và nút Sáng/Trưa/Tối. Đọc bằng TalkBack có tên bữa và trạng thái, ví dụ “Trưa, đã cắt cơm, Nguyễn Văn Nam”.

### Thẻ Thực đơn

| Thứ tự | Tên ô nếu đang xem ngày 10/11 | Dữ liệu |
|---:|---|---|
| 1 | Bữa sáng (10/11) | `Menu(10/11, BREAKFAST)` |
| 2 | Bữa trưa (10/11) | `Menu(10/11, LUNCH)` |
| 3 | Bữa tối (10/11) | `Menu(10/11, DINNER)` |
| 4 | Bữa sáng (11/11) · Ngày kế tiếp | `Menu(11/11, BREAKFAST)` |

Giữ bố cục dọc, icon mặt trời/trăng, ngày đậm và thẻ sáng theo [ảnh mẫu](../references/bo-cuc-the-thuc-don.jpg). Món ăn có thể xuống dòng. Ô cuối có nhấn nhẹ và ngày cụ thể để phân biệt. Khi ngày đang xem không phải hôm nay, dùng **Ngày kế tiếp** để tránh hiểu nhầm với ngày mai theo đồng hồ.

- **Chỉnh sửa** mở màn hoặc sheet đủ cao cho bốn ô. Sửa từng dòng món, đổi thứ tự, thêm/xóa và đánh dấu **Có kế hoạch riêng**. Ô chọn bên trái dùng ở chế độ sửa để chọn bữa cần tạo lại, có nhãn **Chọn bữa cho Magic Menu**. Ô có sổ đã chốt hiện khóa và lối vào điều chỉnh phù hợp.
- **Magic Menu** cho chọn một bữa, bữa trống hoặc bốn ô được phép sửa. Chọn món/bữa muốn giữ, tạo gợi ý, đổi lại một món hoặc một bữa, rồi **Áp dụng**. **Hủy** quay về bản đã lưu. Với bữa sáng ngày kế tiếp, luồng đọc/ghi dùng chính xác ngày đó kể cả qua tháng/năm.
- **Sao chép ảnh thực đơn** tạo PNG chỉ từ tiêu đề đến hết ô thứ tư. Ảnh dùng nền sáng, không có nút Chỉnh sửa, nút xuất, thanh điều hướng, checkbox, bàn phím hoặc Gemini. Nút **Chia sẻ** dùng cùng ảnh, mở trình chọn ứng dụng Android. App đích có thể hỗ trợ chia sẻ và dán ảnh theo cách khác nhau.
- Nếu nội dung thực đơn trống, hiện **Chưa có thực đơn** trong ô, giữ đầy đủ bốn ô. Khi tạo ảnh trong chế độ đọc, dùng dữ liệu đã lưu để ảnh khớp với thẻ. Trong lúc tạo hiện phản hồi và tránh bấm lặp.

## Tài chính

| Từ trên xuống | Nội dung và thao tác |
|---|---|
| Header | Chọn tháng. **Đã chốt x/y ngày** và **Xem ngày chưa chốt** |
| Thẻ Tiền ăn tháng | Tiền ăn phát sinh từ ngày đã chốt và cơ sở tính |
| Thẻ Thu tiền ăn | Đã nhận tiền ăn, còn phải thu, lối vào giao dịch và ghi thu cá nhân/nhóm |
| Thẻ Tiền mua thực tế | Tổng giao dịch mua đã ghi, xem theo ngày |
| Thẻ Bù giá gạo | **Chiến sĩ**, tổng theo kỳ, đã nhận, còn chờ và **Ghi nhận đã nhận** |
| Sổ chấm cơm | Nhóm **Cán bộ · Chiến sĩ · Khách**, lọc **Có cắt cơm · Còn thiếu · Có dư** |
| Thẻ từng người | Họ tên, nhóm, số bữa S/T/Tối, tiền ăn, đã nộp, thiếu/dư, số bữa cắt |
| Chi tiết | Bảng ngày × ba bữa của người đó, **Xem bảng cả nhóm**, **Xuất sổ** |

Bảng cả nhóm giống Excel về cấu trúc. Mỗi người một hàng, cột tên cố định, hàng đầu gom theo ngày, mỗi ngày có Sáng/Trưa/Tối. Có bảng trạng thái đọc được bằng chữ, kể cả ngày nháp. Số chính thức hiển thị từ ngày đã chốt. Với font lớn hoặc TalkBack, có lối xem chi tiết ngày của từng người. Trong form thu/hoàn/cấn trừ, luôn hiện kỳ, người, số tiền và tác động trước khi xác nhận.

## Ghi chú

Thanh tìm kiếm, **Thêm ghi chú**, ghi chú ghim trước, ghi chú thường theo ngày cập nhật. Cho sửa, ghim/bỏ ghim và tìm nội dung. **Lịch sử hoạt động** đọc Audit Log đã lưu, mở màn riêng có lọc theo Chấm cơm, Thực đơn, Thu chi và Chốt sổ. Mỗi mục hiện thời gian, nguồn **Bạn**, **Gemini** hoặc **Gemini (mô phỏng)**, nội dung và chi tiết trước/sau khi cần.

## Cài đặt

| Thẻ | Quản lý |
|---|---|
| Người ăn | Cán bộ, Chiến sĩ, Khách; tên, nhóm, ngày hiệu lực, lịch |
| Định mức | Giá bữa, phí gạo/gia vị/chất đốt, lịch sử giá |
| Bù giá gạo | Mức VND/kg, lượng gram mỗi bữa, ngày hiệu lực và danh sách mức đã dùng |
| Gemini | Kết nối, chế độ debug khi phù hợp, giọng nói, trạng thái cấu hình |
| Chẩn đoán | **AI Debug Center**, lọc lượt thao tác, dòng thời gian, **Xuất báo cáo lỗi**, dọn log kỹ thuật |
| Dữ liệu | Sao lưu, khôi phục, kiểm tra thông tin bản sao trước khi ghi |
| Giao diện | Hiệu ứng kính, cỡ chữ theo hệ thống, hiển thị dễ đọc |

## Chat Gemini kiểu Messenger/Zalo

| Vùng | Hành vi |
|---|---|
| Thanh đầu | Avatar, **Gemini — Trợ lý bếp ăn**, nút thu nhỏ. Back quay về đúng tab và vị trí |
| Ngữ cảnh | **Đang xem** với ngày/bữa/tháng/nhóm liên quan. Có thể mở để kiểm tra hoặc đổi phạm vi |
| Dòng chat | Tin người dùng bên phải, Gemini bên trái, có giờ và chỉ báo **Đang xử lý**. Giữ bản nháp và lịch sử khi quay lại |
| Gợi ý | Chip theo màn, ví dụ **Suất ăn hôm nay**, **Magic Menu**, **Ghi khoản chi**, **Chốt sổ**. Bấm điền yêu cầu hoặc mở bước chọn phạm vi rõ ràng |
| Thanh nhập | **+ · Nhắn lệnh cho Gemini… · Micro · Gửi**. Nằm trên bàn phím. Nút + chọn ảnh/tệp. Micro bấm nghe, bấm dừng, cho sửa chữ rồi mới gửi |
| Thẻ thao tác | Ứng viên trùng tên, kết quả truy vấn, xem trước, đã lưu, hủy, lỗi, hết hiệu lực hoặc hoàn tác. Dùng cùng lời và dữ liệu với màn nghiệp vụ |

**Ví dụ trong chat.** Người dùng nhắn “Ghi tiền mua rau hôm nay 85 nghìn”. Gemini hiện thẻ **Xác nhận khoản chi**, ghi **Mua rau · 85.000₫ · ngày cụ thể** và có **Xác nhận lưu · Chỉnh sửa · Hủy**. Lưu thành công chuyển chính thẻ đó thành **Đã ghi khoản chi**, có nút **Xem khoản chi**. Với lệnh cắt nháp đúng người và bữa, dùng thẻ kết quả có **Hoàn tác** và **Xem chấm cơm**.

Thông báo lỗi hoặc mất mạng xuất hiện trong ngữ cảnh tin nhắn, cho **Thử lại** khi phù hợp và giữ nội dung đang nhập. Khi lệnh bị thiếu ngày/bữa/người, hỏi ngay trong dòng chat, giữ các trường đã hiểu. Thẻ lệnh cũ giữ ngày cụ thể ở thời điểm gửi dù người dùng đổi tab hay đổi ngày.

Với lỗi có mã theo dõi, thêm **Xem chẩn đoán** trên thẻ chat. Nút mở đúng lượt trong AI Debug Center. Màn chi tiết có nhãn bước lỗi, trạng thái ghi dữ liệu và nút xuất. Bản xem trước báo cáo cho nhập cách tái hiện rồi lưu hoặc chia sẻ theo [đặc tả chẩn đoán](10_AI_DEBUG_CENTER_VA_HANDOFF.md).
