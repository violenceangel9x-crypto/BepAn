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
import vn.bepan.domain.*
import vn.bepan.diagnostics.*
import vn.bepan.assistant.*
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LoggingIntegrationTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: KitchenDatabase
    private lateinit var log: TechnicalLog
    private lateinit var repo: KitchenRepository
    private val date = "2026-11-12"
    @Before fun setup() = runBlocking {
        app.deleteDatabase("test.db")
        db = KitchenDatabase.open(app, "test.db")
        log = TechnicalLog(File(app.cacheDir, id()), scope)
        repo = KitchenRepository(db, log); repo.initialize(true)
    }
    @After fun close() { db.close(); scope.cancel() }
    private suspend fun cut() = Command(kind = Kind.ATTENDANCE, date = date, expectedVersion = repo.snapshot(date).day.version, personId = "demo-00", meal = "LUNCH", enabled = false)
    private suspend fun purchase() = Command(kind = Kind.PURCHASE, date = date, expectedVersion = repo.snapshot(date).day.version, amount = 85000, text = "Mua rau")
    private suspend fun assertRejected(code: String, block: suspend () -> Unit) {
        try { block(); fail("Expected $code") } catch (e: RuleViolation) { assertEquals(code, e.code) }
    }
    @Test fun L01_manualPersistsAcrossReopen() = runBlocking {
        val ctx = Context(); repo.commit(cut(), ctx)
        db.close(); db = KitchenDatabase.open(app, "test.db"); repo = KitchenRepository(db, log)
        assertEquals(9, repo.snapshot(date).count(Meal.LUNCH))
        assertEquals(RoomEvidence("COMMITTED", 1, 1), repo.evidence(ctx.traceId))
        assertTrue(log.snapshot().any { it.code == EventCode.WRITE_COMMITTED })
    }
    @Test fun L02_concurrentRetryOneReceiptAndSavedAction() = runBlocking {
        val ctx = Context(source = Source.ASSISTANT, provider = Provider.MOCK); val command = purchase()
        repo.dao.action(AssistantAction(command.operationId,ctx.traceId,id(),"MOCK",CommandCodec.encode(command),"preview"))
        val results = (1..4).map { async(Dispatchers.IO) { repo.commit(command,ctx) } }.awaitAll()
        assertEquals(1, results.distinct().size); assertEquals(1, repo.dao.purchases().size)
        assertEquals("SAVED", repo.dao.action(command.operationId)!!.status)
        assertEquals(RoomEvidence("COMMITTED",1,1), repo.evidence(ctx.traceId))
        assertTrue(log.snapshot().any { it.code == EventCode.IDEMPOTENT_REPLAY })
        assertRejected("OPERATION_ID_CONFLICT") { repo.commit(command.copy(amount=1),ctx) }
    }
    @Test fun L03_timeoutDoesNotWrite() = runBlocking {
        val ctx = Context(source=Source.ASSISTANT,provider=Provider.MOCK)
        log.record(ctx,EventCode.REQUEST_STARTED)
        assertRejected("GATEWAY_TIMEOUT") { MockGateway().interpret("timeout",date,date,ctx.traceId,id()) }
        log.record(ctx,EventCode.GATEWAY_TIMEOUT,reason=Reason.TIMEOUT)
        assertEquals(RoomEvidence("NOT_COMMITTED",0,0),repo.evidence(ctx.traceId))
        val report = DebugReport.render(ctx.traceId,log.snapshot(),repo.evidence(ctx.traceId),34,false)
        assertTrue(report.contains("GATEWAY_TIMEOUT")); assertFalse(report.contains("WRITE_COMMITTED"))
    }
    @Test fun L04_invalidPurchaseRollsBack() = runBlocking {
        val ctx=Context(); assertRejected("INVALID_ARGUMENT") { repo.commit(purchase().copy(amount=-1),ctx) }
        assertTrue(repo.dao.purchases().isEmpty()); assertEquals(RoomEvidence("NOT_COMMITTED"),repo.evidence(ctx.traceId))
    }
    @Test fun L05_stalePreviewAndClosedDay() = runBlocking {
        val c=purchase(); repo.commit(cut(),Context())
        assertRejected("STALE_VERSION") { repo.commit(c,Context()) }
        val fresh=purchase()
        repo.commit(Command(kind=Kind.CLOSE_DAY,date=date,expectedVersion=fresh.expectedVersion),Context())
        assertRejected("DAY_CLOSED") { repo.commit(fresh,Context()) }
        assertTrue(repo.dao.purchases().isEmpty())
    }
    @Test fun L06_auditFailureRollsBackBusinessAndCard() = runBlocking {
        val command=cut(); val ctx=Context()
        repo.dao.action(AssistantAction(command.operationId,ctx.traceId,id(),"NONE",CommandCodec.encode(command),"preview"))
        repo.auditFaultForTest=true
        assertRejected("AUDIT_FAILED") { repo.commit(command,ctx) }
        assertEquals(10,repo.snapshot(date).count(Meal.LUNCH)); assertEquals(RoomEvidence("NOT_COMMITTED"),repo.evidence(ctx.traceId))
        assertEquals("PENDING",repo.dao.action(command.operationId)!!.status)
        assertTrue(log.snapshot().any { it.code==EventCode.WRITE_ROLLED_BACK })
    }
    @Test fun L07_technicalFailureDoesNotBlockCommit() = runBlocking {
        log.disabledForTest=true; val ctx=Context(); repo.commit(cut(),ctx); log.snapshot()
        assertEquals(9,repo.snapshot(date).count(Meal.LUNCH)); assertEquals(RoomEvidence("COMMITTED",1,1),repo.evidence(ctx.traceId)); assertTrue(log.incomplete)
    }
    @Test fun L08_reportDoesNotCopyPrivateData() = runBlocking {
        val markers=listOf("TEST_API_SECRET_NOT_FOR_EXPORT","TEST_PERSON_NAME_NOT_FOR_EXPORT","TEST_CHAT_TEXT_NOT_FOR_EXPORT","TEST_HTTP_HEADER_NOT_FOR_EXPORT","99912345")
        val ctx=Context(source=Source.ASSISTANT,provider=Provider.MOCK)
        repo.dao.person(Person("private",markers[1],"OFFICER",date))
        repo.dao.chat(ChatMessage(id(),Instant.now().toString(),"USER",markers.joinToString(),ctx.traceId))
        repo.commit(purchase().copy(text=markers.joinToString(),amount=99912345),ctx)
        val events=log.snapshot()
        val report=DebugReport.render(ctx.traceId,events,repo.evidence(ctx.traceId),34,false)
        markers.forEach { assertFalse(report.contains(it)); assertFalse(events.joinToString { it.json().toString() }.contains(it)) }
        assertTrue(report.contains("WRITE_COMMITTED")); assertTrue(report.contains(ctx.traceId))
        assertNull(SafeEvent.parse("{\"traceId\":\"${markers[0]}\"}"))
    }
    @Test fun L09_rotationAgeAndManualClearLeaveBusiness() = runBlocking {
        val directory=File(app.cacheDir,id()); val small=TechnicalLog(directory,scope,maxBytes=1400)
        val ctx=Context(); repeat(40) { small.record(ctx,EventCode.REQUEST_STARTED) }; small.snapshot()
        assertTrue(directory.listFiles()!!.sumOf { it.length() }<=1400)
        directory.listFiles()!!.forEach { it.setLastModified(System.currentTimeMillis()-8*86400000L) }
        assertTrue(small.snapshot().isEmpty())
        repo.commit(cut(),ctx); small.clear()
        assertEquals(RoomEvidence("COMMITTED",1,1),repo.evidence(ctx.traceId))
    }
    @Test fun L10_receiptRecoversWithoutTechnicalCommit() = runBlocking {
        log.disabledForTest=true; val command=purchase(); val ctx=Context(); repo.commit(command,ctx)
        db.close(); db=KitchenDatabase.open(app,"test.db"); repo=KitchenRepository(db,log)
        val original=repo.commit(command,ctx)
        assertEquals(1,repo.dao.purchases().size); assertEquals("COMMITTED",repo.evidence(ctx.traceId).outcome)
        assertTrue(DebugReport.render(ctx.traceId,emptyList(),repo.evidence(ctx.traceId),34,true).contains("incomplete: true"))
    }
    @Test fun L11_reportAvailableWhenRoomUnavailable() = runBlocking {
        val ctx=Context(source=Source.SYSTEM); log.record(ctx,EventCode.ROOM_OPEN_FAILED,reason=Reason.DATABASE_UNAVAILABLE)
        db.close()
        val evidence=repo.evidence(ctx.traceId)
        assertEquals("UNKNOWN",evidence.outcome)
        assertTrue(DebugReport.render(ctx.traceId,log.snapshot(),evidence,34,false).contains("ROOM_OPEN_FAILED"))
    }
    @Test fun undoPreservesAuditAndRejectsStaleUndo() = runBlocking {
        val command=cut(); val ctx=Context(); repo.commit(command,ctx)
        val undoCtx=Context(); repo.undo(command,undoCtx)
        assertEquals(10,repo.snapshot(date).count(Meal.LUNCH)); assertEquals(command.operationId,repo.dao.audits(undoCtx.traceId).single().parentOperationId)
        val next=cut(); repo.commit(next,Context()); repo.commit(purchase(),Context())
        assertRejected("STALE_VERSION") { repo.undo(next,Context()) }
    }
    @Test fun nextBreakfastAcrossMonthAndScheduleResume() = runBlocking {
        repo.commit(Command(kind=Kind.MENU,date="2026-12-01",expectedVersion=0,meal="BREAKFAST",text="Bún bò"),Context())
        assertEquals("Bún bò",repo.snapshot("2026-11-30").menus.single().content)
        repo.commit(Command(kind=Kind.SCHEDULE,date="2026-11-30",expectedVersion=0,personId="demo-00",meal="LUNCH",enabled=false),Context())
        assertEquals(9,repo.snapshot("2026-12-01").count(Meal.LUNCH))
        val snap=repo.snapshot("2026-12-02")
        repo.commit(Command(kind=Kind.SCHEDULE,date="2026-12-02",expectedVersion=snap.day.version,personId="demo-00",meal="LUNCH",enabled=true),Context())
        assertEquals(10,repo.snapshot("2026-12-02").count(Meal.LUNCH))
    }
    @Test fun closeOnlyOnceAndSoldierSubsidy() = runBlocking {
        assertTrue(repo.dao.closedMeals().isEmpty()); val command=Command(kind=Kind.CLOSE_DAY,date=date,expectedVersion=0)
        repo.commit(command,Context()); repo.commit(command,Context())
        assertEquals(30,repo.dao.closedMeals().size)
        assertEquals(8*476L,Accounting.roundSubsidy(repo.dao.closedMeals().sumOf { it.subsidyMilli }))
        assertEquals(720000L,repo.dao.closedMeals().sumOf { it.price })
    }
    @Test fun cancelledActionCannotCommit() = runBlocking {
        val command=purchase(); val ctx=Context()
        repo.dao.action(AssistantAction(command.operationId,ctx.traceId,id(),"MOCK",CommandCodec.encode(command),"preview", "CANCELLED"))
        assertRejected("ACTION_EXPIRED") { repo.commit(command,ctx) }
        assertTrue(repo.dao.purchases().isEmpty())
    }
    @Test fun adjustmentKeepsRevisionsAndOneMonthlyContribution() = runBlocking {
        repo.commit(Command(kind=Kind.CLOSE_DAY,date=date,expectedVersion=0),Context())
        val version=repo.snapshot(date).day.version
        val adjustment=Command(kind=Kind.ADJUST_ATTENDANCE,date=date,expectedVersion=version,personId="demo-02",meal="LUNCH",enabled=false,text="Đối soát phiếu thử")
        repo.commit(adjustment,Context()); repo.commit(adjustment,Context())
        assertTrue(repo.snapshot(date).day.closed)
        assertEquals(2,repo.dao.revisions(date).size)
        assertEquals(691200L,repo.dao.closedMeals().filter { it.enabled }.sumOf { it.price })
        val restore=adjustment.copy(operationId=id(),expectedVersion=version+1,enabled=true,text="Sửa phiếu thử")
        repo.commit(restore,Context())
        assertEquals(720000L,repo.dao.closedMeals().filter { it.enabled }.sumOf { it.price })
        assertEquals(8*476L,Accounting.roundSubsidy(repo.dao.closedMeals().sumOf { it.subsidyMilli }))
        assertEquals(3,repo.dao.revisions(date).size)
    }
}
