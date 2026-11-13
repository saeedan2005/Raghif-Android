package com.raghif.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.RadioButton
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults
import com.raghif.app.reports.ReportFileNames
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
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
        RaghifSectionTitle("نوع التقرير")
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                ReportTypeOption("تقرير السجل اليومي",dailyReport,Modifier.weight(1f)) {
                    tap();dailyReport=true;showPreview=false
                }
                ReportTypeOption("تقرير الإيرادات",!dailyReport,Modifier.weight(1f)) {
                    tap();dailyReport=false;showPreview=false
                }
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
            fileName=if(dailyReport) ReportFileNames.daily(bakery.name,singleDay)
                     else runCatching { ReportFileNames.financial(bakery.name,from,to) }
                         .getOrDefault("تقرير_الايرادات_"+bakery.name),
            enabled=ready,
            createPdf={context->
                if(dailyReport) {
                    RaghifPdf.daily(context,bakery,requireNotNull(dayRecord))
                } else {
                    RaghifPdf.financial(context,bakery,rows,from,to)
                }
            }
        )

    }
}

@Composable
private fun ReportTypeOption(label:String,selected:Boolean,modifier:Modifier=Modifier,onClick:()->Unit) {
    Row(
        modifier=modifier
            .heightIn(min=48.dp)
            .background(if(selected) RaghifPalette.Brown else RaghifPalette.Cream,RoundedCornerShape(18.dp))
            .border(1.dp,if(selected) RaghifPalette.Golden else RaghifPalette.GoldBorder,RoundedCornerShape(18.dp))
            .clickable(onClick=onClick)
            .padding(horizontal=3.dp,vertical=2.dp),
        horizontalArrangement=Arrangement.Center,
        verticalAlignment=Alignment.CenterVertically
    ) {
        RadioButton(selected=selected,onClick=onClick,modifier=Modifier.size(36.dp),
            colors=androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor=RaghifPalette.LightGold,
                unselectedColor=RaghifPalette.Brown
            ))
        Text(label,fontWeight=FontWeight.Bold,fontSize=12.sp,lineHeight=14.sp,
            maxLines=2,
            color=if(selected) Color.White else RaghifPalette.TextBrown)
    }
}
