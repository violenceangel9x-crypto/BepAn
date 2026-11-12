package vn.bepan.data

import vn.bepan.domain.*
import java.time.YearMonth

data class PersonMonth(val person: Person, val groups: Set<String>, val meals: List<ClosedMeal>, val projected: Long,
    val paid: Long, val carryIn: Long, val carryOut: Long, val refunded: Long, val reserved: Long) {
    val charge get() = meals.filter { it.enabled }.sumOf { it.price }
    val balance get() = Math.subtractExact(Math.addExact(paid, carryIn), Math.addExact(charge, Math.addExact(refunded, carryOut)))
    val available get() = (balance.coerceAtLeast(0) - reserved).coerceAtLeast(0)
    val needsReview get() = reserved > balance.coerceAtLeast(0) || (balance < 0 && (refunded > 0 || carryOut > 0))
    val cuts get() = meals.count { it.registered == true && !it.enabled }
    fun groupCharge(group: String) = meals.filter { it.group == group && it.enabled }.sumOf { it.price }
}
fun Settlement.remaining(rows: List<Settlement>): Long = amount - rows.filter { it.parentId == id && it.status in listOf("REFUNDED", "CARRIED") }.sumOf { it.amount }
data class FinanceMonth(val period: String, val people: List<PersonMonth>, val days: List<Day>, val cash: List<CashEntry>,
    val periodCash: List<CashEntry>, val allocations: List<PaymentAllocation>, val settlements: List<Settlement>,
    val today: String, val cashBalance: Long?, val opening: CashOpening?, val revisions: List<CloseRevision> = emptyList()) {
    val calendar: List<String> get() = YearMonth.parse(period).let { month -> (1..month.lengthOfMonth()).map { month.atDay(it).toString() } }
    val closedDates get() = days.filter { it.closed }.map { it.date }.toSet()
    val unclosed get() = calendar.filter { it <= today && it !in closedDates }
    val future get() = calendar.filter { it > today && it !in closedDates }
    val charge get() = people.sumOf { it.charge }
    val paid get() = people.sumOf { it.paid }
    val outstanding get() = people.sumOf { (-it.balance).coerceAtLeast(0) }
    val surplus get() = people.sumOf { it.balance.coerceAtLeast(0) }
    val subsidy get() = Accounting.roundSubsidy(people.flatMap { it.meals }.filter { it.group == Group.SOLDIER.name && it.enabled }.sumOf { it.subsidyMilli })
    val receivedSubsidy get() = periodCash.filter { it.kind == "SUBSIDY_RECEIPT" }.sumOf { it.amount }
    val subsidyRemaining get() = subsidy - receivedSubsidy
    val cashIn get() = cash.filter { it.amount > 0 }.sumOf { it.amount }
    val cashOut get() = -cash.filter { it.amount < 0 }.sumOf { it.amount }
    fun person(id: String) = people.firstOrNull { it.person.id == id }
}
object FinanceCalculator {
    fun person(person: Person, meals: List<ClosedMeal>, projected: Long, allocations: List<PaymentAllocation>,
        settlements: List<Settlement>, period: String, groups: Set<String>): PersonMonth {
        val rows = settlements.filter { it.personId == person.id }
        return PersonMonth(person, groups, meals, projected, allocations.filter { it.personId == person.id }.sumOf { it.amount },
            rows.filter { it.status == "CARRIED" && it.toPeriod == period }.sumOf { it.amount },
            rows.filter { it.status == "CARRIED" && it.fromPeriod == period }.sumOf { it.amount },
            rows.filter { it.status == "REFUNDED" && it.fromPeriod == period }.sumOf { it.amount },
            rows.filter { it.status == "PENDING" && it.fromPeriod == period }.sumOf { it.remaining(rows) })
    }
    fun projected(person: Person, month: YearMonth, memberships: List<MembershipPeriod>, rates: List<Rate>, meals: List<ClosedMeal>,
        closedDates: Set<String>, overrides: List<MealOverride>, schedules: List<Schedule>): Long =
        (1..month.lengthOfMonth()).sumOf { number ->
            val date = month.atDay(number).toString()
            if (date in closedDates) meals.filter { it.date == date && it.enabled }.sumOf { it.price }
            else {
                val active = KitchenRepository.activePerson(person, memberships, date)
                val rate = rates.lastOrNull { it.effective <= date }
                if (active == null || rate == null) 0L else Meal.entries.sumOf { meal ->
                    val override = overrides.firstOrNull { it.date == date && it.personId == person.id && it.meal == meal.name }
                    val plan = schedules.filter { it.personId == person.id && it.meal == meal.name && it.start <= date && (it.end.isBlank() || it.end >= date) }
                    requireRule(plan.map { it.enabled }.distinct().size <= 1, "SCHEDULE_CONFLICT")
                    val enabled = override?.enabled ?: plan.lastOrNull()?.enabled ?: (active.group != Group.GUEST.name)
                    if (enabled) KitchenRepository.price(rate, meal) else 0L
                }
            }
        }
}
