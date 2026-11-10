package com.raghif.app.reports

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
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

/** A4 landscape financial report matching the approved four-column reference. */
internal object FinancialLandscapePdf {
    private const val width=842
    private const val height=595
    private const val left=33f
    private const val right=809f
    private val coffee=Color.rgb(89,35,6)
    private val brown=Color.rgb(146,62,7)
    private val gold=Color.rgb(247,175,33)
    private val pale=Color.rgb(255,238,201)
    private val num=NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits=2
        minimumFractionDigits=0
    }
    private val widths=listOf(165f,192f,207f,212f) // right -> left, total 776

    fun generate(bakery:Bakery, records:List<DailyRecord>, from:String, to:String):ByteArray {
        val rows=records.filter { it.bakeryId==bakery.id && it.date>=from && it.date<=to }
            .sortedBy { it.date }
        require(rows.isNotEmpty()) { "لا توجد سجلات مالية في الفترة المحددة" }
        val document=PdfDocument()
        try {
            var pageNumber=1
            var page=newPage(document,pageNumber,bakery.name,from,to)
            page.tableRow(listOf("التاريخ","الإيراد","المبيعات آجل","الإجمالي"),true)
            rows.forEach { record ->
                if(page.y+41f>518f) {
                    page.finish()
                    pageNumber++
                    page=newPage(document,pageNumber,bakery.name,from,to)
                    page.tableRow(listOf("التاريخ","الإيراد","المبيعات آجل","الإجمالي"),true)
                }
                page.tableRow(listOf(
                    pretty(record.date),
                    money(record.revenue),
                    money(ReportMath.deferredValue(record)),
                    money(ReportMath.totalValue(record))
                ),false)
            }
            if(page.y+44f>518f) {
                page.finish()
                pageNumber++
                page=newPage(document,pageNumber,bakery.name,from,to)
                page.tableRow(listOf("التاريخ","الإيراد","المبيعات آجل","الإجمالي"),true)
            }
            val cash=rows.sumOf { it.revenue.toDouble() }
            val credit=rows.sumOf { ReportMath.deferredValue(it) }
            page.tableRow(listOf("الإجمالي",money(cash),money(credit),money(cash+credit)),true)
            page.finish()
            return ByteArrayOutputStream().use { out->document.writeTo(out);out.toByteArray() }
        } finally { document.close() }
    }

    private fun money(value:Number):String=synchronized(num){num.format(value)}
    private fun pretty(date:String):String=runCatching {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    }.getOrDefault(date)
    private fun newPage(doc:PdfDocument,num:Int,bakery:String,from:String,to:String):Sheet {
        val page=doc.startPage(PdfDocument.PageInfo.Builder(width,height,num).create())
        return Sheet(doc,page,num,bakery,pretty(from),pretty(to))
    }

    private class Sheet(
        private val doc:PdfDocument,
        private val page:PdfDocument.Page,
        number:Int,bakery:String,from:String,to:String
    ) {
        private val canvas:Canvas=page.canvas
        var y=170f
        init {
            canvas.drawColor(Color.WHITE)
            val background=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader=LinearGradient(0f,0f,width.toFloat(),105f,
                    intArrayOf(coffee,brown,coffee),null,Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f,0f,width.toFloat(),108f,background)
            rect(0f,108f,width.toFloat(),115f,gold)
            draw("رغيف",33f,11f,175f,28f,Color.WHITE,bold=true)
            draw("إدارة البيانات اليومية للمخابز",32f,54f,250f,12f,Color.WHITE)
            draw(bakery+" — التقرير المالي",260f,19f,548f,27f,Color.WHITE,center=true,bold=true)
            draw("للفترة من "+from+" إلى "+to,262f,68f,544f,14f,Color.WHITE,center=true)
            draw("التقرير خاص بالمخبز المحدد فقط",left,129f,right-left,16f,coffee,center=true,bold=true)
            rect(left,537f,right,538f,gold)
            draw("تقرير رغيف المالي • جميع المبالغ بالريال",left,547f,614f,11f,coffee)
            draw("صفحة "+number,687f,547f,122f,11f,coffee,center=true)
        }
        fun tableRow(data:List<String>,header:Boolean) {
            val h=if(header)44f else 40f
            val color=if(header) brown else if(((y-170f)/40f).toInt()%2==0) pale else Color.WHITE
            rect(left,y,right,y+h,color)
            var edge=right
            data.forEachIndexed { index,value->
                val w=widths[index]
                val x=edge-w
                draw(value,x+3f,y+10f,w-6f,if(header)14f else 15f,
                    if(header) Color.WHITE else coffee,center=true,bold=header)
                stroke(x,y,x,y+h,gold)
                edge=x
            }
            stroke(left,y+h,right,y+h,gold)
            y+=h
        }
        private fun draw(value:String,x:Float,top:Float,w:Float,textSize:Float,color:Int,center:Boolean=false,bold:Boolean=false) {
            val p=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color=color;this.textSize=textSize
                typeface=Typeface.create("sans-serif",if(bold) Typeface.BOLD else Typeface.NORMAL)
            }
            val layout=StaticLayout.Builder.obtain(value,0,value.length,p,w.toInt().coerceAtLeast(1))
                .setAlignment(if(center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setIncludePad(false).build()
            canvas.save();canvas.translate(x,top);layout.draw(canvas);canvas.restore()
        }
        private fun rect(x:Float,t:Float,r:Float,b:Float,c:Int) {
            canvas.drawRect(x,t,r,b,Paint().apply { color=c })
        }
        private fun stroke(x:Float,t:Float,r:Float,b:Float,c:Int) {
            canvas.drawLine(x,t,r,b,Paint(Paint.ANTI_ALIAS_FLAG).apply { color=c;strokeWidth=1f })
        }
        fun finish() {doc.finishPage(page)}
    }
}
