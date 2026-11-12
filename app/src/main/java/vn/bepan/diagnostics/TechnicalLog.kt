package vn.bepan.diagnostics

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.json.JSONObject
import vn.bepan.domain.*
import java.io.File
import java.time.Instant

enum class EventCode {
    REQUEST_STARTED, GATEWAY_RETURNED, GATEWAY_TIMEOUT, GATEWAY_UNAVAILABLE,
    VALIDATION_REJECTED, CLARIFICATION_REQUIRED, PREVIEW_READY, CONFIRMATION_RECEIVED,
    PREVIEW_EXPIRED, USER_CANCELLED, READ_COMPLETED, ROOM_OPEN_FAILED,
    WRITE_STARTED, WRITE_COMMITTED, WRITE_ROLLED_BACK, IDEMPOTENT_REPLAY, OPERATION_ID_CONFLICT
}
enum class Reason { NONE, INVALID_ARGUMENT, DAY_CLOSED, STALE_VERSION, AUDIT_FAILED, DATABASE_UNAVAILABLE, NETWORK, TIMEOUT, CONFIGURATION, PROTOCOL, UNDO_CONFLICT, CANCELLED }
data class SafeEvent(
    val traceId: String, val code: EventCode, val source: Source, val provider: Provider,
    val kind: Kind? = null, val operationId: String = "", val reason: Reason = Reason.NONE,
    val timestamp: String = Instant.now().toString(), val eventId: String = id(), val requestId: String = "", val attempt: Int = 1
) {
    init {
        require(safeId(traceId) && (operationId.isEmpty() || safeId(operationId)) && safeId(eventId))
        require((requestId.isEmpty() || safeId(requestId)) && attempt in 1..1000)
        Instant.parse(timestamp)
    }
    fun json(): JSONObject = JSONObject().put("schemaVersion", 1).put("eventId", eventId)
        .put("level",if(reason == Reason.NONE) "INFO" else "WARN")
        .put("component",when { code.name.startsWith("GATEWAY") -> "GATEWAY"; code.name.startsWith("WRITE") || code == EventCode.ROOM_OPEN_FAILED -> "ROOM"; else -> "COMMAND" })
        .put("timestampUtc", timestamp).put("eventCode", code.name).put("source", source.name)
        .put("provider", provider.name).put("traceId", traceId).put("operationId", operationId)
        .put("actionType", kind?.name ?: "NONE").put("reasonCode", reason.name).put("requestId",requestId).put("attempt",attempt)
    companion object {
        fun safeId(value: String): Boolean = runCatching { java.util.UUID.fromString(value).toString() == value }.getOrDefault(false)
        fun parse(line: String): SafeEvent? = runCatching {
            val j = JSONObject(line)
            SafeEvent(j.getString("traceId"), EventCode.valueOf(j.getString("eventCode")), Source.valueOf(j.getString("source")), Provider.valueOf(j.getString("provider")),
                j.optString("actionType").takeIf { it != "NONE" }?.let { Kind.valueOf(it) }, j.optString("operationId"), Reason.valueOf(j.getString("reasonCode")), j.getString("timestampUtc"), j.getString("eventId"),j.optString("requestId"),j.optInt("attempt",1))
        }.getOrNull()
    }
}

class TechnicalLog(
    private val directory: File, scope: CoroutineScope,
    private val maxBytes: Long = 5 * 1024 * 1024, private val retentionMs: Long = 7 * 86400000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private sealed interface Work {
        data class Append(val event: SafeEvent): Work
        data class Snapshot(val result: CompletableDeferred<List<SafeEvent>>): Work
        data class Clear(val result: CompletableDeferred<Unit>): Work
    }
    private val queue = Channel<Work>(256)
    @Volatile var incomplete: Boolean = false; private set
    @Volatile var disabledForTest: Boolean = false
    init {
        scope.launch(Dispatchers.IO) {
            for (work in queue) {
                try {
                    when (work) {
                        is Work.Append -> {
                            if (disabledForTest) error("disabled")
                            directory.mkdirs()
                            var file = files().lastOrNull()
                            if (file == null || file.length() > minOf(256 * 1024, maxBytes / 2)) file = File(directory, "${now()}-${id()}.jsonl")
                            file.appendText(work.event.json().toString() + "\n", Charsets.UTF_8)
                            prune()
                        }
                        is Work.Snapshot -> { prune(); work.result.complete(files().flatMap { f -> f.useLines { lines -> lines.mapNotNull(SafeEvent::parse).toList() } }) }
                        is Work.Clear -> { files().forEach { check(it.delete()) }; work.result.complete(Unit) }
                    }
                } catch (_: Exception) {
                    incomplete = true
                    when (work) {
                        is Work.Snapshot -> work.result.complete(emptyList())
                        is Work.Clear -> work.result.complete(Unit)
                        else -> Unit
                    }
                }
            }
        }
    }
    fun record(ctx: vn.bepan.domain.Context, code: EventCode, kind: Kind? = null, operationId: String = "", reason: Reason = Reason.NONE) {
        if (!queue.trySend(Work.Append(SafeEvent(ctx.traceId, code, ctx.source, ctx.provider, kind, operationId, reason, requestId = ctx.requestId, attempt = ctx.attempt))).isSuccess) incomplete = true
    }
    suspend fun snapshot(): List<SafeEvent> { val result = CompletableDeferred<List<SafeEvent>>(); queue.send(Work.Snapshot(result)); return result.await() }
    suspend fun clear() { val result = CompletableDeferred<Unit>(); queue.send(Work.Clear(result)); result.await() }
    private fun files() = directory.listFiles()?.filter { it.extension == "jsonl" }?.sortedBy { it.name } ?: emptyList()
    private fun prune() {
        var bytes = files().sumOf { it.length() }
        files().forEach { file ->
            if (now() - file.lastModified() > retentionMs || bytes > maxBytes) {
                val size = file.length(); check(file.delete()); bytes -= size
            }
        }
    }
}
