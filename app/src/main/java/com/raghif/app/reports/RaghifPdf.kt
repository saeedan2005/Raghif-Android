package com.raghif.app.reports

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.raghif.app.data.Bakery
import com.raghif.app.data.DailyRecord
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Offline A4 PDF reports: Arabic/RTL text rendered with StaticLayout (rather than
 * Canvas.drawText) so Arabic joining and bidi ordering are handled by Android.
 * Reports deliberately contain only the selected bakery's records.
 */
object RaghifPdf {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val LEFT = 32f
    private const val RIGHT = 563f
    private val dark = Color.rgb(94, 43, 12)
    private val brown = Color.rgb(156, 74, 16)
    private val gold = Color.rgb(245, 177, 49)
    private val pale = Color.rgb(255, 246, 226)
    private val numberFormat = NumberFormat.getNumberInstance(Locale("ar")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun financial(bakery: Bakery, records: List<DailyRecord>, from: String, to: String): ByteArray {
        require(records.isNotEmpty()) { "لا توجد سجلات للتصدير" }
        val rows = records.filter { it.bakeryId == bakery.id && it.date >= from && it.date <= to }
            .sortedBy { it.date }
        require(rows.isNotEmpty()) { "لا توجد سجلات لهذا المخبز في الفترة المحددة" }
        val document = PdfDocument()
        try {
            var number = 1
            var sheet = page(document, number, "التقرير المالي", bakery, "الفترة من " + formatDate(from) + " إلى " + formatDate(to))
            sheet.financialHeader()
            rows.forEach { record ->
                if (sheet.y + 43f > 749f) {
                    sheet.finish()
                    number++
                    sheet = page(document, number, "التقرير المالي", bakery, "تابع الفترة من " + formatDate(from) + " إلى " + formatDate(to))
                    sheet.financialHeader()
                }
                sheet.financialRow(record)
            }
            if (sheet.y + 54f > 749f) {
                sheet.finish()
                number++
                sheet = page(document, number, "التقرير المالي", bakery, "إجمالي الفترة")
                sheet.financialHeader()
            }
            val revenue = rows.sumOf { it.revenue.toDouble() }
            val deferred = rows.sumOf { ReportMath.deferredValue(it) }
            sheet.tableRow(listOf("الإجمالي", amount(revenue), amount(deferred), amount(revenue + deferred)), true)
            sheet.finish()
            return bytes(document)
        } finally {
            document.close()
        }
    }

    fun daily(bakery: Bakery, record: DailyRecord): ByteArray {
        require(record.bakeryId == bakery.id) { "السجل لا ينتمي إلى المخبز المحدد" }
        val document = PdfDocument()
        try {
            val sheet = page(document, 1, "تقرير السجل اليومي", bakery, "التاريخ: " + formatDate(record.date))
            sheet.labelRow("كمية الدقيق", amount(record.flourKg) + " كيلو")
            sheet.labelRow("عدد الحبات المنتجة", amount(record.pieces) + " حبة")
            sheet.labelRow("عشاء العمال", amount(record.workersMeal) + " حبة")
            sheet.labelRow("التالف", amount(record.waste) + " حبة")
            sheet.labelRow("المباع آجل", amount(record.creditSold) + " حبة")
            sheet.labelRow("الإيراد النقدي", amount(record.revenue) + " ريال")
            sheet.labelRow("قيمة المباع آجل", amount(ReportMath.deferredValue(record)) + " ريال")
            sheet.labelRow("إجمالي المبيعات", amount(ReportMath.totalValue(record)) + " ريال")
            sheet.labelRow("الفرق في الحبات", amount(ReportMath.productionDifference(record)) + " حبة")
            sheet.labelRow("متوسط إنتاج الكيس", ReportMath.averagePiecesPerFlourBag(record)?.let { amount(it) + " حبة" } ?: "غير متاح")
            if (record.note.isNotBlank()) {
                sheet.y += 10f
                sheet.text("ملاحظات: " + record.note, LEFT + 8f, sheet.y, PAGE_WIDTH - 2f * LEFT - 16f, 12f, dark)
            }
            sheet.finish()
            return bytes(document)
        } finally {
            document.close()
        }
    }

    private fun bytes(document: PdfDocument): ByteArray =
        ByteArrayOutputStream().use { output -> document.writeTo(output); output.toByteArray() }

    private fun amount(value: Number): String = synchronized(numberFormat) { numberFormat.format(value) }
    private fun formatDate(value: String): String =
        runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) }.getOrDefault(value)

    private fun page(document: PdfDocument, number: Int, title: String, bakery: Bakery, period: String): Page {
        val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create())
        return Page(document, pdfPage, number, title, bakery.name, period)
    }

    private class Page(
        private val document: PdfDocument,
        private val pdfPage: PdfDocument.Page,
        private val number: Int,
        title: String,
        bakery: String,
        period: String
    ) {
        private val canvas: Canvas = pdfPage.canvas
        var y = 178f

        init {
            canvas.drawColor(Color.WHITE)
            val header = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, PAGE_WIDTH.toFloat(), 111f,
                    intArrayOf(Color.rgb(100, 40, 0), Color.rgb(212, 110, 8), Color.rgb(100, 40, 0)),
                    null, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 111f, header)
            rect(0f, 111f, PAGE_WIDTH.toFloat(), 119f, gold)
            text("رغيف", LEFT, 8f, PAGE_WIDTH - 2f * LEFT, 32f, Color.WHITE, center = true, bold = true)
            text("إدارة البيانات اليومية للمخابز", LEFT, 42f, PAGE_WIDTH - 2f * LEFT, 12f, Color.rgb(255,235,188), center = true, bold = true)
            text(title, LEFT, 66f, PAGE_WIDTH - 2f * LEFT, 22f, Color.WHITE, center = true, bold = true)
            text(bakery, LEFT, 126f, PAGE_WIDTH - 2f * LEFT, 18f, dark, center = true, bold = true)
            text(period, LEFT, 150f, PAGE_WIDTH - 2f * LEFT, 11f, dark, center = true)
            rect(LEFT, 771f, RIGHT, 773f, gold)
            text("رغيف • تقرير صادر من التطبيق", LEFT, 788f, 400f, 10f, dark)
            text("صفحة " + number, 445f, 788f, 118f, 10f, dark, center = true)
        }

        fun financialHeader() {
            tableRow(listOf("التاريخ", "الإيراد (ريال)", "المباع آجل (ريال)", "الإجمالي (ريال)"), true)
        }

        fun financialRow(r: DailyRecord) {
            tableRow(
                listOf(formatDate(r.date), amount(r.revenue), amount(ReportMath.deferredValue(r)), amount(ReportMath.totalValue(r))),
                false
            )
        }

        fun labelRow(label: String, value: String) {
            val h = 43f
            rect(LEFT, y, RIGHT, y + h, if ((y.toInt() / h.toInt()) % 2 == 0) pale else Color.WHITE)
            line(LEFT, y + h, RIGHT, y + h, gold)
            text(label, 330f, y + 12f, 220f, 13f, dark, bold = true)
            text(value, 48f, y + 12f, 261f, 13f, brown, center = true)
            y += h
        }

        fun tableRow(values: List<String>, header: Boolean) {
            val widths = listOf(121f, 130f, 147f, 133f) // RTL: first cell at the right
            val h = if (header) 45f else 40f
            val color = if (header) brown else if ((y.toInt() / 40) % 2 == 0) pale else Color.WHITE
            rect(LEFT, y, RIGHT, y + h, color)
            var edge = RIGHT
            values.forEachIndexed { index, value ->
                val width = widths[index]
                val left = edge - width
                text(value, left + 3f, y + if (header) 10f else 11f, width - 6f, if (header) 11f else 12f, if (header) Color.WHITE else dark, center = true, bold = header)
                line(left, y, left, y + h, gold)
                edge = left
            }
            line(LEFT, y + h, RIGHT, y + h, gold)
            y += h
        }

        fun text(value: String, x: Float, top: Float, width: Float, size: Float, color: Int, center: Boolean = false, bold: Boolean = false) {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            }
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, p, width.toInt().coerceAtLeast(1))
                .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setIncludePad(false)
                .build()
            canvas.save()
            canvas.translate(x, top)
            layout.draw(canvas)
            canvas.restore()
        }

        private fun rect(left: Float, top: Float, right: Float, bottom: Float, color: Int) {
            canvas.drawRect(left, top, right, bottom, Paint().apply { this.color = color })
        }

        private fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
            canvas.drawLine(x1, y1, x2, y2, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; strokeWidth = 1f })
        }

        fun finish() { document.finishPage(pdfPage) }
    }
}
