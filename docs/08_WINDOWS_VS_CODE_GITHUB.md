# Làm việc trên Windows, VS Code và Note 20 Ultra

## 1. Chọn thư mục làm việc

**Dự án mới.** Giải nén, dùng **File → Open Folder** ở VS Code, chọn thư mục có `AGENTS.md`. Mở Codex IDE, đăng nhập nếu cần, dán [PROMPT_CODEX.md](../PROMPT_CODEX.md). Codex tạo dự án Android ở đây.

**Đã có repo.** Mở gốc repo có Gradle Wrapper. Đặt nguyên bộ trong `docs/product-kit/` và nhắn Codex “Đọc docs/product-kit/PROMPT_CODEX.md và triển khai theo mốc 0 trên repo hiện có.” Cho Codex hợp nhất chỉ dẫn vào `AGENTS.md`, `PRODUCT.md` và `DESIGN.md` ở gốc. Chỉnh đường dẫn tương đối và tạo `docs/HANDOFF.md` tại repo để tiếp tục qua các phiên. [Codex trong IDE](https://developers.openai.com/codex/ide) · [Chỉ dẫn AGENTS.md](https://developers.openai.com/codex/guides/agents-md)

Có thể cài Android Studio để dùng SDK Manager và emulator, sau đó tiếp tục viết mã trong VS Code. Chuẩn bị JDK phù hợp AGP, Android SDK/platform-tools và Git. Dùng Gradle Wrapper của dự án. Nếu tạo app mới, `minSdk 26` là điểm bắt đầu để dùng API ngày của Java, hoặc theo phiên bản Android thiết bị cần hỗ trợ. Xác nhận compile/target SDK, Kotlin, AGP, Compose và JDK tương thích, ghi phiên bản vào README. Giữ cấu hình của repo cũ khi phù hợp. Node/npm và Python chỉ cần cho các công cụ thiết kế hoặc gateway tùy triển khai.

```powershell
git --version
java -version
adb version
adb devices -l
```

Nếu `adb` chưa có trong PATH, mở SDK Manager lấy vị trí Android SDK rồi gọi `platform-tools\adb.exe` từ đường dẫn đó. Ví dụ với SDK mặc định dưới LocalAppData.

```powershell
$BepAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $BepAdb devices -l
```

## 2. Build và cài thử USB

1. Trên điện thoại, bật **Tùy chọn nhà phát triển → Gỡ lỗi USB**, cắm cáp dữ liệu, mở khóa và cho phép máy Windows. `adb devices -l` cần hiện `device`. Với `unauthorized`, kiểm tra hộp thoại trên máy. Nếu máy chưa hiện, thử cáp/cổng USB khác và kiểm tra driver phù hợp.
2. Chạy Gradle Wrapper với task theo tên module/variant thực tế. Ví dụ dự án chuẩn có module `:app` và variant debug.

   ```powershell
   .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   ```

3. Lấy serial ở `adb devices -l`, kiểm tra đường dẫn APK vừa build, rồi cài đúng thiết bị. Mở app từ launcher để thử. Nếu chạy từ terminal, dùng activity và application ID thực tế trong variant đã build.

   ```powershell
   $BepSerial = 'SERIAL_HIEN_TREN_ADB'
   $BepApk = '.\app\build\outputs\apk\debug\app-debug.apk'
   adb -s $BepSerial install -r $BepApk
   ```

4. Dùng application ID riêng cho debug nếu phù hợp. Nếu thấy lỗi chữ ký, xác định package/keystore trước và chọn variant thử phù hợp để bảo toàn dữ liệu. Chạy test thiết bị khi repo đã có bộ test tương ứng.

   ```powershell
   .\gradlew.bat :app:connectedDebugAndroidTest
   ```

5. Thử theo [bảng nghiệm thu](07_LO_TRINH_VA_NGHIEM_THU.md). Có thể chụp màn hình có dữ liệu mẫu qua USB cho lượt rà UI.

   ```powershell
   adb -s $BepSerial shell screencap -p /sdcard/bep-an-review.png
   adb -s $BepSerial pull /sdcard/bep-an-review.png .\bep-an-review.png
   ```

## 3. Thử Gemini thật trong bản debug

Chạy gateway trên Windows theo README của repo, đặt khóa Gemini trong môi trường của tiến trình gateway và chọn model hỗ trợ Function Calling. Ví dụ gateway lắng nghe tại cổng 8787 trên PC. Từ điện thoại có USB, dùng chuyển tiếp ngược sau.

```powershell
adb -s $BepSerial reverse tcp:8787 tcp:8787
```

Bản debug dùng base URL `http://127.0.0.1:8787` nếu repo đã cho phép HTTP cục bộ đúng phạm vi debug. Bản dùng thật kết nối gateway HTTPS được cấu hình và xác thực. Thử chế độ mock và Gemini thật riêng, ghi kết quả nào đã chạy. [Android Debug Bridge](https://developer.android.com/tools/adb) · [Chạy app trên thiết bị](https://developer.android.com/studio/run/device)

## 4. Tiếp tục trên máy Windows khác

- Khi muốn đồng bộ mã nguồn, dùng repo GitHub riêng và một nhánh triển khai. Giữ mã, tài liệu cần bàn giao, Gradle Wrapper, schema Room, test và dữ liệu mẫu. Trước khi commit, xem `git status` và diff. Giữ khóa API, token, `local.properties`, keystore, database thật, sổ có tên người và ảnh/tệp nghiệp vụ ở ngoài Git.
- Trên máy khác, clone repo hoặc mở bản clone đã có. Chọn đúng nhánh, cập nhật với `git pull --ff-only` khi cây làm việc đã sẵn sàng. Thiết lập JDK, Android SDK, ADB và cấu hình gateway cục bộ của máy đó.
- Giao Codex đọc `AGENTS.md`, `docs/HANDOFF.md` và đặc tả liên quan. Ghi bước tiếp theo ở HANDOFF trước khi rời máy. Việc đồng bộ mã qua GitHub và việc sao lưu Room trên điện thoại là hai thao tác độc lập.

Khi gửi ảnh để Impeccable rà, dùng bản debug có dữ liệu mẫu và kèm tên màn, trạng thái, cỡ chữ, thiết bị và thao tác tái hiện.

Khi cần giao ca lỗi cho GPT hoặc Codex, mở **Cài đặt → Chẩn đoán → AI Debug Center**, chọn lần lỗi rồi **Xuất báo cáo lỗi**. Đính kèm báo cáo đã xem trước và ghi mã theo dõi trong HANDOFF khi sửa trên repo. [Cách xuất báo cáo](10_AI_DEBUG_CENTER_VA_HANDOFF.md)
