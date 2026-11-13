package vn.bepan

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.bepan.assistant.*
import vn.bepan.domain.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HttpGatewayTest {
    private class Response(private val status: Int, private val payload: String) : HttpURLConnection(URL("https://gateway.invalid")) {
        val body = ByteArrayOutputStream()
        override fun getOutputStream() = body
        override fun getInputStream() = ByteArrayInputStream(payload.toByteArray())
        override fun getErrorStream() = ByteArrayInputStream(payload.toByteArray())
        override fun getResponseCode() = status
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun connect() = Unit
    }
    @Test fun financeScopeAndRedirectPolicyAreExplicit() = runBlocking {
        val trace = id(); val request = id()
        val response = Response(200, JSONObject().put("traceId", trace).put("requestId", request).put("text", "Hỏi thêm").toString())
        HttpGateway("https://gateway.invalid") { response }.interpret("Sổ tháng đang xem", "2026-11-13", "2026-11-13", trace, request, scope = AssistantScope("FINANCE", "2026-10", "SOLDIER", "DUE"))
        val context = JSONObject(response.body.toString("UTF-8")).getJSONObject("context")
        assertEquals("FINANCE", context.getString("route")); assertEquals("2026-10", context.getString("selectedPeriod"))
        assertFalse(response.instanceFollowRedirects)
        val redirect = Response(302, "PRIVATE_HTTP_BODY")
        try { HttpGateway("https://gateway.invalid") { redirect }.interpret("Hỏi", "2026-11-13", "2026-11-13", trace, request); fail() }
        catch (e: RuleViolation) { assertEquals("GATEWAY_UNAVAILABLE", e.code); assertFalse(e.toString().contains("PRIVATE_HTTP_BODY")) }
        assertFalse(redirect.instanceFollowRedirects)
    }
}
