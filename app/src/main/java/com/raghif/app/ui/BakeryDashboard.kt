package com.raghif.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raghif.app.data.Bakery
import com.raghif.app.data.DailyRecord
import java.time.LocalDate
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BakeryDashboard(
    bakery: Bakery,
    records: List<DailyRecord>,
    onDaily: () -> Unit,
    onStats: () -> Unit,
    onSearch: () -> Unit,
    onFinancial: () -> Unit,
    onReports: () -> Unit
) {
    val latest = records.maxByOrNull { it.date }
    val today = records.firstOrNull { it.date == LocalDate.now().toString() }
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        RaghifSectionHeading(bakery.name + " — المخبز المحدد")
        RaghifSectionHeading("إدارة المخبز")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            MenuTile("إدخال البيانات اليومية",Icons.Filled.EditNote,Modifier.weight(1f),onDaily)
            MenuTile("الإحصاءات",Icons.Filled.BarChart,Modifier.weight(1f),onStats)
            MenuTile("تصدير تقرير PDF",Icons.Filled.PictureAsPdf,Modifier.weight(1f),onReports)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            MenuTile("البحث في السجلات",Icons.Filled.Search,Modifier.weight(1f),onSearch)
            MenuTile("السجلات السابقة",Icons.Filled.History,Modifier.weight(1f),onSearch)
            MenuTile("التقارير المالية",Icons.Filled.ReceiptLong,Modifier.weight(1f),onFinancial)
        }
        RaghifSectionHeading("إحصاءات اليوم")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Metric("الإيراد", (today?.revenue?.let { fmt.format(it) } ?: "0") + " ريال",Modifier.weight(1f))
            Metric("الإنتاج", (today?.pieces?.let { fmt.format(it) } ?: "0") + " حبة",Modifier.weight(1f))
            Metric("السجلات",fmt.format(records.size),Modifier.weight(1f))
        }
        RaghifSectionHeading("آخر سجل تم إدخاله")
        Card(
            modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(18.dp),
            colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
            border=BorderStroke(1.dp,RaghifPalette.gold)
        ) {
            if(latest == null) {
                Text("لم يتم إدخال سجلات حتى الآن.",Modifier.padding(17.dp))
            } else {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text("التاريخ: " + latest.date,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
                    Text("عدد الحبات: " + fmt.format(latest.pieces) + " حبة")
                    Text("كمية الدقيق: " + fmt.format(latest.flourKg) + " كيلو")
                    Text("الإيراد: " + fmt.format(latest.revenue) + " ريال")
                }
            }
        }
        Text("مع رغيف .. إدارة أسهل لمخبز أكثر نجاحًا",textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().padding(vertical=10.dp),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)
    }
}

@Composable
private fun MenuTile(label:String, icon:ImageVector, modifier:Modifier, onClick:()->Unit) {
    Card(
        modifier=modifier.height(117.dp).clickable(onClick=onClick),
        shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
        border=BorderStroke(1.dp,RaghifPalette.gold),
        elevation=CardDefaults.cardElevation(defaultElevation=3.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Icon(icon,contentDescription=null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.height(40.dp))
            Text(label,fontSize=13.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,lineHeight=17.sp)
        }
    }
}

@Composable
private fun Metric(label:String,value:String,modifier:Modifier) {
    Card(
        modifier=modifier.height(98.dp),
        shape=RoundedCornerShape(17.dp),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
        border=BorderStroke(1.dp,RaghifPalette.gold)
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Text(label,fontSize=12.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
            Text(value,fontSize=13.sp,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary,textAlign=TextAlign.Center,lineHeight=16.sp)
        }
    }
}
