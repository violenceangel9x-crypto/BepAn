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
import vn.bepan.assistant.ApiKeyStore
import vn.bepan.assistant.GeminiConnection
import vn.bepan.ui.KitchenViewModel

@RunWith(AndroidJUnit4::class)
class ApiKeySettingsTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val app get() = ui.activity.application as BepApplication
    private val vm get() = ViewModelProvider(ui.activity)[KitchenViewModel::class.java]
    private fun capture(name: String) {
        val target = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let { java.io.File(it) } ?: ui.activity.getExternalFilesDir("evidence")!!
        target.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(target, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun saveReopenAndDeleteADeviceKeyWithoutExposingIt() {
        ui.waitUntil(15000) { vm.state.value.credentialsReady && vm.state.value.snapshot != null }
        Assume.assumeTrue("Never replace an existing user credential in a test", runBlocking { app.apiKeyStore.read() == null })
        val previous = vm.state.value
        val synthetic = "TEST_ONLY_CREDENTIAL_NOT_A_REAL_KEY"
        try {
            ui.onAllNodesWithText("Cài đặt").onLast().performClick()
            val field = ui.onNode(hasSetTextAction() and hasText("API key Gemini"))
            field.performTextInput(synthetic)
            ui.onNodeWithContentDescription("Hiện API key").performClick()
            ui.onNodeWithContentDescription("Ẩn API key").performClick()
            ui.onNodeWithText("Lưu API key").performScrollTo().performClick()
            ui.waitUntil(10000) { vm.state.value.hasApiKey && !vm.state.value.mock && !vm.state.value.credentialBusy && vm.state.value.connection == GeminiConnection.DIRECT }
            assertEquals(synthetic, runBlocking { ApiKeyStore(ui.activity).read() })
            val encrypted = java.io.File(ui.activity.noBackupFilesDir, "credentials/gemini.bin").readBytes().toString(Charsets.ISO_8859_1)
            assertFalse(encrypted.contains(synthetic))
            ui.onAllNodesWithText(synthetic).assertCountEquals(0)
            capture("api-key-saved.png")
            ui.activityRule.scenario.recreate()
            ui.waitUntil(15000) { vm.state.value.credentialsReady && vm.state.value.hasApiKey }
            ui.onAllNodesWithText(synthetic).assertCountEquals(0)
            ui.onNodeWithText("Xóa API key").performScrollTo().performClick()
            ui.waitUntil(10000) { !vm.state.value.hasApiKey && !vm.state.value.credentialBusy }
            assertNull(runBlocking { ApiKeyStore(ui.activity).read() })
            assertFalse(java.io.File(ui.activity.noBackupFilesDir, "credentials/gemini.bin").exists())
            assertFalse(runBlocking { app.log.snapshot() }.any { it.json().toString().contains(synthetic) })
            capture("api-key-deleted.png")
            ui.onNodeWithContentDescription("Mở Gemini").performClick()
            ui.onNode(hasSetTextAction()).performTextInput("Hôm nay có bao nhiêu suất trưa?")
            ui.onNodeWithContentDescription("Gửi tin nhắn").performClick()
            ui.waitUntil(10000) { vm.state.value.chat.lastOrNull()?.error == "API_KEY_MISSING" }
            capture("api-key-missing-chat.png")
        } finally {
            runBlocking { app.apiKeyStore.clear() }
            ui.runOnIdle { vm.configure(previous.mock, previous.endpoint); vm.setConnection(previous.connection) }
            ui.waitUntil(10000) { vm.state.value.mock == previous.mock && vm.state.value.connection == previous.connection }
        }
    }
}
