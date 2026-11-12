package vn.bepan.data

import vn.bepan.domain.*
import java.io.OutputStream
import java.io.Writer
import java.math.BigDecimal
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class LedgerSheet(val name: String, val rows: List<List<Any>>, val freezeRows: Int = 1, val freezeColumns: Int = 1)

object FinanceExcel {
    const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    fun sheets(month: FinanceMonth, exportedAt: String = Instant.now().toString()): List<LedgerSheet> = buildList {
        add(LedgerSheet("Đối soát", listOf(
            listOf("BẾP ĂN · ĐỐI SOÁT NỘI BỘ", month.period), listOf("Xuất lúc UTC", exportedAt),
            listOf("Ngày đã chốt", month.closedDates.size), listOf("Ngày đã tới chưa chốt", month.unclosed.joinToString()),
            listOf("Ngày tương lai chưa chốt", month.future.size), listOf("Tiền ăn chính thức", month.charge),
            listOf("Đã nộp tiền ăn", month.paid), listOf("Còn thiếu", month.outstanding), listOf("Dư đối chiếu", month.surplus),
            listOf("Bù gạo được cấp · làm tròn tổng kỳ", month.subsidy), listOf("Bù gạo đã nhận · gắn kỳ quyền lợi", month.receivedSubsidy),
            listOf("Bù gạo còn chờ / âm là nhận vượt", month.subsidyRemaining), listOf("Thu tiền thực tế trong tháng", month.cashIn),
            listOf("Chi tiền thực tế trong tháng", month.cashOut), listOf("Quỹ đến hiện tại", month.cashBalance ?: "Chưa xác lập đầu quỹ"),
            listOf("Mốc số dư đầu quỹ", month.opening?.date ?: "Chưa xác lập"),
            listOf("Cơ sở", "Ngày nháp không phát sinh tiền ăn chính thức. Số đối chiếu theo lịch sử thu đã nhập."),
            listOf("Đổi nhóm", "Tiền ăn theo nhóm snapshot; số đối chiếu cá nhân toàn kỳ không cộng lặp giữa sheet nhóm."),
            listOf("Họ tên", "Tiền ăn cả kỳ", "Đã nộp", "Cấn vào", "Cấn ra", "Đã hoàn", "Giữ chờ", "Số đối chiếu", "Dư khả dụng")
        ) + month.people.map { listOf(it.person.name, it.charge, it.paid, it.carryIn, it.carryOut, it.refunded, it.reserved, it.balance, it.available) }))
        Group.entries.forEach { group ->
            val people = month.people.filter { group.name in it.groups }
            if (people.isNotEmpty()) {
                val columns = listOf("Họ tên") + month.calendar.flatMap { date -> Meal.entries.map { "$date · ${it.label}" } } +
                    listOf("Tổng sáng nhóm", "Tổng trưa nhóm", "Tổng tối nhóm", "Tiền ăn nhóm", "Đã nộp phân bổ nhóm", "Đối chiếu cá nhân toàn kỳ", "Đã hoàn toàn kỳ", "Cấn vào toàn kỳ", "Cấn ra toàn kỳ", "Dư khả dụng toàn kỳ", "Bù chính xác nhóm (₫)")
                val rows = people.map { person ->
                    val meals = person.meals.filter { it.group == group.name }
                    val cells = month.calendar.flatMap { date -> Meal.entries.map { meal ->
                        if (date !in month.closedDates) { if (date > month.today) "Tương lai" else "Nháp" }
                        else meals.firstOrNull { it.date == date && it.meal == meal.name }?.let {
                            when { it.registered == null -> "Chưa xác định · sổ cũ"; it.enabled -> "Có ăn"; it.registered -> "Cắt"; else -> "Chưa đăng ký" }
                        } ?: "—"
                    } }
                    listOf<Any>(person.person.name) + cells + Meal.entries.map { meal -> meals.count { it.meal == meal.name && it.enabled } } +
                        listOf(person.groupCharge(group.name), month.allocations.filter { it.personId == person.person.id && it.group == group.name }.sumOf { it.amount },
                            person.balance, person.refunded, person.carryIn, person.carryOut, person.available,
                            BigDecimal.valueOf(if (group == Group.SOLDIER) meals.filter { it.enabled }.sumOf { it.subsidyMilli } else 0).movePointLeft(3))
                }
                val groupMeals = people.flatMap { it.meals }.filter { it.group == group.name }
                val total = listOf<Any>("TỔNG NHÓM") + List(month.calendar.size * 3) { "" } + Meal.entries.map { meal -> groupMeals.count { it.meal == meal.name && it.enabled } } +
                    listOf(groupMeals.filter { it.enabled }.sumOf { it.price }, month.allocations.filter { it.group == group.name }.sumOf { it.amount }) +
                    List(5) { "" } + listOf(BigDecimal.valueOf(if (group == Group.SOLDIER) groupMeals.filter { it.enabled }.sumOf { it.subsidyMilli } else 0).movePointLeft(3))
                add(LedgerSheet(group.label, listOf(listOf("SỔ ${group.label.uppercase()} · ${month.period}"),
                    listOf("Ba bữa mỗi ngày; tiền ăn theo nhóm chốt. Số đối chiếu cá nhân toàn kỳ chỉ tham khảo, không cộng lặp giữa nhóm."), columns) + rows + listOf(total), 3))
            }
        }
        fun cashSheet(name: String, entries: List<CashEntry>) = LedgerSheet(name,
            listOf(listOf("ID", "Loại", "Ngày tiền thực tế", "Kỳ nghiệp vụ", "Số tiền có dấu", "Nội dung", "Ghi nhận UTC", "Nguồn", "Ngày cũ thiếu chứng từ")) +
                entries.map { listOf(it.id, it.kind, it.date, it.period, it.amount, it.description, it.recordedAt, it.sourceId, if (it.legacy) "Có" else "Không") })
        add(cashSheet("Quỹ tháng", month.cash)); add(cashSheet("Chứng từ kỳ", month.periodCash))
        add(LedgerSheet("Phân bổ", listOf(listOf("Chứng từ", "ID người", "Họ tên", "Kỳ", "Nhóm lúc thu", "Số tiền")) + month.allocations.map {
            listOf(it.entryId, it.personId, month.person(it.personId)?.person?.name ?: it.personId, it.period, Group.valueOf(it.group).label, it.amount)
        }))
        add(LedgerSheet("Quyết toán", listOf(listOf("ID", "Người", "Kỳ đi", "Kỳ đến", "Số tiền", "Trạng thái", "Chứng từ quỹ", "Khoản giữ gốc")) + month.settlements.map {
            listOf(it.id, month.person(it.personId)?.person?.name ?: it.personId, it.fromPeriod, it.toPeriod, it.amount, it.status, it.cashEntryId.orEmpty(), it.parentId.orEmpty())
        }))
    }
    fun write(month: FinanceMonth, output: OutputStream, exportedAt: String = Instant.now().toString()) {
        val sheets = sheets(month, exportedAt)
        val ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        val rel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        ZipOutputStream(output).use { zip ->
            fun part(name: String, body: (Writer) -> Unit) { zip.putNextEntry(ZipEntry(name)); val writer = zip.bufferedWriter(Charsets.UTF_8); body(writer); writer.flush(); zip.closeEntry() }
            part("[Content_Types].xml") { w -> w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
                sheets.indices.forEach { w.write("<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>") }; w.write("</Types>") }
            part("_rels/.rels") { it.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"$rel/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>") }
            part("xl/workbook.xml") { w -> w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"$ns\" xmlns:r=\"$rel\"><sheets>"); sheets.forEachIndexed { i, sheet -> w.write("<sheet name=\"${escape(sheet.name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>") }; w.write("</sheets></workbook>") }
            part("xl/_rels/workbook.xml.rels") { w -> w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"); sheets.indices.forEach { w.write("<Relationship Id=\"rId${it + 1}\" Type=\"$rel/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>") }; w.write("<Relationship Id=\"rIdStyles\" Type=\"$rel/styles\" Target=\"styles.xml\"/></Relationships>") }
            part("xl/styles.xml") { it.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"$ns\"><numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.###;[Red]-#,##0.###\"/></numFmts><fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts><fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>") }
            sheets.forEachIndexed { index, sheet -> part("xl/worksheets/sheet${index + 1}.xml") { w ->
                w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"$ns\"><sheetViews><sheetView workbookViewId=\"0\"><pane xSplit=\"${sheet.freezeColumns}\" ySplit=\"${sheet.freezeRows}\" topLeftCell=\"${column(sheet.freezeColumns)}${sheet.freezeRows + 1}\" activePane=\"bottomRight\" state=\"frozen\"/></sheetView></sheetViews><cols><col min=\"1\" max=\"1\" width=\"28\" customWidth=\"1\"/><col min=\"2\" max=\"200\" width=\"15\" customWidth=\"1\"/></cols><sheetData>")
                sheet.rows.forEachIndexed { rowIndex, row -> w.write("<row r=\"${rowIndex + 1}\">"); row.forEachIndexed { colIndex, value ->
                    val cell = "${column(colIndex)}${rowIndex + 1}"
                    if (value is Number) w.write("<c r=\"$cell\" s=\"1\" t=\"n\"><v>${if (value is BigDecimal) value.toPlainString() else value.toString()}</v></c>")
                    else w.write("<c r=\"$cell\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(value.toString())}</t></is></c>")
                }; w.write("</row>") }; w.write("</sheetData></worksheet>")
            } }
        }
    }
    internal fun column(index: Int): String { var n = index + 1; var result = ""; while (n > 0) { result = ('A'.code + (n - 1) % 26).toChar() + result; n = (n - 1) / 26 }; return result }
    private fun escape(value: String) = value.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
