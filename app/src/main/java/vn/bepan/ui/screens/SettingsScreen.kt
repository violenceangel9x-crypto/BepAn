@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.bepan.BuildConfig
import vn.bepan.assistant.GeminiConnection
import vn.bepan.ui.*
import vn.bepan.ui.components.LightAction
import vn.bepan.ui.components.ScreenHeader
import vn.bepan.ui.components.Section
import vn.bepan.ui.components.lightFieldColors

@Composable
fun SettingsScreen(state: UiState, vm: KitchenViewModel, diagnose: () -> Unit, configure: () -> Unit = {}) {
    var endpoint by remember(state.endpoint) { mutableStateOf(state.endpoint) }
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val canSaveKey = state.credentialsReady && apiKey.isNotBlank() && !state.credentialBusy && !state.busy
    val saveKey = {
        vm.saveApiKey(apiKey) {
            apiKey = ""
            showKey = false
            advanced = false
            focus.clearFocus()
            keyboard?.hide()
        }
    }

    LazyColumn(
        modifier = Modifier.imePadding(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("Cài đặt", "Bếp ăn ${BuildConfig.VERSION_NAME}")
        }
        item { Section("Người ăn & định mức", "Hồ sơ, nhóm, giá và bù gạo theo ngày hiệu lực") {
            Button(onClick = configure, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Quản lý người ăn & định mức") }
        } }
        item {
            Section(
                title = "Gemini",
                subtitle = if (state.mock) "Mô phỏng AI · chỉ có trong bản debug"
                else if (state.connection == GeminiConnection.DIRECT) "Gemini thật · API key trên điện thoại"
                else "Gemini thật · qua gateway"
            ) {
                if (BuildConfig.DEBUG) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mô phỏng AI", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Switch(
                            checked = state.mock,
                            onCheckedChange = vm::setMock,
                            enabled = !state.credentialBusy && !state.busy,
                            colors = SwitchDefaults.colors(checkedThumbColor = Yellow, checkedTrackColor = DarkSurface)
                        )
                    }
                    HorizontalDivider(color = BorderLight)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (state.hasApiKey) Color(0xFFE8F5E9) else SlotCardBg,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (state.hasApiKey) {
                                Icon(Icons.Default.CheckCircle, null, tint = SuccessLight, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                if (state.hasApiKey) "Đã lưu API key" else "Chưa lưu API key",
                                color = if (state.hasApiKey) SuccessLight else MutedOnLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        showKey = false
                    },
                    label = { Text("API key Gemini") },
                    placeholder = {
                        Text(if (state.hasApiKey) "Nhập khóa mới để thay thế" else "Dán API key của bạn")
                    },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canSaveKey) saveKey() }),
                    singleLine = true,
                    enabled = !state.credentialBusy && !state.busy,
                    isError = state.credentialError.isNotEmpty(),
                    trailingIcon = {
                        IconButton(
                            onClick = { showKey = !showKey },
                            enabled = apiKey.isNotEmpty(),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = Purple,
                                disabledContentColor = DisabledContent
                            )
                        ) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                if (showKey) "Ẩn API key" else "Hiện API key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = lightFieldColors()
                )
                if (state.credentialError.isNotEmpty()) {
                    Text(state.credentialError, color = ErrorLight, fontSize = 13.sp)
                }
                Text(
                    "Khóa được mã hóa và lưu riêng trên điện thoại. Lưu khóa sẽ bật Gemini thật.",
                    fontSize = 13.sp,
                    color = MutedOnLight
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = saveKey,
                        enabled = canSaveKey,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Yellow,
                            contentColor = Ink,
                            disabledContainerColor = DisabledContainer,
                            disabledContentColor = DisabledContent
                        )
                    ) {
                        Text(if (state.credentialBusy) "Đang lưu…" else "Lưu API key", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = {
                            apiKey = ""
                            showKey = false
                            vm.deleteApiKey()
                        },
                        enabled = state.hasApiKey && !state.credentialBusy && !state.busy,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Purple,
                            disabledContentColor = DisabledContent
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Xóa API key")
                    }
                }
                LightAction(
                    if (advanced) "Ẩn kết nối nâng cao" else "Kết nối nâng cao",
                    { advanced = !advanced }
                )
                if (advanced) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GeminiConnection.entries.forEach { connection ->
                            FilterChip(
                                selected = state.connection == connection,
                                onClick = { vm.setConnection(connection) },
                                label = {
                                    Text(if (connection == GeminiConnection.DIRECT) "API key trên điện thoại" else "Qua gateway")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Paper,
                                    labelColor = Ink,
                                    selectedContainerColor = Yellow,
                                    selectedLabelColor = Ink
                                )
                            )
                        }
                    }
                    if (state.connection == GeminiConnection.GATEWAY) {
                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            label = { Text("Địa chỉ gateway") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = lightFieldColors()
                        )
                        LightAction("Lưu kết nối", { vm.configure(state.mock, endpoint) })
                        Text(
                            "Chế độ này dùng khóa của gateway, không gửi API key trên điện thoại đến gateway.",
                            fontSize = 13.sp,
                            color = MutedOnLight
                        )
                    }
                }
            }
        }
        item {
            Section("Chẩn đoán", "Xem offline, đối chiếu receipt và xuất báo cáo đã lọc") {
                Button(
                    onClick = diagnose,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = Paper),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.BugReport, null, tint = Cyan)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Debug Center", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    if (vm.log.incomplete) "Bộ ghi kỹ thuật đang thiếu log."
                    else "Bộ ghi kỹ thuật đang hoạt động.",
                    color = MutedOnLight,
                    fontSize = 13.sp
                )
                LightAction("Dọn log kỹ thuật", vm::clearLog)
            }
        }
        item {
            Section("Định mức dùng thử", "Cần đối chiếu trước khi dùng dữ liệu vận hành") {
                Surface(
                    color = SlotCardBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Sáng 14.400₫ · Trưa 28.800₫ · Tối 28.800₫\nPhí/suất: gạo 3.000₫ + gia vị 1.000₫ + chất đốt 800₫\nBù gạo chiến sĩ: 700₫/kg · 136/272/272 g",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Ink,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Text(
                    "Mở Người ăn & định mức để cấu hình có hiệu lực. Sao lưu/khôi phục thuộc mốc vận hành tiếp theo.",
                    fontSize = 13.sp,
                    color = MutedOnLight
                )
            }
        }
    }
}
