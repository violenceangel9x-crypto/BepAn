@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import vn.bepan.domain.Meal
import java.time.LocalDate

@Composable
fun MenuEditorDialog(
    date: String,
    meal: Meal,
    initial: String,
    busy: Boolean,
    error: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    var suggestion by remember { mutableStateOf<String?>(null) }
    var replaceIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${meal.label} · $date") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Mỗi món một dòng. Magic Menu tạo gợi ý cho bữa này.")
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    label = { Text("Danh sách món") }
                )
                Text("Phạm vi gợi ý")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = replaceIndex == null,
                        onClick = { replaceIndex = null },
                        label = { Text("Cả bữa") }
                    )
                    text.lines().filter { it.isNotBlank() }.forEachIndexed { i, dish ->
                        FilterChip(
                            selected = replaceIndex == i,
                            onClick = { replaceIndex = i },
                            label = { Text(dish) }
                        )
                    }
                }
                TextButton(onClick = {
                    scope.launch {
                        suggestion = withContext(Dispatchers.IO) {
                            val json = JSONObject(
                                context.assets.open("menu-templates.v1.json").bufferedReader().use { it.readText() }
                            )
                            val array = json.getJSONArray("templates")
                            val dayClass = if (LocalDate.parse(date).dayOfWeek.value >= 6) "WEEKEND" else "WEEKDAY"
                            val templates = (0 until array.length())
                                .map { array.getJSONObject(it) }
                                .filter { it.getString("meal") == meal.name && it.getString("dayClass") == dayClass }
                            templates.randomOrNull()?.getJSONArray("items")?.let { items ->
                                val proposed = (0 until items.length()).map { items.getJSONObject(it).getString("name") }
                                val chosen = replaceIndex
                                if (chosen == null) proposed.joinToString("\n")
                                else text.lines().filter { it.isNotBlank() }.mapIndexed { i, line ->
                                    if (i == chosen && i < proposed.size) proposed[i] else line
                                }.joinToString("\n")
                            }
                        }
                    }
                }) {
                    Text("Magic Menu · gợi ý offline")
                }
                suggestion?.let { proposed ->
                    Text("Xem trước\n$proposed")
                    TextButton(onClick = {
                        text = proposed
                        suggestion = null
                    }) {
                        Text("Áp dụng vào bản nháp")
                    }
                    TextButton(onClick = { suggestion = null }) {
                        Text("Hủy gợi ý")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = !busy && text.length <= 16000
            ) {
                Text(if (busy) "Đang lưu…" else "Lưu bữa này")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Hủy")
            }
        }
    )
}
