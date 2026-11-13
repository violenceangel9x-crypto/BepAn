package vn.bepan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.bepan.data.*
import vn.bepan.domain.*
import vn.bepan.ui.KitchenViewModel
import java.io.File
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class FinanceNativeTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val app get() = ui.activity.application as BepApplication
    private val repo get() = app.repository
    private val vm get() = ViewModelProvider(ui.activity)[KitchenViewModel::class.java]
    private fun await(check: () -> Boolean) = ui.waitUntil(20000, check)
    private fun list() = ui.onNodeWithTag("finance-list")
    private fun click(text: String) {
        list().performScrollToNode(hasText(text) and hasClickAction())
        ui.onNode(hasText(text) and hasClickAction()).performScrollTo().performClick()
    }
    private fun fill(label: String, value: String) {
        ui.onNode(hasSetTextAction() and hasText(label)).performScrollTo().performTextReplacement(value)
    }
    private fun capture(name: String) {
        ui.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(ui.activity.getExternalFilesDir("evidence"),name).apply { parentFile!!.mkdirs() }.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
        bitmap.recycle()
    }
    private fun confirm(): FinanceCommand {
        await { vm.state.value.financePreview != null && !vm.state.value.busy }
        val command = vm.state.value.financePreview!!
        assertNull(runBlocking { repo.dao.receipt(command.operationId) })
        ui.onNodeWithText("Xác nhận tài chính").performClick()
        await { !vm.state.value.busy && vm.state.value.financePreview == null }
        runBlocking {
            assertNotNull(repo.dao.receipt(command.operationId))
            assertEquals(1,repo.dao.audits(repo.dao.receipt(command.operationId)!!.traceId).count { it.operationId == command.operationId })
        }
        return command
    }
    private fun send(text: String) {
        val ids = vm.state.value.chat.map { it.id }.toSet()
        ui.onNode(hasSetTextAction()).performTextReplacement(text)
        ui.onNodeWithContentDescription("Gửi tin nhắn").performClick()
        await { !vm.state.value.busy && vm.state.value.chat.any { it.role == "ASSISTANT" && it.id !in ids } }
    }

    @Test fun manualAndAssistantShareOneAuditedFinancialLedger() {
        await { vm.state.value.snapshot != null && vm.state.value.credentialsReady }
        val originalMock = vm.state.value.mock
        ui.runOnIdle { vm.setMock(true) }
        await { vm.state.value.mock }
        val period = runBlocking {
            generateSequence(YearMonth.of(2040,2)) { it.plusYears(4) }.first {
                repo.dao.closedMeals().none { row -> row.date.startsWith(it.toString()) }
            }.toString()
        }
        val date = "$period-01"
        runBlocking {
            repo.commit(Command(kind=Kind.ATTENDANCE,date=date,expectedVersion=repo.snapshot(date).day.version,
                personId="demo-00",meal="LUNCH",enabled=false),Context())
            repo.commit(Command(kind=Kind.CLOSE_DAY,date=date,expectedVersion=repo.snapshot(date).day.version),Context())
        }
        ui.runOnIdle { vm.changeFinancePeriod(period); vm.financeFilters("SOLDIER","ALL") }
        await { vm.state.value.ledger?.period == period && vm.state.value.financeGroup == "SOLDIER" }
        ui.onNode(hasText("Tài chính") and hasClickAction()).performClick()
        click("Thu tiền nhóm · chọn người")
        fill("Tổng tiền thu nguyên đồng","1000001")
        ui.onNodeWithText("Xem trước phân bổ").performClick()
        await { vm.state.value.financePreview != null }
        capture("finance-group-preview.png")
        val collection=confirm()
        runBlocking {
            assertEquals(1000001L,repo.dao.allocations(period).filter { it.entryId == collection.operationId }.sumOf { it.amount })
            assertEquals(8,repo.dao.allocations(period).count { it.entryId == collection.operationId })
        }
        clickDescription("Chờ hoàn cho Lê Minh Anh")
        fill("Số tiền quyết toán nguyên đồng","10000")
        ui.onNodeWithText("Xem trước quyết toán").performClick()
        val reservation=confirm()
        click("Ghi nhận đã trả · ${reservation.operationId.take(8)}")
        fill("Số tiền quyết toán nguyên đồng","4000")
        ui.onNodeWithText("Xem trước quyết toán").performClick(); confirm()
        await { vm.state.value.ledger?.person("demo-02")?.reserved == 6000L }
        click("Hủy chờ hoàn · ${reservation.operationId.take(8)}"); confirm()
        clickDescription("Cấn cho Lê Minh Anh")
        fill("Số tiền quyết toán nguyên đồng","5000")
        ui.onNodeWithText("Xem trước quyết toán").performClick(); confirm()
        click("Ghi nhận bù gạo đã nhận")
        fill("Tiền bù gạo thực nhận nguyên đồng","1000")
        ui.onNodeWithText("Xem trước nhận bù gạo").performClick(); confirm()
        await { vm.state.value.ledger?.receivedSubsidy == 1000L }
        runBlocking {
            val month=FinanceRepository(repo.db,app.log).month(period)
            assertEquals(691200L,month.charge); assertEquals(3808L,month.subsidy)
            assertEquals(44001L,month.person("demo-02")!!.balance)
            assertEquals(0L,month.person("demo-02")!!.reserved)
            assertEquals(5000L,FinanceRepository(repo.db,app.log).month(YearMonth.parse(period).plusMonths(1).toString()).person("demo-02")!!.carryIn)
        }
        click("Xem bảng chấm cơm tháng")
        list().performScrollToNode(hasTestTag("finance-table-header"))
        list().performScrollToNode(hasTestTag("finance-row-demo-02"))
        val name=ui.onNode(hasText("Lê Minh Anh") and hasClickAction())
        val left=name.fetchSemanticsNode().boundsInRoot.left
        ui.onNodeWithTag("finance-row-demo-02").performTouchInput { swipeLeft() }
        assertEquals(left,name.fetchSemanticsNode().boundsInRoot.left,1f)
        name.assertIsDisplayed()
        capture("finance-pinned-name.png")
        ui.onNodeWithContentDescription("Mở Gemini").performClick()
        send("Sổ tháng đang xem")
        val read=vm.state.value.chat.last { it.role == "USER" }
        assertEquals(period,read.selectedPeriod); assertEquals("FINANCE",read.route)
        send("Thu tiền 100000 của Nam")
        ui.onNodeWithText("Nguyễn Văn Nam · Cán bộ").performScrollTo().performClick()
        await { vm.state.value.actions.any { it.status == "PENDING" && it.command.contains("COLLECTION") } }
        val action=vm.state.value.actions.last { it.status == "PENDING" && it.command.contains("COLLECTION") }
        assertNull(runBlocking { repo.dao.receipt(action.operationId) })
        ui.onNodeWithText("Xác nhận lưu").performScrollTo().performClick()
        await { runBlocking { repo.dao.receipt(action.operationId) != null } }
        ui.runOnIdle { vm.retry(action.traceId) }
        await { !vm.state.value.busy }
        runBlocking {
            assertEquals(1,repo.dao.cashEntries().count { it.id == action.operationId })
            assertEquals(1,repo.dao.audits(action.traceId).count { it.operationId == action.operationId })
        }
        capture("finance-assistant-saved.png")
        ui.runOnIdle { vm.changeFinancePeriod(YearMonth.parse(period).plusMonths(1).toString()) }
        await { vm.state.value.financePeriod != period }
        val prior=vm.state.value.chat.map { it.id }.toSet()
        ui.runOnIdle { vm.retry(read.traceId) }
        await { !vm.state.value.busy && vm.state.value.chat.any { it.role == "ASSISTANT" && it.id !in prior } }
        assertTrue(vm.state.value.chat.last { it.role == "ASSISTANT" }.text.contains(period))
        ui.activityRule.scenario.recreate()
        await { vm.state.value.ledger != null }
        assertEquals(YearMonth.parse(period).plusMonths(1).toString(),vm.state.value.financePeriod)
        ui.runOnIdle { vm.setMock(originalMock) }
    }
    private fun clickDescription(description: String) {
        list().performScrollToNode(hasContentDescription(description))
        ui.onNodeWithContentDescription(description).performScrollTo().performClick()
    }
}
