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
import vn.bepan.diagnostics.*
import vn.bepan.domain.*
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FullMonthTest {
    @Test fun leapMonthReconcilesAllPeopleGroupsCashSettlementsAndExportAfterReopen() = runBlocking<Unit> {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.deleteDatabase("full-month.db")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var db = KitchenDatabase.open(app, "full-month.db")
        val log = TechnicalLog(File(app.cacheDir, id()), scope)
        try {
            val repo = KitchenRepository(db, log, { "2028-02-01" }, { Instant.parse("2028-02-01T00:00:00Z") })
            repo.initialize(false)
            listOf("O" to "OFFICER", "S" to "SOLDIER", "T" to "SOLDIER").forEach { (person, group) ->
                repo.dao.person(Person(person, "Người mẫu $person", group, "2020-01-01"))
            }
            val cfg = ConfigurationRepository(db, log)
            cfg.commit(cfg.profile("T", "Người mẫu T", "2028-02-16", "OFFICER", true))
            cfg.commit(cfg.profile("T", "Người mẫu T", "2028-02-21", "OFFICER", false))
            cfg.commit(cfg.profile("T", "Người mẫu T", "2028-02-25", "SOLDIER", true))
            cfg.commit(cfg.rate(Rate("2028-02-16", lunch = 30000, subsidyPerKg = 900)))
            suspend fun write(date: String, kind: Kind, person: String = "", meal: String = "", text: String = "", amount: Long = 0, end: String = ""): Command {
                val command = Command(kind = kind, date = date, expectedVersion = repo.snapshot(date).day.version,
                    personId = person, meal = meal, text = text, amount = amount, endDate = end)
                repo.commit(command, Context()); return command
            }
            write("2028-02-01", Kind.PURCHASE, text = "Khoản mua mẫu", amount = 85000)
            write("2028-02-05", Kind.ATTENDANCE, "S", "LUNCH")
            write("2028-02-07", Kind.ATTENDANCE, "T", "DINNER")
            val cancelledGuest = write("2028-02-10", Kind.GUEST, meal = "LUNCH", text = "Khách mẫu hủy")
            write("2028-02-10", Kind.ATTENDANCE, "guest-${cancelledGuest.operationId}", "LUNCH")
            write("2028-02-11", Kind.SCHEDULE, "S", "LUNCH", end = "2028-02-13")
            write("2028-02-20", Kind.GUEST, meal = "LUNCH", text = "Khách mẫu ăn trưa")
            (1..28).filter { it != 14 }.forEach { day -> write("2028-02-${day.toString().padStart(2, '0')}", Kind.CLOSE_DAY) }
            val restoreLunch=repo.prepare(Command(kind=Kind.ADJUST_ATTENDANCE,date="2028-02-05",
                expectedVersion=repo.snapshot("2028-02-05").day.version,personId="S",meal="LUNCH",enabled=true,text="Đối chiếu mẫu: ăn lại"))
            repo.commit(restoreLunch.command,Context())
            assertEquals(5500800L,FinanceRepository(db,log).month("2028-02").charge)
            write("2028-02-05",Kind.ADJUST_ATTENDANCE,"S","LUNCH",text="Đối chiếu mẫu: xác nhận cắt")
            assertEquals(3,repo.dao.revisions("2028-02-05").size)
            var finance = FinanceRepository(db, log, { "2028-03-01" }, { Instant.parse("2028-03-01T00:00:00Z") })
            var month = finance.month("2028-02")
            assertEquals(5472000L, month.charge)
            assertEquals(1959600L, month.person("O")!!.charge)
            assertEquals(1844400L, month.person("S")!!.charge)
            assertEquals(1638000L, month.person("T")!!.charge)
            assertEquals(22780L, month.subsidy)
            assertEquals(27, month.closedDates.size); assertEquals(29, month.calendar.size)
            assertEquals(listOf("2028-02-14", "2028-02-29"), month.unclosed)
            assertEquals(month.charge, Group.entries.sumOf { group -> month.people.sumOf { it.groupCharge(group.name) } })
            finance.commit(finance.preview(Kind.OPENING_BALANCE, "2028-02", 1000000, cashDate = "2028-02-01"))
            val collection = finance.preview(Kind.COLLECTION, "2028-02", 6000001, listOf("T", "S", "O"))
            val ctx = Context(source = Source.ASSISTANT, provider = Provider.MOCK)
            finance.commit(collection, ctx); finance.commit(collection, ctx)
            val reserve = finance.preview(Kind.RESERVE_REFUND, "2028-02", 40401, listOf("O"))
            finance.commit(reserve)
            finance.commit(finance.preview(Kind.REFUND, "2028-02", 10000, listOf("O"), settlementId = reserve.operationId))
            finance.commit(finance.preview(Kind.REFUND, "2028-02", 30401, listOf("O"), settlementId = reserve.operationId))
            finance.commit(finance.preview(Kind.CARRY, "2028-02", 155600, listOf("S"), targetPeriod = "2028-03"))
            finance.commit(finance.preview(Kind.SUBSIDY_RECEIPT, "2028-02", 22780))
            db.close(); db = KitchenDatabase.open(app, "full-month.db")
            finance = FinanceRepository(db, log, { "2028-03-01" })
            month = finance.month("2028-02")
            assertEquals(0L, month.person("O")!!.balance); assertEquals(0L, month.person("S")!!.balance)
            assertEquals(362000L, month.person("T")!!.balance); assertEquals(30000L, month.outstanding)
            assertEquals(0L, month.people.sumOf { it.reserved }); assertEquals(22780L, month.receivedSubsidy)
            assertEquals(6897380L, finance.cashBalance()); assertEquals(85000L, month.cashOut)
            assertEquals(155600L, finance.month("2028-03").person("S")!!.carryIn)
            assertEquals(6000001L, db.dao().allocations("2028-02").sumOf { it.amount })
            assertEquals("FULFILLED", db.dao().settlement(reserve.operationId)!!.status)
            assertEquals(5, db.dao().cashEntries().size)
            assertEquals(1, db.dao().receipts(ctx.traceId).size); assertEquals(1, db.dao().audits(ctx.traceId).size)
            val report = DebugReport.render(ctx.traceId, log.snapshot(), RoomEvidence("COMMITTED", 1, 1), 34, false)
            assertFalse(report.contains("Người mẫu")); assertFalse(report.contains("6000001")); assertFalse(report.contains("6.000.001"))
            val output = File(File(requireNotNull(System.getProperty("bep.export"))).parentFile, "finance-month-fixture.xlsx")
            output.outputStream().use { FinanceExcel.write(month, it, "2028-03-01T00:00:00Z") }
        } finally { db.close(); scope.cancel() }
    }
}
