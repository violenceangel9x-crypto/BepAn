package vn.bepan.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import vn.bepan.domain.*
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

enum class GeminiConnection { DIRECT, GATEWAY }

/** User-supplied device credentials go only to Google's fixed HTTPS endpoint. */
class DirectGeminiGateway(
    private val loadKey: suspend () -> String?,
    private val loadSystemPrompt: () -> String,
    private val loadDeclarations: () -> String,
    private val model: String = "gemini-3.6-flash",
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) : Gateway {
    override suspend fun interpret(message: String, selected: String, reference: String, trace: String, request: String, attempt: Int): Interpretation = withContext(Dispatchers.IO) {
        val key = try { loadKey() } catch (_: Exception) { throw RuleViolation("CREDENTIAL_UNAVAILABLE") }
        requireRule(!key.isNullOrBlank(), "API_KEY_MISSING")
        requireRule(model.matches(Regex("[a-zA-Z0-9._-]+")) && message.length in 1..8000, "INVALID_ARGUMENT")
        val connection = openConnection(URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"))
        try {
            connection.connectTimeout = 10000; connection.readTimeout = 30000
            connection.instanceFollowRedirects = false; connection.useCaches = false
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("x-goog-api-key", key)
            val context = JSONObject().put("selectedDate", selected).put("referenceDate", reference).put("timeZone", "Asia/Ho_Chi_Minh")
            val content = JSONObject().put("message", message).put("context", context)
            val body = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", loadSystemPrompt()))))
                .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", content.toString())))))
                .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", JSONArray(loadDeclarations()))))
                .put("generationConfig", JSONObject().put("maxOutputTokens", 2048))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) {
                // Ignore provider prose: error responses may repeat request data or credentials.
                val invalidKey = if (connection.responseCode == 400) runCatching {
                    val error = connection.errorStream?.use { JSONObject(it.readBytesBounded(8192).toString(Charsets.UTF_8)) }
                    val details = error?.optJSONObject("error")?.optJSONArray("details") ?: JSONArray()
                    (0 until details.length()).any { details.optJSONObject(it)?.optString("reason") == "API_KEY_INVALID" }
                }.getOrDefault(false) else false
                throw RuleViolation(when {
                    invalidKey || connection.responseCode in listOf(401, 403) -> "API_KEY_REJECTED"
                    connection.responseCode == 429 -> "RATE_LIMITED"
                    connection.responseCode == 404 -> "MODEL_UNAVAILABLE"
                    connection.responseCode in 400..499 -> "PROTOCOL"
                    else -> "GATEWAY_UNAVAILABLE"
                })
            }
            val json = connection.inputStream.use { JSONObject(it.readBytesBounded(65536).toString(Charsets.UTF_8)) }
            parse(json)
        } catch (_: SocketTimeoutException) { throw RuleViolation("GATEWAY_TIMEOUT") }
        catch (error: RuleViolation) { throw error }
        catch (_: org.json.JSONException) { throw RuleViolation("PROTOCOL") }
        catch (_: Exception) { throw RuleViolation("GATEWAY_UNAVAILABLE") }
        finally { connection.disconnect() }
    }

    companion object {
        private val supported = setOf("get_meal_counts", "get_shopping_budget", "set_attendance", "record_purchase", "close_day")
        internal fun parse(json: JSONObject): Interpretation {
            val parts = json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                ?: throw RuleViolation("PROTOCOL")
            val calls = (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optJSONObject("functionCall") }.map { call ->
                requireRule(call.optString("name") in supported && call.optJSONObject("args") != null, "PROTOCOL")
                Call(call.getString("name"), call.getJSONObject("args"))
            }
            requireRule(calls.size <= 1, "MULTI_STEP_UNSUPPORTED")
            val text = if (calls.isNotEmpty()) "" else (0 until parts.length()).mapNotNull { parts.optJSONObject(it) }
                .filterNot { it.optBoolean("thought") }.joinToString("") { it.optString("text") }.take(2000)
            requireRule(calls.isNotEmpty() || text.isNotBlank(), "PROTOCOL")
            return Interpretation(calls, text)
        }
    }
}
