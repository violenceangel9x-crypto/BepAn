package vn.bepan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.bepan.data.FinanceRepository
import vn.bepan.domain.Kind
import vn.bepan.ui.KitchenViewModel

/** Run each phase in a separate am instrument invocation, with am force-stop between phases. */
@RunWith(AndroidJUnit4::class)
class FinanceRecoveryTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val app get()=ui.activity.application as BepApplication
    private val vm get()=ViewModelProvider(ui.activity)[KitchenViewModel::class.java]
    private val marker get()=app.getSharedPreferences("finance_recovery_fixture",0)
    private fun ready()=ui.waitUntil(20000) { vm.state.value.ledger!=null }
    @Test fun prepareBeforeProcessDeath() {
        ready()
        ui.runOnIdle { vm.changeFinancePeriod("2092-02"); vm.financeFilters("SOLDIER","EXCESS") }
        ui.waitUntil(20000) { vm.state.value.ledger?.period=="2092-02" }
        // Remove only this test's old pending draft, never unrelated user drafts.
        marker.getString("operation",null)?.let { op -> runBlocking {
            app.repository.dao.financeDrafts().firstOrNull { it.id==op }?.let {
                FinanceRepository(app.repository.db,app.log).cancel(vn.bepan.data.FinanceCommand.decode(it.command))
            }
        } }
        ui.runOnIdle { vm.previewFinance(Kind.COLLECTION,12345,listOf("demo-02")) }
        ui.waitUntil(20000) { vm.state.value.financePreview?.amount==12345L }
        val command=vm.state.value.financePreview!!
        assertTrue(marker.edit().putString("operation",command.operationId).commit())
        assertNull(runBlocking { app.repository.dao.receipt(command.operationId) })
        ui.activityRule.scenario.recreate(); ready()
        ui.waitUntil(20000) { vm.state.value.financePreview?.operationId==command.operationId }
        assertEquals("SOLDIER",vm.state.value.financeGroup); assertEquals("EXCESS",vm.state.value.financeFilter)
    }
    @Test fun restoreAndCommitAfterProcessDeath() {
        ready()
        val op=marker.getString("operation",null)!!
        ui.waitUntil(20000) { vm.state.value.financePreview?.operationId==op }
        assertNull(runBlocking { app.repository.dao.receipt(op) })
        ui.onNode(hasText("Tài chính") and hasClickAction()).performClick()
        ui.onNodeWithText("Xác nhận tài chính").performClick()
        ui.waitUntil(20000) { !vm.state.value.busy && vm.state.value.financePreview==null }
        assertNotNull(runBlocking { app.repository.dao.receipt(op) })
    }
    @Test fun savedReceiptSurvivesSecondProcessDeath() {
        ready()
        val op=marker.getString("operation",null)!!
        assertNull(vm.state.value.financePreview)
        assertEquals("2092-02",vm.state.value.financePeriod)
        runBlocking {
            val dao=app.repository.dao
            val receipt=dao.receipt(op)!!
            assertEquals(1,dao.cashEntries().count { it.id==op })
            assertEquals(12345L,dao.allocations("2092-02").single { it.entryId==op }.amount)
            assertEquals(1,dao.audits(receipt.traceId).count { it.operationId==op })
        }
        ui.runOnIdle { vm.changeFinancePeriod(vn.bepan.domain.today().take(7)); vm.financeFilters("OFFICER","ALL") }
        ui.waitUntil(20000) { vm.state.value.ledger?.period==vn.bepan.domain.today().take(7) && vm.state.value.financeGroup=="OFFICER" }
    }
}
