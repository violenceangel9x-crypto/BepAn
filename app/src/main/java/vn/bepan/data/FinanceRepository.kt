package vn.bepan.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import vn.bepan.domain.*
import vn.bepan.diagnostics.TechnicalLog
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

data class FinanceCommand(val operationId: String = id(), val kind: Kind, val expected: String, val period: String,
    val amount: Long = 0, val people: List<String> = emptyList(), val targetPeriod: String = "", val settlementId: String = "",
    val text: String = "", val cashDate: String = today()) {
    fun encode(): String = JSONObject().put("operationId", operationId).put("kind", kind.name).put("expected", expected)
        .put("period", period).put("amount", amount).put("people", JSONArray(people)).put("targetPeriod", targetPeriod)
        .put("settlementId", settlementId).put("text", text).put("cashDate", cashDate).toString()
    companion object {
        fun decode(raw: String): FinanceCommand {
            val j = JSONObject(raw); val ids = j.getJSONArray("people")
            return FinanceCommand(j.getString("operationId"), Kind.valueOf(j.getString("kind")), j.getString("expected"), j.getString("period"),
                j.getLong("amount"), (0 until ids.length()).map { ids.getString(it) }, j.getString("targetPeriod"),
                j.getString("settlementId"), j.getString("text"), j.getString("cashDate"))
        }
    }
}

class FinanceRepository(val db: KitchenDatabase, log: TechnicalLog, private val currentDate: () -> String = ::today,
    private val now: () -> Instant = Instant::now) {
    val dao = db.dao()
    val writer = AuditedWriter(db, log)
    suspend fun token(): String = (dao.financeVersion() ?: 0).toString()
    suspend fun preview(kind: Kind, period: String, amount: Long = 0, people: List<String> = emptyList(),
        targetPeriod: String = "", settlementId: String = "", text: String = "", cashDate: String = currentDate()): FinanceCommand = db.withTransaction {
        YearMonth.parse(period); LocalDate.parse(cashDate)
        requireRule(cashDate <= currentDate() && amount in 0..1_000_000_000L && text.length <= 500, "INVALID_ARGUMENT")
        val command = FinanceCommand(kind = kind, expected = token(), period = period, amount = amount,
            people = people.sorted(), targetPeriod = targetPeriod, settlementId = settlementId, text = text, cashDate = cashDate)
        validate(command)
        dao.financeDraft(FinanceDraft(command.operationId, command.encode()))
        command
    }
    private suspend fun validate(command: FinanceCommand) {
        YearMonth.parse(command.period); LocalDate.parse(command.cashDate)
        requireRule(command.cashDate <= currentDate() && command.amount in 0..1_000_000_000L, "INVALID_ARGUMENT")
        when (command.kind) {
            Kind.OPENING_BALANCE -> requireRule(dao.cashOpening() == null, "OPENING_EXISTS")
            else -> throw RuleViolation("COMMAND_UNSUPPORTED")
        }
    }
    suspend fun cancel(command: FinanceCommand) = db.withTransaction { dao.deleteFinanceDraft(command.operationId) }
    suspend fun commit(command: FinanceCommand, ctx: Context = Context()): Receipt = writer.write(command.operationId,
        command.kind, command.cashDate, command.encode(), command.expected, ::token, ctx) {
        requireRule(dao.financeDrafts().any { it.id == command.operationId && it.command == command.encode() }, "ACTION_EXPIRED")
        validate(command)
        when (command.kind) {
            Kind.OPENING_BALANCE -> dao.cashOpening(CashOpening(date = command.cashDate, amount = command.amount))
            else -> throw RuleViolation("COMMAND_UNSUPPORTED")
        }
        dao.ensureFinance(FinanceVersion()); dao.bumpFinance(); dao.deleteFinanceDraft(command.operationId)
        BusinessChange("${command.period} · ${command.kind}", "", "${money(command.amount)} · ${command.cashDate}")
    }
    suspend fun cashBalance(): Long? = db.withTransaction {
        val opening = dao.cashOpening() ?: return@withTransaction null
        dao.cashEntries().filter { it.date >= opening.date }.fold(opening.amount) { balance, entry -> Math.addExact(balance, entry.amount) }
    }
}
