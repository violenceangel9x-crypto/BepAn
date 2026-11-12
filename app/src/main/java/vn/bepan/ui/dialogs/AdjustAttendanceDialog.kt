package vn.bepan.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import vn.bepan.data.Serving
import vn.bepan.domain.Meal

@Composable
fun AdjustAttendanceDialog(
    serving: Serving,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Điều chỉnh ngày đã chốt") },
        text = {
            Column {
                Text("${serving.person.name} · ${serving.meal.label}\n${if (serving.enabled) "Có ăn → Cắt cơm" else "Cắt cơm → Có ăn"}")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(500) },
                    label = { Text("Lý do điều chỉnh") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank()
            ) {
                Text("Xem trước chênh lệch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
