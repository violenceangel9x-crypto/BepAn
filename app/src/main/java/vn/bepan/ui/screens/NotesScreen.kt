@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.bepan.data.Note
import vn.bepan.domain.normalized
import vn.bepan.ui.*
import vn.bepan.ui.components.LightAction
import vn.bepan.ui.components.ScreenHeader
import vn.bepan.ui.components.Section
import vn.bepan.ui.components.lightFieldColors

@Composable
fun NotesScreen(state: UiState, vm: KitchenViewModel, history: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var edit by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Note?>(null) }
    var text by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("Ghi chú", "Những việc cần nhớ của bếp")
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Tìm ghi chú") },
                modifier = Modifier.fillMaxWidth(),
                colors = lightFieldColors(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        selected = null
                        text = ""
                        edit = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Ink)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Thêm ghi chú", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = history,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lịch sử hoạt động")
                }
            }
        }
        items(
            state.notes.filter { normalized(it.text).contains(normalized(search)) },
            key = { it.id }
        ) { note ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Paper, contentColor = Ink),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (note.pinned) Modifier.border(1.5.dp, Purple, RoundedCornerShape(20.dp))
                        else Modifier
                    )
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (note.pinned) "Đã ghim" else "Ghi chú",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (note.pinned) Purple else Ink
                        )
                        if (note.pinned) {
                            Icon(Icons.Default.PushPin, "Đã ghim", tint = Purple, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(note.text, fontSize = 15.sp, lineHeight = 22.sp, color = Ink)
                    HorizontalDivider(color = BorderLight)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LightAction("Sửa", {
                            selected = note
                            text = note.text
                            edit = true
                        })
                        LightAction(
                            if (note.pinned) "Bỏ ghim" else "Ghim",
                            { vm.note(note.text, note, !note.pinned) }
                        )
                    }
                }
            }
        }
        if (state.notes.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Chưa có ghi chú. Thêm một việc để bắt đầu.",
                        color = MutedOnDark,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (edit) {
        AlertDialog(
            onDismissRequest = { edit = false },
            title = { Text("Ghi chú") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 4,
                    label = { Text("Nội dung") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.note(text, selected)
                        edit = false
                    },
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Ink)
                ) {
                    Text("Lưu", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { edit = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
