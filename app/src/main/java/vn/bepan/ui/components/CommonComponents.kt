package vn.bepan.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.bepan.ui.*
import java.time.LocalDate

@Composable
fun Section(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Paper, contentColor = Ink),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MutedOnLight, fontSize = 13.sp)
            }
            content()
        }
    }
}

@Composable
fun LightAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = Purple),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text(label)
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String = "",
    back: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (back != null) {
            IconButton(onClick = back) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MutedOnDark, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun DateHeader(
    date: String,
    isClosed: Boolean,
    onDateChanged: (String) -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { onDateChanged(LocalDate.parse(date).minusDays(1).toString()) }) {
                Icon(Icons.Default.ChevronLeft, "Ngày trước")
            }
            TextButton(
                onClick = {
                    val d = LocalDate.parse(date)
                    DatePickerDialog(
                        context,
                        { _, y, m, day -> onDateChanged(LocalDate.of(y, m + 1, day).toString()) },
                        d.year,
                        d.monthValue - 1,
                        d.dayOfMonth
                    ).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(date, color = Paper, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { onDateChanged(LocalDate.parse(date).plusDays(1).toString()) }) {
                Icon(Icons.Default.ChevronRight, "Ngày kế tiếp")
            }
        }
        Text(
            if (isClosed) "Đã chốt" else "Nháp",
            color = Cyan,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatusBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            Modifier.weight(1f),
            color = if (isError) MaterialTheme.colorScheme.error else Cyan
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, "Đóng thông báo")
        }
    }
}

@Composable
fun lightFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedLabelColor = Purple,
    unfocusedLabelColor = MutedOnLight,
    cursorColor = Purple,
    focusedBorderColor = Purple,
    unfocusedBorderColor = UnfocusedBorder
)
