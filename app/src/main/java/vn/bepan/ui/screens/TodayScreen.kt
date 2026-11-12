@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vn.bepan.data.DaySnapshot
import vn.bepan.data.KitchenRepository
import vn.bepan.data.Person
import vn.bepan.data.Serving
import vn.bepan.domain.*
import vn.bepan.ui.*
import vn.bepan.ui.components.*
import vn.bepan.ui.dialogs.*

@Composable
fun TodayScreen(state: UiState, vm: KitchenViewModel) {
    var edit by remember { mutableStateOf<Pair<String, Meal>?>(null) }
    var editorVersion by remember { mutableLongStateOf(0) }
    var purchase by remember { mutableStateOf(false) }
    var guest by remember { mutableStateOf(false) }
    var schedulePerson by remember { mutableStateOf<Person?>(null) }
    var adjust by remember { mutableStateOf<Serving?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    val snap = state.snapshot

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("Bếp ăn", "Mỗi bữa ăn, một ngày chu toàn")
            DateHeader(
                date = state.date,
                isClosed = state.snapshot?.day?.closed == true,
                onDateChanged = vm::changeDate
            )
        }
        if (snap == null) {
            item {
                Text(
                    if (state.roomFailed) "Dữ liệu chưa sẵn sàng. Mở Cài đặt → Chẩn đoán."
                    else "Đang mở sổ tại thiết bị…",
                    color = MutedOnDark
                )
            }
        }
        if (snap != null) {
            item {
                Section(
                    title = "Ngân sách đi chợ",
                    subtitle = if (snap.day.closed) "Đã chốt · từ sổ ngày" else "Dự kiến · theo suất đang chọn"
                ) {
                    Text(money(snap.budget), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)

                    // Thanh tiến trình ngân sách trực quan
                    val progress = if (snap.budget > 0) (snap.bought.toFloat() / snap.budget.toFloat()).coerceIn(0f, 1f) else 0f
                    val isOverBudget = snap.budget < snap.bought
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isOverBudget) ErrorLight else Yellow,
                            trackColor = BorderLight,
                        )
                    }

                    HorizontalDivider(color = BorderLight)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ShoppingBag, null, tint = MutedOnLight, modifier = Modifier.size(18.dp))
                            Text("Đã mua", color = MutedOnLight, fontSize = 14.sp)
                        }
                        Text(money(snap.bought), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (isOverBudget) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                null,
                                tint = if (isOverBudget) ErrorLight else SuccessLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                if (isOverBudget) "Vượt ngân sách" else "Còn lại",
                                color = if (isOverBudget) ErrorLight else SuccessLight,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            money(kotlin.math.abs(snap.budget - snap.bought)),
                            color = if (isOverBudget) ErrorLight else SuccessLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    Surface(
                        color = SlotCardBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            Meal.entries.joinToString(" · ") { "${it.label} ${snap.count(it)} suất" },
                            fontSize = 13.sp,
                            color = MutedOnLight,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            item {
                Section("Thực đơn", "Sáng · Trưa · Tối · Sáng ngày kế tiếp") {
                    MenuCardContent(state.date, snap.menus) { date, meal ->
                        editorVersion = if (date == snap.day.date) snap.day.version else snap.nextVersion
                        edit = date to meal
                    }
                    HorizontalDivider(color = BorderLight)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LightAction(
                            label = if (exporting) "Đang tạo ảnh…" else "Sao chép ảnh thực đơn",
                            onClick = {
                                exporting = true
                                scope.launch {
                                    try {
                                        MenuImage.export(context, state.date, snap.menus, true)
                                        vm.feedback("Đã sao chép ảnh thực đơn")
                                    } catch (_: Exception) {
                                        vm.feedback("Chưa tạo được ảnh. Hãy rút ngắn nội dung và thử lại.", true)
                                    } finally {
                                        exporting = false
                                    }
                                }
                            },
                            enabled = !exporting
                        )
                        LightAction(
                            label = "Chia sẻ",
                            onClick = {
                                exporting = true
                                scope.launch {
                                    try {
                                        MenuImage.export(context, state.date, snap.menus, false)
                                    } catch (_: Exception) {
                                        vm.feedback("Chưa chia sẻ được ảnh", true)
                                    } finally {
                                        exporting = false
                                    }
                                }
                            },
                            enabled = !exporting
                        )
                    }
                }
            }
            item {
                Section("Chấm cơm", "Người thường xuyên được mặc định ba bữa. Chạm bữa để đổi suất.") {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Tìm người") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = lightFieldColors(),
                        singleLine = true
                    )
                    FlowRow {
                        TextButton(
                            onClick = { guest = true },
                            enabled = !snap.day.closed,
                            colors = ButtonDefaults.textButtonColors(contentColor = Purple),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Thêm khách")
                        }
                    }
                }
            }
            val people = snap.servings.map { it.person }.distinctBy { it.id }.filter { normalized(it.name).contains(normalized(search)) }
            items(people, key = { it.id }) { person ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Paper, contentColor = Ink),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar tròn chữ cái đầu
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        when (person.group) {
                                            Group.OFFICER.name -> Color(0xFFE8EEF5)
                                            Group.SOLDIER.name -> Color(0xFFE5F6ED)
                                            else -> Color(0xFFFFF7DB)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    person.name.takeLast(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = when (person.group) {
                                        Group.OFFICER.name -> Color(0xFF2C4A70)
                                        Group.SOLDIER.name -> Color(0xFF1B6B40)
                                        else -> Color(0xFF7A5C00)
                                    }
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(person.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                                Text(
                                    Group.valueOf(person.group).label,
                                    color = MutedOnLight,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(10.dp))
                        
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Meal.entries.forEach { meal ->
                                val serving = snap.servings.first { it.person.id == person.id && it.meal == meal }
                                FilterChip(
                                    selected = serving.enabled,
                                    onClick = {
                                        if (snap.day.closed) adjust = serving
                                        else vm.attendance(person.id, meal, !serving.enabled)
                                    },
                                    enabled = serving.registered,
                                    label = {
                                        Text("${meal.label} · ${if (!serving.registrationKnown) "Chưa xác định · sổ cũ" else if (serving.enabled) "Có ăn" else if (serving.registered) "Đã cắt" else "Chưa đăng ký"}")
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .semantics {
                                            contentDescription = "${person.name}, ${meal.label}"
                                            stateDescription = if (serving.enabled) "Có ăn" else if (serving.registered) "Đã cắt cơm" else "Chưa đăng ký"
                                        },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Yellow,
                                        selectedLabelColor = Ink,
                                        containerColor = SlotCardBg,
                                        labelColor = Ink
                                    )
                                )
                            }
                        }
                        
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (person.group != Group.GUEST.name) {
                                TextButton(
                                    onClick = { schedulePerson = person },
                                    enabled = !snap.day.closed,
                                    colors = ButtonDefaults.textButtonColors(contentColor = Purple),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Đặt lịch cắt / ăn lại")
                                }
                            }
                            if (!snap.day.closed) {
                                TextButton(
                                    onClick = {
                                        vm.preview(
                                            Kind.ATTENDANCE,
                                            person = person.id,
                                            meal = Meal.entries.joinToString(",") { it.name }
                                        )
                                    },
                                    enabled = snap.servings.any { it.person.id == person.id && it.enabled },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Purple),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cắt cả ngày")
                                }
                            }
                        }
                    }
                }
            }
            item {
                Section("Tiền mua", "Giao dịch được ghi quỹ ngay khi lưu") {
                    snap.purchases.forEach {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(it.description, color = Ink, fontWeight = FontWeight.Medium)
                            Text(money(it.amount), fontWeight = FontWeight.SemiBold, color = Ink)
                        }
                    }
                    if (snap.purchases.isEmpty()) {
                        Text("Chưa ghi khoản mua nào.", color = MutedOnLight)
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { purchase = true },
                        enabled = !snap.day.closed,
                        colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Ink),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text("Ghi tiền mua", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Button(
                    onClick = { vm.preview(Kind.CLOSE_DAY) },
                    enabled = !snap.day.closed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (snap.day.closed) DarkSurface else Yellow,
                        contentColor = Ink,
                        disabledContainerColor = Color(0xFF2C394A),
                        disabledContentColor = MutedOnDark
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Text(
                        if (snap.day.closed) "Ngày đã chốt" else "Chốt sổ và chuẩn bị ngày mai",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    if (purchase) {
        PurchaseDialog({ purchase = false }) { amount, text ->
            purchase = false
            vm.preview(Kind.PURCHASE, amount, text)
        }
    }
    if (guest) {
        GuestDialog({ guest = false }) { name, meals ->
            guest = false
            vm.preview(Kind.GUEST, text = name, meal = meals)
        }
    }
    schedulePerson?.let { person ->
        ScheduleDialog(person, state.date, { schedulePerson = null }) { meal, end, enabled ->
            schedulePerson = null
            vm.preview(Kind.SCHEDULE, person = person.id, meal = meal.name, end = end, enabled = enabled)
        }
    }
    edit?.let { (date, meal) ->
        MenuEditorDialog(
            date = date,
            meal = meal,
            initial = snap?.menus?.firstOrNull { it.date == date && it.meal == meal.name }?.content ?: "",
            busy = state.busy,
            error = state.error,
            onDismiss = { if (!state.busy) edit = null },
            onSave = { text -> vm.saveMenu(date, meal, text, editorVersion) { edit = null } }
        )
    }
    adjust?.let { serving ->
        AdjustAttendanceDialog(
            serving = serving,
            onDismiss = { adjust = null },
            onConfirm = { reason ->
                vm.preview(
                    Kind.ADJUST_ATTENDANCE,
                    person = serving.person.id,
                    meal = serving.meal.name,
                    enabled = !serving.enabled,
                    text = reason
                )
                adjust = null
            }
        )
    }
}
