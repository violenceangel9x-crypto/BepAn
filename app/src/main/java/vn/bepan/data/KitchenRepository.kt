package vn.bepan.data

import androidx.room.withTransaction
import vn.bepan.domain.*
import vn.bepan.diagnostics.*
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate

data class Serving(val person: Person, val meal: Meal, val enabled: Boolean, val registered: Boolean,
    val unitPrice: Long = 0, val registrationKnown: Boolean = true)
data class DaySnapshot(val day: Day, val servings: List<Serving>, val menus: List<Menu>, val purchases: List<Purchase>, val budget: Long, val charge: Long, val rate: Rate, val nextVersion: Long = 0) {
    fun count(meal: Meal) = servings.count { it.meal == meal && it.enabled }
    val bought: Long get() = purchases.sumOf { it.amount }
}

class KitchenRepository(val db: KitchenDatabase, val log: TechnicalLog) {
    val dao = db.dao()
    var auditFaultForTest: Boolean = false
    suspend fun initialize(debug: Boolean) = db.withTransaction {
        dao.ensureFinance(FinanceVersion())
        if (dao.rates().isEmpty()) dao.rate(Rate("2000-01-01"))
        if (debug && dao.people().isEmpty()) {
            listOf("Nguyễn Văn Nam", "Trần Minh Nam", "Lê Minh Anh", "Phạm Đức Hùng", "Hoàng Văn Bình", "Đỗ Quang Huy", "Vũ Thanh Sơn", "Bùi Minh Tuấn", "Đặng Hải Long", "Ngô Đức Thành").forEachIndexed { i, name ->
                dao.person(Person("demo-${i.toString().padStart(2, '0')}", name, if (i < 2) Group.OFFICER.name else Group.SOLDIER.name, "2020-01-01"))
            }
        }
    }
    suspend fun snapshot(date: String): DaySnapshot = db.withTransaction { snapshotInside(date) }
    private suspend fun snapshotInside(date: String): DaySnapshot {
        LocalDate.parse(date)
        dao.ensureDay(Day(date))
        val day = dao.day(date)!!
        val people = dao.people()
        val memberships = dao.memberships()
        val closed = dao.closedMeals().filter { it.date == date }
        val overrides = dao.overrides().filter { it.date == date }
        val schedules = dao.schedules().filter { it.start <= date && (it.end.isEmpty() || it.end >= date) }
        val rate = dao.rates().lastOrNull { it.effective <= date } ?: throw RuleViolation("RATE_MISSING")
        val servings = if (day.closed) closed.map { row -> Serving(people.first { it.id == row.personId }.copy(group = row.group), Meal.valueOf(row.meal), row.enabled, row.registered == true, row.price, row.registered != null) }
        else people.mapNotNull { person -> activePerson(person, memberships, date) }.flatMap { person ->
            Meal.entries.map { meal ->
                val baseline = person.group != Group.GUEST.name
                val plan = schedules.filter { it.personId == person.id && it.meal == meal.name }
                requireRule(plan.map { it.enabled }.distinct().size <= 1, "SCHEDULE_CONFLICT")
                val override = overrides.firstOrNull { it.personId == person.id && it.meal == meal.name }
                Serving(person, meal, override?.enabled ?: plan.lastOrNull()?.enabled ?: baseline, baseline || override != null || plan.isNotEmpty(), price(rate, meal))
            }
        }
        val charge = if (day.closed) closed.filter { it.enabled }.sumOf { it.price } else servings.filter { it.enabled }.sumOf { price(rate, it.meal) }
        val budget = if (day.closed) closed.sumOf { it.budget } else servings.filter { it.enabled }.sumOf { Accounting.budget(1, price(rate, it.meal), rate.riceFee, rate.seasoning, rate.fuel) }
        val next = LocalDate.parse(date).plusDays(1).toString()
        dao.ensureDay(Day(next))
        return DaySnapshot(day, servings, dao.menus().filter { it.date == date || (it.date == next && it.meal == Meal.BREAKFAST.name) }, dao.purchases().filter { it.date == date }, budget, charge, rate, dao.day(next)!!.version)
    }
    suspend fun commit(command: Command, ctx: vn.bepan.domain.Context): Receipt {
        val hash = MessageDigest.getInstance("SHA-256").digest(command.canonical().toByteArray()).joinToString("") { "%02x".format(it) }
        log.record(ctx, EventCode.WRITE_STARTED, command.kind, command.operationId)
        try {
            val result = db.withTransaction {
                val existing = dao.receipt(command.operationId)
                if (existing != null) {
                    requireRule(existing.fingerprint == hash, "OPERATION_ID_CONFLICT")
                    return@withTransaction existing to true
                }
                requireRule(dao.action(command.operationId)?.status !in listOf("CANCELLED", "EXPIRED", "UNDONE"), "ACTION_EXPIRED")
                val snap = snapshotInside(command.date)
                requireRule(!snap.day.closed || command.kind in listOf(Kind.ADJUST_ATTENDANCE, Kind.NOTE), "DAY_CLOSED")
                requireRule(snap.day.version == command.expectedVersion, "STALE_VERSION")
                var before = ""; var after = ""; var target = command.date
                when (command.kind) {
                    Kind.ATTENDANCE, Kind.ADJUST_ATTENDANCE -> {
                        val meals = command.meal.split(',').map { Meal.valueOf(it) }.distinct()
                        requireRule(meals.isNotEmpty(), "INVALID_ARGUMENT")
                        if (command.kind == Kind.ADJUST_ATTENDANCE) requireRule(snap.day.closed && command.text.isNotBlank() && command.text.length <= 500, "REASON_REQUIRED")
                        val changed = meals.map { meal -> snap.servings.firstOrNull { it.person.id == command.personId && it.meal == meal } ?: throw RuleViolation("PERSON_MISSING") }.filter { it.enabled != command.enabled }
                        requireRule(changed.isNotEmpty(), "NO_CHANGE")
                        requireRule(changed.all { it.registered }, "NOT_REGISTERED")
                        before = changed.joinToString { "${it.meal.label}: ${if(it.enabled) "Có ăn" else "Cắt cơm"}" }
                        after = changed.joinToString { "${it.meal.label}: ${if(command.enabled) "Có ăn" else "Cắt cơm"}" }
                        target = "${changed.first().person.name} · ${command.date}"
                        changed.forEach { serving ->
                            if (snap.day.closed) {
                                val row = dao.closedMeals().first { it.date == command.date && it.personId == command.personId && it.meal == serving.meal.name }
                                dao.updateClosedMeal(row.copy(enabled = command.enabled, budget = if(command.enabled) row.budgetPerServing else 0, subsidyMilli = if(command.enabled) row.subsidyPerServingMilli else 0))
                            } else dao.override(MealOverride(command.date, command.personId, serving.meal.name, command.enabled))
                        }
                        if (snap.day.closed) {
                            dao.closeRevision(CloseRevision(command.date, snap.day.version+1, command.text, snapshotJson(dao.closedMeals().filter { it.date == command.date }, snap), command.operationId))
                            after += " · Lý do: ${command.text}"
                        }
                    }
                    Kind.PURCHASE -> {
                        requireRule(command.amount in 1..1_000_000_000L && command.text.isNotBlank() && command.text.length <= 500, "INVALID_ARGUMENT")
                        dao.purchase(Purchase(command.operationId, command.date, command.amount, command.text))
                        dao.cashEntry(CashEntry(command.operationId, "PURCHASE", today(), command.date.take(7), -command.amount,
                            command.text, Instant.now().toString(), "purchase:${command.operationId}", command.operationId))
                        after = "${command.text} · ${money(command.amount)}"
                    }
                    Kind.MENU -> {
                        Meal.valueOf(command.meal)
                        requireRule(command.text.length <= 16000, "INVALID_ARGUMENT")
                        before = snap.menus.firstOrNull { it.date == command.date && it.meal == command.meal }?.content ?: ""
                        after = command.text
                        dao.menu(Menu(command.date, command.meal, command.text))
                    }
                    Kind.CLOSE_DAY -> {
                        val rows = snap.servings.map { s -> ClosedMeal(command.date, s.person.id, s.meal.name, s.person.group, s.enabled,
                            price(snap.rate, s.meal), if (s.enabled) Accounting.budget(1, price(snap.rate, s.meal), snap.rate.riceFee, snap.rate.seasoning, snap.rate.fuel) else 0,
                            if (s.enabled && s.person.group == Group.SOLDIER.name) Accounting.subsidyMilli(1, grams(snap.rate, s.meal), snap.rate.subsidyPerKg) else 0,
                            Accounting.budget(1, price(snap.rate, s.meal), snap.rate.riceFee, snap.rate.seasoning, snap.rate.fuel),
                            if (s.person.group == Group.SOLDIER.name) Accounting.subsidyMilli(1, grams(snap.rate, s.meal), snap.rate.subsidyPerKg) else 0, s.registered) }
                        dao.closeMeals(rows)
                        dao.closeRevision(CloseRevision(command.date, snap.day.version+1, "Chốt lần đầu", snapshotJson(rows, snap), command.operationId))
                        dao.ensureDay(Day(LocalDate.parse(command.date).plusDays(1).toString()))
                        before = "Nháp"; after = "Đã chốt"
                    }
                    Kind.SCHEDULE -> {
                        requireRule(dao.people().any { it.id == command.personId && it.group != Group.GUEST.name }, "PERSON_MISSING")
                        Meal.valueOf(command.meal)
                        if (command.endDate.isNotEmpty()) requireRule(LocalDate.parse(command.endDate) >= LocalDate.parse(command.date), "INVALID_ARGUMENT")
                        val conflicts = dao.schedules().filter { it.personId == command.personId && it.meal == command.meal && (it.end.isEmpty() || it.end >= command.date) && (command.endDate.isEmpty() || it.start <= command.endDate) }
                        // Eating again closes the preceding open cut on the day before X.
                        requireRule(conflicts.none { it.enabled != command.enabled && it.start >= command.date }, "SCHEDULE_CONFLICT")
                        conflicts.filter { it.enabled != command.enabled }.forEach { dao.updateSchedule(it.copy(end = LocalDate.parse(command.date).minusDays(1).toString())) }
                        dao.schedule(Schedule(command.operationId, command.personId, command.meal, command.date, command.endDate, command.enabled))
                        // Invalidate every existing affected draft so previews of future dates expire.
                        dao.days().filter { !it.closed && it.date != command.date && it.date >= command.date && (command.endDate.isEmpty() || it.date <= command.endDate) }.forEach { dao.updateDay(it.copy(version = it.version + 1)) }
                        after = "${command.date} → ${command.endDate.ifEmpty { "Không hạn" }} · ${command.meal} · ${command.enabled}"
                    }
                    Kind.GUEST -> {
                        requireRule(command.text.isNotBlank() && command.text.length <= 120, "INVALID_ARGUMENT")
                        val guestId = "guest-${command.operationId}"
                        dao.person(Person(guestId, command.text, Group.GUEST.name, command.date, command.date))
                        command.meal.split(',').forEach { m -> Meal.valueOf(m); dao.override(MealOverride(command.date, guestId, m, true)) }
                        after = "${command.text} · ${command.meal} · Thu sau"
                    }
                    Kind.NOTE -> {
                        requireRule(command.text.isNotBlank() && command.text.length <= 16000, "INVALID_ARGUMENT")
                        command.personId.takeIf { it.isNotEmpty() }?.let { noteId -> dao.note(noteId)?.let { before = "${it.text} · Ghim: ${it.pinned}" } }
                        dao.note(Note(command.personId.ifEmpty { command.operationId }, command.text, command.enabled, Instant.now().toString()))
                        after = "${command.text} · Ghim: ${command.enabled}"
                    }
                    else -> throw RuleViolation("COMMAND_UNSUPPORTED")
                }
                val version = snap.day.version + 1
                if (command.kind in listOf(Kind.PURCHASE, Kind.CLOSE_DAY, Kind.ADJUST_ATTENDANCE, Kind.GUEST, Kind.ATTENDANCE, Kind.SCHEDULE)) {
                    dao.ensureFinance(FinanceVersion()); dao.bumpFinance()
                }
                dao.updateDay(snap.day.copy(version = version, closed = snap.day.closed || command.kind == Kind.CLOSE_DAY))
                if (auditFaultForTest) throw RuleViolation("AUDIT_FAILED")
                dao.audit(Audit(id(), command.operationId, traceId = ctx.traceId, timestamp = Instant.now().toString(), source = ctx.source.name, provider = ctx.provider.name,
                    kind = command.kind.name, target = target, before = before, after = after, parentOperationId = command.parentOperationId))
                val receipt = Receipt(command.operationId, ctx.traceId, command.kind.name, hash, command.date, version, Instant.now().toString())
                dao.receipt(receipt)
                dao.actionStatus(command.operationId, ActionStatus.SAVED.name)
                if (command.parentOperationId.isNotEmpty()) dao.actionStatus(command.parentOperationId, ActionStatus.UNDONE.name)
                receipt to false
            }
            log.record(ctx, if (result.second) EventCode.IDEMPOTENT_REPLAY else EventCode.WRITE_COMMITTED, command.kind, command.operationId)
            return result.first
        } catch (error: Exception) {
            val code = (error as? RuleViolation)?.code
            val event = when(code) { "OPERATION_ID_CONFLICT" -> EventCode.OPERATION_ID_CONFLICT; "STALE_VERSION", "DAY_CLOSED" -> EventCode.PREVIEW_EXPIRED; else -> EventCode.WRITE_ROLLED_BACK }
            log.record(ctx, event, command.kind, command.operationId, when(code) { "DAY_CLOSED" -> Reason.DAY_CLOSED; "STALE_VERSION" -> Reason.STALE_VERSION; "AUDIT_FAILED" -> Reason.AUDIT_FAILED; else -> Reason.INVALID_ARGUMENT })
            throw error
        }
    }
    suspend fun undo(command: Command, ctx: vn.bepan.domain.Context): Receipt {
        requireRule(command.kind == Kind.ATTENDANCE, "UNDO_UNSUPPORTED")
        val original = dao.receipt(command.operationId) ?: throw RuleViolation("RECEIPT_MISSING")
        return commit(command.copy(operationId = "${java.util.UUID.nameUUIDFromBytes((command.operationId + "undo").toByteArray())}", expectedVersion = original.version,
            enabled = !command.enabled, parentOperationId = command.operationId), ctx)
    }
    suspend fun evidence(trace: String): RoomEvidence = try {
        val receipts = dao.receipts(trace); val audits = dao.audits(trace)
        RoomEvidence(if (receipts.isNotEmpty()) "COMMITTED" else "NOT_COMMITTED", receipts.size, audits.size)
    } catch (_: Exception) { RoomEvidence("UNKNOWN") }
    companion object {
        fun activePerson(person: Person, memberships: List<MembershipPeriod>, date: String): Person? {
            val history = memberships.filter { it.personId == person.id }
            if (history.isEmpty()) return person.takeIf { it.starts <= date && (it.ends.isEmpty() || it.ends >= date) }
            val member = history.lastOrNull { it.effective <= date } ?: return null
            return person.copy(group = member.group).takeIf { member.active }
        }
        private fun snapshotJson(rows: List<ClosedMeal>, snap: DaySnapshot): String = org.json.JSONObject()
            .put("servings", org.json.JSONArray(rows.map { row -> org.json.JSONObject().put("personId",row.personId).put("meal",row.meal).put("group",row.group).put("enabled",row.enabled).put("registered",row.registered ?: org.json.JSONObject.NULL).put("price",row.price).put("budget",row.budget).put("subsidyMilli",row.subsidyMilli).put("budgetPerServing",row.budgetPerServing).put("subsidyPerServingMilli",row.subsidyPerServingMilli) }))
            .put("menus", org.json.JSONArray(snap.menus.filter { it.date == snap.day.date }.map { org.json.JSONObject().put("meal",it.meal).put("content",it.content) }))
            .put("purchasedAtClose",snap.bought).toString()
        fun price(rate: Rate, meal: Meal) = when(meal) { Meal.BREAKFAST -> rate.breakfast; Meal.LUNCH -> rate.lunch; Meal.DINNER -> rate.dinner }
        fun grams(rate: Rate, meal: Meal) = when(meal) { Meal.BREAKFAST -> rate.breakfastGrams; Meal.LUNCH -> rate.lunchGrams; Meal.DINNER -> rate.dinnerGrams }
    }
}
