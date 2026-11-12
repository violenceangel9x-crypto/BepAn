package vn.bepan.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.bepan.domain.RuleViolation
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The API key never enters Room, ordinary preferences, backup, or diagnostic state. */
class ApiKeyStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "credentials/gemini.bin"))
    private val lock = Any()
    private val alias = "bepan.gemini.api-key.v1"
    private val aad = "bepan:gemini-api-key:v1".toByteArray(Charsets.UTF_8)

    suspend fun read(): String? = withContext(Dispatchers.IO) { synchronized(lock) {
        val input = try { file.openRead() } catch (_: FileNotFoundException) { return@synchronized null }
        val bytes = input.use { it.readBytesBounded(8192) }
        check(bytes.size > 30 && bytes[0] == 1.toByte() && bytes[1] == 12.toByte())
        val key = keyStore().getKey(alias, null) as? SecretKey ?: throw RuleViolation("CREDENTIAL_UNAVAILABLE")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(2, 14)))
        cipher.updateAAD(aad)
        cipher.doFinal(bytes.copyOfRange(14, bytes.size)).toString(Charsets.UTF_8)
    } }

    suspend fun save(value: String) = withContext(Dispatchers.IO) { synchronized(lock) {
        val trimmed = value.trim()
        if (!validInput(trimmed)) throw RuleViolation("INVALID_API_KEY_INPUT")
        val store = keyStore()
        val key = store.getKey(alias, null) as? SecretKey ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
            generateKey()
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(aad)
        check(cipher.iv.size == 12)
        val bytes = byteArrayOf(1, 12) + cipher.iv + cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
        check(file.baseFile.parentFile!!.let { it.isDirectory || it.mkdirs() })
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) }
        catch (error: Exception) { file.failWrite(output); throw error }
    } }

    suspend fun clear() = withContext(Dispatchers.IO) { synchronized(lock) {
        file.delete()
        keyStore().deleteEntry(alias)
    } }

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    companion object {
        fun validInput(value: String) = value.length in 16..4096 && value.all { it.code in 33..126 }
    }
}

internal fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (output.size() <= limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit + 1 - output.size()))
        if (count < 0) break
        output.write(buffer, 0, count)
    }
    if (output.size() > limit) throw RuleViolation("PROTOCOL")
    return output.toByteArray()
}
