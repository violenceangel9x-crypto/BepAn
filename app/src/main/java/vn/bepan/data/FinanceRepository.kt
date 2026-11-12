package vn.bepan.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import vn.bepan.domain.*
import vn.bepan.diagnostics.TechnicalLog
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

data class FinanceShare(val personId: String, val group: String, val amount: Long)
data class FinanceCommand(val operationId: String = id(), val kind: Kind, val expected: String, val period: String,
    val amount: Long = 0, val people: List<String> = emptyList(), val targetPeriod: String = "", val settlementId: String = "",
    val text: String = "", val cashDate: String = today(), val shares: List<FinanceShare> = emptyList()) {
    fun encode(): String {
        val json = JSONObject().put("operationId", operationId).put("kind", kind.name).put("expected", expected)
        .put("period", period).put("amount", amount).put("people", JSONArray(people)).put("targetPeriod", targetPeriod)
        .put("settlementId", settlementId).put("text", text).put("cashDate", cashDate)
        if (shares.isNotEmpty()) json.put("shares", JSONArray(shares.map { JSONObject().put("personId", it.personId).put("group", it.group).put("amount", it.amount) }))
        return json.toString()
    }
    companion object {
        fun decode(raw: String): FinanceCommand {
            val j = JSONObject(raw); val ids = j.getJSONArray("people")
            val shares = j.optJSONArray("shares") ?: JSONArray()
            return FinanceCommand(j.getString("operationId"), Kind.valueOf(j.getString("kind")), j.getString("expected"), j.getString("period"),
                j.getLong("amount"), (0 until ids.length()).map { ids.getString(it) }, j.getString("targetPeriod"),
                j.getString("settlementId"), j.getString("text"), j.getString("cashDate"), (0 until shares.length()).map {
                    val share = shares.getJSONObject(it); FinanceShare(share.getString("personId"), share.getString("group"), share.getLong("amount")) })
        }
    }
}

class FinanceRepository(val db: KitchenDatabase, log: TechnicalLog, private val currentDate: () -> String = ::today,
    private val now: () -> Instant = Instant::now) {
    val dao = db.dao()
    val writer = AuditedWriter(db, log)
    suspend fun token(): String = (dao.financeVersion() ?: 0).toString()
    suspend fun preview(kind: Kind, period: String, amount: Long = 0, people: List<String> = emptyList(),
        targetPeriod: String = "", settlementId: String = "", text: String = "", cashDate: String = currentDate(), group: String = ""): FinanceCommand = db.withTransaction {
        YearMonth.parse(period); LocalDate.parse(cashDate)
        requireRule(cashDate <= currentDate() && amount in 0..1_000_000_000L && text.length <= 500, "INVALID_ARGUMENT")
        val shares = if (kind == Kind.COLLECTION) {
            requireRule(amount > 0 && people.isNotEmpty() && people.size <= 5000 && people.distinct().size == people.size, "INVALID_ARGUMENT")
            val ledger = month(period)
            Accounting.allocate(amount, people).map { (id, share) ->
                val row = ledger.person(id) ?: throw RuleViolation("PERSON_MISSING")
                val snapshotGroup = group.ifEmpty { row.meals.lastOrNull()?.group ?: row.groups.sorted().first() }
                requireRule(snapshotGroup in row.groups, "PERSON_MISSING")
                FinanceShare(id, snapshotGroup, share)
            }
        } else emptyList()
        val command = FinanceCommand(kind = kind, expected = token(), period = period, amount = amount,
            people = people.sorted(), targetPeriod = targetPeriod, settlementId = settlementId, text = text, cashDate = cashDate, shares = shares)
        validate(command)
        dao.financeDraft(FinanceDraft(command.operationId, command.encode()))
        command
    }
    private suspend fun validate(command: FinanceCommand) {
        YearMonth.parse(command.period); LocalDate.parse(command.cashDate)
        requireRule(command.cashDate <= currentDate() && command.amount in 0..1_000_000_000L, "INVALID_ARGUMENT")
        when (command.kind) {
            Kind.OPENING_BALANCE -> requireRule(dao.cashOpening() == null, "OPENING_EXISTS")
            Kind.COLLECTION -> {
                requireRule(command.amount > 0 && command.people.isNotEmpty() && command.people.distinct().size == command.people.size, "INVALID_ARGUMENT")
                val expected = Accounting.allocate(command.amount, command.people)
                requireRule(command.shares.size == command.people.size && command.shares.map { it.personId }.toSet() == command.people.toSet(), "INVALID_ARGUMENT")
                val ledger = month(command.period)
                command.shares.forEach { share -> requireRule(expected[share.personId] == share.amount && share.group in (ledger.person(share.personId)?.groups ?: emptySet()), "INVALID_ARGUMENT") }
            }
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
            Kind.COLLECTION -> {
                dao.cashEntry(CashEntry(command.operationId, "COLLECTION", command.cashDate, command.period, command.amount,
                    command.text.ifBlank { "Thu tiền ăn" }, now().toString(), "operation:${command.operationId}"))
                dao.allocations(command.shares.map { PaymentAllocation(command.operationId, it.personId, command.period, it.group, it.amount) })
            }
            else -> throw RuleViolation("COMMAND_UNSUPPORTED")
        }
        dao.ensureFinance(FinanceVersion()); dao.bumpFinance(); dao.deleteFinanceDraft(command.operationId)
        BusinessChange("${command.period} · ${command.kind}", "", "${money(command.amount)} · ${command.cashDate}")
    }
    suspend fun cashBalance(): Long? = db.withTransaction {
        val opening = dao.cashOpening() ?: return@withTransaction null
        dao.cashEntries().filter { it.date >= opening.date }.fold(opening.amount) { balance, entry -> Math.addExact(balance, entry.amount) }
    }
    suspend fun month(period: String): FinanceMonth = db.withTransaction {
        val month = YearMonth.parse(period)
        val start = month.atDay(1).toString(); val end = month.plusMonths(1).atDay(1).toString()
        val days = dao.days(start, end); val closedDates = days.filter { it.closed }.map { it.date }.toSet()
        val meals = dao.closedMeals(start, end).filter { it.date in closedDates }
        val allocations = dao.allocations(period); val settlements = dao.settlements(period)
        val memberships = dao.memberships(); val rates = dao.rates()
        val overrides = dao.overrides().filter { it.date >= start && it.date < end }; val schedules = dao.schedules()
        val people = dao.people().mapNotNull { person ->
            val rows = meals.filter { it.personId == person.id }
            val historyGroups = rows.map { it.group } + allocations.filter { it.personId == person.id }.map { it.group }
            val activeGroups = (1..month.lengthOfMonth()).mapNotNull { KitchenRepository.activePerson(person, memberships, month.atDay(it).toString())?.group }
            val involved = rows.isNotEmpty() || allocations.any { it.personId == person.id } || settlements.any { it.personId == person.id }
            if (!involved && activeGroups.isEmpty()) null else FinanceCalculator.person(person, rows,
                FinanceCalculator.projected(person, month, memberships, rates, rows, closedDates, overrides, schedules),
                allocations, settlements, period, (historyGroups + activeGroups).toSet().ifEmpty { setOf(person.group) })
        }
        FinanceMonth(period, people, days, dao.cashEntries(start, end), dao.periodCash(period), allocations, settlements,
            currentDate(), cashBalance(), dao.cashOpening(), dao.revisions(start, end))
    }
}
