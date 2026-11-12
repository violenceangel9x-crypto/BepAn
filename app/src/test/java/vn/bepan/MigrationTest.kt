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
