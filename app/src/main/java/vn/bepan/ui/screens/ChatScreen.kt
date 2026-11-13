@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vn.bepan.assistant.CommandCodec
import vn.bepan.data.FinanceCommand
import vn.bepan.domain.Group
import vn.bepan.domain.Kind
import vn.bepan.domain.businessZone
import vn.bepan.ui.*
import vn.bepan.ui.components.LightAction
import vn.bepan.ui.components.ScreenHeader

@Composable
fun ChatScreen(
    state: UiState,
    vm: KitchenViewModel,
    listening: Boolean,
    voiceStatus: String,
    voice: () -> Unit,
    back: () -> Unit,
    diagnose: (String) -> Unit,
    openToday: () -> Unit,
    openFinance: () -> Unit = {}
) {
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var followLatest by remember { mutableStateOf(true) }
    val dragging by list.interactionSource.collectIsDraggedAsState()
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var attachment by rememberSaveable { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        attachment = uri?.lastPathSegment ?: ""
    }

    LaunchedEffect(list) {
        snapshotFlow { dragging to list.canScrollForward }.collect { (dragged, canScrollForward) ->
            if (dragged) followLatest = false
            else if (!canScrollForward) followLatest = true
        }
    }
    LaunchedEffect(state.chat.lastOrNull()?.id) {
        if (state.chat.lastOrNull()?.role == "USER") followLatest = true
    }
    LaunchedEffect(state.chat.lastOrNull()?.id, list.layoutInfo.viewportEndOffset, followLatest) {
        if (followLatest && state.chat.isNotEmpty()) list.animateScrollToItem(state.chat.lastIndex)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        if (keyboardOpen) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                }
                Text("Gemini", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    if (state.mock) "Mô phỏng AI" else "Gemini thật",
                    fontSize = 12.sp,
                    color = Cyan,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        } else {
            ScreenHeader("Gemini", "Trợ lý bếp ăn · ${if (state.mock) "Mô phỏng AI" else "Gemini thật"}", back)
        }
        Text(
            if (state.chatRoute == "FINANCE") "Đang xem kỳ ${state.financePeriod} · ${Group.valueOf(state.financeGroup).label}"
            else "Đang xem ${state.date} · ${if (state.snapshot?.day?.closed == true) "Đã chốt" else "Nháp"}",
            color = Cyan,
            fontSize = if (keyboardOpen) 12.sp else 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = if (keyboardOpen) 4.dp else 8.dp)
        )
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.chat.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Purple)
                                Text("Trợ lý sẵn sàng", fontWeight = FontWeight.Bold, color = Paper)
                            }
                            Text(
                                "Bạn cần chuẩn bị bữa nào?\nTôi dùng cùng sổ với màn Hôm nay.",
                                color = MutedOnDark,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            items(state.chat, key = { it.id }) { message ->
                val user = message.role == "USER"
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (user) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (user) Purple else Paper,
                            contentColor = if (user) Color.White else Ink
                        ),
                        shape = if (user) {
                            RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                        } else {
                            RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                        },
                        modifier = Modifier.widthIn(max = 340.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!user) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Purple, modifier = Modifier.size(14.dp))
                                    Text(
                                        if (message.provider == "MOCK") "Gemini · mô phỏng" else "Gemini",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Purple
                                    )
                                }
                            }
                            Text(message.text, fontSize = 15.sp, lineHeight = 22.sp)
                            Text(
                                runCatching {
                                    java.time.Instant.parse(message.timestamp).atZone(businessZone).toLocalTime().toString().take(5)
                                }.getOrDefault(""),
                                fontSize = 11.sp,
                                color = if (user) Color.White.copy(alpha = 0.7f) else MutedOnLight
                            )
                            if (message.error.isNotEmpty()) {
                                HorizontalDivider(color = BorderLight)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LightAction("Xem chẩn đoán", { diagnose(message.traceId) })
                                    LightAction("Thử lại lượt này", { vm.retry(message.traceId) }, !state.busy)
                                }
                            }
                            state.actions.firstOrNull { it.operationId == message.operationId }?.let { action ->
                                val financial = runCatching { FinanceCommand.decode(action.command) }.getOrNull()
                                val command = if (financial == null) runCatching { CommandCodec.decode(action.command) }.getOrNull() else null
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlotCardBg),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        when (action.status) {
                                            "PENDING" -> {
                                                Surface(
                                                    color = Color(0xFFFFF3CD),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        "Chờ xác nhận",
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF856404),
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Button(
                                                    onClick = { vm.confirm(action) },
                                                    enabled = !state.busy,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Ink),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(if (state.busy) "Đang lưu…" else "Xác nhận lưu", fontWeight = FontWeight.Bold)
                                                }
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    TextButton(
                                                        onClick = { vm.cancel(action) },
                                                        enabled = !state.busy,
                                                        colors = ButtonDefaults.textButtonColors(contentColor = Ink)
                                                    ) {
                                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Hủy")
                                                    }
                                                    TextButton(
                                                        onClick = { vm.cancel(action); vm.draft(message.text) },
                                                        enabled = !state.busy,
                                                        colors = ButtonDefaults.textButtonColors(contentColor = Purple)
                                                    ) {
                                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Chỉnh sửa")
                                                    }
                                                }
                                            }
                                            "SAVED" -> {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Default.CheckCircle, null, tint = SuccessLight, modifier = Modifier.size(18.dp))
                                                    Text("Đã lưu", color = SuccessLight, fontWeight = FontWeight.Bold)
                                                }
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    LightAction("Xem mục đã thay đổi", {
                                                        if (financial != null) { vm.changeFinancePeriod(financial.period); openFinance() }
                                                        else command?.let { vm.changeDate(it.date); openToday() }
                                                    })
                                                    if (command?.kind == Kind.ATTENDANCE) {
                                                        TextButton(
                                                            onClick = { vm.undo(action) },
                                                            enabled = !state.busy,
                                                            colors = ButtonDefaults.textButtonColors(contentColor = Purple)
                                                        ) {
                                                            Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Hoàn tác")
                                                        }
                                                    }
                                                }
                                            }
                                            "EXPIRED" -> Text("Hết hiệu lực · tạo lại yêu cầu", color = ErrorLight, fontWeight = FontWeight.Medium)
                                            "UNDONE" -> Text("Đã hoàn tác", color = SuccessLight, fontWeight = FontWeight.Medium)
                                            "CANCELLED" -> Text("Đã hủy", color = MutedOnLight)
                                            else -> Text("Chưa lưu", color = MutedOnLight)
                                        }
                                        LightAction("Xem chẩn đoán", { diagnose(action.traceId) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        state.choice?.let { choice ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Chọn người phù hợp:", color = Paper, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    choice.people.forEach { person ->
                        OutlinedButton(
                            onClick = { vm.choose(person.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Yellow)
                        ) {
                            Text("${person.name} · ${Group.valueOf(person.group).label}", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        if (list.canScrollForward) {
            TextButton(
                onClick = {
                    followLatest = true
                    scope.launch {
                        if (state.chat.isNotEmpty()) list.animateScrollToItem(state.chat.lastIndex)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Tin mới nhất", color = Yellow, fontWeight = FontWeight.Bold)
            }
        }
        if (state.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Cyan)
        }
        if (voiceStatus.isNotEmpty()) {
            Text(voiceStatus, color = Cyan, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }
        if (attachment.isNotEmpty()) {
            TextButton(onClick = { attachment = "" }) {
                Text("Đã chọn tệp tại máy · chưa gửi AI · Bỏ chọn", color = Cyan)
            }
        }
        if (!keyboardOpen) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Hôm nay có bao nhiêu suất trưa?",
                    "Cắt bữa tối hôm nay cho Nam",
                    "Ghi tiền mua rau hôm nay 85 nghìn",
                    "Chốt hôm nay và chuẩn bị ngày mai"
                ).forEach { suggestion ->
                    SuggestionChip(
                        onClick = { vm.draft(suggestion) },
                        label = { Text(suggestion) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = DarkSurface,
                            labelColor = Paper
                        )
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(DarkSurface).padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = { picker.launch(arrayOf("image/*", "application/pdf", "text/plain")) }) {
                Icon(Icons.Default.Add, "Chọn ảnh hoặc tệp tại máy", tint = MutedOnDark)
            }
            OutlinedTextField(
                value = state.draft,
                onValueChange = vm::draft,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhắn lệnh…") },
                maxLines = 5,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Paper,
                    unfocusedTextColor = Paper,
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = Color(0xFF384656),
                    cursorColor = Purple
                )
            )
            IconButton(onClick = voice) {
                Icon(
                    if (listening) Icons.Default.Stop else Icons.Default.Mic,
                    if (listening) "Dừng nhận dạng" else "Nhập bằng giọng nói tiếng Việt",
                    tint = if (listening) Color(0xFFFF806E) else MutedOnDark
                )
            }
            IconButton(
                onClick = vm::send,
                enabled = !state.busy && state.draft.isNotBlank()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (!state.busy && state.draft.isNotBlank()) Yellow else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        "Gửi tin nhắn",
                        tint = if (!state.busy && state.draft.isNotBlank()) Ink else Color(0xFF687585),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
