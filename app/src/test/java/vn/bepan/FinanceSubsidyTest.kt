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
class FinanceSubsidyTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: KitchenDatabase
    private lateinit var repo: KitchenRepository
    private lateinit var finance: FinanceRepository
    private lateinit var cfg: ConfigurationRepository
    @Before fun setup() = runBlocking {
        app.deleteDatabase("subsidy-month.db"); db = KitchenDatabase.open(app, "subsidy-month.db")
        val log = TechnicalLog(File(app.cacheDir, id()), scope)
        repo = KitchenRepository(db, log); repo.initialize(false)
        finance = FinanceRepository(db, log, { "2026-12-01" }); cfg = ConfigurationRepository(db, log)
    }
    @After fun close() { db.close(); scope.cancel() }
    private suspend fun closeDay(date: String) { repo.commit(Command(kind = Kind.CLOSE_DAY, date = date, expectedVersion = repo.snapshot(date).day.version), Context()) }
    @Test fun effectiveRatesAndCrossPeriodReceiptsKeepPersonalPaymentsSeparate() = runBlocking {
        db.dao().person(Person("soldier", "Chiến sĩ mẫu", "SOLDIER", "2020-01-01"))
        db.dao().person(Person("officer", "Cán bộ mẫu", "OFFICER", "2020-01-01"))
        db.dao().person(Person("guest", "Khách mẫu", "GUEST", "2026-11-01", "2026-11-01"))
        db.dao().override(MealOverride("2026-11-01", "guest", "LUNCH", true))
        closeDay("2026-11-01")
        assertEquals(476L, finance.month("2026-11").subsidy)
        cfg.commit(cfg.rate(Rate("2026-11-02", subsidyPerKg = 900)))
        closeDay("2026-11-02")
        assertEquals(1088L, finance.month("2026-11").subsidy)
        val command = finance.preview(Kind.SUBSIDY_RECEIPT, "2026-11", 1000, cashDate = "2026-12-01")
        finance.commit(command); finance.commit(command)
        assertEquals(1000L, finance.month("2026-11").receivedSubsidy)
        assertEquals(88L, finance.month("2026-11").subsidyRemaining)
        assertEquals(0L, finance.month("2026-11").cashIn)
        assertEquals(1000L, finance.month("2026-12").cashIn)
        assertEquals(0L, finance.month("2026-11").paid)
        assertEquals(-144000L, finance.month("2026-11").person("soldier")!!.balance)
        finance.commit(finance.preview(Kind.SUBSIDY_RECEIPT, "2026-11", 100, cashDate = "2026-12-01"))
        assertEquals(-12L, finance.month("2026-11").subsidyRemaining)
    }
    @Test fun threeBreakfastsRoundOnlyAtMonthAndAdjustmentShowsOverReceipt() = runBlocking {
        repeat(3) { i ->
            val person = "soldier-$i"; db.dao().person(Person(person, "Chiến sĩ mẫu $i", "SOLDIER", "2020-01-01"))
            listOf("LUNCH", "DINNER").forEach { db.dao().override(MealOverride("2026-11-01", person, it, false)) }
        }
        closeDay("2026-11-01")
        assertEquals(286L, finance.month("2026-11").subsidy)
        finance.commit(finance.preview(Kind.SUBSIDY_RECEIPT, "2026-11", 286))
        repo.commit(Command(kind = Kind.ADJUST_ATTENDANCE, date = "2026-11-01", expectedVersion = repo.snapshot("2026-11-01").day.version,
            personId = "soldier-0", meal = "BREAKFAST", enabled = false, text = "Đối soát"), Context())
        assertEquals(190L, finance.month("2026-11").subsidy)
        assertEquals(-96L, finance.month("2026-11").subsidyRemaining)
        assertEquals(1, db.dao().cashEntries().size)
    }
}
