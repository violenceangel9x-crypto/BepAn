package vn.bepan

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Enable the installed TalkBack service on the emulator before invoking this test. */
@RunWith(AndroidJUnit4::class)
class TalkBackFinanceTest {
    @Test fun actualTalkBackCanFocusNamedHistoricalMeal() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext.applicationContext as BepApplication
        val automation=instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val manager=app.getSystemService(AccessibilityManager::class.java)
        fun until(check:()->Boolean) {
            val deadline=SystemClock.uptimeMillis()+20000
            while(!check() && SystemClock.uptimeMillis()<deadline) SystemClock.sleep(100)
            assertTrue("Accessibility condition timed out",check())
        }
        until { manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isNotEmpty() && manager.isTouchExplorationEnabled }
        val date=runBlocking { app.repository.dao.closedMeals().first {
            it.personId=="demo-02" && it.meal=="BREAKFAST" && it.group=="SOLDIER" && it.enabled && it.date.endsWith("-01")
        }.date }
        runBlocking { app.settings.edit {
            it[stringPreferencesKey("finance_period")]=date.take(7)
            it[stringPreferencesKey("finance_group")]="SOLDIER"
            it[stringPreferencesKey("finance_filter")]="ALL"
        } }
        app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        fun nodes():List<AccessibilityNodeInfo> {
            val result=mutableListOf<AccessibilityNodeInfo>()
            fun walk(node:AccessibilityNodeInfo?) { if(node==null)return; result.add(node); for(i in 0 until node.childCount) walk(node.getChild(i)) }
            walk(automation.rootInActiveWindow); return result
        }
        until { nodes().any { it.text?.toString()=="Tài chính" } }
        var tab=nodes().first { it.text?.toString()=="Tài chính" }
        while(!tab.isClickable) tab=tab.parent
        assertTrue(tab.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        until { nodes().any { it.isScrollable } }
        fun reveal(predicate:(AccessibilityNodeInfo)->Boolean):AccessibilityNodeInfo {
            repeat(25) {
                nodes().firstOrNull(predicate)?.let { return it }
                nodes().firstOrNull { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                SystemClock.sleep(250)
            }
            val directory=app.getExternalFilesDir("evidence")!!.apply { mkdirs() }
            File(directory,"talkback-failure.txt").writeText(nodes().joinToString("\n") { "${it.text} | ${it.contentDescription} | scroll=${it.isScrollable}" })
            val bitmap=automation.takeScreenshot()
            File(directory,"talkback-failure.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
            error("Accessible finance control not found")
        }
        val balance=reveal { it.text?.toString()?.let { text -> text.startsWith("Đã nộp") && text.contains("Còn thiếu") && text.contains("Dư") }==true }
        val balanceText=balance.text.toString()
        assertTrue(balance.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS))
        until { nodes().any { it.text?.toString()==balanceText && it.isAccessibilityFocused } }
        val button=reveal { it.text?.toString()=="Xem bảng chấm cơm tháng" }
        var clickable=button
        while(!clickable.isClickable) clickable=clickable.parent
        assertTrue(clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        SystemClock.sleep(500)
        val meal=reveal { it.contentDescription?.toString()?.let { label -> label.startsWith("Lê Minh Anh, $date,") && label.contains("Sáng") && label.contains("Có ăn") } == true }
        val label=meal.contentDescription.toString()
        assertTrue(meal.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS))
        until { nodes().any { it.contentDescription?.toString()==label && it.isAccessibilityFocused } }
        val bounds=android.graphics.Rect(); meal.getBoundsInScreen(bounds)
        assertTrue(bounds.height()/app.resources.displayMetrics.density>=47f)
        val directory=app.getExternalFilesDir("evidence")!!.apply { mkdirs() }
        File(directory,"talkback-check.json").writeText(JSONObject().put("spokenServiceEnabled",true).put("touchExplorationEnabled",manager.isTouchExplorationEnabled)
            .put("historicalMealHasPersonDateMealState",true).put("accessibilityFocus",true).put("debtAndSurplusFocus",true).put("fontScale",app.resources.configuration.fontScale)
            .put("screenWidthDp",app.resources.configuration.screenWidthDp).toString(2))
        val bitmap=automation.takeScreenshot()
        File(directory,"finance-talkback-focus.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }
}
