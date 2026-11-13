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
import vn.bepan.ui.KitchenViewModel
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ConfigurationNativeTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val vm get()=ViewModelProvider(ui.activity)[KitchenViewModel::class.java]
    private fun date(value:String) = ui.onNode(hasSetTextAction() and hasText("Ngày hiệu lực YYYY-MM-DD")).performScrollTo().performTextReplacement(value)
    private fun confirm() {
        ui.onNodeWithText("Xem trước cấu hình").performScrollTo().performClick()
        ui.waitUntil(20000) { vm.state.value.configurationPreview!=null }
        val command=vm.state.value.configurationPreview!!
        val dao=(ui.activity.application as BepApplication).repository.dao
        assertNull(runBlocking { dao.receipt(command.operationId) })
        ui.onNodeWithText("Xác nhận lưu").performClick()
        ui.waitUntil(20000) { !vm.state.value.busy && vm.state.value.configurationPreview==null }
        runBlocking { assertNotNull(dao.receipt(command.operationId)); assertEquals(1,dao.audits(dao.receipt(command.operationId)!!.traceId).size) }
    }
    @Test fun editExistingPersonAndEffectiveRateThroughRealForms() {
        ui.waitUntil(20000) { vm.state.value.snapshot!=null }
        val effective=generateSequence(LocalDate.of(2140,1,1)) { it.plusDays(1) }.first { day ->
            vm.state.value.rates.none { it.effective==day.toString() } && vm.state.value.memberships.none { it.effective==day.toString() }
        }.toString()
        ui.onNode(hasText("Cài đặt") and hasClickAction()).performClick()
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Quản lý người ăn & định mức"))
        ui.onNodeWithText("Quản lý người ăn & định mức").performClick()
        ui.onNodeWithText("Thêm người mới").performClick()
        ui.onNodeWithText("Lê Minh Anh").performScrollTo().performClick()
        date(effective); confirm()
        ui.onNodeWithText("Định mức").performClick()
        date(effective); confirm()
        val repo=(ui.activity.application as BepApplication).repository
        runBlocking {
            assertEquals(10,repo.dao.people().size)
            assertEquals("SOLDIER",repo.dao.memberships().single { it.personId=="demo-02" && it.effective==effective }.group)
            assertTrue(repo.dao.rates().any { it.effective==effective })
        }
    }
}
