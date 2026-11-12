package vn.bepan.domain

import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

val businessZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
fun today(): String = LocalDate.now(businessZone).toString()
fun id(): String = UUID.randomUUID().toString()
fun money(v: Long): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(v) + "₫"
fun normalized(s: String): String = Normalizer.normalize(s.lowercase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")

enum class Meal(val label: String, val price: Long, val grams: Long) {
    BREAKFAST("Sáng", 14400, 136), LUNCH("Trưa", 28800, 272), DINNER("Tối", 28800, 272)
}
enum class Group(val label: String) { OFFICER("Cán bộ"), SOLDIER("Chiến sĩ"), GUEST("Khách") }
enum class Source { MANUAL, ASSISTANT, SYSTEM }
enum class Provider { NONE, MOCK, GEMINI }
enum class Kind { ATTENDANCE, ADJUST_ATTENDANCE, PURCHASE, MENU, CLOSE_DAY, SCHEDULE, GUEST, NOTE }
enum class ActionStatus { PENDING, SAVED, CANCELLED, EXPIRED, UNDONE, ERROR }
class RuleViolation(val code: String) : Exception(code)
fun requireRule(condition: Boolean, code: String) { if (!condition) throw RuleViolation(code) }

data class Context(val traceId: String = id(), val source: Source = Source.MANUAL, val provider: Provider = Provider.NONE, val requestId: String = "", val attempt: Int = 1)
data class Command(
    val operationId: String = id(), val kind: Kind, val date: String,
    val expectedVersion: Long, val personId: String = "", val meal: String = "",
    val enabled: Boolean = false, val amount: Long = 0, val text: String = "",
    val endDate: String = "", val group: String = "", val parentOperationId: String = ""
) {
    // Delimit by length so arbitrary menu/note content cannot collide.
    fun canonical(): String = listOf(kind.name, date, expectedVersion, personId, meal, enabled, amount, text, endDate, group, parentOperationId)
        .joinToString("") { val s = it.toString(); "${s.length}:$s" }
}
object Accounting {
    fun budget(count: Int, price: Long, rice: Long = 3000, seasoning: Long = 1000, fuel: Long = 800): Long {
        require(count >= 0 && price >= 0 && rice >= 0 && seasoning >= 0 && fuel >= 0)
        val fees = Math.addExact(Math.addExact(rice, seasoning), fuel)
        require(price >= fees)
        return Math.multiplyExact(count.toLong(), price - fees)
    }
    fun subsidyMilli(count: Int, grams: Long, vndPerKg: Long): Long =
        Math.multiplyExact(Math.multiplyExact(count.toLong(), grams), vndPerKg)
    fun roundSubsidy(milli: Long): Long = Math.addExact(milli, 500) / 1000
    fun allocate(amount: Long, people: List<String>): Map<String, Long> {
        require(amount > 0 && people.isNotEmpty() && people.distinct().size == people.size)
        return people.sorted().mapIndexed { index, person -> person to amount / people.size + if (index < amount % people.size) 1 else 0 }.toMap()
    }
}
