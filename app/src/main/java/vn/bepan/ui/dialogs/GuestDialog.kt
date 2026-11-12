package vn.bepan.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import vn.bepan.domain.Meal

@Composable
fun GuestDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var meals by remember { mutableStateOf(setOf(Meal.LUNCH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm khách · Thu sau") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên khách") }
                )
                Meal.entries.forEach { meal ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = meal in meals,
                            onCheckedChange = { checked ->
                                meals = if (checked) meals + meal else meals - meal
                            }
                        )
                        Text(meal.label)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, meals.joinToString(",") { it.name }) },
                enabled = name.isNotBlank() && meals.isNotEmpty()
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
