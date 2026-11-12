package vn.bepan.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

const val KITCHEN_SCHEMA_VERSION = 6

@Entity(primaryKeys = ["personId", "effective"], foreignKeys = [ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.RESTRICT)])
data class MembershipPeriod(val personId: String, val effective: String, val group: String, val active: Boolean)

@Entity data class Person(@PrimaryKey val id: String, val name: String, val group: String, val starts: String, val ends: String = "")
@Entity data class Day(@PrimaryKey val date: String, val version: Long = 0, val closed: Boolean = false)
@Entity(primaryKeys = ["date", "personId", "meal"], indices = [Index("personId")])
data class MealOverride(val date: String, val personId: String, val meal: String, val enabled: Boolean)
@Entity(indices = [Index("personId")]) data class Schedule(@PrimaryKey val id: String, val personId: String, val meal: String, val start: String, val end: String, val enabled: Boolean)
@Entity(primaryKeys = ["date", "meal"]) data class Menu(val date: String, val meal: String, val content: String)
@Entity(indices = [Index("date")]) data class Purchase(@PrimaryKey val id: String, val date: String, val amount: Long, val description: String)
@Entity(primaryKeys = ["date", "personId", "meal"], indices = [Index("personId")])
data class ClosedMeal(val date: String, val personId: String, val meal: String, val group: String, val enabled: Boolean, val price: Long, val budget: Long, val subsidyMilli: Long, val budgetPerServing: Long, val subsidyPerServingMilli: Long,
    @ColumnInfo(defaultValue = "NULL") val registered: Boolean? = true)
@Entity(primaryKeys = ["date", "version"])
data class CloseRevision(val date: String, val version: Long, val reason: String, val snapshot: String, val operationId: String)
@Entity data class Rate(@PrimaryKey val effective: String, val breakfast: Long = 14400, val lunch: Long = 28800, val dinner: Long = 28800, val riceFee: Long = 3000, val seasoning: Long = 1000, val fuel: Long = 800, val subsidyPerKg: Long = 700, val breakfastGrams: Long = 136, val lunchGrams: Long = 272, val dinnerGrams: Long = 272)
@Entity(indices = [Index(value = ["operationId", "eventIndex"], unique = true), Index("traceId")])
data class Audit(@PrimaryKey val id: String, val operationId: String, val eventIndex: Int = 0, val traceId: String, val timestamp: String, val source: String, val provider: String, val kind: String, val target: String, val before: String, val after: String, val parentOperationId: String = "")
@Entity(indices = [Index("traceId")])
data class Receipt(@PrimaryKey val operationId: String, val traceId: String, val kind: String, val fingerprint: String, val date: String, val version: Long, val timestamp: String)
@Entity(indices = [Index("traceId")])
data class AssistantAction(@PrimaryKey val operationId: String, val traceId: String, val requestId: String, val provider: String, val command: String, val summary: String, val status: String = "PENDING")
@Entity data class ChatMessage(@PrimaryKey val id: String, val timestamp: String, val role: String, val text: String, val traceId: String, val operationId: String = "", val error: String = "",
    @ColumnInfo(defaultValue = "'NONE'") val provider: String = "NONE",
    @ColumnInfo(defaultValue = "''") val selectedDate: String = "",
    @ColumnInfo(defaultValue = "''") val referenceDate: String = "",
    @ColumnInfo(defaultValue = "1") val attempt: Int = 1)
@Entity data class Note(@PrimaryKey val id: String, val text: String, val pinned: Boolean = false, val updated: String)

