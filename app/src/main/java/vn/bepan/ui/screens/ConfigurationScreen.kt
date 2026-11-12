@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vn.bepan.data.Rate
import vn.bepan.domain.*
import vn.bepan.ui.*
import vn.bepan.ui.components.*
import java.time.LocalDate

@Composable
fun ConfigurationScreen(state: UiState, vm: KitchenViewModel, back: () -> Unit) {
    var profile by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableStateOf("") }
    var choosing by remember { mutableStateOf(false) }
    val person = state.people.firstOrNull { it.id == selected }
    var name by rememberSaveable(selected) { mutableStateOf(person?.name.orEmpty()) }
    var group by rememberSaveable(selected) { mutableStateOf(person?.group ?: Group.OFFICER.name) }
    var active by rememberSaveable(selected) { mutableStateOf(true) }
    var effective by rememberSaveable { mutableStateOf(today()) }
    val validDate = runCatching { LocalDate.parse(effective) }.isSuccess
    val base = state.rates.lastOrNull() ?: Rate(today())
    var amounts by rememberSaveable { mutableStateOf(listOf(base.breakfast, base.lunch, base.dinner, base.riceFee,
        base.seasoning, base.fuel, base.subsidyPerKg, base.breakfastGrams, base.lunchGrams, base.dinnerGrams).map { it.toString() }) }
    val labels = listOf("Giá sáng (₫)", "Giá trưa (₫)", "Giá tối (₫)", "Gạo mỗi suất/bữa (₫)", "Gia vị mỗi suất/bữa (₫)",
        "Chất đốt mỗi suất/bữa (₫)", "Bù gạo chiến sĩ (₫/kg)", "Gạo sáng (g)", "Gạo trưa (g)", "Gạo tối (g)")
    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader("Người ăn & định mức", "Lưu lịch sử theo ngày hiệu lực", back)
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = profile, onClick = { profile = true }, label = { Text("Người ăn") })
            FilterChip(selected = !profile, onClick = { profile = false }, label = { Text("Định mức") })
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Section(if (profile) "Hồ sơ người ăn" else "Định mức có hiệu lực") {
                    if (profile) {
                        Box {
                            OutlinedButton(onClick = { choosing = true }) { Text(person?.name ?: "Thêm người mới") }
                            DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                                DropdownMenuItem(text = { Text("Thêm người mới") }, onClick = { selected = ""; choosing = false })
                                state.people.filter { it.group != Group.GUEST.name }.forEach { row ->
                                    DropdownMenuItem(text = { Text(row.name) }, onClick = { selected = row.id; choosing = false })
                                }
                            }
                        }
                        OutlinedTextField(name, { name = it.take(120) }, label = { Text("Họ tên") }, modifier = Modifier.fillMaxWidth(), colors = lightFieldColors())
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Group.OFFICER, Group.SOLDIER).forEach { value ->
                                FilterChip(selected = group == value.name, onClick = { group = value.name }, label = { Text(value.label) })
                            }
                        }
                        Row { Checkbox(checked = active, onCheckedChange = { active = it }); Text("Đang ăn từ ngày hiệu lực") }
                        if (selected.isNotBlank()) state.memberships.filter { it.personId == selected }.forEach {
                            Text("${it.effective} · ${Group.valueOf(it.group).label} · ${if (it.active) "Đang ăn" else "Ngừng ăn"}", color = MutedOnLight)
                        }
                    } else {
                        Text("Áp dụng cho ngày nháp. Ngày đã chốt giữ giá và nhóm trong sổ.", color = MutedOnLight)
                        labels.forEachIndexed { i, label ->
                            OutlinedTextField(amounts[i], { value -> amounts = amounts.toMutableList().also { it[i] = value } },
                                label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(), colors = lightFieldColors())
                        }
                        Text("Các mốc đã lưu: ${state.rates.joinToString { it.effective }}", color = MutedOnLight)
                    }
                    OutlinedTextField(effective, { effective = it }, label = { Text("Ngày hiệu lực YYYY-MM-DD") },
                        isError = !validDate, modifier = Modifier.fillMaxWidth(), colors = lightFieldColors())
                    if (state.error.isNotEmpty()) Text(state.error, color = ErrorLight)
                    Button(enabled = !state.busy && validDate && (if (profile) name.isNotBlank() else amounts.all { it.toLongOrNull() != null }), onClick = {
                        if (profile) vm.previewProfile(selected, name, effective, group, active)
                        else {
                            val v = amounts.map { it.toLong() }
                            vm.previewRate(Rate(effective, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9]))
                        }
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Xem trước cấu hình") }
                }
            }
        }
    }
    state.configurationPreview?.let { command ->
        AlertDialog(onDismissRequest = { if (!state.busy) vm.cancelConfiguration() }, title = { Text("Xác nhận cấu hình") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Hiệu lực ${command.effective}")
                if (command.rate == null) Text("${command.name}\n${Group.valueOf(command.group).label} · ${if (command.active) "Đang ăn" else "Ngừng ăn"}")
                else { val r = command.rate
                    Text("Sáng ${money(r.breakfast)} · Trưa ${money(r.lunch)} · Tối ${money(r.dinner)}")
                    Text("Phí: gạo ${money(r.riceFee)}, gia vị ${money(r.seasoning)}, chất đốt ${money(r.fuel)} mỗi suất/bữa")
                    Text("Bù gạo ${money(r.subsidyPerKg)}/kg · ${r.breakfastGrams}/${r.lunchGrams}/${r.dinnerGrams} g") }
                Text("Cập nhật ngày nháp từ mốc này; giữ điều chỉnh thủ công và sổ đã chốt.")
                if (state.error.isNotEmpty()) Text(state.error, color = MaterialTheme.colorScheme.error)
            } }, confirmButton = { Button(onClick = vm::confirmConfiguration, enabled = !state.busy) { Text(if (state.busy) "Đang lưu…" else "Xác nhận lưu") } },
            dismissButton = { TextButton(onClick = vm::cancelConfiguration, enabled = !state.busy) { Text("Hủy") } })
    }
}
