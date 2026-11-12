package vn.bepan

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.bepan.data.*
import vn.bepan.diagnostics.TechnicalLog
import vn.bepan.domain.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FinanceMonthTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: KitchenDatabase
    private lateinit var repo: KitchenRepository
    private lateinit var finance: FinanceRepository
    private lateinit var cfg: ConfigurationRepository
    @Before fun setup() = runBlocking {
        app.deleteDatabase("finance-month.db"); db = KitchenDatabase.open(app, "finance-month.db")
        val log = TechnicalLog(File(app.cacheDir, id()), scope)
        repo = KitchenRepository(db, log); repo.initialize(true)
        finance = FinanceRepository(db, log, { "2026-11-12" }); cfg = ConfigurationRepository(db, log)
    }
    @After fun close() { db.close(); scope.cancel() }
    @Test fun calendarsDoNotInventClosedOrUnclosedFromFuture() = runBlocking {
        assertEquals(28, finance.month("2027-02").calendar.size)
        assertEquals(29, finance.month("2028-02").calendar.size)
        assertEquals(30, finance.month("2026-11").calendar.size)
        assertEquals(31, finance.month("2026-12").calendar.size)
        val month = finance.month("2026-11")
        assertEquals(12, month.unclosed.size); assertEquals(18, month.future.size)
        assertEquals(0L, month.charge); assertTrue(db.dao().days().isEmpty())
    }
    @Test fun historicalGuestAndStoppedPeopleStayNamedAndGroupTotalsMatch() = runBlocking {
        val date = "2026-11-01"
        repo.commit(Command(kind = Kind.GUEST, date = date, expectedVersion = 0, meal = "LUNCH", text = "Khách lịch sử"), Context())
        repo.commit(Command(kind = Kind.CLOSE_DAY, date = date, expectedVersion = repo.snapshot(date).day.version), Context())
        cfg.commit(cfg.profile("demo-02", "Người đổi nhóm", "2026-11-02", "OFFICER", true))
        cfg.commit(cfg.profile("demo-00", "Người ngừng ăn", "2026-11-02", "OFFICER", false))
        val month = finance.month("2026-11")
        assertEquals("Khách lịch sử", month.people.single { it.person.group == "GUEST" }.person.name)
        assertEquals(0, month.people.single { it.person.group == "GUEST" }.cuts)
        assertNotNull(month.person("demo-00"))
        assertEquals("SOLDIER", month.person("demo-02")!!.meals.first().group)
        assertEquals(month.charge, Group.entries.sumOf { group -> month.people.sumOf { it.groupCharge(group.name) } })
        assertEquals(748800L, month.charge)
    }
    @Test fun closedDayAdjustmentReplacesContributionAndProjectionUsesRates() = runBlocking {
        val date = "2026-11-01"
        repo.commit(Command(kind = Kind.CLOSE_DAY, date = date, expectedVersion = 0), Context())
        repo.commit(Command(kind = Kind.ADJUST_ATTENDANCE, date = date, expectedVersion = 1, personId = "demo-00", meal = "LUNCH", enabled = false, text = "Đối soát"), Context())
        val month = finance.month("2026-11")
        assertEquals(691200L, month.charge); assertEquals(1, month.closedDates.size)
        assertEquals(2, month.revisions.size)
        assertEquals(30 * 72000L - 28800, month.person("demo-00")!!.projected)
        assertEquals(-43200L, month.person("demo-00")!!.balance)
    }
}
