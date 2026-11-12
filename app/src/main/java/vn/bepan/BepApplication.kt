package vn.bepan

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import vn.bepan.data.*
import vn.bepan.diagnostics.TechnicalLog
import java.io.File

val android.content.Context.settings by preferencesDataStore("settings")
class BepApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val log by lazy { TechnicalLog(File(noBackupFilesDir, "technical"), scope) }
    val apiKeyStore by lazy { vn.bepan.assistant.ApiKeyStore(this) }
    // Constructed independently of Room, so diagnostics can still open if Room fails.
    val repository by lazy { KitchenRepository(KitchenDatabase.open(this), log) }
}
