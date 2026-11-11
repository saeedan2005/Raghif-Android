package com.raghif.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raghif.app.data.AppDatabase
import com.raghif.app.data.Bakery
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RaghifDashboard(
    db: AppDatabase,
    bakery: Bakery,
    onDaily: () -> Unit,
    onStats: () -> Unit,
    onFinancial: () -> Unit,
    onSearch: () -> Unit,
    onHistory: () -> Unit,
    onExport: () -> Unit
) {
    val records by db.dailyDao().observe(bakery.id).collectAsState(initial=emptyList())
    val latest = records.firstOrNull()
    val tap = LocalRaghifSound.current
    val currency = NumberFormat.getNumberInstance(java.util.Locale.US)
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(bakery.name,color=RaghifPalette.TextBrown,fontSize=23.sp,fontWeight=FontWeight.Bold)
                Text("المخبز المحدد في النظام",fontSize=12.sp,color=RaghifPalette.Brown)
                Text("📅  " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                    color=RaghifPalette.TextBrown)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DashboardTile("إدخال البيانات اليومية",Icons.Default.EditNote,Modifier.weight(1f)){tap();onDaily()}
            DashboardTile("الإحصاءات",Icons.Default.BarChart,Modifier.weight(1f)){tap();onStats()}
            DashboardTile("تصدير تقرير PDF",Icons.Default.PictureAsPdf,Modifier.weight(1f)){tap();onExport()}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DashboardTile("التقارير المالية",Icons.Default.Assessment,Modifier.weight(1f)){tap();onFinancial()}
            DashboardTile("السجلات السابقة",Icons.Default.History,Modifier.weight(1f)){tap();onHistory()}
            DashboardTile("البحث في السجلات",Icons.Default.Search,Modifier.weight(1f)){tap();onSearch()}
        }
        RaghifSectionTitle("آخر سجل تم إدخاله")
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            if(latest == null) {
                Text("لم تُسجّل بيانات لهذا المخبز بعد. اختر إدخال البيانات اليومية.",
                    color=RaghifPalette.TextBrown,modifier=Modifier.padding(14.dp))
            } else {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                    DashboardValue("التاريخ",latest!!.date)
                    DashboardValue("عدد الحبات",currency.format(latest!!.pieces)+" حبة")
                    DashboardValue("الإيراد",currency.format(latest!!.revenue)+" ريال")
                }
            }
        }
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            Text("مع رغيف.. إدارة أسهل لمخبز أكثر نجاحًا",color=RaghifPalette.TextBrown,
                fontSize=14.sp,fontWeight=FontWeight.Bold,
                modifier=Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun DashboardTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    RaghifCreamPanel(modifier.clickable(onClick=onClick)) {
        Column(
            modifier=Modifier.fillMaxWidth().height(107.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.Center
        ) {
            Icon(icon,contentDescription=null,tint=RaghifPalette.Orange,
                modifier=Modifier.height(42.dp))
            Spacer(Modifier.height(7.dp))
            Text(label,color=RaghifPalette.TextBrown,fontWeight=FontWeight.Bold,
                fontSize=12.sp,lineHeight=16.sp,maxLines=2)
        }
    }
}

@Composable private fun DashboardValue(label:String,value:String) {
    Column(horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(label,color=RaghifPalette.TextBrown,fontSize=12.sp)
        Text(value,fontWeight=FontWeight.Bold,color=RaghifPalette.Brown,fontSize=12.sp)
    }
}
