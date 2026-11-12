package vn.bepan.data

import androidx.room.withTransaction
import org.json.JSONObject
import vn.bepan.domain.*
import vn.bepan.diagnostics.TechnicalLog
import java.time.LocalDate

data class ConfigurationCommand(val operationId: String = id(), val expected: String, val effective: String,
    val personId: String = "", val name: String = "", val group: String = "", val active: Boolean = true, val rate: Rate? = null) {
    val kind get() = if (rate == null) Kind.PROFILE else Kind.RATE
    fun canonical() = JSONObject().put("expected", expected).put("effective", effective).put("personId", personId)
        .put("name", name).put("group", group).put("active", active).put("rate", rate?.toString()).toString()
}

class ConfigurationRepository(private val db: KitchenDatabase, log: TechnicalLog) {
    private val dao = db.dao()
    val writer = AuditedWriter(db, log)
    suspend fun token(): String = fingerprint(listOf(dao.people(), dao.memberships(), dao.rates(), dao.days()).toString())
    suspend fun profile(personId: String, name: String, effective: String, group: String, active: Boolean): ConfigurationCommand = db.withTransaction {
        LocalDate.parse(effective); Group.valueOf(group)
        requireRule(name.isNotBlank() && name.length <= 120, "INVALID_ARGUMENT")
        val identity = personId.ifEmpty { id() }
        dao.people().firstOrNull { it.id == identity }?.let { requireRule(effective >= it.starts, "INVALID_ARGUMENT") }
        requireRule(dao.memberships().none { it.personId == identity && it.effective == effective }, "EFFECTIVE_DATE_EXISTS")
        ConfigurationCommand(expected = token(), effective = effective, personId = identity, name = name.trim(), group = group, active = active)
    }
    suspend fun rate(value: Rate): ConfigurationCommand = db.withTransaction {
        validateRate(value)
        requireRule(dao.rates().none { it.effective == value.effective }, "EFFECTIVE_DATE_EXISTS")
        ConfigurationCommand(expected = token(), effective = value.effective, rate = value)
    }
    suspend fun commit(command: ConfigurationCommand, ctx: Context = Context()): Receipt = writer.write(
        command.operationId, command.kind, command.effective, command.canonical(), command.expected, ::token, ctx) {
        LocalDate.parse(command.effective)
        val before = if (command.rate != null) dao.rates().lastOrNull { it.effective <= command.effective }?.toString().orEmpty()
            else dao.people().firstOrNull { it.id == command.personId }?.toString().orEmpty()
        if (command.rate != null) {
            validateRate(command.rate)
            requireRule(command.rate.effective == command.effective, "INVALID_ARGUMENT")
            dao.rate(command.rate)
        } else {
            Group.valueOf(command.group)
            requireRule(command.name.isNotBlank() && command.name.length <= 120, "INVALID_ARGUMENT")
            val old = dao.people().firstOrNull { it.id == command.personId }
            requireRule(old == null || command.effective >= old.starts, "INVALID_ARGUMENT")
            if (old != null && dao.memberships().none { it.personId == old.id }) {
                requireRule(command.effective >= old.starts, "INVALID_ARGUMENT")
                if (command.effective != old.starts) dao.membership(MembershipPeriod(old.id, old.starts, old.group, true))
                if (old.ends.isNotBlank()) {
                    val end = LocalDate.parse(old.ends).plusDays(1).toString()
                    if (end != command.effective) dao.membership(MembershipPeriod(old.id, end, old.group, false))
                }
            }
            dao.person(Person(command.personId, command.name.trim(), command.group, old?.starts ?: command.effective, old?.ends.orEmpty()))
            dao.membership(MembershipPeriod(command.personId, command.effective, command.group, command.active))
        }
        dao.days().filter { !it.closed && it.date >= command.effective }.forEach { dao.updateDay(it.copy(version = it.version + 1)) }
        BusinessChange(if (command.rate != null) "Định mức · ${command.effective}" else command.name, before,
            command.rate?.toString() ?: "${command.group} · ${command.effective} · ${if (command.active) "Đang ăn" else "Ngừng ăn"}")
    }
    companion object {
        fun validateRate(rate: Rate) {
            LocalDate.parse(rate.effective)
            val values = listOf(rate.breakfast, rate.lunch, rate.dinner, rate.riceFee, rate.seasoning, rate.fuel,
                rate.subsidyPerKg, rate.breakfastGrams, rate.lunchGrams, rate.dinnerGrams)
            requireRule(values.all { it in 0..1_000_000_000L }, "INVALID_ARGUMENT")
            Meal.entries.forEach { requireRule(KitchenRepository.price(rate, it) >= rate.riceFee + rate.seasoning + rate.fuel, "INVALID_ARGUMENT") }
        }
    }
}
