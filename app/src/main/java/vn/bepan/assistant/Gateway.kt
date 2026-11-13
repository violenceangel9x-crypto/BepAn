package vn.bepan.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import vn.bepan.domain.*
import java.net.HttpURLConnection
import java.net.URI
import java.text.Normalizer
import java.time.LocalDate

data class Call(val name: String, val args: JSONObject)
data class Interpretation(val calls: List<Call> = emptyList(), val text: String = "")
interface Gateway { suspend fun interpret(message: String, selected: String, reference: String, trace: String, request: String, attempt: Int = 1, scope: AssistantScope = AssistantScope()): Interpretation }

class MockGateway : Gateway {
    override suspend fun interpret(message: String, selected: String, reference: String, trace: String, request: String, attempt: Int, scope: AssistantScope): Interpretation {
        val s = normalized(message)
        if (s.contains("timeout")) throw RuleViolation("GATEWAY_TIMEOUT")
        if ("bu gao" in s || "so thang" in s || "tai chinh" in s || "con no" in s || "so du" in s || "thu tien" in s) {
            val explicitMonth = Regex("\\b(\\d{1,2})/(\\d{4})\\b").find(s)
            val period = explicitMonth?.let { java.time.YearMonth.of(it.groupValues[2].toInt(), it.groupValues[1].toInt()).toString() }
                ?: scope.period.ifEmpty { selected.take(7) }
            val args = JSONObject().put("period", period)
            if ("thu tien" in s) {
                val amount = Regex("\\b(\\d+)\\s*(nghin|ngan|trieu|dong)?").findAll(s.replace(Regex("\\d{1,2}/\\d{4}"), "")).lastOrNull()
                    ?: return Interpretation(text = "Bạn muốn thu bao nhiêu đồng?")
                val money = amount.groupValues[1].toLong() * when (amount.groupValues[2]) { "nghin", "ngan" -> 1000L; "trieu" -> 1000000L; else -> 1L }
                args.put("amountVnd", money)
                val name = message.substringAfter("của ", "").substringBefore(" tháng ").trim()
                if (name.isNotBlank()) args.put("personName", name)
                else if ("chien si" in s) args.put("group", "SOLDIER")
                else if ("can bo" in s) args.put("group", "OFFICER")
                else return Interpretation(text = "Bạn muốn thu của ai hoặc nhóm nào?")
                return Interpretation(listOf(Call("record_collection", args)))
            }
            if ("bu gao" in s) return Interpretation(listOf(Call("get_rice_subsidy", args)))
            val name = message.substringAfter("của ", "").substringBefore(" tháng ").trim()
            if (name.isNotBlank()) return Interpretation(listOf(Call("get_person_balance", args.put("personName", name))))
            return Interpretation(listOf(Call("get_finance_month", args)))
        }
        val explicit = Regex("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b").find(s)
        val iso = Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b").find(s)?.value
        val date = when {
            explicit != null -> LocalDate.of(explicit.groupValues[3].toInt(), explicit.groupValues[2].toInt(), explicit.groupValues[1].toInt()).toString()
            iso != null -> LocalDate.parse(iso).toString()
            "hom nay" in s || "toi nay" in s -> reference
            Regex("\\bmai\\b").containsMatchIn(s) -> LocalDate.parse(reference).plusDays(1).toString()
            "dang xem" in s -> selected
            else -> reference
        }
        val args = JSONObject().put("date", date)
        return when {
            "suat" in s -> Interpretation(listOf(Call("get_meal_counts", args)))
            "ngan sach" in s || "tien di cho" in s -> Interpretation(listOf(Call("get_shopping_budget", args)))
            "chot" in s -> Interpretation(listOf(Call("close_day", args)))
            "mua" in s || "ghi tien" in s -> {
                val withoutDates = s.replace(Regex("\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b|\\b\\d{4}-\\d{2}-\\d{2}\\b"), " ")
                val match = Regex("(-?\\d+(?:[.,]\\d+)?)\\s*(nghin|ngan|trieu)?").findAll(withoutDates).lastOrNull()
                    ?: return Interpretation(text = "Bạn muốn ghi khoản mua bao nhiêu đồng?")
                val raw = match.groupValues[1]
                val unit = match.groupValues[2]
                if (unit.isNotEmpty() && Regex("[.,]\\d{3}$").containsMatchIn(raw)) return Interpretation(text = "Số tiền có nhiều cách hiểu. Bạn nhập số nguyên đồng, ví dụ 1000000 đồng nhé.")
                val number = if (unit.isEmpty()) raw.replace(".", "").replace(",", "").toBigDecimal() else raw.replace(',', '.').toBigDecimal()
                val amount = runCatching { number.multiply(when(unit) { "trieu" -> 1000000.toBigDecimal(); "nghin", "ngan" -> 1000.toBigDecimal(); else -> 1.toBigDecimal() }).longValueExact() }.getOrNull()
                    ?: return Interpretation(text = "Bạn nhập lại số tiền nguyên đồng nhé.")
                Interpretation(listOf(Call("record_purchase", args.put("amountVnd", amount).put("description", if ("rau" in s) "Mua rau" else if ("thit" in s) "Mua thịt" else "Khoản mua"))))
            }
            "cat" in s || "an lai" in s -> {
                val meal = when { "sang" in s -> Meal.BREAKFAST; "trua" in s -> Meal.LUNCH; "toi" in s -> Meal.DINNER; else -> null }
                    ?: return Interpretation(text = "Bạn muốn cắt bữa Sáng, Trưa hay Tối?")
                val name = message.substringAfterLast("cho ", "").trim()
                if (name.isEmpty()) return Interpretation(text = "Bạn muốn đổi suất cho ai?")
                Interpretation(listOf(Call("set_attendance", args.put("personName", name).put("meal", meal.name).put("enabled", "an lai" in s))))
            }
            else -> Interpretation(text = "Mô phỏng hỗ trợ hỏi suất, ngân sách, cắt một bữa, ghi tiền mua và chốt ngày. Gửi “timeout” để thử chẩn đoán.")
        }
    }
}

