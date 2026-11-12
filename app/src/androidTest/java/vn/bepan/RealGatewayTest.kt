package vn.bepan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import vn.bepan.data.ChatMessage
import vn.bepan.domain.*
import vn.bepan.ui.KitchenViewModel

/** Explicit opt-in: requires a live loopback gateway and adb reverse. Uses only the demo app. */
@RunWith(AndroidJUnit4::class)
class RealGatewayTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val vm get() = ViewModelProvider(ui.activity)[KitchenViewModel::class.java]
    private val repo get() = (ui.activity.application as BepApplication).repository
    private fun outputDirectory(): java.io.File {
        val path = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        return (path?.let { java.io.File(it) } ?: ui.activity.getExternalFilesDir("evidence")!!).apply { mkdirs() }
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(outputDirectory(), name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun send(text: String): ChatMessage {
        val before = vm.state.value.chat.map { it.id }.toSet()
        ui.onNode(hasSetTextAction()).performTextClearance()
        ui.onNode(hasSetTextAction()).performTextInput(text)
        ui.onNodeWithContentDescription("Gửi tin nhắn").performClick()
        ui.waitUntil(45000) { !vm.state.value.busy && vm.state.value.chat.any { it.role == "ASSISTANT" && it.id !in before } }
        var reply = vm.state.value.chat.last { it.role == "ASSISTANT" && it.id !in before }
        if (reply.error in setOf("GATEWAY_TIMEOUT", "GATEWAY_UNAVAILABLE", "RATE_LIMITED")) {
            val trace = reply.traceId
            val report = runBlocking { vn.bepan.diagnostics.DebugReport.render(trace, vm.log.snapshot(), repo.evidence(trace), android.os.Build.VERSION.SDK_INT, vm.log.incomplete) }
            java.io.File(outputDirectory(), "gemini-transient-${reply.id}.md").writeText(report, Charsets.UTF_8)
            assertEquals(0, runBlocking { repo.evidence(trace).receipts })
            val failedId = reply.id
            ui.onAllNodesWithText("Thử lại lượt này").onLast().performScrollTo().performClick()
            ui.waitUntil(45000) { !vm.state.value.busy && vm.state.value.chat.any { it.role == "ASSISTANT" && it.traceId == trace && it.id != failedId } }
            reply = vm.state.value.chat.last { it.role == "ASSISTANT" && it.traceId == trace }
            assertEquals(2, reply.attempt)
        }
        return reply.also {
            assertEquals("GEMINI", it.provider)
            assertEquals("Provider must return a successful interpretation", "", it.error)
        }
    }
    @Test fun fourRealCommandsUseSharedLocalHandlersAndSaveOnce() {
        Assume.assumeTrue("Pass -Pandroid.testInstrumentationRunnerArguments.realGateway=true to call the live provider", InstrumentationRegistry.getArguments().getString("realGateway") == "true")
        assertTrue(BuildConfig.DEBUG)
        ui.waitUntil(15000) { runBlocking { runCatching { repo.dao.people().size == 10 }.getOrDefault(false) } }
        val previous = vm.state.value
        val date = java.time.LocalDate.of(2042, 1, 1).plusDays((System.nanoTime() % 2000).coerceAtLeast(0)).toString()
        try {
            ui.runOnIdle { vm.configure(false, "http://127.0.0.1:8787"); vm.changeDate(date) }
            ui.waitUntil(10000) { !vm.state.value.mock && vm.state.value.snapshot?.day?.date == date }
            ui.onNodeWithContentDescription("Mở Gemini").performClick()
            val counts = send("Ngày đang xem có bao nhiêu suất trưa?")
            assertTrue(counts.text.contains("Sáng: 10 suất"))
            ui.waitUntil(10000) { ui.onAllNodesWithText(counts.text).fetchSemanticsNodes().isNotEmpty() }
            capture("gemini-counts.png")

            send("Cắt bữa tối ngày đang xem cho Nam")
            ui.waitUntil(10000) { vm.state.value.choice?.people?.size == 2 }
            ui.onNodeWithText("Nguyễn Văn Nam · Cán bộ").performClick()
            ui.waitUntil(10000) { runBlocking { repo.snapshot(date).count(Meal.DINNER) == 9 } }
            ui.waitUntil(10000) { vm.state.value.actions.any { it.status == "SAVED" && vn.bepan.assistant.CommandCodec.decode(it.command).date == date } }
            val cut = vm.state.value.actions.last { vn.bepan.assistant.CommandCodec.decode(it.command).date == date }
            assertEquals(1, runBlocking { repo.evidence(cut.traceId).receipts })
            ui.onNodeWithText("Hoàn tác").performScrollTo()
            capture("gemini-cut.png")
            ui.onNodeWithText("Hoàn tác").performClick()
            ui.waitUntil(10000) { runBlocking { repo.snapshot(date).count(Meal.DINNER) == 10 } }

            val purchaseReply = send("Ghi tiền mua rau ngày đang xem 85 nghìn")
            ui.waitUntil(10000) { vm.state.value.actions.any { it.operationId == purchaseReply.operationId } }
            val purchase = vm.state.value.actions.single { it.operationId == purchaseReply.operationId }
            assertEquals("PENDING", purchase.status)
            assertNull(runBlocking { repo.dao.receipt(purchase.operationId) })
            assertEquals(0L, runBlocking { repo.snapshot(date).bought })
            ui.onNodeWithText("Xác nhận lưu").performScrollTo().performClick()
            ui.waitUntil(10000) { runBlocking { repo.snapshot(date).bought == 85000L } }
            ui.runOnIdle { vm.confirm(purchase) }
            ui.waitUntil(10000) { !vm.state.value.busy }
            assertEquals(85000L, runBlocking { repo.snapshot(date).bought })
            assertEquals(1, runBlocking { repo.evidence(purchase.traceId).receipts })
            assertEquals(1, runBlocking { repo.evidence(purchase.traceId).audits })
            capture("gemini-purchase-saved.png")

            val closeReply = send("Chốt ngày đang xem và chuẩn bị ngày kế tiếp")
            assertFalse(runBlocking { repo.snapshot(date).day.closed })
            ui.onNodeWithText("Xác nhận lưu").performScrollTo().performClick()
            ui.waitUntil(10000) { runBlocking { repo.snapshot(date).day.closed } }
            assertEquals(1, runBlocking { repo.evidence(closeReply.traceId).receipts })
            assertEquals(1, runBlocking { repo.dao.revisions(date).size })
            assertNotNull(runBlocking { repo.dao.day(java.time.LocalDate.parse(date).plusDays(1).toString()) })
            capture("gemini-close-saved.png")
            java.io.File(outputDirectory(), "gemini-result.txt").writeText("Provider: GEMINI\nCounts: PASS\nDuplicate name, cut and undo: PASS\nPurchase preview, confirm and receipt replay: PASS\nClose preview and next draft: PASS\n", Charsets.UTF_8)
        } finally {
            capture("gemini-final-state.png")
            ui.runOnIdle { vm.configure(previous.mock, previous.endpoint) }
            ui.waitUntil(10000) { vm.state.value.mock == previous.mock && vm.state.value.endpoint == previous.endpoint }
        }
    }
}
