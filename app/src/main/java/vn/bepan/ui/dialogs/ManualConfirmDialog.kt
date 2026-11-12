package vn.bepan.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import vn.bepan.data.DaySnapshot
import vn.bepan.data.KitchenRepository
import vn.bepan.data.Serving
import vn.bepan.domain.*

@Composable
fun ManualConfirmDialog(
    command: Command,
    snapshot: DaySnapshot?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text(if (command.kind == Kind.CLOSE_DAY) "Chốt sổ và chuẩn bị ngày mai" else "Xem trước thay đổi")
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Ngày ${command.date}")
                when (command.kind) {
                    Kind.PURCHASE -> {
                        Text("${command.text}\n${money(command.amount)}\nQuỹ giảm ngay khi lưu.")
                    }
                    Kind.CLOSE_DAY -> {
                        snapshot?.let { snap ->
                            Text(
                                "Sáng ${snap.count(Meal.BREAKFAST)} · Trưa ${snap.count(Meal.LUNCH)} · Tối ${snap.count(Meal.DINNER)}\n" +
                                        "Tiền ăn ${money(snap.charge)}\n" +
                                        "Ngân sách ${money(snap.budget)}\n" +
                                        "Đã mua ${money(snap.bought)}\n" +
                                        "Ngày này sẽ đóng góp vào sổ tháng đúng một lần."
                            )
                        }
                    }
                    Kind.SCHEDULE -> {
                        Text(
                            "${command.personId} · ${command.meal}\n" +
                                    "${command.date} → ${command.endDate.ifEmpty { "Không hạn" }}\n" +
                                    if (command.enabled) "Ăn lại" else "Cắt cơm"
                        )
                    }
                    Kind.ATTENDANCE, Kind.ADJUST_ATTENDANCE -> {
                        val rows = snapshot?.servings?.filter {
                            it.person.id == command.personId &&
                                    it.meal.name in command.meal.split(',') &&
                                    it.enabled != command.enabled
                        }.orEmpty()
                        val difference = rows.sumOf {
                            it.unitPrice
                        } * (if (command.enabled) 1L else -1L)
                        Text(
                            "${rows.firstOrNull()?.person?.name ?: command.personId}\n" +
                                    "${rows.joinToString { it.meal.label }} → ${if (command.enabled) "Có ăn" else "Cắt cơm"}\n" +
                                    "Chênh lệch tiền ăn: ${money(difference)}\n" +
                                    command.text
                        )
                    }
                    Kind.GUEST -> {
                        Text(
                            "${command.text}\n${command.meal} · Thu sau\n" +
                                    "Tiền ăn chỉ chính thức sau chốt ngày."
                        )
                    }
                    else -> Text(command.text)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !busy) {
                Text(if (busy) "Đang lưu…" else "Xác nhận lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Hủy")
            }
        }
    )
}
