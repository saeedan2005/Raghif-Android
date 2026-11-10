package com.raghif.app.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raghif.app.data.DailyRecord

private val orange = Color(0xFFE69A14)
private val green = Color(0xFF4D9B4C)
private val guides = Color(0xFFDFD3C3)

/** Two accessible, offline charts using actual bakery records rather than sample data. */
@Composable
fun RecordCharts(records: List<DailyRecord>) {
    val recent = records.sortedBy { it.date }.takeLast(20)
    if (recent.isEmpty()) return
    val first = recent.first().date
    val last = recent.last().date
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("الإنتاج خلال الفترة",fontWeight=FontWeight.Bold)
            TrendLine(recent.map { it.pieces.toFloat() })
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(first)
                Text(last)
            }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("الإيرادات خلال الفترة",fontWeight=FontWeight.Bold)
            RevenueBars(recent.map { it.revenue.toFloat() })
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(first)
                Text(last)
            }
        }
    }
    Text("تعرض الرسوم آخر " + recent.size + " سجل من الفترة المختارة.")
}

@Composable
private fun TrendLine(values: List<Float>) {
    Canvas(Modifier.fillMaxWidth().height(168.dp)) {
        val plotWidth = size.width - 16.dp.toPx()
        val plotHeight = size.height - 14.dp.toPx()
        val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        for (i in 0..3) {
            val yy = plotHeight * i / 3f
            drawLine(guides, Offset(0f, yy), Offset(plotWidth, yy), strokeWidth=1.dp.toPx())
        }
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = if (values.size == 1) plotWidth / 2f else index.toFloat() / (values.size - 1) * plotWidth
            val y = plotHeight - value.coerceAtLeast(0f) / maxValue * plotHeight
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(orange, radius=3.dp.toPx(), center=Offset(x,y))
        }
        if (values.size > 1) drawPath(path,orange,style=Stroke(width=2.dp.toPx()))
    }
}

@Composable
private fun RevenueBars(values: List<Float>) {
    Canvas(Modifier.fillMaxWidth().height(168.dp)) {
        val plotWidth = size.width - 12.dp.toPx()
        val plotHeight = size.height - 14.dp.toPx()
        val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        for (i in 0..3) {
            val yy = plotHeight * i / 3f
            drawLine(guides, Offset(0f, yy), Offset(plotWidth, yy), strokeWidth=1.dp.toPx())
        }
        val segment = plotWidth / values.size
        val barWidth = segment * 0.65f
        values.forEachIndexed { index,value ->
            val top = plotHeight - value.coerceAtLeast(0f) / maxValue * plotHeight
            drawRect(
                green,
                topLeft=Offset(index*segment + (segment-barWidth)/2f,top),
                size=Size(barWidth,plotHeight-top)
            )
        }
    }
}