@Dao interface KitchenDao {
    @Query("SELECT * FROM Person ORDER BY id") suspend fun people(): List<Person>
    @Query("SELECT * FROM Person ORDER BY id") fun observePeople(): Flow<List<Person>>
    @Upsert suspend fun person(value: Person)
    @Query("SELECT * FROM MembershipPeriod ORDER BY personId, effective") suspend fun memberships(): List<MembershipPeriod>
    @Insert suspend fun membership(value: MembershipPeriod)
    @Query("SELECT COUNT(*) FROM Audit") suspend fun auditCount(): Long
    @Query("SELECT version FROM FinanceVersion WHERE id=1") suspend fun financeVersion(): Long?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun ensureFinance(value: FinanceVersion)
    @Query("UPDATE FinanceVersion SET version=version+1 WHERE id=1") suspend fun bumpFinance()
    @Query("SELECT * FROM CashOpening WHERE id=1") suspend fun cashOpening(): CashOpening?
    @Insert suspend fun cashOpening(value: CashOpening)
    @Query("SELECT * FROM CashEntry ORDER BY date, recordedAt, id") suspend fun cashEntries(): List<CashEntry>
    @Query("SELECT * FROM CashEntry WHERE date>=:start AND date<:end ORDER BY date, recordedAt, id") suspend fun cashEntries(start: String, end: String): List<CashEntry>
    @Query("SELECT * FROM CashEntry WHERE period=:period ORDER BY date, recordedAt, id") suspend fun periodCash(period: String): List<CashEntry>
    @Insert suspend fun cashEntry(value: CashEntry)
    @Query("SELECT * FROM PaymentAllocation WHERE period=:period ORDER BY entryId,personId") suspend fun allocations(period: String): List<PaymentAllocation>
    @Insert suspend fun allocations(value: List<PaymentAllocation>)
    @Query("SELECT * FROM Settlement WHERE fromPeriod=:period OR toPeriod=:period ORDER BY id") suspend fun settlements(period: String): List<Settlement>
    @Query("SELECT * FROM Settlement WHERE id=:id") suspend fun settlement(id: String): Settlement?
    @Insert suspend fun settlement(value: Settlement)
    @Update suspend fun updateSettlement(value: Settlement)
    @Upsert suspend fun financeDraft(value: FinanceDraft)
    @Query("SELECT * FROM FinanceDraft ORDER BY rowid DESC") suspend fun financeDrafts(): List<FinanceDraft>
    @Query("DELETE FROM FinanceDraft WHERE id=:id") suspend fun deleteFinanceDraft(id: String)
    @Query("SELECT * FROM ClosedMeal WHERE date>=:start AND date<:end") suspend fun closedMeals(start: String, end: String): List<ClosedMeal>
    @Query("SELECT * FROM Day WHERE date>=:start AND date<:end ORDER BY date") suspend fun days(start: String, end: String): List<Day>
    @Query("SELECT * FROM Day WHERE date=:date") suspend fun day(date: String): Day?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun ensureDay(value: Day)
    @Update suspend fun updateDay(value: Day)
    @Query("SELECT * FROM Day ORDER BY date") fun observeDays(): Flow<List<Day>>
    @Query("SELECT * FROM Day ORDER BY date") suspend fun days(): List<Day>
    @Query("SELECT * FROM MealOverride") suspend fun overrides(): List<MealOverride>
    @Upsert suspend fun override(value: MealOverride)
    @Query("SELECT * FROM Schedule ORDER BY id") suspend fun schedules(): List<Schedule>
    @Insert suspend fun schedule(value: Schedule)
    @Update suspend fun updateSchedule(value: Schedule)
    @Query("SELECT * FROM Menu") suspend fun menus(): List<Menu>
    @Upsert suspend fun menu(value: Menu)
    @Query("SELECT * FROM Purchase ORDER BY date DESC") suspend fun purchases(): List<Purchase>
    @Insert suspend fun purchase(value: Purchase)
    @Query("SELECT * FROM ClosedMeal") suspend fun closedMeals(): List<ClosedMeal>
    @Insert suspend fun closeMeals(value: List<ClosedMeal>)
    @Update suspend fun updateClosedMeal(value: ClosedMeal)
    @Insert suspend fun closeRevision(value: CloseRevision)
    @Query("SELECT * FROM CloseRevision WHERE date=:date ORDER BY version") suspend fun revisions(date: String): List<CloseRevision>
    @Query("SELECT * FROM CloseRevision WHERE date>=:start AND date<:end ORDER BY date,version") suspend fun revisions(start: String, end: String): List<CloseRevision>
    @Query("SELECT * FROM Rate ORDER BY effective") suspend fun rates(): List<Rate>
    @Insert suspend fun rate(value: Rate)
    @Query("SELECT * FROM Audit ORDER BY timestamp DESC") fun observeAudits(): Flow<List<Audit>>
    @Query("SELECT * FROM Audit WHERE traceId=:trace") suspend fun audits(trace: String): List<Audit>
    @Insert suspend fun audit(value: Audit)
    @Query("SELECT * FROM Receipt WHERE operationId=:operationId") suspend fun receipt(operationId: String): Receipt?
    @Query("SELECT * FROM Receipt WHERE traceId=:trace") suspend fun receipts(trace: String): List<Receipt>
    @Insert suspend fun receipt(value: Receipt)
    @Upsert suspend fun action(value: AssistantAction)
    @Query("SELECT * FROM AssistantAction ORDER BY rowid") fun observeActions(): Flow<List<AssistantAction>>
    @Query("SELECT * FROM AssistantAction WHERE operationId=:id") suspend fun action(id: String): AssistantAction?
    @Query("UPDATE AssistantAction SET status=:status WHERE operationId=:id") suspend fun actionStatus(id: String, status: String)
    @Insert suspend fun chat(value: ChatMessage)
    @Query("SELECT * FROM ChatMessage ORDER BY timestamp, rowid") fun observeChat(): Flow<List<ChatMessage>>
    @Query("SELECT * FROM ChatMessage WHERE traceId=:trace AND role='USER' LIMIT 1") suspend fun userMessage(trace: String): ChatMessage?
    @Query("UPDATE ChatMessage SET attempt=:attempt WHERE id=:id") suspend fun chatAttempt(id: String, attempt: Int)
    @Query("SELECT * FROM AssistantAction WHERE requestId=:request LIMIT 1") suspend fun actionForRequest(request: String): AssistantAction?
    @Query("SELECT * FROM Note ORDER BY pinned DESC, updated DESC") fun observeNotes(): Flow<List<Note>>
    @Query("SELECT * FROM Note WHERE id=:id") suspend fun note(id: String): Note?
    @Upsert suspend fun note(value: Note)
}

