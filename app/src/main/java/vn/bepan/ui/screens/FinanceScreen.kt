@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.screens

import androidx.compose.foundation.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.bepan.data.*
import vn.bepan.domain.*
import vn.bepan.ui.*
import vn.bepan.ui.components.*
import java.time.YearMonth

@Composable
fun FinanceScreen(state: UiState, vm: KitchenViewModel, openToday: () -> Unit = {}) {
    var group by rememberSaveable { mutableStateOf(Group.OFFICER.name) }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var table by rememberSaveable { mutableStateOf(false) }
    var detail by rememberSaveable { mutableStateOf("") }
    var showUnclosed by rememberSaveable { mutableStateOf(false) }
    var opening by rememberSaveable { mutableStateOf(false) }
    var openingAmount by rememberSaveable { mutableStateOf("") }
    var openingDate by rememberSaveable { mutableStateOf(today()) }
    var collecting by rememberSaveable { mutableStateOf(false) }
    var collectPeople by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var collectAmount by rememberSaveable { mutableStateOf("") }
    var collectDate by rememberSaveable { mutableStateOf(today()) }
    var collectNote by rememberSaveable { mutableStateOf("") }
    var settlementKind by rememberSaveable { mutableStateOf("") }
    var settlementPerson by rememberSaveable { mutableStateOf("") }
    var settlementId by rememberSaveable { mutableStateOf("") }
    var settlementAmount by rememberSaveable { mutableStateOf("") }
    var settlementDate by rememberSaveable { mutableStateOf(today()) }
    var targetPeriod by rememberSaveable { mutableStateOf(YearMonth.parse(state.financePeriod).plusMonths(1).toString()) }
    var receivingSubsidy by rememberSaveable { mutableStateOf(false) }
    var subsidyAmount by rememberSaveable { mutableStateOf("") }
    var subsidyDate by rememberSaveable { mutableStateOf(today()) }
    var exportPeriod by rememberSaveable { mutableStateOf(state.financePeriod) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(FinanceExcel.MIME)) { uri ->
        if (uri != null) vm.exportFinance(uri, exportPeriod)
    }
    val tableScroll = rememberScrollState()
    val ledger = state.ledger
    val rows = ledger?.people.orEmpty().filter { group in it.groups }.filter {
        when (filter) { "CUT" -> it.meals.any { row -> row.group == group && row.registered == true && !row.enabled }
            "DUE" -> it.balance < 0; "EXCESS" -> it.balance > 0; else -> true }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ScreenHeader("Tài chính", "Sổ tháng · số liệu từ các ngày đã chốt")
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { vm.changeFinancePeriod(YearMonth.parse(state.financePeriod).minusMonths(1).toString()) }) { Icon(Icons.Default.ChevronLeft, "Tháng trước") }
                Text("Tháng ${state.financePeriod}", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { vm.changeFinancePeriod(YearMonth.parse(state.financePeriod).plusMonths(1).toString()) }) { Icon(Icons.Default.ChevronRight, "Tháng sau") }
            }
        }
        if (ledger == null) item { Text("Đang mở sổ tháng tại thiết bị…") }
        else {
            state.lastFinanceReceipt?.let { receipt -> item { Section("Chứng từ đã lưu", "${receipt.kind} · ${receipt.date}") { Text(receipt.operationId, color = Ink) } } }
            item { Section("Tiền ăn tháng", "${ledger.closedDates.size} ngày đã chốt · còn ${ledger.unclosed.size} ngày đã tới chưa chốt · ${ledger.future.size} ngày tương lai") {
                Text(money(ledger.charge), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Đã nộp ${money(ledger.paid)} · Còn thiếu ${money(ledger.outstanding)} · Dư ${money(ledger.surplus)}", color = MutedOnLight)
                LightAction("Xem ngày chưa chốt", { showUnclosed = true })
                Button(onClick = { exportPeriod = ledger.period; export.launch("bep-an-doi-soat-${ledger.period}.xlsx") }, enabled = !state.exportingFinance) {
                    Text(if (state.exportingFinance) "Đang xuất Excel…" else "Xuất Excel đối soát tháng")
                }
                Text("Công nợ tạm tính theo ngày đã chốt và lịch sử thu đã nhập.", color = MutedOnLight)
            } }
            item { Section("Quỹ tiền thực tế", "Thu/chi tháng theo ngày nhận hoặc trả tiền") {
                Text("Thu ${money(ledger.cashIn)} · Chi ${money(ledger.cashOut)}", fontWeight = FontWeight.Bold)
                Text(ledger.cashBalance?.let { "Quỹ đến hiện tại: ${money(it)} · mốc đầu ${ledger.opening?.date}" }
                    ?: "Chưa xác lập số dư đầu quỹ; thu/chi tháng chưa phải số dư quỹ.", color = MutedOnLight)
                if (ledger.opening == null) LightAction("Xác lập số dư đầu quỹ", { opening = true })
                ledger.cash.forEach { entry -> Text("${entry.date} · ${entry.description} · ${money(entry.amount)}${if (entry.legacy) " · ngày cũ chưa đủ chứng từ" else ""}", color = Ink) }
            } }
            item { Section("Bù giá gạo · Chiến sĩ", "Quyền lợi theo ngày chốt; làm tròn một lần ở tổng kỳ") {
                Text("Được cấp ${money(ledger.subsidy)}", fontWeight = FontWeight.Bold)
                Text("Đã nhận ${money(ledger.receivedSubsidy)} · ${if (ledger.subsidyRemaining >= 0) "Còn chờ" else "Nhận vượt"} ${money(kotlin.math.abs(ledger.subsidyRemaining))}", color = MutedOnLight)
                LightAction("Ghi nhận bù gạo đã nhận", { subsidyAmount = ledger.subsidyRemaining.coerceAtLeast(0).toString(); receivingSubsidy = true })
                ledger.periodCash.filter { it.kind == "SUBSIDY_RECEIPT" }.forEach { Text("${it.date} · đã nhận ${money(it.amount)} · ${it.id.take(8)}", color = Ink) }
            } }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Group.entries.forEach { value -> FilterChip(selected = group == value.name, onClick = { group = value.name }, label = { Text(value.label) }) }
            } }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "Tất cả", "CUT" to "Có cắt cơm", "DUE" to "Còn thiếu", "EXCESS" to "Có dư").forEach { (key, label) ->
                    FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
                }
            } }
            item { Section("Sổ ${Group.valueOf(group).label}", "Tiền ăn nhóm ${money(ledger.people.sumOf { it.groupCharge(group) })} · công nợ mỗi người tính toàn kỳ") {
                LightAction(if (table) "Xem thẻ người" else "Xem bảng chấm cơm tháng", { table = !table })
                Button(onClick = { collectPeople = rows.map { it.person.id }; collectAmount = ""; collecting = true }, enabled = rows.isNotEmpty()) { Text("Thu tiền nhóm · chọn người") }
            } }
            if (rows.isEmpty()) item { Text("Chưa có người hoặc phát sinh phù hợp trong kỳ.", color = MutedOnDark) }
            if (table) {
                item { MonthTable(ledger, emptyList(), group, tableScroll, true) { detail = it } }
                items(rows, key = { it.person.id }) { row -> MonthTable(ledger, listOf(row), group, tableScroll, false) { detail = it } }
            }
            else items(rows, key = { it.person.id }) { row ->
                Section(row.person.name, "Tiền ăn nhóm ${money(row.groupCharge(group))} · ${row.cuts} bữa cắt trong toàn kỳ") {
                    Text("Tiền ăn cả kỳ ${money(row.charge)} · Đã nộp ${money(row.paid)}", color = Ink)
                    Text("${if (row.balance < 0) "Còn thiếu" else "Dư"} ${money(kotlin.math.abs(row.balance))}", color = if (row.balance < 0) ErrorLight else SuccessLight, fontWeight = FontWeight.Bold)
                    if (row.needsReview) Text("Cần đối soát phần đã giữ/quyết toán sau điều chỉnh.", color = ErrorLight)
                    LightAction("Chi tiết sổ · ${row.person.name}", { detail = row.person.id })
                    LightAction("Thu tiền · ${row.person.name}", { collectPeople = listOf(row.person.id); collectAmount = ""; collecting = true })
                    if (row.available > 0) FlowRow {
                        LightAction("Lập chờ hoàn", { settlementKind = Kind.RESERVE_REFUND.name; settlementPerson = row.person.id; settlementId = ""; settlementAmount = row.available.toString() })
                        LightAction("Cấn sang kỳ sau", { settlementKind = Kind.CARRY.name; settlementPerson = row.person.id; settlementId = ""; settlementAmount = row.available.toString(); targetPeriod = YearMonth.parse(ledger.period).plusMonths(1).toString() })
                    }
                }
            }
            val pending = ledger.settlements.filter { it.fromPeriod == ledger.period && it.status == "PENDING" }
            if (pending.isNotEmpty()) item { Section("Các khoản chờ hoàn", "Phần đã giữ chưa làm giảm quỹ") {
                pending.forEach { reservation ->
                    Text("${state.people.firstOrNull { it.id == reservation.personId }?.name ?: reservation.personId} · còn giữ ${money(reservation.remaining(ledger.settlements))}", color = Ink)
                    FlowRow {
                        LightAction("Ghi nhận đã trả · ${reservation.id.take(8)}", { settlementKind = Kind.REFUND.name; settlementPerson = reservation.personId; settlementId = reservation.id; settlementAmount = reservation.remaining(ledger.settlements).toString() })
                        LightAction("Hủy chờ hoàn · ${reservation.id.take(8)}", { vm.previewFinance(Kind.CANCEL_SETTLEMENT, people = listOf(reservation.personId), settlementId = reservation.id) })
                    }
                }
            } }
        }
    }
    ledger?.person(detail)?.let { person ->
        AlertDialog(onDismissRequest = { detail = "" }, title = { Text(person.person.name) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kỳ ${ledger.period} · dự kiến tham khảo ${money(person.projected)}")
                Text("Tiền ăn đã chốt ${money(person.charge)}\nĐã nộp ${money(person.paid)}\nCấn vào ${money(person.carryIn)} · cấn ra ${money(person.carryOut)}\nĐã hoàn ${money(person.refunded)} · giữ chờ ${money(person.reserved)}\nDư khả dụng ${money(person.available)}")
                person.meals.groupBy { it.date }.forEach { (date, meals) ->
                    TextButton(onClick = { vm.changeDate(date); detail = ""; openToday() }) { Text("$date · mở sổ ngày") }
                    Text("$date · ${meals.joinToString { "${Meal.valueOf(it.meal).label}: ${mealStatus(it)} (${Group.valueOf(it.group).label})" }}")
                    Text("Chính thức: ${money(meals.filter { it.enabled }.sumOf { it.price })}")
                    ledger.revisions.filter { it.date == date }.forEach { Text("Phiên ${it.version}: ${it.reason}") }
                }
                if (person.meals.isEmpty()) Text("Chưa có ngày đã chốt; tiền ăn chính thức là 0₫.")
                ledger.allocations.filter { it.personId == person.person.id }.forEach { Text("Đã nộp ${money(it.amount)} · chứng từ ${it.entryId.take(8)}") }
            }
        }, confirmButton = { TextButton(onClick = { detail = "" }) { Text("Đóng") } })
    }
    if (showUnclosed && ledger != null) AlertDialog(onDismissRequest = { showUnclosed = false }, title = { Text("Ngày đã tới chưa chốt") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { if (ledger.unclosed.isEmpty()) Text("Không có ngày đang chờ chốt.")
            ledger.unclosed.forEach { date -> TextButton(onClick = { vm.changeDate(date); showUnclosed = false; openToday() }) { Text("$date · mở dữ liệu Hôm nay") } } } },
        confirmButton = { TextButton(onClick = { showUnclosed = false }) { Text("Đóng") } })
    if (opening) AlertDialog(onDismissRequest = { opening = false }, title = { Text("Số dư đầu quỹ") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Số tiền có trước các giao dịch của ngày mốc. Giao dịch trước ngày mốc được loại khỏi phép tính quỹ hiện tại.")
            OutlinedTextField(openingAmount, { openingAmount = it }, label = { Text("Số dư đầu quỹ nguyên đồng") })
            OutlinedTextField(openingDate, { openingDate = it }, label = { Text("Ngày bắt đầu YYYY-MM-DD") })
        }
    }, confirmButton = { Button(enabled = (openingAmount.toLongOrNull() ?: -1) in 0..1_000_000_000L && runCatching { java.time.LocalDate.parse(openingDate) }.isSuccess,
        onClick = { vm.previewFinance(Kind.OPENING_BALANCE, openingAmount.toLong(), cashDate = openingDate); opening = false }) { Text("Xem trước") } },
        dismissButton = { TextButton(onClick = { opening = false }) { Text("Hủy") } })
    if (collecting && ledger != null) AlertDialog(onDismissRequest = { collecting = false }, title = { Text("Thu tiền ăn · kỳ ${ledger.period}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Chọn người để chia đều. Phân đồng lẻ theo ID cố định; xem từng phần trước khi lưu.")
            ledger.people.filter { group in it.groups }.forEach { row -> Row {
                Checkbox(checked = row.person.id in collectPeople, onCheckedChange = { checked -> collectPeople = if (checked) collectPeople + row.person.id else collectPeople - row.person.id })
                Text(row.person.name)
            } }
            OutlinedTextField(collectAmount, { collectAmount = it }, label = { Text("Tổng tiền thu nguyên đồng") })
            OutlinedTextField(collectDate, { collectDate = it }, label = { Text("Ngày nhận tiền YYYY-MM-DD") })
            OutlinedTextField(collectNote, { collectNote = it }, label = { Text("Nội dung thu") })
            Text("Ngày nhận độc lập kỳ ăn. Nếu nhập lịch sử, chọn đúng ngày tiền thực tế đã nhận.")
        }
    }, confirmButton = { Button(enabled = (collectAmount.toLongOrNull() ?: 0) in 1..1_000_000_000L && collectPeople.isNotEmpty() && runCatching { java.time.LocalDate.parse(collectDate) }.isSuccess,
        onClick = { vm.previewFinance(Kind.COLLECTION, collectAmount.toLong(), collectPeople, text = collectNote, cashDate = collectDate, group = group); collecting = false }) { Text("Xem trước phân bổ") } },
        dismissButton = { TextButton(onClick = { collecting = false }) { Text("Hủy") } })
    if (settlementKind.isNotBlank() && ledger != null) {
        val kind = Kind.valueOf(settlementKind)
        AlertDialog(onDismissRequest = { settlementKind = "" }, title = { Text(when (kind) { Kind.CARRY -> "Cấn sang kỳ sau"; Kind.REFUND -> "Ghi nhận tiền đã trả"; else -> "Lập chờ hoàn" }) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.people.firstOrNull { it.id == settlementPerson }?.name.orEmpty())
                ledger.person(settlementPerson)?.let { Text("Đối chiếu ${money(it.balance)} · đang giữ ${money(it.reserved)} · khả dụng ${money(it.available)}") }
                OutlinedTextField(settlementAmount, { settlementAmount = it }, label = { Text("Số tiền quyết toán nguyên đồng") })
                if (kind == Kind.CARRY) OutlinedTextField(targetPeriod, { targetPeriod = it }, label = { Text("Kỳ đến YYYY-MM") })
                if (kind == Kind.REFUND) OutlinedTextField(settlementDate, { settlementDate = it }, label = { Text("Ngày đã trả tiền YYYY-MM-DD") })
                Text(if (kind == Kind.REFUND) "Có thể ghi nhận trả từng phần của khoản đang giữ. Quỹ giảm khi xác nhận đã trả." else "Chờ hoàn hoặc cấn không tạo dòng tiền thu/chi thực tế.")
            }
        }, confirmButton = { Button(enabled = (settlementAmount.toLongOrNull() ?: 0) in 1..1_000_000_000L, onClick = {
            vm.previewFinance(kind, settlementAmount.toLong(), listOf(settlementPerson), targetPeriod = if (kind == Kind.CARRY) targetPeriod else "",
                settlementId = settlementId, cashDate = if (kind == Kind.REFUND) settlementDate else today()); settlementKind = ""
        }) { Text("Xem trước quyết toán") } }, dismissButton = { TextButton(onClick = { settlementKind = "" }) { Text("Hủy") } })
    }
    if (receivingSubsidy && ledger != null) AlertDialog(onDismissRequest = { receivingSubsidy = false }, title = { Text("Bù gạo đã nhận · kỳ ${ledger.period}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Được cấp ${money(ledger.subsidy)} · đã nhận ${money(ledger.receivedSubsidy)}. Ngày nhận có thể ở tháng sau kỳ bù.")
            OutlinedTextField(subsidyAmount, { subsidyAmount = it }, label = { Text("Tiền bù gạo thực nhận nguyên đồng") })
            OutlinedTextField(subsidyDate, { subsidyDate = it }, label = { Text("Ngày nhận bù gạo YYYY-MM-DD") })
        }
    }, confirmButton = { Button(enabled = (subsidyAmount.toLongOrNull() ?: 0) in 1..1_000_000_000L && runCatching { java.time.LocalDate.parse(subsidyDate) }.isSuccess,
        onClick = { vm.previewFinance(Kind.SUBSIDY_RECEIPT, subsidyAmount.toLong(), cashDate = subsidyDate); receivingSubsidy = false }) { Text("Xem trước nhận bù gạo") } },
        dismissButton = { TextButton(onClick = { receivingSubsidy = false }) { Text("Hủy") } })
    state.financePreview?.let { command ->
        AlertDialog(onDismissRequest = { if (!state.busy) vm.cancelFinance() }, title = { Text("Xem trước tài chính") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Kỳ ${command.period}\nNgày quỹ ${command.cashDate}\n${money(command.amount)}")
                if (command.kind == Kind.OPENING_BALANCE) Text("Xác lập số dư đầu; không tạo khoản vừa thu. Chỉ cộng giao dịch từ ngày mốc này.")
                if (command.kind == Kind.COLLECTION) {
                    Text("Quỹ tăng ${money(command.amount)}; từng số đối chiếu kỳ ${command.period} tăng theo phân bổ:")
                    command.shares.forEach { share -> Text("${state.people.firstOrNull { it.id == share.personId }?.name ?: share.personId} · ${Group.valueOf(share.group).label}: ${money(share.amount)}") }
                }
                if (command.kind == Kind.SUBSIDY_RECEIPT) {
                    Text("Quỹ ngày ${command.cashDate} tăng ${money(command.amount)}; ghi nhận đã nhận cho kỳ ${command.period}. Tiền nộp cá nhân giữ nguyên.")
                    if (ledger?.period == command.period && command.amount > ledger.subsidyRemaining)
                        Text("Số nhận vượt khoản còn chờ ${money(command.amount - ledger.subsidyRemaining)}; chênh lệch sẽ hiện để đối soát.", color = MaterialTheme.colorScheme.error)
                }
                if (command.kind in listOf(Kind.RESERVE_REFUND, Kind.REFUND, Kind.CANCEL_SETTLEMENT, Kind.CARRY)) {
                    Text(command.people.joinToString { id -> state.people.firstOrNull { it.id == id }?.name ?: id })
                    Text(when (command.kind) { Kind.RESERVE_REFUND -> "Giữ ${money(command.amount)} chờ hoàn; quỹ giữ nguyên."
                        Kind.REFUND -> "Đã trả ${money(command.amount)}: giảm quỹ và phần dư, tiêu thụ khoản giữ ${command.settlementId.take(8)}."
                        Kind.CANCEL_SETTLEMENT -> "Hủy phần chờ còn lại, giải phóng số dư; không đổi các khoản đã trả."
                        else -> "Cấn ${command.period} → ${command.targetPeriod}, giảm dư kỳ đi và tăng số đối chiếu kỳ đến; quỹ giữ nguyên." })
                }
                if (state.error.isNotEmpty()) Text(state.error, color = MaterialTheme.colorScheme.error)
            } }, confirmButton = { Button(onClick = vm::confirmFinance, enabled = !state.busy) { Text(if (state.busy) "Đang lưu…" else "Xác nhận tài chính") } },
            dismissButton = { TextButton(onClick = vm::cancelFinance, enabled = !state.busy) { Text("Hủy") } })
    }
}
private fun mealStatus(row: ClosedMeal): String = when { row.registered == null -> "Chưa xác định · sổ cũ"; row.enabled -> "Có ăn"; row.registered -> "Cắt cơm"; else -> "Chưa đăng ký" }

