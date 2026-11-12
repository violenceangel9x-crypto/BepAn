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
class HistoricalMealsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: KitchenDatabase
    private lateinit var repo: KitchenRepository
    private val date = "2026-11-12"
    @Before fun setup() = runBlocking {
        app.deleteDatabase("history-test.db")
        db = KitchenDatabase.open(app, "history-test.db")
        repo = KitchenRepository(db, TechnicalLog(File(app.cacheDir, id()), scope))
        repo.initialize(true)
    }
    @After fun close() { db.close(); scope.cancel() }
    private suspend fun save(kind: Kind, person: String = "", meal: String = "", enabled: Boolean = false, text: String = ""): Command {
        val command = Command(kind = kind, date = date, expectedVersion = repo.snapshot(date).day.version,
            personId = person, meal = meal, enabled = enabled, text = text)
        repo.commit(command, Context())
        return command
    }
    @Test fun closingGuestDoesNotRegisterOtherMeals() = runBlocking {
        val guest = save(Kind.GUEST, meal = "LUNCH", text = "Khách mẫu")
        save(Kind.CLOSE_DAY)
        val servings = repo.snapshot(date).servings.filter { it.person.id == "guest-${guest.operationId}" }
        assertTrue(servings.single { it.meal == Meal.LUNCH }.registered)
        assertFalse(servings.single { it.meal == Meal.BREAKFAST }.registered)
        assertFalse(servings.single { it.meal == Meal.DINNER }.registered)
        try {
            save(Kind.ADJUST_ATTENDANCE, "guest-${guest.operationId}", "BREAKFAST", true, "Điều chỉnh thử")
            fail("An unregistered meal must not become a paid serving")
        } catch (e: RuleViolation) { assertEquals("NOT_REGISTERED", e.code) }
        assertEquals(748800L, repo.snapshot(date).charge)
    }
    @Test fun cancelledGuestMealStaysRegisteredAfterClose() = runBlocking {
        val guest = save(Kind.GUEST, meal = "LUNCH", text = "Khách mẫu")
        save(Kind.ATTENDANCE, "guest-${guest.operationId}", "LUNCH")
        save(Kind.CLOSE_DAY)
        val lunch = repo.snapshot(date).servings.single { it.person.id == "guest-${guest.operationId}" && it.meal == Meal.LUNCH }
        assertTrue(lunch.registered); assertFalse(lunch.enabled)
    }
    @Test fun closedMealPreviewUsesSavedPriceEvenWhenRatesChange() = runBlocking {
        save(Kind.CLOSE_DAY)
        repo.dao.rate(Rate(date, lunch = 40000))
        val before = repo.snapshot(date)
        val serving = before.servings.single { it.person.id == "demo-00" && it.meal == Meal.LUNCH }
        assertEquals(28800L, serving.unitPrice)
        save(Kind.ADJUST_ATTENDANCE, "demo-00", "LUNCH", false, "Đối soát giá cũ")
        assertEquals(-serving.unitPrice, repo.snapshot(date).charge - before.charge)
    }
}
