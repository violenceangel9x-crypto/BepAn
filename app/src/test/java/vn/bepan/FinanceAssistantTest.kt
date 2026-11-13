package vn.bepan

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.bepan.assistant.*
import vn.bepan.data.*
import vn.bepan.diagnostics.*
import vn.bepan.domain.*
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FinanceAssistantTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: KitchenDatabase
    private lateinit var repo: KitchenRepository
    private lateinit var finance: FinanceRepository
    private lateinit var assistant: FinanceAssistant
    private lateinit var log: TechnicalLog
    private val context = AssistantScope("FINANCE", "2026-10", "SOLDIER", "DUE")
    @Before fun setup() = runBlocking<Unit> {
        app.deleteDatabase("finance-ai.db"); db = KitchenDatabase.open(app, "finance-ai.db")
        log = TechnicalLog(File(app.cacheDir, id()), scope); repo = KitchenRepository(db, log); repo.initialize(true)
        finance = FinanceRepository(db, log, { "2026-11-13" }); assistant = FinanceAssistant(finance)
        repo.commit(Command(kind = Kind.CLOSE_DAY, date = "2026-10-01", expectedVersion = 0), Context())
    }
    @After fun close() { db.close(); scope.cancel() }
    @Test fun monthReadsFrozenContextInsteadOfTodayDate() = runBlocking {
        val call = MockGateway().interpret("Sổ tháng đang xem", "2026-11-13", "2026-11-13", id(), id(), scope = context).calls.single()
        val result = assistant.propose(call, context, "2026-11-13")
        assertTrue(result.text.contains("2026-10")); assertTrue(result.text.contains(money(720000)))
        assertNull(result.command); assertTrue(db.dao().cashEntries().isEmpty())
    }
    @Test fun duplicateNameMustChooseAndCollectionNeedsExplicitCommit() = runBlocking {
        val call = Call("record_collection", JSONObject().put("period", "VIEW_PERIOD").put("personName", "Nam").put("amountVnd", 100000))
        val choice = assistant.propose(call, context, "2026-11-13")
        assertEquals(2, choice.choices.size); assertTrue(db.dao().cashEntries().isEmpty())
        val result = assistant.propose(call, context, "2026-11-13", "demo-00")
        val command = result.command!!
        assertEquals("2026-10", command.period); assertTrue(db.dao().cashEntries().isEmpty())
        val ctx = Context(source = Source.ASSISTANT, provider = Provider.MOCK)
        finance.commit(command, ctx); finance.commit(command, ctx)
        assertEquals(1, db.dao().cashEntries().size)
        assertEquals("ASSISTANT", db.dao().audits(ctx.traceId).single().source)
        assertEquals(100000L, finance.month("2026-10").paid)
    }
    @Test fun groupProposalFreezesSharesAndRejectsNonIntegralMoney() = runBlocking {
        val args = JSONObject().put("period", "2026-10").put("group", "SOLDIER").put("amountVnd", 1000001)
        val command = assistant.propose(Call("record_collection", args), context, "2026-11-13").command!!
        assertEquals(8, command.shares.size); assertEquals(1000001L, command.shares.sumOf { it.amount })
        assertTrue(command.shares.all { it.group == "SOLDIER" })
        try { assistant.propose(Call("record_collection", args.put("amountVnd", 1.5)), context, "2026-11-13"); fail() } catch (e: RuleViolation) { assertEquals("INVALID_ARGUMENT", e.code) }
    }
    @Test fun persistedMessageRetainsPeriodRouteGroupForRetry() = runBlocking {
        val trace = id()
        db.dao().chat(ChatMessage(id(), Instant.now().toString(), "USER", "Hỏi số mẫu", trace, selectedDate = "2026-11-13", referenceDate = "2026-11-13",
            route = context.route, selectedPeriod = context.period, selectedGroup = context.group, selectedFilter = context.filter))
        db.close(); db = KitchenDatabase.open(app, "finance-ai.db")
        val message = db.dao().userMessage(trace)!!
        assertEquals(context, AssistantScope(message.route, message.selectedPeriod, message.selectedGroup, message.selectedFilter))
        assertEquals("2026-10", AssistantScope(message.route, message.selectedPeriod).resolvePeriod(JSONObject(), "2026-12-01"))
    }
}