class HttpGateway(private val endpoint: String, private val openConnection: (java.net.URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }) : Gateway {
    override suspend fun interpret(message: String, selected: String, reference: String, trace: String, request: String, attempt: Int, scope: AssistantScope): Interpretation = withContext(Dispatchers.IO) {
        val uri = runCatching { URI(endpoint) }.getOrElse { throw RuleViolation("CONFIGURATION") }
        requireRule(uri.scheme == "https" || (vn.bepan.BuildConfig.DEBUG && uri.scheme == "http" && uri.host in listOf("127.0.0.1", "localhost")), "CONFIGURATION")
        val connection = openConnection(uri.resolve("/v1/assistant/interpret").toURL())
        try {
            connection.connectTimeout = 10000; connection.readTimeout = 30000
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val body = JSONObject().put("requestId", request).put("traceId", trace).put("attempt", attempt).put("message", message)
                .put("context", scope.json(selected, reference))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) {
                val response = connection.errorStream?.use { stream ->
                    val buffer = ByteArray(1024); val count = stream.read(buffer)
                    if (count > 0) runCatching { JSONObject(String(buffer, 0, count, Charsets.UTF_8)).optString("error") }.getOrDefault("") else ""
                }.orEmpty()
                throw RuleViolation(response.takeIf { it in setOf("CONFIGURATION", "GATEWAY_TIMEOUT", "RATE_LIMITED", "PROTOCOL") } ?: "GATEWAY_UNAVAILABLE")
            }
            val bytes = connection.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (output.size() <= 65536) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            requireRule(bytes.size <= 65536, "PROTOCOL")
            val json = JSONObject(bytes.toString(Charsets.UTF_8))
            requireRule(json.getString("requestId") == request && json.getString("traceId") == trace, "PROTOCOL")
            val calls = json.optJSONArray("calls") ?: JSONArray()
            requireRule(calls.length() <= 1, "MULTI_STEP_UNSUPPORTED")
            Interpretation((0 until calls.length()).map { i -> val c = calls.getJSONObject(i); Call(c.getString("name"), c.getJSONObject("args")) }, json.optString("text").take(2000))
        } catch (_: java.net.SocketTimeoutException) { throw RuleViolation("GATEWAY_TIMEOUT") }
        finally { connection.disconnect() }
    }
}

object CommandCodec {
    fun encode(c: Command): String = JSONObject().put("operationId", c.operationId).put("kind", c.kind.name).put("date", c.date)
        .put("expectedVersion", c.expectedVersion).put("personId", c.personId).put("meal", c.meal).put("enabled", c.enabled)
        .put("amount", c.amount).put("text", c.text).put("endDate", c.endDate).put("group", c.group).put("parentOperationId", c.parentOperationId).toString()
    fun decode(raw: String): Command {
        val j = JSONObject(raw)
        return Command(j.getString("operationId"), Kind.valueOf(j.getString("kind")), j.getString("date"), j.getLong("expectedVersion"), j.getString("personId"), j.getString("meal"), j.getBoolean("enabled"), j.getLong("amount"), j.getString("text"), j.getString("endDate"), j.getString("group"), j.getString("parentOperationId"))
    }
    fun resolveDate(args: JSONObject, selected: String, reference: String): String {
        val date = args.optString("date", args.optString("dayRef", "VIEW_DATE"))
        return when(date) { "TODAY" -> reference; "TOMORROW" -> LocalDate.parse(reference).plusDays(1).toString(); "VIEW_DATE" -> selected; else -> LocalDate.parse(date).toString() }
    }
}
