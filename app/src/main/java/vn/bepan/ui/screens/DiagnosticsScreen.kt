@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.bepan.BuildConfig
import vn.bepan.diagnostics.*
import vn.bepan.ui.*
import vn.bepan.ui.components.LightAction
import vn.bepan.ui.components.ScreenHeader
import vn.bepan.ui.components.Section

@Composable
fun DiagnosticsScreen(state: UiState, vm: KitchenViewModel, back: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    var preview by remember { mutableStateOf(false) }
    var steps by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var actual by remember { mutableStateOf("") }
    var report by remember { mutableStateOf("") }
    val context = LocalContext.current
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(report) }
                ?: error("EXPORT_FAILED")
        }
            .onSuccess { vm.feedback("Đã lưu báo cáo") }
            .onFailure { vm.feedback("Chưa lưu được. Nội dung xem trước vẫn còn.", true) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "AI Debug Center",
            "${if (state.mock) "Mô phỏng AI" else "Gemini thật"} · ${BuildConfig.VERSION_NAME} · Room ${if (state.evidence.outcome == "UNKNOWN") "chưa kiểm tra" else "v${vn.bepan.data.KITCHEN_SCHEMA_VERSION}"}",
            back
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Tìm mã theo dõi") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (SafeEvent.safeId(query)) {
                    TextButton(onClick = { vm.diagnostics(query) }) {
                        Text("Tra Room theo mã này")
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "Tất cả", "ERROR" to "Lỗi", "SAVED" to "Đã xử lý").forEach { (key, label) ->
                        FilterChip(
                            selected = filter == key,
                            onClick = { filter = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Yellow,
                                selectedLabelColor = Ink
                            )
                        )
                    }
                    TextButton(onClick = { vm.diagnostics() }) {
                        Text("Tải lại")
                    }
                }
                Text(
                    if (vm.log.incomplete) "Dòng thời gian có thể thiếu. Ưu tiên kết quả kiểm tra Room."
                    else "Log kỹ thuật: tối đa 7 ngày / 5 MiB.",
                    color = Cyan
                )
            }
            val traces = state.events.groupBy { it.traceId }.entries.toList().asReversed().filter { (trace, events) ->
                trace.contains(query, true) && when (filter) {
                    "ERROR" -> events.any { it.reason != Reason.NONE }
                    "SAVED" -> events.any { it.code == EventCode.WRITE_COMMITTED }
                    else -> true
                }
            }
            if (state.selectedTrace.isNotEmpty()) {
                item {
                    Section("Chi tiết lượt", state.selectedTrace) {
                        Text(
                            when (state.evidence.outcome) {
                                "COMMITTED" -> "Đã lưu theo Room"
                                "NOT_COMMITTED" -> "Chưa có receipt của lượt này"
                                else -> "Chưa kiểm tra được Room"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text("Receipt ${state.evidence.receipts} · Audit ${state.evidence.audits}")
                        val events = state.events.filter { it.traceId == state.selectedTrace }
                        if (events.isEmpty() || (state.evidence.outcome == "COMMITTED" && events.none { it.code == EventCode.WRITE_COMMITTED })) {
                            Text("Dòng thời gian kỹ thuật chưa đủ.", color = MutedOnLight)
                        }
                        events.takeLast(200).forEach {
                            Text("${it.code}\n${it.timestamp} · ${it.reason}", fontSize = 13.sp)
                        }
                        LightAction("Sao chép mã", {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                ClipData.newPlainText("Mã theo dõi", state.selectedTrace)
                            )
                        })
                        LightAction("Xuất báo cáo lỗi · xem trước", {
                            steps = ""
                            expected = ""
                            actual = ""
                            preview = true
                        })
                    }
                }
            }
            items(traces, key = { it.key }) { (trace, events) ->
                OutlinedButton(
                    onClick = { vm.diagnostics(trace) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(trace)
                        Text(
                            "${events.first().source} · ${events.first().provider} · ${events.last().code}",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            if (traces.isEmpty()) {
                item {
                    Text("Chưa có lượt phù hợp hoặc log đã hết hạn.", color = MutedOnDark)
                }
            }
        }
    }

    if (preview) {
        val generated = remember(state.selectedTrace, state.events, state.evidence, steps, expected, actual) {
            DebugReport.render(
                state.selectedTrace,
                state.events,
                state.evidence,
                Build.VERSION.SDK_INT,
                vm.log.incomplete,
                steps,
                expected,
                actual
            )
        }
        AlertDialog(
            onDismissRequest = { preview = false },
            title = { Text("Xem trước báo cáo đã lọc") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Dùng mô tả chung. Kiểm tra phần tự nhập trước khi lưu hoặc chia sẻ.")
                    OutlinedTextField(
                        value = steps,
                        onValueChange = { steps = it.take(2000) },
                        label = { Text("Các bước đã làm") }
                    )
                    OutlinedTextField(
                        value = expected,
                        onValueChange = { expected = it.take(2000) },
                        label = { Text("Mong đợi") }
                    )
                    OutlinedTextField(
                        value = actual,
                        onValueChange = { actual = it.take(2000) },
                        label = { Text("Thực tế") }
                    )
                    SelectionContainer {
                        Text(generated, fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        report = generated
                        save.launch("bep-an-debug-${java.util.UUID.randomUUID()}.md")
                    }) {
                        Text("Lưu file Markdown")
                    }
                    TextButton(onClick = {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                            ClipData.newPlainText("Báo cáo chẩn đoán", generated)
                        )
                    }) {
                        Text("Sao chép nội dung đã xem")
                    }
                    TextButton(onClick = {
                        runCatching {
                            val folder = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
                            val file = java.io.File(folder, "bep-an-debug-${java.util.UUID.randomUUID()}.md").apply {
                                writeText(generated, Charsets.UTF_8)
                            }
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.files",
                                file
                            )
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/markdown"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        clipData = ClipData.newUri(context.contentResolver, "Báo cáo", uri)
                                    },
                                    "Chia sẻ báo cáo đã xem"
                                )
                            )
                        }.onFailure { vm.feedback("Chưa chia sẻ được. Bạn có thể lưu hoặc sao chép.", true) }
                    }) {
                        Text("Chia sẻ file đã xem")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { preview = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}
