package vn.bepan.assistant

import vn.bepan.data.*
import vn.bepan.domain.*

data class FinanceReply(val text: String, val command: FinanceCommand? = null, val choices: List<Person> = emptyList())

class FinanceAssistant(private val repository: FinanceRepository) {
    suspend fun propose(call: Call, scope: AssistantScope, selectedDate: String, selectedPerson: String = ""): FinanceReply {
        val period = scope.resolvePeriod(call.args, selectedDate)
        val ledger = repository.month(period)
        when (call.name) {
            "get_finance_month" -> return FinanceReply("Kỳ $period · ${ledger.closedDates.size} ngày đã chốt · ${ledger.unclosed.size} ngày đã tới chưa chốt\nTiền ăn ${money(ledger.charge)} · Đã nộp ${money(ledger.paid)}\nCòn thiếu ${money(ledger.outstanding)} · Dư ${money(ledger.surplus)}")
            "get_rice_subsidy" -> return FinanceReply("Bù gạo chiến sĩ · kỳ $period\nĐược cấp ${money(ledger.subsidy)} · Đã nhận ${money(ledger.receivedSubsidy)}\nChênh lệch còn chờ / âm là nhận vượt ${money(ledger.subsidyRemaining)}")
        }
        requireRule(call.name in setOf("get_person_balance", "record_collection"), "COMMAND_UNSUPPORTED")
        val group = call.args.optString("group")
        requireRule(group.isEmpty() || group in Group.entries.map { it.name }, "INVALID_ARGUMENT")
        val query = normalized(call.args.optString("personName")).trim().trimEnd('?', '.', '!')
            .replace(Regex("^(chien si|can bo|khach)\\s+"), "")
        val candidates = ledger.people.filter { group.isBlank() || group in it.groups }.filter { person ->
            if (selectedPerson.isNotBlank()) person.person.id == selectedPerson
            else query.isNotBlank() && normalized(person.person.name).split(' ').takeLast(query.split(' ').size).joinToString(" ") == query
        }
        val groupCollection = call.name == "record_collection" && query.isBlank() && group.isNotBlank() && selectedPerson.isBlank()
        if (!groupCollection && candidates.size != 1) return FinanceReply(
            if (candidates.isEmpty()) "Chưa rõ người phù hợp trong kỳ $period. Bạn nhập tên hoặc nhóm nhé." else "Có nhiều người trùng tên trong kỳ $period. Chọn đúng người bên dưới.",
            choices = candidates.map { it.person })
        if (call.name == "get_person_balance") {
            val row = candidates.single()
            return FinanceReply("${row.person.name} · kỳ $period\nTiền ăn ${money(row.charge)} · Đã nộp ${money(row.paid)}\nSố đối chiếu ${money(row.balance)} · Đang giữ ${money(row.reserved)} · Dư khả dụng ${money(row.available)}\nTạm tính theo các ngày đã chốt.")
        }
        val raw = call.args.opt("amountVnd") as? Number ?: throw RuleViolation("INVALID_ARGUMENT")
        val amount = runCatching { raw.toString().toBigDecimal().longValueExact() }.getOrNull() ?: throw RuleViolation("INVALID_ARGUMENT")
        requireRule(amount in 1..1_000_000_000L, "INVALID_ARGUMENT")
        val ids = if (groupCollection) ledger.people.filter { group in it.groups }.map { it.person.id } else candidates.map { it.person.id }
        requireRule(ids.isNotEmpty(), "PERSON_MISSING")
        val command = repository.preview(Kind.COLLECTION, period, amount, ids, text = "Thu tiền ăn", group = group)
        return FinanceReply("Xem trước thu tiền ăn · kỳ $period\nNgày tiền ${command.cashDate} · Quỹ tăng ${money(amount)}\n" +
            command.shares.joinToString("\n") { share ->
                val row = ledger.person(share.personId)!!
                "${row.person.name} · ${Group.valueOf(share.group).label}: ${money(share.amount)} · số đối chiếu ${money(row.balance)} → ${money(row.balance + share.amount)}"
            } + "\nChỉ lưu sau khi bạn xác nhận.", command)
    }
    companion object { val functions = setOf("get_finance_month", "get_person_balance", "get_rice_subsidy", "record_collection") }
}
