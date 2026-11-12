package vn.bepan

import org.junit.Assert.*
import org.junit.Test
import vn.bepan.domain.*
import java.time.YearMonth

class AccountingTest {
    @Test fun budgetFixture() {
        assertEquals(240000, Accounting.budget(10, 28800)); assertEquals(216000, Accounting.budget(9, 28800))
        assertEquals(131000, Accounting.budget(9, 28800) - 85000)
    }
    @Test fun groupAllocationsAndLeapYear() {
        assertEquals(listOf(333334L,333334L,333333L), Accounting.allocate(1000001, listOf("c","a","b")).values.toList())
        assertEquals(21600000, Accounting.allocate(21600000, (1..10).map { "$it" }).values.sum())
        assertEquals(2088000, YearMonth.of(2028,2).lengthOfMonth() * 72000L)
    }
    @Test fun subsidyOnlyRoundsAtTotal() {
        val first = Meal.entries.sumOf { Accounting.subsidyMilli(1,it.grams,700) }
        val second = Meal.entries.sumOf { Accounting.subsidyMilli(1,it.grams,900) }
        assertEquals(476000, first); assertEquals(1088, Accounting.roundSubsidy(first + second))
        assertEquals(286, Accounting.roundSubsidy(Accounting.subsidyMilli(3,136,700)))
    }
    @Test(expected = IllegalArgumentException::class) fun invalidFeesRejected() { Accounting.budget(1, 1000) }
    @Test fun vietnameseNames() { assertEquals("do duc nam", normalized("Đỗ Đức Nam")) }
}
