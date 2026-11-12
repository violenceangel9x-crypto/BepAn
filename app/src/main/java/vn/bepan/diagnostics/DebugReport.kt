package vn.bepan.diagnostics

import vn.bepan.BuildConfig
import java.time.Instant
import vn.bepan.domain.id
import vn.bepan.data.KITCHEN_SCHEMA_VERSION

data class RoomEvidence(val outcome: String, val receipts: Int = 0, val audits: Int = 0)
object DebugReport {
    fun render(traceId: String, events: List<SafeEvent>, room: RoomEvidence, api: Int, incomplete: Boolean,
        steps: String = "", expected: String = "", actual: String = ""): String {
        require(SafeEvent.safeId(traceId))
        require(room.outcome in listOf("COMMITTED", "NOT_COMMITTED", "UNKNOWN"))
        val selected = events.filter { it.traceId == traceId }
        val retained = if (selected.size <= 200) selected else {
            (selected.take(1) + selected.filter { it.reason != Reason.NONE }.takeLast(100) + selected.takeLast(99)).distinctBy { it.eventId }.sortedBy { it.timestamp }
        }
        return buildString {
            appendLine("# Bếp ăn — Báo cáo chẩn đoán")
            appendLine("schemaVersion: 1\nreportId: ${id()}\nexportedAt: ${Instant.now()}\ntraceId: $traceId")
            appendLine("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nbuild: ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}\nAndroid API: $api")
            appendLine("Room schema: ${if (room.outcome == "UNKNOWN") "UNKNOWN" else KITCHEN_SCHEMA_VERSION.toString()}\nprovider: ${selected.firstOrNull()?.provider ?: "UNKNOWN"}")
            appendLine("events: ${retained.size}\nomitted: ${selected.size - retained.size}\nincomplete: ${incomplete || selected.isEmpty() || (room.outcome == "COMMITTED" && selected.none { it.code == EventCode.WRITE_COMMITTED })}")
            appendLine("\n## Mô tả đã xem trước")
            appendLine("Các bước: ${steps.take(2000)}\nMong đợi: ${expected.take(2000)}\nThực tế: ${actual.take(2000)}")
            appendLine("\n## Kết quả kiểm tra tại máy")
            appendLine("Room: ${room.outcome}\nReceipt count: ${room.receipts}\nAudit count: ${room.audits}")
            appendLine("\n## Dòng thời gian")
            retained.forEach { appendLine("- ${it.timestamp} | ${it.code} | ${it.source} | ${it.provider} | ${it.kind ?: "NONE"} | ${it.reason} | operationId=${it.operationId} | requestId=${it.requestId} | attempt=${it.attempt}") }
        }.also { check(it.toByteArray(Charsets.UTF_8).size <= 512 * 1024) }
    }
}
