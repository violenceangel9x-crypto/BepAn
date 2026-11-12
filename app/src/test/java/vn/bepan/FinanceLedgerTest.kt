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
}
