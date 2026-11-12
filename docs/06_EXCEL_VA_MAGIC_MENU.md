# Đối chiếu Excel và xây Magic Menu

## Sổ chấm cơm

Bản gốc có tên **Số chấm cơm 07.26.xlsx**. Hai sheet nghiệp vụ là `07.26 CB` và `07,26 CS ` (tên sheet chiến sĩ có khoảng trắng cuối). Các sheet phụ, bìa và sheet khôi phục không dùng để dựng phép tính. Sổ tháng chia từng ngày thành ba cột `S / T / C`. Trong app dùng **Sáng / Trưa / Tối**. Sổ chiến sĩ có khối tổng bữa, tiền ăn, bù giá gạo, số nhận và trả lại.

| Phần trong Excel | Áp dụng trong app |
|---|---|
| Họ tên, cấp bậc, nhóm trong sổ | Tên, nhóm Cán bộ/Chiến sĩ/Khách. Có thể hiện cấp bậc nếu hồ sơ đã có và cần đối chiếu |
| Ngày 1 đến cuối tháng, mỗi ngày 3 cột | Bảng cả nhóm, tên cố định, ngày và ba bữa. Số cột theo tháng và năm nhuận |
| Cột `CT:CV` ở sheet chiến sĩ | Tổng bữa sáng/trưa/tối từ ngày đã chốt |
| Ô `CW6` ở sheet chiến sĩ | `(CT6×0,136 + CU6×0,272 + CV6×0,272)×700`, dùng làm công thức tham chiếu |
| Ô `DB6` và `DE6` ở sheet chiến sĩ | Tiền ăn cơ bản và trả lại. App dùng tiền thực nộp, phát sinh đã chốt và quy tắc hoàn/cấn hiện hành |
| Cột tổng tiền theo mẫu | Có thẻ tiền ăn, sổ thu chi và bù gạo theo kỳ. Hiện từng cơ sở tính để đối soát |

Excel mẫu dùng đơn giá cố định và phép tính cho tháng 31 ngày. App dùng ngày hiệu lực, số ngày thực trong kỳ và các bản chốt. Một số công thức tham chiếu xuất hiện cả ở sheet cán bộ. Quy tắc sản phẩm chỉ tính bù gạo cho chiến sĩ. Sổ có dữ liệu cá nhân được giữ riêng để đối chiếu khi cần mẫu in chính thức. Dùng [fixture](../data/cham-com-fixtures.v1.json) để phát triển và kiểm thử.

Đề xuất xuất một sheet mỗi nhóm có dữ liệu, kèm tổng Sáng/Trưa/Tối, tiền ăn, đã nộp, dư/thiếu, số bữa cắt và thẻ/khối tổng bù chiến sĩ. Nếu công việc yêu cầu khớp mẫu nộp, đối chiếu vùng in, tiêu đề, cách hiển thị ô và cột quyết toán với file gốc trên máy người dùng. Mở lại Excel và đối chiếu số liệu với app.

## Nguồn Magic Menu

**Bảng menu cho app.xlsx** có bốn sheet `09,26`, `10,26`, `11,26`, `12,26`. Mỗi sheet có năm khối tuần. Hai cụm cột **A:G** và **H:N** đặt ngày trong tuần và ba bữa theo cặp **Tên món ăn / Đ.Lượng**. Các cột món là **B/D/F** và **I/K/M**. Có dòng ghi ngày ở định dạng Excel hoặc chữ “Ngày …”. Khi dùng mẫu ghép món chỉ cần thứ trong tuần và bữa. Nếu làm chức năng nhập lịch theo ngày thật, kiểm tra cả tiêu đề tuần và ngày cụ thể, đặc biệt khi qua tháng.

Từ 420 vị trí bữa đọc được trong bốn sheet, có 416 bữa thường và 4 bữa trưa ghi **Có KH riêng**. Gộp bữa giống nhau theo loại ngày, bữa và thứ tự món còn **165 mẫu**. Xem [menu-templates.v1.json](../data/menu-templates.v1.json) để lấy bữa mẫu đã chuẩn hóa, số lần xuất hiện và sheet/vùng nguồn. Các ô định lượng đi cùng dòng có món đang trống.

| Bữa | Cấu trúc từ mẫu | Số mẫu riêng |
|---|---|---:|
| Sáng ngày thường | Cơm tẻ, món mặn, canh | 20 |
| Sáng cuối tuần | Một món, như bánh cuốn, bún, xôi, bánh mì hoặc mì | 5 |
| Trưa ngày thường / cuối tuần | Cơm, ba món ăn kèm, rau, canh, tráng miệng | 53 / 18 |
| Tối ngày thường / cuối tuần | Cơm, ba món ăn kèm, rau, canh | 53 / 16 |

Từ **Chiều** trong Excel ánh xạ thành **Tối**. “Có KH riêng” được lưu là loại bữa riêng có nhãn rõ. Khi tạo bữa thường, dùng nhóm mẫu thường.

## Chọn và chỉnh thực đơn

1. Xác định ngày/bữa đích, loại ngày thường/cuối tuần, những ô còn được sửa và lựa chọn giữ món hoặc giữ nguyên cả bữa.
2. Lấy mẫu hoàn chỉnh phù hợp từ JSON hoặc kho món trong Room. Ưu tiên mẫu ít trùng gần đây. Có thể khởi đầu bằng cửa sổ ba ngày, sau đó tinh chỉnh theo phản hồi. Cho phép món nền như cơm lặp. Nếu thiếu mẫu, báo rõ và cho mở rộng tiêu chí hoặc sửa tay.
3. Cho thay một món cùng vị trí hoặc tạo lại nguyên bữa. Tên và vị trí món trong dữ liệu trích là gợi ý theo cấu trúc Excel. Bổ sung nhóm nguyên liệu đã được rà nếu muốn lọc ràng buộc như “tránh cá”. Khi điều kiện không kiểm tra được từ kho món, hỏi thêm hoặc hiện rõ trong bản xem trước.
4. Hiện bốn ô hoặc bữa đã chọn, có ngày/bữa đích và món giữ nguyên. **Áp dụng** lưu những ô thay đổi đã được chọn. Nếu ngày hoặc bữa đã đổi trong lúc chờ, hiện bản xem trước mới. **Hủy** giữ dữ liệu đã lưu.
5. Cùng một quy trình dùng được offline và qua chat Gemini. Gemini hiểu yêu cầu tiếng Việt, app chọn/kiểm tra mẫu và hiện bản xem trước. Kho mẫu này đủ để ghép bữa. Muốn dự toán giá nguyên liệu, cần thêm định lượng, đơn vị và giá được người dùng cung cấp.

Ca thử quan trọng là chỉnh ô sáng ngày 01/12 từ thẻ ngày 30/11, mở ngày 01/12 thấy đúng món, rồi sửa trực tiếp ở đó và trở lại 30/11 để đối chiếu.
