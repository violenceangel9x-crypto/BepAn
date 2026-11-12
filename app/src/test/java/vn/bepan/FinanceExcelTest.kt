package vn.bepan

import org.junit.Test
import org.junit.Assert.*
import vn.bepan.data.*
import vn.bepan.domain.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class FinanceExcelTest {
    private fun fixture(): FinanceMonth {
        val person = Person("sample", "Tên Việt & <Mẫu> =1+1", "SOLDIER", "2020-01-01")
        val meals = Meal.entries.map { ClosedMeal("2028-02-01", person.id, it.name, "SOLDIER", true, it.price, 0,
            Accounting.subsidyMilli(1, it.grams, 700), 0, Accounting.subsidyMilli(1, it.grams, 700), true) }
        val cash = CashEntry("collection", "COLLECTION", "2028-03-01", "2028-02", 100000, "Thu mẫu", "2028-03-01T00:00:00Z", "sample")
        val allocation = PaymentAllocation(cash.id, person.id, "2028-02", "SOLDIER", 100000)
        return FinanceMonth("2028-02", listOf(FinanceCalculator.person(person, meals, 29 * 72000L, listOf(allocation), emptyList(), "2028-02", setOf("SOLDIER"))),
            listOf(Day("2028-02-01", closed = true)), emptyList(), listOf(cash), listOf(allocation), emptyList(), "2028-03-01", null, null)
    }
    @Test fun workbookXmlRelationshipsNumbersAndVietnameseRoundTrip() {
        val ledger = fixture(); val out = ByteArrayOutputStream(); FinanceExcel.write(ledger, out, "2028-03-01T00:00:00Z")
        val parts = linkedMapOf<String, ByteArray>()
        ZipInputStream(out.toByteArray().inputStream()).use { zip -> while (true) {
            val entry = zip.nextEntry ?: break; parts[entry.name] = zip.readBytes(); zip.closeEntry()
        } }
        assertTrue(parts.containsKey("[Content_Types].xml")); assertTrue(parts.containsKey("xl/_rels/workbook.xml.rels"))
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true; setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        parts.values.forEach { factory.newDocumentBuilder().parse(it.inputStream()) }
        val workbook = factory.newDocumentBuilder().parse(parts.getValue("xl/workbook.xml").inputStream())
        val sheets = workbook.getElementsByTagNameNS("*", "sheet")
        assertEquals(6, sheets.length)
        assertTrue((0 until sheets.length).any { (sheets.item(it) as org.w3c.dom.Element).getAttribute("name") == "Chiến sĩ" })
        val summary = factory.newDocumentBuilder().parse(parts.getValue("xl/worksheets/sheet1.xml").inputStream())
        val cells = summary.getElementsByTagNameNS("*", "c")
        val charge = (0 until cells.length).map { cells.item(it) as org.w3c.dom.Element }.single { it.getAttribute("r") == "B6" }
        assertEquals("n", charge.getAttribute("t")); assertEquals("72000", charge.textContent)
        val group = parts.getValue("xl/worksheets/sheet2.xml").toString(Charsets.UTF_8)
        assertTrue(group.contains("2028-02-29")); assertTrue(group.contains("Tên Việt &amp; &lt;Mẫu&gt; =1+1"))
        assertFalse(group.contains("<f>")); assertTrue(group.contains("state=\"frozen\""))
        val file = File(requireNotNull(System.getProperty("bep.export"))); file.parentFile?.mkdirs(); file.writeBytes(out.toByteArray())
    }
    @Test fun sourceSheetsUseSameTotalsAndDoNotRoundEachPerson() {
        val ledger = fixture(); val sheets = FinanceExcel.sheets(ledger, "2028-03-01T00:00:00Z")
        val summary = sheets.single { it.name == "Đối soát" }
        assertEquals(ledger.charge, summary.rows.single { it.first() == "Tiền ăn chính thức" }[1])
        assertEquals(ledger.subsidy, summary.rows.single { it.first() == "Bù gạo được cấp · làm tròn tổng kỳ" }[1])
        assertEquals(100000L, sheets.single { it.name == "Phân bổ" }.rows[1][5])
        assertEquals(2, sheets.single { it.name == "Chứng từ kỳ" }.rows.size)
        assertEquals(1, sheets.single { it.name == "Quỹ tháng" }.rows.size)
        assertEquals("AA", FinanceExcel.column(26)); assertEquals("AZ", FinanceExcel.column(51))
    }
}
