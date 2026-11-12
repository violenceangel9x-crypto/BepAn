package vn.bepan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.bepan.data.Menu
import vn.bepan.domain.Meal
import vn.bepan.ui.*

@Composable
fun MenuCardContent(
    date: String,
    menus: List<Menu>,
    edit: ((String, Meal) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MenuImage.slots(date).forEachIndexed { index, (d, meal) ->
            val isNextDay = index == 3
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isNextDay) NextDayCardBg else SlotCardBg)
                    .then(
                        if (isNextDay) Modifier.border(1.dp, Purple.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        else Modifier
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (meal == Meal.DINNER) Icons.Default.DarkMode else Icons.Default.WbSunny,
                        null,
                        tint = if (isNextDay) Purple else SunIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "${meal.label} · $d",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Ink,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    if (isNextDay) {
                        Surface(
                            color = Purple,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Ngày kế tiếp",
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                
                val content = menus.firstOrNull { it.date == d && it.meal == meal.name }?.content?.ifBlank { "Chưa có thực đơn" }
                    ?: "Chưa có thực đơn"
                Text(
                    content,
                    lineHeight = 24.sp,
                    fontSize = 14.sp,
                    color = if (content == "Chưa có thực đơn") MutedOnLight else Ink
                )
                
                if (edit != null) {
                    TextButton(
                        onClick = { edit(d, meal) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Purple),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chỉnh sửa · Magic Menu")
                    }
                }
            }
        }
    }
}
