@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.bepan.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import vn.bepan.BuildConfig
import vn.bepan.data.Menu
import vn.bepan.ui.components.MenuCardContent
import vn.bepan.ui.components.StatusBanner
import vn.bepan.ui.dialogs.ManualConfirmDialog
import vn.bepan.ui.screens.*
import kotlin.math.roundToInt

private val titles = listOf("Hôm nay", "Tài chính", "Ghi chú", "Cài đặt")
private val tabIcons = listOf(
    Icons.Default.Today,
    Icons.Default.AccountBalanceWallet,
    Icons.AutoMirrored.Filled.Notes,
    Icons.Default.Settings
)

@Composable
fun KitchenApp(
    vm: KitchenViewModel,
    listening: Boolean = false,
    voiceStatus: String = "",
    voice: () -> Unit = {}
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route ?: "home"
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabStates = rememberSaveableStateHolder()
    var bubbleX by rememberSaveable { mutableFloatStateOf(0f) }
    var bubbleY by rememberSaveable { mutableFloatStateOf(0f) }
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val bubbleWidth = with(density) { (config.screenWidthDp - 96).coerceAtLeast(0).dp.toPx() }
    val bubbleHeight = with(density) { (config.screenHeightDp - 280).coerceAtLeast(0).dp.toPx() }

    Scaffold(
        bottomBar = {
            if (route == "home") {
                NavigationBar(
                    containerColor = DarkSurface,
                    modifier = Modifier.heightIn(min = if (density.fontScale >= 1.5f) 112.dp else 80.dp)
                ) {
                    titles.forEachIndexed { i, title ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(tabIcons[i], null) },
                            label = {
                                Text(
                                    title,
                                    modifier = Modifier.widthIn(max = (config.screenWidthDp / 4 - 8).coerceAtLeast(48).dp),
                                    maxLines = 2,
                                    textAlign = TextAlign.Center
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Yellow,
                                selectedIconColor = Ink,
                                selectedTextColor = Yellow
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (route == "home") {
                FloatingActionButton(
                    onClick = { nav.navigate("chat") },
                    containerColor = Purple,
                    contentColor = Color.White,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                bubbleX.coerceIn(-bubbleWidth, 0f).roundToInt(),
                                bubbleY.coerceIn(-bubbleHeight, 0f).roundToInt()
                            )
                        }
                        .pointerInput(bubbleWidth, bubbleHeight) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                bubbleX = (bubbleX + drag.x).coerceIn(-bubbleWidth, 0f)
                                bubbleY = (bubbleY + drag.y).coerceIn(-bubbleHeight, 0f)
                            }
                        }
                ) {
                    Icon(Icons.Default.AutoAwesome, "Mở Gemini")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            if (BuildConfig.DEBUG && route == "home") {
                Text(
                    "BẢN THỬ · DỮ LIỆU MẪU",
                    color = Cyan,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            if (state.error.isNotEmpty() || state.notice.isNotEmpty()) {
                StatusBanner(
                    message = state.error.ifEmpty { state.notice },
                    isError = state.error.isNotEmpty(),
                    onDismiss = vm::clearError
                )
            }
            NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f)) {
                composable("home") {
                    tabStates.SaveableStateProvider(tab) {
                        when (tab) {
                            0 -> TodayScreen(state, vm)
                            1 -> FinanceScreen(state)
                            2 -> NotesScreen(state, vm) { nav.navigate("audit") }
                            3 -> SettingsScreen(state, vm) {
                                vm.diagnostics()
                                nav.navigate("diagnostics")
                            }
                        }
                    }
                }
                composable("chat") {
                    ChatScreen(
                        state = state,
                        vm = vm,
                        listening = listening,
                        voiceStatus = voiceStatus,
                        voice = voice,
                        back = { nav.popBackStack() },
                        diagnose = { trace ->
                            vm.diagnostics(trace)
                            nav.navigate("diagnostics")
                        },
                        openToday = {
                            tab = 0
                            nav.popBackStack("home", false)
                        }
                    )
                }
                composable("audit") {
                    HistoryScreen(state) { nav.popBackStack() }
                }
                composable("diagnostics") {
                    DiagnosticsScreen(state, vm) { nav.popBackStack() }
                }
            }
        }
    }

    state.manualPreview?.let { command ->
        ManualConfirmDialog(
            command = command,
            snapshot = state.snapshot,
            busy = state.busy,
            onDismiss = vm::cancelManual,
            onConfirm = vm::confirmManual
        )
    }
}

@Preview(name = "Thực đơn dài · 360dp", widthDp = 360, showBackground = true)
@Composable
private fun MenuPreview() {
    BepTheme {
        Column(Modifier.padding(16.dp)) {
            MenuCardContent(
                "2026-11-30",
                listOf(
                    Menu(
                        "2026-11-30",
                        "LUNCH",
                        "Cơm tẻ\nThịt kho trứng\nRau cải xào tỏi\nCanh bầu nấu tôm\nTráng miệng"
                    )
                )
            )
        }
    }
}

@Preview(name = "Thực đơn · chữ 1.5x", widthDp = 320, fontScale = 1.5f)
@Composable
private fun LargeMenuPreview() {
    MenuPreview()
}
