package vn.bepan

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.io.File

internal fun oldDatabase(context: Context, name: String, version: Int, insert: (SQLiteDatabase) -> Unit) {
    context.deleteDatabase(name)
    val path = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
    val schema = JSONObject(File(System.getProperty("bep.schemas"), "vn.bepan.data.KitchenDatabase/$version.json").readText()).getJSONObject("database")
    SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
            for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
        }
        val queries = schema.getJSONArray("setupQueries")
        for (i in 0 until queries.length()) db.execSQL(queries.getString(i))
        insert(db); db.version = version
    }
}
