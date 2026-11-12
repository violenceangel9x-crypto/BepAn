# Lộ trình triển khai và tiêu chí nghiệm thu

## Mốc triển khai

| Mốc | Phạm vi | Sản phẩm có thể kiểm tra |
|---|---|---|
| 0 · Nền app, AI và chẩn đoán | Gradle, bốn tab, Room, Audit Log, Technical Log, AI Debug Center, Debug Handoff, chat, gateway và mock debug | Build debug. Hỏi suất trưa và cắt bữa trên dữ liệu Room. Có lịch sử, mã theo dõi, thử lưu lặp và xuất được một báo cáo lỗi đã lọc. Ghi riêng kết quả Gemini thật khi có cấu hình |
| 1 · Hôm nay | Chấm mặc định, lịch cắt, khách, ngân sách, bốn ô thực đơn, Magic Menu, ảnh PNG, ghi mua, chốt sổ, micro | Chạy trọn ngày bằng tay và bằng lệnh AI đã hỗ trợ. Test trên Note 20 Ultra, có ca cắt một bữa, hoàn tác và xác nhận ghi chi/chốt |
| 2 · Tài chính | Tổng hợp tháng, sổ giống Excel, ba nhóm, lọc cắt cơm, thu cá nhân/nhóm, hoàn/cấn, bù gạo | Đối soát số giữa ngày, người, nhóm, quỹ và kỳ. Có ca đổi giá giữa tháng, tháng nhuận, nhập thực nhận cuối kỳ |
| 3 · Ghi chú, cài đặt và bàn giao | Hoàn thiện ghi chú, bộ lọc lịch sử/chẩn đoán, thiết lập, sao lưu/khôi phục, xuất sổ và rà các màn | APK debug và lệnh build tái lập, bảng kiểm có kết quả, khôi phục thử thành công, HANDOFF đủ để tiếp tục trên máy khác |

Mốc 0 và 1 dùng màn Hôm nay + chat làm chuẩn thiết kế. Có thể làm một phần UI tài chính sớm để xác định bố cục. Mỗi mốc đưa chức năng đã chạy vào menu thật. Khi chưa xong, chỉ hiện trạng thái hoặc mô phỏng có nhãn trong bản debug.

## Nghiệm thu nghiệp vụ và AI

Dùng [fixture chấm cơm](../data/cham-com-fixtures.v1.json) và [lệnh AI mẫu](../data/gemini-cases.v1.json). Thực hiện các ca áp dụng cho từng mốc và ghi kết quả thật.

| Ca | Cách kiểm tra | Đạt khi |
|---|---|---|
| N01 · Chấm mặc định | 10 người thường xuyên, khách có lịch riêng, cắt trưa một người | Suất duy nhất theo người/ngày/bữa, số trưa giảm đúng một và khách tính đúng lịch |
| N02 · Ngân sách | Trưa 10 suất, phí 4.800₫/suất, mua 85.000₫. Cắt một suất | Ngân sách 240.000₫ → 216.000₫. Đã mua vẫn 85.000₫, chênh lệch riêng bữa trong fixture là 131.000₫ |
| N03 · Chốt | Xem tổng tháng trước chốt, chốt, bấm lại. Điều chỉnh và chốt lại | Ngày nháp chưa có tiền ăn chính thức. Tổng sau mỗi lần dựa trên một bản chốt có hiệu lực |
| N04 · Lịch | Cắt cơm qua tháng, cho ăn lại từ ngày X, tạo ngày mai hai lần | Ngày X áp dụng đúng, kế hoạch đã chuẩn bị giữ nguyên, không sinh suất trùng |
| N05 · Thực đơn | Sửa sáng 01/12 trên thẻ 30/11. Đổi lại từ ngày 01/12. Thử hủy Magic Menu | Hai màn hiện cùng món. Chỉ bữa chọn được áp dụng, bản hủy giữ dữ liệu |
| N06 · Ảnh | Món dài làm thẻ vượt vùng nhìn, cuộn trước khi xuất. Sao chép và chia sẻ | PNG chứa đúng tiêu đề và bốn ô theo thứ tự, chữ và ô cuối đầy đủ, nhận được qua ứng dụng hỗ trợ |
| N07 · Sổ tháng | Kiểm tra tháng 28/29/30/31 ngày, chuyển nhóm, bộ lọc cắt cơm và ngày nháp | Cột đúng kỳ, khách chỉ tính cắt khi từng đăng ký, nhóm/ngày đã chốt giữ lịch sử |
| N08 · Thu nhóm | 10 người nộp 21.600.000₫. Thử 1.000.001₫ chia 3 người, sau đó đổi nhóm | Quỹ tăng một lần, tổng phân bổ đúng đến đồng, lịch sử giữ người lúc thu |
| N09 · Hoàn/cấn | Một người có dư 28.800₫. Thử chờ hoàn, hoàn thực tế hoặc cấn tháng sau | Khoản dùng một lần. Chờ hoàn chưa trừ quỹ, hoàn thực tế trừ quỹ, cấn giảm số cần nộp kỳ sau |
| N10 · Bù gạo | Một chiến sĩ đủ ba bữa, cùng ngày có cán bộ và khách, đổi mức 700→900₫/kg ngày sau | Hai ngày của chiến sĩ cộng 1.088₫, bù gạo ra khỏi ngân sách. Trường hợp 285,6₫ làm tròn tổng 286₫ |
| N11 · Thực nhận bù gạo | Bù tháng 11, nhập nhận ngày 01/12 | Đã nhận hiện đúng ở kỳ bù 11, quỹ ghi giao dịch vào ngày nhận tháng 12 |
| A01 · Chat và cập nhật | Hỏi số trưa, cắt một bữa nháp bằng AI và quay về Hôm nay | Số từ Room, UI cập nhật, có Hoàn tác, lịch sử có nguồn Gemini |
| A02 · Trùng/thiếu | Có hai người tên Nam hoặc chỉ nói “Cắt cơm cho Nam” | Có thẻ chọn người và hỏi đúng trường thiếu, không ghi trước khi rõ |
| A03 · Xác nhận | Nhắn mua rau 85 nghìn, xác nhận hai lần hoặc mở lại sau timeout | Hiện 85.000₫ và ngày cụ thể trước lưu. Một dòng thu/chi thực tế, thẻ đã lưu sau commit |
| A04 · Bản xem trước cũ | Tạo bản xem trước, thay đổi đối tượng hoặc chốt ngày bằng màn khác, quay lại xác nhận | Thẻ hết hiệu lực, hiện dữ liệu mới. Ghi sai bị chặn |
| A05 · Lệnh nhiều bước | Cắt một bữa, thêm hai khách và sửa menu | Một bản xem trước rõ phạm vi và tác động, báo phần thiếu. Kết quả chính xác hoặc rollback phần ghi chung nếu lỗi |
| A06 · Mất mạng | Dùng chế độ máy bay, từ chối micro, thử lại sau khi có mạng | Thao tác tay và Magic Menu cục bộ dùng được, giữ tin nháp, chat có trạng thái. Ghi rõ đang dùng Gemini thật hay mô phỏng |
| D01 · Lưu và phục hồi | Đóng/mở app, nâng bản debug nếu có migration, sao lưu và khôi phục | Số liệu giữ đúng, ca khôi phục lỗi giữ dữ liệu cũ |

