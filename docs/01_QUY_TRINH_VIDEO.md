# Chuyển quy trình trong video sang Codex và Android

Video [Dạy AI làm giao diện đẹp hơn](../references/video-quy-trinh-ui-ux.mp4) dài khoảng 2 phút 59 giây. Video dùng Antigravity với dự án web để minh họa. Áp dụng cùng thứ tự làm việc cho Kotlin và Compose như bảng dưới đây. Các mốc là điểm gần đoạn tương ứng trong video.

| Mốc | Video thực hiện | Codex thực hiện trong app Bếp ăn |
|---|---|---|
| 00:18 | Mở đúng thư mục dự án trong Antigravity | Mở gốc repo Android trong VS Code, dùng Codex chat |
| 00:36 | Cài Impeccable | Cài cho Codex ở scope dự án nếu môi trường có Node/npm |
| 00:51 | Chạy init và ghi bối cảnh vào `PRODUCT.md` | Dùng `PRODUCT.md` đã chuẩn bị, bổ sung câu hỏi thật sự còn thiếu |
| 01:12 | Cài UI/UX Pro Max | Tra hướng mobile Android, rồi chốt token và component trong `DESIGN.md` |
| 01:27 | Giải thích các thư mục công cụ | Dùng cấu trúc do installer cho Codex tạo. Giữ các file bối cảnh của dự án ở gốc |
| 01:42 | Tạo `AGENTS.md` để phối hợp hai công cụ | Dùng `AGENTS.md` đi kèm. Trong repo cũ, hợp nhất với chỉ dẫn hiện có |
| 01:57 | Giao prompt, AI tra thiết kế rồi dựng UI | Dựng Compose và dữ liệu thật cho luồng Hôm nay → Gemini → cập nhật Chấm cơm |
| 02:15 | Rà giao diện chạy, video cho thấy 9 vấn đề | Rà ảnh Compose trên thiết bị, xác định lỗi chữ, tương phản, lồng thẻ, cuộn và cử chỉ |
| 02:27 | Dùng polish, rà lại trên màn nhỏ | Sửa theo mức ảnh hưởng, chụp lại các trạng thái và ghi kết quả vào HANDOFF |
| 02:54 | Nhắc lại các bước | Lặp quy trình trên Tài chính, Ghi chú và Cài đặt sau khi chốt hệ thiết kế |

## Cài công cụ cho Codex

Chạy các lệnh này trong PowerShell tại gốc repo. Nếu dự án đã có bộ công cụ tương ứng, dùng bản đã cài và kiểm tra lệnh theo tài liệu của bản đó. Hai công cụ phục vụ thiết kế trong VS Code. APK chỉ cần phụ thuộc Android của dự án.

```powershell
node --version
npm --version
npx impeccable install
npx ui-ux-pro-max-cli init --ai codex
```

Ở installer Impeccable, chọn **Codex** và phạm vi **project**, rồi tải lại VS Code hoặc mở phiên Codex mới. Với UI/UX Pro Max, kiểm tra tài liệu và file `SKILL.md` ở thư mục do installer báo. Nếu phiên bản đó dùng Python để tra dữ liệu, kiểm tra `py -3 --version` trên Windows và dùng đường dẫn script thật mà skill đã cài cung cấp. Nếu phiên bản Codex IDE chưa nhận công cụ, vẫn làm được theo các file thiết kế và checklist trong bộ này. [Cài Impeccable](https://impeccable.style/tutorials/getting-started/) · [UI/UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) · [Agent Skills của Codex](https://developers.openai.com/codex/skills)

## Lời nhắn cho từng lượt thiết kế

1. **Khởi tạo bối cảnh.** Trong chat Codex, dùng `$impeccable init` nếu công cụ xuất hiện. Sau đó yêu cầu đọc `PRODUCT.md`, `DESIGN.md`, `AGENTS.md` và [đặc tả màn hình](03_MAN_HINH_VA_TUONG_TAC.md). Rà câu hỏi còn thiếu và giữ những yêu cầu đã chốt. Init tập trung vào bối cảnh sản phẩm. `DESIGN.md` trong bộ này ghi hệ thiết kế ban đầu. [Impeccable init](https://impeccable.style/docs/init/)
2. **Tra hệ thiết kế.** Chọn UI/UX Pro Max theo tên tool hiện trong Codex. Dùng truy vấn sau và yêu cầu lưu điều áp dụng được vào `DESIGN.md`.

   > Đề xuất hệ thiết kế cho app quản lý bếp ăn Android Kotlin/Jetpack Compose bằng tiếng Việt, dùng một tay. Dùng bảng màu SaveSmart và Glassmorphism có tương phản cao. Ưu tiên số tiền đi chợ, thẻ thực đơn bốn hàng, bảng chấm cơm và chat Gemini. Ghi rõ đề xuất nào giữ, đề xuất nào sửa và component nào sẽ dùng chung.

   Nếu bản cài cung cấp `scripts/search.py`, có thể chạy trực tiếp từ gốc repo. Đặt `$BepDesignSearch` thành đường dẫn script thực tế, rồi chạy lệnh sau. So sánh đề xuất sinh ra với `DESIGN.md` và đưa quyết định đã chọn vào file đó.

   ```powershell
   py -3 $BepDesignSearch 'Android Jetpack Compose kitchen dashboard glassmorphism Vietnamese' --design-system --persist -p 'Bep An'
   ```

3. **Dựng một màn có thao tác thật.** Dùng [PROMPT_CODEX.md](../PROMPT_CODEX.md). Tạo theme, thẻ ngân sách và thực đơn, chấm bữa, chat, rồi kết nối hành động chung qua Room. Chụp màn Hôm nay và chat ở trạng thái bình thường, đang xử lý, có thẻ xác nhận, lỗi và khi bàn phím đang mở.
4. **Nhận xét và rà lỗi.** Trong chat Codex, dùng `$impeccable critique` cho phân cấp và thao tác. Dùng `$impeccable audit` cho chất lượng triển khai. Kèm ảnh Android và đường dẫn Composable. Lưu mỗi vấn đề gồm màn hình, cách tái hiện, ảnh hoặc số đo và cách kiểm chứng sau sửa. [Critique](https://impeccable.style/docs/critique/) · [Audit](https://impeccable.style/docs/audit/)
5. **Tinh chỉnh.** Dùng `$impeccable polish` với danh sách lỗi và yêu cầu sửa trực tiếp Compose. Chạy lại ca tái hiện, chụp cùng trạng thái và ghi kết quả. Đo cỡ vùng bấm, tương phản sau ghép alpha, chiều cao khi chữ lớn và vị trí composer khi mở IME. [Polish](https://impeccable.style/docs/polish/)

Video quét trang web đang chạy để phát hiện chữ xám khó đọc, thẻ lồng, nhấp nháy và xuống dòng kém. Trên Android, đối chiếu lỗi tương ứng bằng Compose Preview, Android Emulator/Note 20 Ultra, TalkBack và thao tác trực tiếp. Kết quả quét trang HTML mẫu chỉ phản ánh HTML. Tiêu chí ứng dụng thật nằm ở [07_LO_TRINH_VA_NGHIEM_THU.md](07_LO_TRINH_VA_NGHIEM_THU.md).