@Composable
private fun MonthTable(ledger: FinanceMonth, people: List<PersonMonth>, group: String, scroll: ScrollState, header: Boolean, detail: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Paper).padding(8.dp)) {
        if (header) Row { Text("Họ tên", color = Ink, modifier = Modifier.width(120.dp)); Row(Modifier.horizontalScroll(scroll)) {
            ledger.calendar.forEach { Text("${it.takeLast(2)}\nS · T · C", color = Ink, modifier = Modifier.width(156.dp)) }
        } }
        people.forEach { person ->
            HorizontalDivider(color = BorderLight)
            Row(Modifier.height(IntrinsicSize.Min)) {
                TextButton(onClick = { detail(person.person.id) }, modifier = Modifier.width(120.dp)) { Text(person.person.name, color = Purple) }
                Row(Modifier.horizontalScroll(scroll)) {
                    ledger.calendar.forEach { date -> Row(Modifier.width(156.dp)) { Meal.entries.forEach { meal ->
                        val row = person.meals.firstOrNull { it.date == date && it.meal == meal.name && it.group == group }
                        val label = if (date !in ledger.closedDates) "Nháp" else row?.let { mealStatus(it) } ?: "—"
                        TextButton(onClick = { detail(person.person.id) }, modifier = Modifier.width(52.dp).heightIn(min = 48.dp)
                            .semantics { contentDescription = "${person.person.name}, $date, ${meal.label}, $label" }) {
                            Text(when(label) { "Có ăn" -> "Ăn"; "Cắt cơm" -> "Cắt"; "Chưa đăng ký" -> "—"; else -> label }, color = Ink)
                        }
                    } } }
                }
            }
        }
    }
}