## Nghiệm thu nền dữ liệu và chẩn đoán ở mốc 0

Chạy [các ca kiểm tra log](../data/logging-cases.v1.json). Dùng dữ liệu giả và mô phỏng có nhãn nếu chưa kết nối Gemini thật.

| Ca | Đạt khi |
|---|---|
| L01–L02 · Ghi và gửi lại | Thao tác tay/AI lưu qua khởi động lại. Cùng `operationId` chỉ commit một lần, Audit có đúng nguồn, gửi lại trả cùng kết quả |
| L03–L05 · Lỗi lệnh | Timeout, dữ liệu sai hoặc thẻ cũ có bước lỗi rõ. Mở đúng lượt từ chat. Lệnh chưa ghi có kết quả đối chiếu tương ứng |
| L06–L07 · Ranh giới giao dịch | Audit lỗi thì rollback nghiệp vụ. Technical Log lỗi thì app vẫn lưu nghiệp vụ khi Room/Audit ghi được |
| L08–L09 · Báo cáo và dung lượng | Xem trước, lưu file đọc được. Mẫu bí mật và dữ liệu cá nhân không xuất hiện. Xoay/dọn log giữ nguyên Audit và dữ liệu |
| L10–L11 · Khôi phục chẩn đoán | Room đã commit nhưng log chưa đủ thì hiển thị kết quả từ receipt. Room không mở được thì đọc log còn có và ghi rõ trạng thái chưa kiểm tra được |

## Nghiệm thu UI trên thiết bị

- Đọc được tổng tiền, ngày, nhãn nháp/chốt ngay khi mở **Hôm nay**. Cuộn được đến nút cuối, nút nổi Gemini tránh che các vùng bấm. Ghi ảnh trước/sau khi chỉnh.
- Mở biểu mẫu tiền và màn chat trên máy 320–360dp và trên Note 20 Ultra khi có. Bật bàn phím, tăng cỡ chữ hệ thống, thử tin nhiều dòng và tên tiếng Việt dài. Kiểm tra nút Lưu/Gửi, nút trong thẻ chat và thao tác Back.
- Dùng TalkBack hoặc công cụ kiểm tra khả năng tiếp cận. Đọc đúng nhãn icon, trạng thái bữa, nút xác nhận, thứ tự bảng và tiêu điểm sau khi mở chat. Kiểm tra tương phản sau khi phối kính và khi tắt kính.
- Rà màn trống, thiếu cấu hình, tải, lỗi, kết nối chậm, ngày đã chốt và bảng dài. Chạy lại ca bị lỗi sau khi sửa. Ghi rõ công cụ, thiết bị và kết quả.

Mỗi lần bàn giao có đường dẫn APK nếu đã build, lệnh đã chạy, ảnh với dữ liệu mẫu khi có, ca đã đạt/chưa đạt và bước tiếp theo trong [HANDOFF.md](HANDOFF.md).
