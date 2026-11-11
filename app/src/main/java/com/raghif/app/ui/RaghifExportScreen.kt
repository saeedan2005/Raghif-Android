package com.raghif.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raghif.app.data.AppDatabase
import com.raghif.app.data.Bakery
import com.raghif.app.data.DailyRecord
import com.raghif.app.reports.RaghifPdf
import com.raghif.app.reports.ReportActions
import java.time.LocalDate

@Composable
fun RaghifExportScreen(database:AppDatabase,bakery:Bakery) {
    val tap=LocalRaghifSound.current
    var dailyReport by remember { mutableStateOf(true) }
    var singleDay by remember { mutableStateOf(LocalDate.now().toString()) }
    var from by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1).toString()) }
    var to by remember { mutableStateOf(LocalDate.now().toString()) }
    var dayRecord by remember { mutableStateOf<DailyRecord?>(null) }
    var rows by remember { mutableStateOf<List<DailyRecord>>(emptyList()) }
    var showPreview by remember { mutableStateOf(false) }

    LaunchedEffect(bakery.id,singleDay) {
        dayRecord=database.dailyDao().byDate(bakery.id,singleDay)
    }
    LaunchedEffect(bakery.id,from,to) {
        rows=if(from<=to) database.dailyDao().between(bakery.id,from,to) else emptyList()
    }
    val ready=if(dailyReport) dayRecord!=null else rows.isNotEmpty() && from<=to

    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                Text(bakery.name,fontWeight=FontWeight.Bold,color=RaghifPalette.Brown)
                Text("التقرير خاص بالمخبز المحدد فقط",color=RaghifPalette.TextBrown)
            }
        }
        RaghifSectionTitle("أنواع التقارير")
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Button(
                    onClick={tap();dailyReport=true;showPreview=false},
                    modifier=Modifier.fillMaxWidth(),
                    enabled=!dailyReport
                ) { Text("تقرير السجل اليومي — يوم واحد") }
                Button(
                    onClick={tap();dailyReport=false;showPreview=false},
                    modifier=Modifier.fillMaxWidth(),
                    enabled=dailyReport
                ) { Text("التقرير المالي — ملخص فترة") }
            }
        }
        RaghifSectionTitle(if(dailyReport) "التاريخ" else "الفترة الزمنية")
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(dailyReport) {
                    RaghifDateField("تاريخ التقرير",singleDay,{singleDay=it;showPreview=false})
                } else {
                    RaghifDateField("من تاريخ",from,{from=it;showPreview=false})
                    RaghifDateField("إلى تاريخ",to,{to=it;showPreview=false})
                }
            }
        }
        if(!ready) {
            RaghifCreamPanel(Modifier.fillMaxWidth()) {
                Text("لا يوجد سجل مطابق للتاريخ أو الفترة المحددة، اختر تاريخًا يحوي سجلات.",
                    color=RaghifPalette.TextBrown)
            }
        }
        RaghifSectionTitle("خيارات التصدير")
        OutlinedButton(
            onClick={tap();showPreview=!showPreview},
            enabled=ready,
            modifier=Modifier.fillMaxWidth()
        ) { Text(if(showPreview) "إخفاء معاينة البيانات" else "معاينة بيانات التقرير قبل التصدير") }
        if(showPreview && ready) {
            RaghifCreamPanel(Modifier.fillMaxWidth()) {
                val count=if(dailyReport) 1 else rows.size
                val revenue=if(dailyReport) (dayRecord?.revenue ?: 0L) else rows.sumOf { it.revenue }
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text("عدد السجلات: " + count, fontWeight=FontWeight.Bold,color=RaghifPalette.TextBrown)
                    Text("إجمالي الإيراد: " + revenue + " ريال",color=RaghifPalette.TextBrown)
                    Text("اسم المخبز: " + bakery.name,color=RaghifPalette.TextBrown)
                }
            }
        }
        ReportActions(
            fileName=if(dailyReport) "raghif_daily_" + bakery.id + "_" + singleDay
                     else "raghif_financial_" + bakery.id + "_" + from + "_" + to,
            enabled=ready,
            createPdf={context->
                if(dailyReport) {
                    RaghifPdf.daily(context,bakery,requireNotNull(dayRecord))
                } else {
                    RaghifPdf.financial(context,bakery,rows,from,to)
                }
            }
        )
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            Text("ملاحظة: التقرير خاص بالمخبز المحدد، وتتم المشاركة بملف PDF وليس برسالة نصية.",
                color=RaghifPalette.TextBrown)
        }
    }
}
