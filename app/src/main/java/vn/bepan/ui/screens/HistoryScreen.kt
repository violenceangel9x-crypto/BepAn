@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.bepan.ui.*
import vn.bepan.ui.components.ScreenHeader
import vn.bepan.ui.components.Section

@Composable
fun HistoryScreen(state: UiState, back: () -> Unit) {
    var source by remember { mutableStateOf("ALL") }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Lịch sử hoạt động", "Lịch sử đã commit tại thiết bị", back)
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL" to "Tất cả", "MANUAL" to "Bạn", "ASSISTANT" to "Gemini").forEach { (value, label) ->
                FilterChip(
                    selected = source == value,
                    onClick = { source = value },
                    label = { Text(label, fontWeight = if (source == value) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Yellow,
                        selectedLabelColor = Ink,
                        containerColor = DarkSurface,
                        labelColor = Paper
                    )
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                state.audits.filter { source == "ALL" || it.source == source },
                key = { it.id }
            ) { row ->
                val isManual = row.source == "MANUAL"
                val authorTitle = if (isManual) "Bạn"
                else if (row.provider == "MOCK") "Gemini (mô phỏng)"
                else "Gemini"
                
                Section(authorTitle, row.timestamp) {
                    Surface(
                        color = if (isManual) Color(0xFFE8EEF5) else Color(0xFFEFE9FC),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (isManual) Icons.Default.Person else Icons.Default.AutoAwesome,
                                null,
                                tint = if (isManual) Color(0xFF2C4A70) else Purple,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "${row.kind} · ${row.target}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (isManual) Color(0xFF2C4A70) else Purple
                            )
                        }
                    }
                    if (row.before.isNotEmpty()) {
                        Text("Trước: ${row.before}", color = MutedOnLight, fontSize = 13.sp)
                    }
                    Text("Sau: ${row.after}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
            if (state.audits.none { source == "ALL" || it.source == source }) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chưa có lịch sử hoạt động phù hợp.", color = MutedOnDark, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}
