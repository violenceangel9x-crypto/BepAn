package vn.bepan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import vn.bepan.ui.KitchenViewModel
import java.io.File

@RunWith(AndroidJUnit4::class)
class FinancePresentationTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val vm get()=ViewModelProvider(ui.activity)[KitchenViewModel::class.java]
    private fun ready()=ui.waitUntil(20000) { vm.state.value.ledger!=null }
    private fun capture(name:String) {
        ui.waitForIdle()
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(ui.activity.getExternalFilesDir("evidence"),name).apply { parentFile!!.mkdirs() }.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }; bitmap.recycle()
    }
    @Test fun largeTextPersonalFormKeepsInputUntilPreviewAndCanCancel() {
        ready()
        ui.runOnIdle { vm.financeFilters("SOLDIER","ALL") }
        ui.onNode(hasText("Tài chính") and hasClickAction()).performClick()
        ui.onNodeWithTag("finance-list").performScrollToNode(hasText("Thu tiền · Lê Minh Anh"))
        ui.onNodeWithText("Thu tiền · Lê Minh Anh").performScrollTo().performClick()
        ui.onNode(hasSetTextAction() and hasText("Tổng tiền thu nguyên đồng")).performScrollTo().performTextInput("54321")
        ui.onNodeWithText("Xem trước phân bổ").assertIsDisplayed().performClick()
        ui.waitUntil(20000) { vm.state.value.financePreview?.amount==54321L }
        val op=vm.state.value.financePreview!!.operationId
        capture("finance-large-preview.png")
        ui.onNodeWithText("Hủy").performClick()
        ui.waitUntil(20000) { vm.state.value.financePreview==null }
        val repo=(ui.activity.application as BepApplication).repository
        assertNull(runBlocking { repo.dao.receipt(op) })
        assertFalse(runBlocking { repo.dao.financeDrafts().any { it.id==op } })
    }
    @Test fun safCancelThenSaveWorkbook() {
        ready()
        ui.runOnIdle { vm.clearError() }
        ui.onNode(hasText("Tài chính") and hasClickAction()).performClick()
        fun export() {
            ui.onNodeWithTag("finance-list").performScrollToNode(hasText("Xuất Excel đối soát tháng"))
            ui.onNodeWithText("Xuất Excel đối soát tháng").performScrollTo().assertIsDisplayed().performClick()
        }
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        fun pickerReady() {
            val found=device.wait(Until.hasObject(By.res("android:id/button1")),10000)
            if(!found) {
                capture("finance-picker-failure.png")
                device.dumpWindowHierarchy(File(ui.activity.getExternalFilesDir("evidence"),"picker-failure.xml"))
            }
            assertTrue(found)
        }
        export()
        pickerReady()
        device.pressBack()
        ui.waitForIdle()
        assertFalse(vm.state.value.notice.startsWith("Đã xuất Excel"))
        export()
        pickerReady()
        val filename="bep-an-native-${System.currentTimeMillis()}.xlsx"
        val field=device.wait(Until.findObject(By.clazz("android.widget.EditText")),10000)!!
        field.text=filename
        val save=device.wait(Until.findObject(By.res("android:id/button1")),10000)!!
        save.click()
        ui.waitUntil(20000) { !vm.state.value.exportingFinance && vm.state.value.notice.startsWith("Đã xuất Excel") }
        // Read the selected file as the test shell; the chosen provider/location may differ by OEM.
        val path=device.executeShellCommand("find /sdcard/ -name $filename").lineSequence().first { it.startsWith("/sdcard/") && it.endsWith(filename) }
        val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cat $path")
        val bytes=android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        val entries=mutableSetOf<String>()
        java.util.zip.ZipInputStream(bytes.inputStream()).use { zip -> while(true) { val entry=zip.nextEntry?:break; entries.add(entry.name) } }
        assertTrue(entries.contains("xl/workbook.xml")); assertTrue(entries.contains("xl/worksheets/sheet1.xml"))
        File(ui.activity.getExternalFilesDir("evidence"),"native-export.xlsx").writeBytes(bytes)
        capture("finance-export-saved.png")
    }
}
