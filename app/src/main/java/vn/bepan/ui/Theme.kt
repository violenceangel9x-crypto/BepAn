package vn.bepan.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand & Base Surfaces (theo DESIGN.md)
val Navy = Color(0xFF1C2632)
val DarkSurface = Color(0xFF202D3D)
val Paper = Color(0xFFF6F7F9)
val Ink = Color(0xFF17212D)
val Yellow = Color(0xFFFFCB15)
val Purple = Color(0xFF6A3BEB)
val Cyan = Color(0xFF4CC0E5)

// Typography Semantic Tokens
val MutedOnLight = Color(0xFF506071)
val MutedOnDark = Color(0xFFB9C5D1)

// Feedback & Status Tokens
val SuccessLight = Color(0xFF14663D)
val SuccessDark = Color(0xFF4ADE80)
val ErrorLight = Color(0xFF9F2F32)
val ErrorDark = Color(0xFFFF806E)
val WarningLight = Color(0xFF7A4B00)

// Borders & Structural Accents
val BorderLight = Color(0xFFDDE2E7)
val BorderDark = Color(0x22FFFFFF)
val UnfocusedBorder = Color(0xFF8090A0)

// Component Backgrounds
val SlotCardBg = Color(0xFFEDF0F3)
val NextDayCardBg = Color(0xFFE9E4FA)
val DisabledContainer = Color(0xFFE4E7EC)
val DisabledContent = Color(0xFF687585)
val SunIconColor = Color(0xFF735300)

@Composable fun BepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Yellow,
            onPrimary = Ink,
            background = Navy,
            surface = DarkSurface,
            onBackground = Paper,
            onSurface = Paper,
            secondary = Cyan,
            error = ErrorDark,
            surfaceContainerLow = DarkSurface,
            surfaceContainer = DarkSurface,
            surfaceContainerHigh = DarkSurface,
            surfaceContainerHighest = DarkSurface
        ),
        content = content
    )
}
