package com.raghif.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raghif.app.data.Bakery
import com.raghif.app.data.DailyRecord
import com.raghif.app.reports.RaghifPdf
import com.raghif.app.reports.ReportActions
import java.time.LocalDate

/** Four choices described in the latest approved PDF reference (page 19). */
@Composable
fun ReportExportScreen(bakery:Bakery, records:List<DailyRecord>) {
    var type by remember(bakery.id) { mutableStateOf("daily") }
    var day by remember(bakery.id) { mutableStateOf(LocalDate.now().toString()) }
    var from by remember(bakery.id) { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var to by remember(bakery.id) { mutableStateOf(LocalDate.now().toString()) }
    val chosen = records.firstOrNull { it.date == day }
    val rangeValid = from <= to
    val matching = records.filter { it.date >= from && it.date <= to }
    val canExport = if(type == "daily") chosen != null else rangeValid && matching.isNotEmpty()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        RaghifSectionHeading("تصدير تقارير PDF")
        Text("المخبز: " + bakery.name,color=MaterialTheme.colorScheme.primary)
        RaghifSectionHeading("أنواع التقارير")
        listOf(
            Triple("daily","تقرير يومي","بيانات سجل يوم واحد"),
            Triple("financial","التقرير المالي","الإيرادات والمبيعات الآجلة"),
            Triple("period","تقرير فترة زمنية","الإنتاج والإيراد خلال الفترة"),
            Triple("general","التقرير العام","إجمالي الإيرادات والإنتاج")
        ).forEach { (code,title,description) ->
            Card(
                modifier=Modifier.fillMaxWidth().clickable { type=code },
                shape=RoundedCornerShape(16.dp),
                border=BorderStroke(1.dp,RaghifPalette.gold),
                colors=CardDefaults.cardColors(
                    containerColor=if(type==code) RaghifPalette.paleGold else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    RadioButton(selected=type==code,onClick={type=code})
                    Column { Text(title,color=if(type==code) RaghifPalette.deep else MaterialTheme.colorScheme.onSurface);Text(description,color=if(type==code) RaghifPalette.coffee else MaterialTheme.colorScheme.onSurface) }
                }
            }
        }
        RaghifSectionHeading("الفترة الزمنية")
        if(type=="daily") {
            CalendarDateField("تاريخ السجل",day,{day=it})
        } else {
            CalendarDateField("من تاريخ",from,{from=it})
            CalendarDateField("إلى تاريخ",to,{to=it})
        }
        if(!rangeValid && type!="daily") Text("تاريخ البداية يجب أن يكون قبل تاريخ النهاية.",color=MaterialTheme.colorScheme.error)
        if(!canExport) Text("لا توجد سجلات مطابقة للتاريخ المحدد في هذا المخبز.",color=MaterialTheme.colorScheme.error)
        RaghifSectionHeading("خيارات التصدير")
        ReportActions(
            fileName="raghif_" + type + "_" + bakery.id + "_" + (if(type=="daily") day else from + "_" + to),
            enabled=canExport,
            createPdf={
                when(type) {
                    "daily" -> RaghifPdf.daily(bakery,requireNotNull(chosen))
                    "financial" -> RaghifPdf.financial(bakery,matching,from,to)
                    "period" -> RaghifPdf.summary(bakery,matching,from,to,"تقرير فترة زمنية")
                    else -> RaghifPdf.summary(bakery,matching,from,to,"التقرير العام")
                }
            }
        )
        Text("تقتصر التقارير على بيانات المخبز المحدد.",color=MaterialTheme.colorScheme.onSurface)
    }
}