@Database(entities = [FinanceVersion::class, CashOpening::class, CashEntry::class, PaymentAllocation::class, Settlement::class, FinanceDraft::class, MembershipPeriod::class, Person::class, Day::class, MealOverride::class, Schedule::class, Menu::class, Purchase::class, ClosedMeal::class, CloseRevision::class, Rate::class, Audit::class, Receipt::class, AssistantAction::class, ChatMessage::class, Note::class], version = KITCHEN_SCHEMA_VERSION, exportSchema = true)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun dao(): KitchenDao
    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Settlement ADD COLUMN parentId TEXT DEFAULT NULL REFERENCES Settlement(id) ON UPDATE NO ACTION ON DELETE RESTRICT")
                db.execSQL("CREATE INDEX index_Settlement_parentId ON Settlement(parentId)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS FinanceVersion (id INTEGER NOT NULL, version INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO FinanceVersion VALUES (1,0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS CashOpening (id INTEGER NOT NULL, date TEXT NOT NULL, amount INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS CashEntry (id TEXT NOT NULL, kind TEXT NOT NULL, date TEXT NOT NULL, period TEXT NOT NULL, amount INTEGER NOT NULL, description TEXT NOT NULL, recordedAt TEXT NOT NULL, sourceId TEXT NOT NULL, purchaseId TEXT, legacy INTEGER NOT NULL, reversalOf TEXT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(purchaseId) REFERENCES Purchase(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX index_CashEntry_date ON CashEntry(date)")
                db.execSQL("CREATE INDEX index_CashEntry_period ON CashEntry(period)")
                db.execSQL("CREATE UNIQUE INDEX index_CashEntry_sourceId ON CashEntry(sourceId)")
                db.execSQL("CREATE UNIQUE INDEX index_CashEntry_purchaseId ON CashEntry(purchaseId)")
                db.execSQL("INSERT INTO CashEntry SELECT p.id,'PURCHASE',COALESCE(date(r.timestamp,'+7 hours'),p.date),substr(p.date,1,7),-p.amount,p.description,COALESCE(r.timestamp,''),'purchase:'||p.id,p.id,CASE WHEN r.timestamp IS NULL THEN 1 ELSE 0 END,'' FROM Purchase p LEFT JOIN Receipt r ON r.operationId=p.id")
                db.execSQL("CREATE TABLE IF NOT EXISTS PaymentAllocation (entryId TEXT NOT NULL, personId TEXT NOT NULL, period TEXT NOT NULL, `group` TEXT NOT NULL, amount INTEGER NOT NULL, PRIMARY KEY(entryId,personId), FOREIGN KEY(entryId) REFERENCES CashEntry(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(personId) REFERENCES Person(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX index_PaymentAllocation_personId ON PaymentAllocation(personId)")
                db.execSQL("CREATE INDEX index_PaymentAllocation_period ON PaymentAllocation(period)")
                db.execSQL("CREATE TABLE IF NOT EXISTS Settlement (id TEXT NOT NULL, personId TEXT NOT NULL, fromPeriod TEXT NOT NULL, toPeriod TEXT NOT NULL, amount INTEGER NOT NULL, status TEXT NOT NULL, cashEntryId TEXT, PRIMARY KEY(id), FOREIGN KEY(personId) REFERENCES Person(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(cashEntryId) REFERENCES CashEntry(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                listOf("personId","fromPeriod","toPeriod","cashEntryId").forEach { db.execSQL("CREATE INDEX index_Settlement_$it ON Settlement($it)") }
                db.execSQL("CREATE TABLE IF NOT EXISTS FinanceDraft (id TEXT NOT NULL, command TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS MembershipPeriod (personId TEXT NOT NULL, effective TEXT NOT NULL, `group` TEXT NOT NULL, active INTEGER NOT NULL, PRIMARY KEY(personId,effective), FOREIGN KEY(personId) REFERENCES Person(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("INSERT INTO MembershipPeriod SELECT id, starts, `group`, 1 FROM Person")
                db.execSQL("INSERT INTO MembershipPeriod SELECT id, date(ends,'+1 day'), `group`, 0 FROM Person WHERE ends!=''")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ClosedMeal ADD COLUMN registered INTEGER DEFAULT NULL")
                // Disabled legacy guest meals have no reliable registration snapshot.
                db.execSQL("UPDATE ClosedMeal SET registered=1 WHERE `group` != 'GUEST' OR enabled=1")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ChatMessage ADD COLUMN provider TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE ChatMessage ADD COLUMN selectedDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ChatMessage ADD COLUMN referenceDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ChatMessage ADD COLUMN attempt INTEGER NOT NULL DEFAULT 1")
            }
        }
        fun open(context: Context, name: String = "kitchen.db"): KitchenDatabase =
            Room.databaseBuilder(context, KitchenDatabase::class.java, name).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
    }
}
