@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.bepan.domain.*
import vn.bepan.ui.*
import vn.bepan.ui.components.ScreenHeader
import vn.bepan.ui.components.Section
import java.time.YearMonth

@Composable
fun FinanceScreen(state: UiState) {
    var period by rememberSaveable { mutableStateOf(state.date.take(7)) }
    var group by remember { mutableStateOf(Group.OFFICER) }
    val closed = state.finance.filter { it.date.startsWith(period) }
    val charges = closed.filter { it.enabled }.sumOf { it.price }
    val subsidy = Accounting.roundSubsidy(closed.sumOf { it.subsidyMilli })

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("Tài chính", "Số chính thức từ những ngày đã chốt")
            
            // Bộ chọn tháng dạng Card nổi
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(onClick = { period = YearMonth.parse(period).minusMonths(1).toString() }) {
                        Icon(Icons.Default.ChevronLeft, "Tháng trước", tint = Yellow)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Tháng $period",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Paper
                        )
                        Text(
                            "${closed.map { it.date }.distinct().size} ngày đã chốt sổ",
                            fontSize = 12.sp,
                            color = Cyan
                        )
                    }
                    IconButton(onClick = { period = YearMonth.parse(period).plusMonths(1).toString() }) {
                        Icon(Icons.Default.ChevronRight, "Tháng kế tiếp", tint = Yellow)
                    }
                }
            }
        }
        item {
            Section(
                title = "Tiền ăn đã chốt",
                subtitle = "${closed.map { it.date }.distinct().size}/${YearMonth.parse(period).lengthOfMonth()} ngày đã chốt"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFFF3CD), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Paid, null, tint = Color(0xFF856404), modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text(money(charges), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Tổng phí các suất ăn chính thức", color = MutedOnLight, fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Section(
                title = "Tiền mua thực tế",
                subtitle = "Gồm mọi giao dịch đã lưu của tháng"
            ) {
                val totalPurchases = state.purchases.filter { it.date.startsWith(period) }.sumOf { it.amount }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE0F7FA), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFF00838F), modifier = Modifier.size(26.dp))
                    }
                    Column {
                        Text(money(totalPurchases), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("Tổng quỹ đã chi đi chợ", color = MutedOnLight, fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Section(
                title = "Bù giá gạo · Chiến sĩ",
                subtitle = "Tính từ ngày đã chốt, làm tròn một lần ở tổng tháng"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, null, tint = SuccessLight, modifier = Modifier.size(26.dp))
                    }
                    Column {
                        Text(money(subsidy), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(
                            "Được cấp theo tính toán. Phần ghi nhận thực nhận sẽ bổ sung ở mốc Tài chính.",
                            color = MutedOnLight,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Group.entries.forEach { g ->
                    FilterChip(
                        selected = group == g,
                        onClick = { group = g },
                        label = { Text(g.label, fontWeight = if (group == g) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Yellow,
                            selectedLabelColor = Ink,
                            containerColor = DarkSurface,
                            labelColor = Paper
                        )
                    )
                }
            }
        }
        val people = closed.filter { it.group == group.name }.groupBy { it.personId }
        if (people.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Chưa có ngày đã chốt cho nhóm này.",
                        color = MutedOnDark,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        items(people.keys.toList()) { personId ->
            val rows = people.getValue(personId)
            val personName = state.people.firstOrNull { it.id == personId }?.name
                ?: "Người ăn $personId"
            Section(personName) {
                Surface(
                    color = SlotCardBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        Meal.entries.joinToString(" · ") { meal ->
                            "${meal.label} ${rows.count { it.meal == meal.name && it.enabled }}"
                        },
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tiền ăn", color = MutedOnLight)
                    Text(
                        money(rows.filter { it.enabled }.sumOf { it.price }),
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }
                val cutMeals = rows.count { it.registered == true && !it.enabled }
                if (cutMeals > 0) {
                    Surface(
                        color = Color(0xFFFFF0ED),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Có cắt cơm: $cutMeals bữa",
                            color = ErrorLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Thu cá nhân/nhóm, hoàn/cấn trừ và xuất Excel thuộc mốc 2; bản hiện tại xem số đã chốt.",
                color = MutedOnDark,
                fontSize = 13.sp
            )
        }
    }
}
