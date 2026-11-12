package vn.bepan

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.bepan.assistant.DirectGeminiGateway
import vn.bepan.domain.RuleViolation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DirectGeminiGatewayTest {
    private val synthetic = "TEST_ONLY_CREDENTIAL_NOT_A_REAL_KEY"
    private class Response(private val status: Int, private val payload: String) : HttpURLConnection(URL("https://generativelanguage.googleapis.com")) {
        val written = ByteArrayOutputStream()
        override fun getOutputStream() = written
        override fun getInputStream() = ByteArrayInputStream(payload.toByteArray(Charsets.UTF_8))
        override fun getErrorStream() = ByteArrayInputStream(payload.toByteArray(Charsets.UTF_8))
        override fun getResponseCode() = status
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun connect() = Unit
    }
    private fun gateway(response: Response, key: String? = synthetic, opened: (URL) -> Unit = {}) = DirectGeminiGateway(
        { key }, { "Only propose declared functions; the app validates and commits." },
        { ApplicationProvider.getApplicationContext<BepApplication>().assets.open("gemini-functions.v1.json").bufferedReader().use { it.readText() } },
        openConnection = { opened(it); response }
    )
    private fun call(response: Response) = runBlocking { gateway(response).interpret("Hôm nay có bao nhiêu suất trưa?", "2026-11-10", "2026-11-12", "trace", "request") }
    private fun expectCode(code: String, block: () -> Unit) {
        try { block(); fail("Expected $code") } catch (error: RuleViolation) {
            assertEquals(code, error.code); assertFalse(error.toString().contains(synthetic))
        }
    }

    @Test fun functionCallingUsesFixedHttpsHeaderAndSharedDeclarations() {
        val response = Response(200, """{"candidates":[{"content":{"parts":[{"functionCall":{"name":"get_meal_counts","args":{"date":"2026-11-12"}}}]}}]}""")
        val result = runBlocking { gateway(response) { url ->
            assertEquals("https", url.protocol); assertEquals("generativelanguage.googleapis.com", url.host)
            assertFalse(url.toString().contains(synthetic))
        }.interpret("Hôm nay có bao nhiêu suất trưa?", "2026-11-10", "2026-11-12", "trace", "request") }
        assertEquals(synthetic, response.getRequestProperty("x-goog-api-key"))
        assertFalse(response.instanceFollowRedirects)
        val body = response.written.toString("UTF-8")
        assertFalse(body.contains(synthetic))
        val json = JSONObject(body)
        assertEquals(5, json.getJSONArray("tools").getJSONObject(0).getJSONArray("functionDeclarations").length())
        val context = JSONObject(json.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text")).getJSONObject("context")
        assertEquals("2026-11-10", context.getString("selectedDate")); assertEquals("2026-11-12", context.getString("referenceDate"))
        assertEquals("2026-11-12", result.calls.single().args.getString("date"))
    }
    @Test fun missingKeyNeverOpensANetworkConnection() {
        var opened = false
        expectCode("API_KEY_MISSING") { runBlocking { gateway(Response(200, "{}"), null) { opened = true }.interpret("Hỏi suất", "2026-11-12", "2026-11-12", "t", "r") } }
        assertFalse(opened)
    }
    @Test fun providerErrorTextCannotEscapeIntoAppErrors() {
        expectCode("API_KEY_REJECTED") { call(Response(400, """{"error":{"message":"$synthetic PRIVATE_CHAT","details":[{"reason":"API_KEY_INVALID"}]}}""")) }
        listOf(401 to "API_KEY_REJECTED", 403 to "API_KEY_REJECTED", 404 to "MODEL_UNAVAILABLE", 429 to "RATE_LIMITED", 503 to "GATEWAY_UNAVAILABLE").forEach { (status, code) ->
            expectCode(code) { call(Response(status, "PRIVATE_CHAT $synthetic")) }
        }
    }
    @Test fun compoundAndUnknownFunctionCallsAreRejected() {
        expectCode("PROTOCOL") { call(Response(200, """{"candidates":[{"content":{"parts":[{"functionCall":{"name":"delete_database","args":{}}}]}}]}""")) }
        expectCode("MULTI_STEP_UNSUPPORTED") { call(Response(200, """{"candidates":[{"content":{"parts":[{"functionCall":{"name":"close_day","args":{}}},{"functionCall":{"name":"close_day","args":{}}}]}}]}""")) }
    }
    @Test fun clarificationExcludesInternalThoughtsAndEmptyOutputFails() {
        val result = call(Response(200, """{"candidates":[{"content":{"parts":[{"thought":true,"text":"INTERNAL_THOUGHT"},{"text":"Bạn muốn đổi bữa nào?"}]}}]}"""))
        assertEquals("Bạn muốn đổi bữa nào?", result.text); assertTrue(result.calls.isEmpty())
        expectCode("PROTOCOL") { call(Response(200, """{"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[]}}]}""")) }
    }
    @Test fun oversizedResponsesAndRedirectsAreRejected() {
        expectCode("PROTOCOL") { call(Response(200, "x".repeat(65537))) }
        expectCode("GATEWAY_UNAVAILABLE") { call(Response(302, "untrusted redirect $synthetic")) }
    }
}
