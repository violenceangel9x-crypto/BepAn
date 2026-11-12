package vn.bepan.data

import androidx.room.withTransaction
import vn.bepan.domain.*
import vn.bepan.diagnostics.*
import java.security.MessageDigest
import java.time.Instant

internal fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

data class BusinessChange(val target: String, val before: String, val after: String)

/** Shared atomic boundary for commands whose concurrency scope is not a meal day. */
class AuditedWriter(private val db: KitchenDatabase, private val log: TechnicalLog) {
    var auditFaultForTest = false
    suspend fun write(operationId: String, kind: Kind, date: String, canonical: String, expected: String,
        current: suspend () -> String, ctx: Context, mutation: suspend () -> BusinessChange): Receipt {
        val dao = db.dao()
        log.record(ctx, EventCode.WRITE_STARTED, kind, operationId)
        try {
            val (receipt, replay) = db.withTransaction {
                val hash = fingerprint(canonical)
                dao.receipt(operationId)?.let {
                    requireRule(it.fingerprint == hash, "OPERATION_ID_CONFLICT")
                    return@withTransaction it to true
                }
                requireRule(dao.action(operationId)?.status !in listOf("CANCELLED", "EXPIRED", "UNDONE"), "ACTION_EXPIRED")
                requireRule(expected == current(), "STALE_VERSION")
                val change = mutation()
                if (auditFaultForTest) throw RuleViolation("AUDIT_FAILED")
                val timestamp = Instant.now().toString()
                val version = dao.auditCount() + 1
                dao.audit(Audit(id(), operationId, traceId = ctx.traceId, timestamp = timestamp,
                    source = ctx.source.name, provider = ctx.provider.name, kind = kind.name,
                    target = change.target, before = change.before, after = change.after))
                val saved = Receipt(operationId, ctx.traceId, kind.name, hash, date, version, timestamp)
                dao.receipt(saved); dao.actionStatus(operationId, ActionStatus.SAVED.name)
                saved to false
            }
            log.record(ctx, if (replay) EventCode.IDEMPOTENT_REPLAY else EventCode.WRITE_COMMITTED, kind, operationId)
            return receipt
        } catch (e: Exception) {
            log.record(ctx, if ((e as? RuleViolation)?.code == "STALE_VERSION") EventCode.PREVIEW_EXPIRED else EventCode.WRITE_ROLLED_BACK,
                kind, operationId, if ((e as? RuleViolation)?.code == "AUDIT_FAILED") Reason.AUDIT_FAILED else Reason.INVALID_ARGUMENT)
            throw e
        }
    }
}
