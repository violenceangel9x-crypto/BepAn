package vn.bepan

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.bepan.data.KitchenDatabase
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], application=Application::class)
class MigrationTest {
    @Test fun fullV2LedgerAndEveryIntermediateFinanceSchemaPreserveOriginalRows() = runBlocking {
        val app=ApplicationProvider.getApplicationContext<Application>()
        for (version in 2..6) {
            val name="full-migration-$version.db"
            val original=linkedMapOf<String,Pair<List<String>,List<List<String?>>>>()
            oldDatabase(app,name,version) { old ->
                old.execSQL("INSERT INTO Person VALUES ('person','Người mẫu','SOLDIER','2020-01-01','2026-11-20')")
                old.execSQL("INSERT INTO Day VALUES ('2026-11-12',7,1)")
                old.execSQL("INSERT INTO MealOverride VALUES ('2026-11-12','person','DINNER',0)")
                old.execSQL("INSERT INTO Schedule VALUES ('schedule','person','LUNCH','2026-11-01','2026-11-03',0)")
                old.execSQL("INSERT INTO Menu VALUES ('2026-11-12','LUNCH','Canh rau mẫu')")
                old.execSQL("INSERT INTO Purchase VALUES ('purchase','2026-11-12',85000,'Khoản mẫu')")
                old.execSQL("INSERT INTO ClosedMeal (date,personId,meal,`group`,enabled,price,budget,subsidyMilli,budgetPerServing,subsidyPerServingMilli) VALUES ('2026-11-12','person','LUNCH','SOLDIER',1,28800,24000,190400,24000,190400)")
                old.execSQL("INSERT INTO CloseRevision VALUES ('2026-11-12',7,'Lý do mẫu','[{\"legacy\":true}]','close')")
                old.execSQL("INSERT INTO Rate VALUES ('2020-01-01',14400,28800,28800,3000,1000,800,700,136,272,272)")
                old.execSQL("INSERT INTO Receipt VALUES ('purchase','trace','PURCHASE','unchanged-hash','2026-11-12',7,'2026-11-13T01:00:00Z')")
                old.execSQL("INSERT INTO Audit VALUES ('audit','purchase',0,'trace','2026-11-13T01:00:00Z','MANUAL','NONE','PURCHASE','target','before','after','')")
                old.execSQL("INSERT INTO AssistantAction VALUES ('pending','trace-pending','request','MOCK','{}','Đề xuất mẫu','PENDING')")
                old.execSQL("INSERT INTO ChatMessage (id,timestamp,role,text,traceId,operationId,error) VALUES ('chat','2026-11-12T01:00:00Z','USER','Tin mẫu','trace-pending','','')")
                old.execSQL("INSERT INTO Note VALUES ('note','Ghi chú mẫu',1,'2026-11-12T01:00:00Z')")
                if (version>=4) old.execSQL("INSERT INTO MembershipPeriod VALUES ('person','2026-11-01','SOLDIER',1)")
                if (version>=5) {
                    old.execSQL("INSERT INTO FinanceVersion VALUES (1,13)")
                    old.execSQL("INSERT INTO CashOpening VALUES (1,'2026-11-01',1000000)")
                    old.execSQL("INSERT INTO CashEntry VALUES ('cash','COLLECTION','2026-11-13','2026-11',100000,'Thu mẫu','2026-11-13T01:00:00Z','source',NULL,0,'')")
                    old.execSQL("INSERT INTO PaymentAllocation VALUES ('cash','person','2026-11','SOLDIER',100000)")
                    old.execSQL("INSERT INTO Settlement (id,personId,fromPeriod,toPeriod,amount,status,cashEntryId) VALUES ('reserve','person','2026-11','',10000,'PENDING',NULL)")
                    old.execSQL("INSERT INTO FinanceDraft VALUES ('draft','{\"fixture\":true}')")
                }
                val schema=JSONObject(File(System.getProperty("bep.schemas"),"vn.bepan.data.KitchenDatabase/$version.json").readText()).getJSONObject("database").getJSONArray("entities")
                for (i in 0 until schema.length()) {
                    val table=schema.getJSONObject(i).getString("tableName")
                    old.rawQuery("SELECT * FROM `$table` ORDER BY rowid",null).use { c ->
                        val rows=mutableListOf<List<String?>>()
                        while(c.moveToNext()) rows.add((0 until c.columnCount).map { if(c.isNull(it)) null else c.getString(it) })
                        original[table]=c.columnNames.toList() to rows
                    }
                }
            }
            val db=KitchenDatabase.open(app,name)
            try {
                val sql=db.openHelper.readableDatabase
                original.forEach { (table,pair) ->
                    val columns=pair.first.joinToString(",") { "`$it`" }
                    sql.query("SELECT $columns FROM `$table` ORDER BY rowid").use { c ->
                        val rows=mutableListOf<List<String?>>()
                        while(c.moveToNext()) rows.add((0 until c.columnCount).map { if(c.isNull(it)) null else c.getString(it) })
                        // MembershipPeriod adds the old start/end history in v3→4; every original event remains.
                        assertTrue("v$version $table rows",rows.containsAll(pair.second))
                        if(table!="MembershipPeriod" && table!="CashEntry" && table!="FinanceVersion") assertEquals("v$version $table count",pair.second.size,rows.size)
                    }
                }
                sql.query("PRAGMA foreign_key_check").use { assertEquals(0,it.count) }
                assertEquals(1,db.dao().receipts("trace").size)
                assertEquals(1,db.dao().audits("trace").size)
                if(version<5) {
                    val cash=db.dao().cashEntries().single()
                    assertEquals(-85000L,cash.amount); assertEquals("2026-11-13",cash.date)
                    assertEquals("purchase",cash.purchaseId); assertFalse(cash.legacy)
                }
                assertEquals(7,sql.version)
            } finally { db.close() }
        }
    }
    @Test fun legacyGuestRegistrationUnknownWithoutChangingMoney() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        oldDatabase(app, "legacy-guests.db", 2) { old ->
            old.execSQL("INSERT INTO Person VALUES ('guest','Khách mẫu','GUEST','2026-11-12','2026-11-12')")
            old.execSQL("INSERT INTO ClosedMeal VALUES ('2026-11-12','guest','BREAKFAST','GUEST',0,14400,0,0,9600,0)")
            old.execSQL("INSERT INTO ClosedMeal VALUES ('2026-11-12','guest','LUNCH','GUEST',1,28800,24000,0,24000,0)")
            old.execSQL("INSERT INTO Purchase VALUES ('purchase','2026-11-12',85000,'Khoản mẫu')")
        }
        val db = KitchenDatabase.open(app, "legacy-guests.db")
        try {
            val rows = db.dao().closedMeals()
            assertNull(rows.single { it.meal == "BREAKFAST" }.registered)
            assertEquals(true, rows.single { it.meal == "LUNCH" }.registered)
            assertEquals(28800L, rows.filter { it.enabled }.sumOf { it.price })
            assertEquals(85000L, db.dao().purchases().sumOf { it.amount })
            assertEquals(-85000L, db.dao().cashEntries().single().amount)
            assertEquals("purchase", db.dao().cashEntries().single().purchaseId)
            assertTrue(db.dao().cashEntries().single().legacy)
        } finally { db.close() }
    }
    @Test fun migrateRealExportedV1PreservesPeopleAuditAndReceipt() = runBlocking {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val name="migration.db"; app.deleteDatabase(name)
        val path=app.getDatabasePath(name); path.parentFile!!.mkdirs()
        val schema=JSONObject(File(System.getProperty("bep.schemas"),"vn.bepan.data.KitchenDatabase/1.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path,null).use { old ->
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val entity=entities.getJSONObject(i); val table=entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                val indices=entity.optJSONArray("indices") ?: org.json.JSONArray()
                for(j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
            }
            val setup=schema.getJSONArray("setupQueries")
            for(i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO Person VALUES ('migration-person','Tên thử','SOLDIER','2026-01-01','')")
            old.execSQL("INSERT INTO ChatMessage VALUES ('message','2026-11-12T00:00:00Z','USER','Tin thử','trace','','')")
            old.execSQL("INSERT INTO Receipt VALUES ('operation','trace','ATTENDANCE','fingerprint','2026-11-12',1,'2026-11-12T00:00:00Z')")
            old.execSQL("INSERT INTO Audit VALUES ('audit','operation',0,'trace','2026-11-12T00:00:00Z','MANUAL','NONE','ATTENDANCE','target','Có ăn','Cắt cơm','')")
            old.version=1
        }
        val upgraded=KitchenDatabase.open(app,name)
        try {
            assertEquals("Tên thử",upgraded.dao().people().single().name)
            assertNotNull(upgraded.dao().receipt("operation"))
            assertEquals(1,upgraded.dao().audits("trace").size)
            assertEquals("NONE",upgraded.dao().userMessage("trace")!!.provider)
            assertEquals(vn.bepan.data.KITCHEN_SCHEMA_VERSION,upgraded.openHelper.readableDatabase.version)
        } finally { upgraded.close() }
    }
}
