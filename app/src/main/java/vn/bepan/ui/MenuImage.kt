package vn.bepan.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import vn.bepan.data.Menu
import vn.bepan.domain.Meal
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MenuImage {
    fun slots(date: String): List<Pair<String, Meal>> = Meal.entries.map { date to it } + (LocalDate.parse(date).plusDays(1).toString() to Meal.BREAKFAST)
    fun render(date: String, menus: List<Menu>, width: Int = 1080): Bitmap {
        require(width in 320..2160)
        val scale = width / 1080f
        val padding = (48 * scale).toInt()
        fun layout(text: String, size: Float, bold: Boolean = false): StaticLayout {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(23, 33, 45); textSize = size * scale; typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL) }
            return StaticLayout.Builder.obtain(text, 0, text.length, paint, width - padding * 2).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(8 * scale, 1f).setIncludePad(true).build()
        }
        val title = layout("THỰC ĐƠN · $date", 48f, true)
        val blocks = slots(date).mapIndexed { i, (d, m) ->
            layout("${m.label.uppercase()} · $d${if (i == 3) " · Ngày kế tiếp" else ""}", 34f, true) to
                layout(menus.firstOrNull { it.date == d && it.meal == m.name }?.content?.ifBlank { "Chưa có thực đơn" } ?: "Chưa có thực đơn", 36f)
        }
        val height = padding * 2 + title.height + blocks.sumOf { it.first.height + it.second.height + padding * 2 }
        require(height.toLong() * width <= 24_000_000) { "MENU_IMAGE_TOO_LARGE" }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap); canvas.drawColor(Color.rgb(246, 247, 249))
        var y = padding
        fun draw(l: StaticLayout) { canvas.save(); canvas.translate(padding.toFloat(), y.toFloat()); l.draw(canvas); canvas.restore(); y += l.height }
        draw(title)
        blocks.forEachIndexed { index, (header, body) ->
            canvas.drawRoundRect(24 * scale, y + padding / 2f, width - 24 * scale, (y + header.height + body.height + padding * 2).toFloat(), 24 * scale, 24 * scale,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if(index == 3) Color.rgb(233,228,250) else Color.rgb(237,240,243) })
            y += padding; draw(header); draw(body); y += padding
        }
        return bitmap
    }
    suspend fun export(context: Context, date: String, menus: List<Menu>, copy: Boolean) {
        val uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000L }?.forEach { it.delete() }
        val file = File(directory, "menu-${java.util.UUID.randomUUID()}.png")
        val bitmap = try { render(date, menus) } catch (_: OutOfMemoryError) { throw vn.bepan.domain.RuleViolation("MENU_IMAGE_TOO_LARGE") }
        try { file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } } finally { bitmap.recycle() }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }
        withContext(Dispatchers.Main) {
        if (copy) (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newUri(context.contentResolver, "Thực đơn", uri))
        else context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); clipData = ClipData.newUri(context.contentResolver, "Thực đơn", uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Chia sẻ thực đơn"))
        }
    }
}
