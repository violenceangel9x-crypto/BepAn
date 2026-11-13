package vn.bepan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import vn.bepan.domain.*

@RunWith(AndroidJUnit4::class)
class NativeFlowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val repo get() = (ui.activity.application as BepApplication).repository
    private var originalMock = true
    @Before fun useLocalMockForUiAcceptance() {
        val vm=androidx.lifecycle.ViewModelProvider(ui.activity)[vn.bepan.ui.KitchenViewModel::class.java]
        ui.waitUntil(15000) { vm.state.value.snapshot!=null && vm.state.value.credentialsReady }
        originalMock=vm.state.value.mock
        ui.runOnIdle { vm.setMock(true) }; ui.waitUntil(10000) { vm.state.value.mock }
    }
    @After fun restoreProviderPreference() {
        val vm=androidx.lifecycle.ViewModelProvider(ui.activity)[vn.bepan.ui.KitchenViewModel::class.java]
        ui.runOnIdle { vm.setMock(originalMock) }; ui.waitUntil(10000) { vm.state.value.mock==originalMock }
    }
    private fun outputDirectory(): java.io.File {
        val path = androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        return (path?.let { java.io.File(it) } ?: ui.activity.getExternalFilesDir("evidence")!!).apply { mkdirs() }
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        val bitmap = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val directory = outputDirectory()
        java.io.File(directory,name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
    private fun send(text: String) {
        val vm = androidx.lifecycle.ViewModelProvider(ui.activity)[vn.bepan.ui.KitchenViewModel::class.java]
        val previousIds = vm.state.value.chat.map { it.id }.toSet()
        ui.onNode(hasSetTextAction()).performTextReplacement(text)
        ui.onNodeWithContentDescription("Gửi tin nhắn").assertIsEnabled()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        ui.waitUntil(15000) { !vm.state.value.busy && vm.state.value.chat.any { it.role == "ASSISTANT" && it.id !in previousIds } }
    }
    @Test fun todayChatDuplicateNameUndoTimeoutAndReport() {
        ui.waitUntil(15000) { runBlocking { runCatching { repo.dao.people().size == 10 }.getOrDefault(false) } }
        val date = today()
        val before = runBlocking { repo.snapshot(date).count(Meal.DINNER) }
        ui.onNodeWithContentDescription("Mở Gemini").performClick()
        send("Hôm nay có bao nhiêu suất trưa?")
        try {
            ui.waitUntil(10000) { ui.onAllNodesWithText("Sáng:", substring=true).fetchSemanticsNodes().isNotEmpty() }
        } finally { capture("chat-read-state.png") }
        send("Cắt bữa tối hôm nay cho Nam")
        ui.waitUntil(10000) { ui.onAllNodesWithText("Nguyễn Văn Nam · Cán bộ").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Nguyễn Văn Nam · Cán bộ").performClick()
        ui.waitUntil(10000) { runBlocking { repo.snapshot(date).count(Meal.DINNER) == before-1 } }
        ui.onNodeWithText("Hoàn tác").performScrollTo()
        capture("chat-cut.png")
        ui.onNodeWithText("Hoàn tác").performClick()
        ui.waitUntil(10000) { runBlocking { repo.snapshot(date).count(Meal.DINNER) == before } }
        send("timeout")
        ui.waitUntil(10000) { ui.onAllNodesWithText("Kết nối Gemini quá thời gian. Bản nháp được giữ để gửi lại.").fetchSemanticsNodes().isNotEmpty() }
        ui.onAllNodesWithText("Xem chẩn đoán").onLast().performScrollTo().performClick()
        ui.onNodeWithText("AI Debug Center").assertIsDisplayed()
        capture("diagnostics-timeout.png")
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Xuất báo cáo lỗi · xem trước"))
        ui.onNodeWithText("Xuất báo cáo lỗi · xem trước").performScrollTo().performClick()
        ui.onNodeWithText("Xem trước báo cáo đã lọc").assertIsDisplayed()
        ui.onNodeWithText("Sao chép nội dung đã xem").performScrollTo().performClick()
        val clipboard = ui.activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val report = clipboard.primaryClip!!.getItemAt(0).text.toString()
        assertTrue(report.contains("GATEWAY_TIMEOUT")); assertTrue(report.contains("Room: NOT_COMMITTED")); assertFalse(report.contains("Nguyễn Văn Nam"))
        java.io.File(outputDirectory(),"timeout-report.md").writeText(report,Charsets.UTF_8)
        capture("report-preview.png")
    }
    @Test fun manualMealLongMenuPurchaseCloseAndAdjustment() {
        ui.waitUntil(15000) { runBlocking { runCatching { repo.dao.people().size == 10 }.getOrDefault(false) } }
        val vm = androidx.lifecycle.ViewModelProvider(ui.activity)[vn.bepan.ui.KitchenViewModel::class.java]
        val date = runBlocking { generateSequence(java.time.LocalDate.of(2040,1,1)) { it.plusDays(1) }
            .first { repo.dao.day(it.toString())==null }.toString() }
        ui.runOnIdle { vm.changeDate(date) }
        ui.waitUntil(10000) { vm.state.value.snapshot?.day?.date == date }
        val list = ui.onNode(hasScrollToIndexAction())
        list.performScrollToNode(hasContentDescription("Nguyễn Văn Nam, Trưa"))
        ui.onNodeWithContentDescription("Nguyễn Văn Nam, Trưa").performClick()
        ui.waitUntil(10000) { vm.state.value.snapshot?.count(Meal.LUNCH) == 9 }
        ui.onNodeWithContentDescription("Nguyễn Văn Nam, Trưa").performClick()
        ui.waitUntil(10000) { vm.state.value.snapshot?.count(Meal.LUNCH) == 10 }
        // Seed a long saved menu, then use the real offscreen image/clipboard button.
        val next = java.time.LocalDate.parse(date).plusDays(1).toString()
        runBlocking {
            repo.commit(Command(kind=Kind.MENU,date=next,expectedVersion=repo.snapshot(next).day.version,meal="BREAKFAST",text=(1..45).joinToString("\n") { "Món sáng ngày kế tiếp $it" }),Context())
        }
        ui.waitUntil(10000) { vm.state.value.snapshot?.menus?.any { it.date == next } == true }
        list.performScrollToNode(hasText("Sao chép ảnh thực đơn"))
        ui.onNodeWithText("Sao chép ảnh thực đơn").performScrollTo().assertIsDisplayed().performClick()
        val clipboard=ui.activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        try { ui.waitUntil(15000) { clipboard.primaryClip?.getItemAt(0)?.uri != null } } finally { capture("menu-copy-state.png") }
        val uri=clipboard.primaryClip!!.getItemAt(0).uri
        val png=ui.activity.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val bitmap=android.graphics.BitmapFactory.decodeByteArray(png,0,png.size)
        java.io.File(outputDirectory(),"menu-long.png").writeBytes(png)
        assertEquals(1080,bitmap.width); assertTrue("PNG height=${bitmap.height}",bitmap.height>2400); bitmap.recycle()
        list.performScrollToNode(hasText("Ghi tiền mua"))
        ui.onNodeWithText("Ghi tiền mua").performClick()
        ui.onNode(hasSetTextAction() and hasText("Nội dung")).performTextInput("Mua rau thử")
        ui.onNode(hasSetTextAction() and hasText("Số tiền nguyên đồng")).performTextInput("85000")
        ui.onNodeWithText("Xem trước").performClick()
        ui.waitUntil(10000) { vm.state.value.manualPreview!=null }
        capture("purchase-preview.png")
        ui.onNodeWithText("Xác nhận lưu").performClick()
        ui.waitUntil(10000) { vm.state.value.manualPreview==null && vm.state.value.snapshot?.bought==85000L }
        list.performScrollToNode(hasText("Chốt sổ và chuẩn bị ngày mai"))
        ui.onNodeWithText("Chốt sổ và chuẩn bị ngày mai").performClick()
        ui.waitUntil(10000) { vm.state.value.manualPreview!=null }
        ui.onNodeWithText("Xác nhận lưu").performClick()
        ui.waitUntil(10000) { runBlocking { repo.dao.day(date)?.closed == true } && vm.state.value.date == next }
        ui.runOnIdle { vm.changeDate(date) }
        ui.waitUntil(10000) { vm.state.value.snapshot?.day?.date == date }
        list.performScrollToNode(hasContentDescription("Nguyễn Văn Nam, Trưa"))
        ui.onNodeWithContentDescription("Nguyễn Văn Nam, Trưa").performClick()
        ui.onNode(hasSetTextAction() and hasText("Lý do điều chỉnh")).performTextInput("Đối soát dữ liệu mẫu")
        ui.onNodeWithText("Xem trước chênh lệch").performClick()
        ui.waitUntil(10000) { vm.state.value.manualPreview!=null }
        ui.onNodeWithText("Xác nhận lưu").performClick()
        ui.waitUntil(10000) { runBlocking { repo.dao.revisions(date).size == 2 } }
        assertEquals(9,runBlocking { repo.snapshot(date).count(Meal.LUNCH) })
        assertEquals(85000L,runBlocking { repo.snapshot(date).bought })
        assertEquals(691200L,runBlocking { repo.snapshot(date).charge })
        capture("closed-day-adjusted.png")
    }
    @Test fun permissionDeniedStillAllowsTextCommands() {
        Assume.assumeTrue("Preserve an already granted microphone permission", androidx.core.content.ContextCompat.checkSelfPermission(ui.activity,
            android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
        ui.waitUntil(15000) { runBlocking { runCatching { repo.dao.people().size == 10 }.getOrDefault(false) } }
        ui.onNodeWithContentDescription("Mở Gemini").performClick()
        ui.onNodeWithContentDescription("Nhập bằng giọng nói tiếng Việt").performClick()
        val device = androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        val deny = device.findObject(androidx.test.uiautomator.UiSelector().resourceIdMatches(".*:id/permission_deny.*_button"))
        if (deny.waitForExists(5000)) deny.click()
        try {
            ui.waitUntil(10000) { runCatching { ui.onAllNodesWithText("Chưa cấp quyền micro. Nhập chữ vẫn dùng được.").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        } finally {
            capture("micro-permission-state.png")
            device.dumpWindowHierarchy(java.io.File(outputDirectory(),"micro-permission-state.xml"))
        }
        send("Hôm nay có bao nhiêu suất trưa?")
        ui.onNodeWithText("Sáng:", substring=true).assertExists()
        capture("micro-denied-text-works.png")
    }
    @Test fun chatKeepsReadersPositionWhenAnAssistantReplyArrives() {
        ui.waitUntil(15000) { runBlocking { runCatching { repo.dao.people().size == 10 }.getOrDefault(false) } }
        val vm = androidx.lifecycle.ViewModelProvider(ui.activity)[vn.bepan.ui.KitchenViewModel::class.java]
        val messages = (1..12).map { index -> vn.bepan.data.ChatMessage(id(), java.time.Instant.now().toString(), "ASSISTANT", "Tin mẫu đọc lại $index\n" + "Nội dung kiểm thử cuộn lịch sử. ".repeat(5), id(), provider = "MOCK") }
        runBlocking { messages.forEach { repo.dao.chat(it) } }
        ui.waitUntil(10000) { vm.state.value.chat.lastOrNull()?.id == messages.last().id }
        ui.onNodeWithContentDescription("Mở Gemini").performClick()
        val list = ui.onNode(hasScrollToIndexAction())
        list.performTouchInput { swipeDown() }
        list.performScrollToIndex(vm.state.value.chat.indexOfFirst { it.id == messages.first().id })
        val firstText = messages.first().text
        ui.onNodeWithText(firstText).assertIsDisplayed()
        val incoming = vn.bepan.data.ChatMessage(id(), java.time.Instant.now().toString(), "ASSISTANT", "Phản hồi mới khi đang đọc lịch sử", id(), provider = "MOCK")
        runBlocking { repo.dao.chat(incoming) }
        ui.waitUntil(10000) { vm.state.value.chat.lastOrNull()?.id == incoming.id }
        ui.onNodeWithText(firstText).assertIsDisplayed()
        ui.onNodeWithText(incoming.text).assertDoesNotExist()
        ui.onNodeWithText("Tin mới nhất").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithText(incoming.text).fetchSemanticsNodes().isNotEmpty() }
        capture("chat-history-kept.png")
    }
}
