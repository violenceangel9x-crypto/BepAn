package vn.bepan.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import vn.bepan.data.Person
import vn.bepan.domain.Meal
import java.time.LocalDate

@Composable
fun ScheduleDialog(
    person: Person,
    date: String,
    onDismiss: () -> Unit,
    onConfirm: (Meal, String, Boolean) -> Unit
) {
    var meal by remember { mutableStateOf(Meal.LUNCH) }
    var end by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lịch · ${person.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Bắt đầu $date. Kết thúc tính cả ngày cuối.")
                Meal.entries.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = meal == m, onClick = { meal = m })
                        Text(m.label)
                    }
                }
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("Ngày cuối YYYY-MM-DD · có thể trống") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                    Text("Ăn lại")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(meal, end, enabled) },
                enabled = end.isEmpty() || runCatching { LocalDate.parse(end) >= LocalDate.parse(date) }.getOrDefault(false)
            ) {
                Text("Xem trước")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
