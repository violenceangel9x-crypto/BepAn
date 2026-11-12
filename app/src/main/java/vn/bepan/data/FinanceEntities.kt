package vn.bepan.data

import androidx.room.*

@Entity data class FinanceVersion(@PrimaryKey val id: Int = 1, val version: Long = 0)
@Entity data class CashOpening(@PrimaryKey val id: Int = 1, val date: String, val amount: Long)
@Entity(indices = [Index("date"), Index("period"), Index(value = ["sourceId"], unique = true), Index(value = ["purchaseId"], unique = true)],
    foreignKeys = [ForeignKey(entity = Purchase::class, parentColumns = ["id"], childColumns = ["purchaseId"], onDelete = ForeignKey.RESTRICT)])
data class CashEntry(@PrimaryKey val id: String, val kind: String, val date: String, val period: String,
    val amount: Long, val description: String, val recordedAt: String, val sourceId: String,
    val purchaseId: String? = null, val legacy: Boolean = false, val reversalOf: String = "")
@Entity(primaryKeys = ["entryId", "personId"], indices = [Index("personId"), Index("period")], foreignKeys = [
    ForeignKey(entity = CashEntry::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.RESTRICT)])
data class PaymentAllocation(val entryId: String, val personId: String, val period: String, val group: String, val amount: Long)
@Entity(indices = [Index("personId"), Index("fromPeriod"), Index("toPeriod"), Index("cashEntryId")], foreignKeys = [
    ForeignKey(entity = Person::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = CashEntry::class, parentColumns = ["id"], childColumns = ["cashEntryId"], onDelete = ForeignKey.RESTRICT)])
data class Settlement(@PrimaryKey val id: String, val personId: String, val fromPeriod: String, val toPeriod: String,
    val amount: Long, val status: String, val cashEntryId: String? = null)
@Entity data class FinanceDraft(@PrimaryKey val id: String, val command: String)
