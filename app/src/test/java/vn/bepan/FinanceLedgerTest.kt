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
class FinanceLedgerTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: KitchenDatabase
    private lateinit var repo: KitchenRepository
    private lateinit var finance: FinanceRepository
    @Before fun setup() = runBlocking {
        app.deleteDatabase("finance-ledger.db"); db = KitchenDatabase.open(app, "finance-ledger.db")
        val log = TechnicalLog(File(app.cacheDir, id()), scope)
        repo = KitchenRepository(db, log); repo.initialize(true)
        finance = FinanceRepository(db, log, { "2099-12-01" })
    }
    @After fun close() { db.close(); scope.cancel() }
    @Test fun purchaseOneCashImpactAndOpeningIsNotAnIncome() = runBlocking {
        assertNull(finance.cashBalance())
        val opening = finance.preview(Kind.OPENING_BALANCE, "2026-11", 1_000_000, cashDate = "2000-01-01")
        finance.commit(opening)
        assertTrue(db.dao().cashEntries().isEmpty())
        val command = Command(kind = Kind.PURCHASE, date = "2026-11-12", expectedVersion = repo.snapshot("2026-11-12").day.version, amount = 85000, text = "Khoản mẫu")
        repo.commit(command, Context()); repo.commit(command, Context())
        assertEquals(1, db.dao().purchases().size); assertEquals(1, db.dao().cashEntries().size)
        assertEquals(915000L, finance.cashBalance())
        assertEquals(-85000L, db.dao().cashEntries().single().amount)
    }
    @Test fun financeDraftSurvivesReopenAndReplayAfterCommit() = runBlocking {
        val command = finance.preview(Kind.OPENING_BALANCE, "2026-11", 100000, cashDate = "2026-11-01")
        db.close(); db = KitchenDatabase.open(app, "finance-ledger.db")
        val log = TechnicalLog(File(app.cacheDir, id()), scope)
        finance = FinanceRepository(db, log, { "2099-12-01" })
        val restored = FinanceCommand.decode(db.dao().financeDrafts().single().command)
        assertEquals(command, restored)
        val receipt = finance.commit(restored)
        assertEquals(receipt, finance.commit(restored)); assertTrue(db.dao().financeDrafts().isEmpty())
    }
    @Test fun auditFailureAndCancellationCannotLeaveOpening() = runBlocking {
        val command = finance.preview(Kind.OPENING_BALANCE, "2026-11", cashDate = "2026-11-01")
        finance.writer.auditFaultForTest = true
        try { finance.commit(command); fail() } catch (e: RuleViolation) { assertEquals("AUDIT_FAILED", e.code) }
        assertNull(db.dao().cashOpening()); assertNull(db.dao().receipt(command.operationId))
        assertEquals(1, db.dao().financeDrafts().size)
        finance.writer.auditFaultForTest = false; finance.cancel(command)
        try { finance.commit(command); fail() } catch (e: RuleViolation) { assertEquals("ACTION_EXPIRED", e.code) }
    }
    @Test fun closingDayInvalidatesFinancialPreviewButNoteDoesNot() = runBlocking {
        val command = finance.preview(Kind.OPENING_BALANCE, "2026-11", cashDate = "2026-11-01")
        val date = "2026-11-12"
        repo.commit(Command(kind = Kind.NOTE, date = date, expectedVersion = repo.snapshot(date).day.version, text = "Ghi chú thử"), Context())
        assertEquals(command.expected, finance.token())
        repo.commit(Command(kind = Kind.CLOSE_DAY, date = date, expectedVersion = repo.snapshot(date).day.version), Context())
        try { finance.commit(command); fail() } catch (e: RuleViolation) { assertEquals("STALE_VERSION", e.code) }
    }
    @Test fun groupCollectionSplitsRemainderAndConcurrentReplayOnlyOnce() = runBlocking {
        val command = finance.preview(Kind.COLLECTION, "2026-11", 1000001, listOf("demo-04", "demo-02", "demo-03"), group = "SOLDIER")
        assertEquals(listOf(333334L, 333334L, 333333L), command.shares.map { it.amount })
        val receipts = (1..4).map { async(Dispatchers.IO) { finance.commit(command) } }.awaitAll()
        assertEquals(1, receipts.distinct().size)
        assertEquals(1, db.dao().cashEntries().size)
        assertEquals(1000001L, db.dao().allocations("2026-11").sumOf { it.amount })
        assertEquals(1000001L, finance.month("2026-11").paid)
        assertEquals(0L, finance.month("2026-11").cashIn)
        assertEquals(1000001L, finance.month("2099-12").cashIn)
        try { finance.commit(command.copy(amount = 2)); fail() } catch (e: RuleViolation) { assertEquals("OPERATION_ID_CONFLICT", e.code) }
    }
    @Test fun collectionForClosedMonthIsNotBlockedAndKeepsHistoricalAllocationGroup() = runBlocking {
        val date = "2026-11-12"
        repo.commit(Command(kind = Kind.CLOSE_DAY, date = date, expectedVersion = repo.snapshot(date).day.version), Context())
        val command = finance.preview(Kind.COLLECTION, "2026-11", 21600000, (0..9).map { "demo-${it.toString().padStart(2, '0')}" })
        finance.commit(command)
        assertEquals(1, db.dao().cashEntries().size); assertEquals(10, db.dao().allocations("2026-11").size)
        val cfg = ConfigurationRepository(db, TechnicalLog(File(app.cacheDir, id()), scope))
        cfg.commit(cfg.profile("demo-02", "Người đổi nhóm", "2026-11-13", "OFFICER", true))
        assertEquals("SOLDIER", db.dao().allocations("2026-11").single { it.personId == "demo-02" }.group)
        assertEquals(21600000L, finance.month("2026-11").paid)
    }
    @Test fun collectionAuditFailureRollsBackAllRowsAndStalePreviewCannotCommit() = runBlocking {
        val command = finance.preview(Kind.COLLECTION, "2026-11", 100000, listOf("demo-00"))
        finance.writer.auditFaultForTest = true
        try { finance.commit(command); fail() } catch (e: RuleViolation) { assertEquals("AUDIT_FAILED", e.code) }
        assertTrue(db.dao().cashEntries().isEmpty()); assertTrue(db.dao().allocations("2026-11").isEmpty())
        assertNull(db.dao().receipt(command.operationId)); assertEquals(1, db.dao().financeDrafts().size)
        finance.writer.auditFaultForTest = false
        val date = "2026-11-12"
        repo.commit(Command(kind = Kind.CLOSE_DAY, date = date, expectedVersion = repo.snapshot(date).day.version), Context())
        try { finance.commit(command); fail() } catch (e: RuleViolation) { assertEquals("STALE_VERSION", e.code) }
    }
}
