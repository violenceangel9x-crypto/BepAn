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
class ConfigurationTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: KitchenDatabase
    private lateinit var repo: KitchenRepository
    private lateinit var cfg: ConfigurationRepository
    @Before fun setup() = runBlocking {
        app.deleteDatabase("config-test.db"); db = KitchenDatabase.open(app, "config-test.db")
        val log = TechnicalLog(File(app.cacheDir, id()), scope)
        repo = KitchenRepository(db, log); cfg = ConfigurationRepository(db, log); repo.initialize(true)
    }
    @After fun close() { db.close(); scope.cancel() }
    @Test fun effectiveGroupStopResumePreserveClosedAndOverride() = runBlocking {
        repo.commit(Command(kind = Kind.CLOSE_DAY, date = "2026-11-12", expectedVersion = 0), Context())
        cfg.commit(cfg.profile("demo-02", "Tên mẫu", "2026-11-13", "OFFICER", true))
        assertEquals("SOLDIER", repo.snapshot("2026-11-12").servings.first { it.person.id == "demo-02" }.person.group)
        assertEquals("OFFICER", repo.snapshot("2026-11-13").servings.first { it.person.id == "demo-02" }.person.group)
        cfg.commit(cfg.profile("demo-02", "Tên mẫu", "2026-11-14", "OFFICER", false))
        assertEquals(9, repo.snapshot("2026-11-14").count(Meal.LUNCH))
        cfg.commit(cfg.profile("demo-02", "Tên mẫu", "2026-11-15", "SOLDIER", true))
        assertEquals(10, repo.snapshot("2026-11-15").count(Meal.LUNCH))
        assertEquals(720000L, repo.snapshot("2026-11-12").charge)
    }
    @Test fun ratesInvalidateDraftPreviewAndKeepClosedSnapshots() = runBlocking {
        repo.commit(Command(kind = Kind.CLOSE_DAY, date = "2026-11-12", expectedVersion = 0), Context())
        val draft = repo.snapshot("2026-11-13")
        val purchase = Command(kind = Kind.PURCHASE, date = draft.day.date, expectedVersion = draft.day.version, amount = 85000, text = "Khoản thử")
        cfg.commit(cfg.rate(Rate("2026-11-13", lunch = 40000, subsidyPerKg = 900)))
        assertEquals(832000L, repo.snapshot("2026-11-13").charge)
        assertEquals(720000L, repo.snapshot("2026-11-12").charge)
        try { repo.commit(purchase, Context()); fail() } catch (e: RuleViolation) { assertEquals("STALE_VERSION", e.code) }
    }
    @Test fun configurationReplayStaleAndAuditRollback() = runBlocking {
        val command = cfg.profile("", "Người mới", "2026-11-12", "OFFICER", true)
        cfg.writer.auditFaultForTest = true
        try { cfg.commit(command); fail() } catch (e: RuleViolation) { assertEquals("AUDIT_FAILED", e.code) }
        assertEquals(10, repo.dao.people().size); assertNull(repo.dao.receipt(command.operationId))
        cfg.writer.auditFaultForTest = false
        val receipt = cfg.commit(command)
        assertEquals(receipt, cfg.commit(command)); assertEquals(11, repo.dao.people().size)
        val stale = cfg.profile("", "Người nữa", "2026-11-12", "SOLDIER", true)
        cfg.commit(cfg.rate(Rate("2026-11-20")))
        try { cfg.commit(stale); fail() } catch (e: RuleViolation) { assertEquals("STALE_VERSION", e.code) }
    }
    @Test fun invalidRateAndDuplicateEffectiveDateRejected() = runBlocking {
        try { cfg.rate(Rate("2026-11-12", breakfast = 100)); fail() } catch (e: RuleViolation) { assertEquals("INVALID_ARGUMENT", e.code) }
        cfg.commit(cfg.rate(Rate("2026-11-12")))
        try { cfg.rate(Rate("2026-11-12")); fail() } catch (e: RuleViolation) { assertEquals("EFFECTIVE_DATE_EXISTS", e.code) }
    }
}
