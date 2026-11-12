package com.raghif.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raghif.app.data.AppDatabase
import com.raghif.app.data.Bakery
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The six-card bakery menu is laid out like the approved Raghif home screen.
 * It remains a live Compose UI: icons, navigation and all database values
 * are genuine controls, never part of a screenshot or sample illustration.
 */
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
    val tap=LocalRaghifSound.current
    val currency=NumberFormat.getNumberInstance(Locale.US)
    val dateText=LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy",Locale("ar","YE"))
    )
    Column(
        modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement=Arrangement.spacedBy(13.dp)
    ) {
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            Row(
                modifier=Modifier.fillMaxWidth().padding(3.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(bakery.name,color=RaghifPalette.TextBrown,fontSize=19.sp,
                        fontWeight=FontWeight.ExtraBold,maxLines=2)
                    Text("المخبز المحدد في النظام",fontSize=11.sp,
                        color=RaghifPalette.Brown)
                }
                Box(Modifier.width(1.dp).height(52.dp)
                    .background(RaghifPalette.Brown))
                Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth,null,tint=RaghifPalette.Brown,
                        modifier=Modifier.size(25.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(dateText,color=RaghifPalette.TextBrown,fontSize=12.sp,
                        fontWeight=FontWeight.Bold)
                }
            }
        }

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DashboardTile("إدخال البيانات اليومية",Icons.Default.EditNote,
                highlighted=true,modifier=Modifier.weight(1f)){tap();onDaily()}
            DashboardTile("الإحصاءات",Icons.Default.BarChart,
                modifier=Modifier.weight(1f)){tap();onStats()}
            DashboardTile("تصدير تقرير PDF",Icons.Default.PictureAsPdf,
                modifier=Modifier.weight(1f)){tap();onExport()}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DashboardTile("التقارير المالية",Icons.Default.Calculate,
                modifier=Modifier.weight(1f)){tap();onFinancial()}
            DashboardTile("السجلات السابقة",Icons.Default.History,
                modifier=Modifier.weight(1f)){tap();onHistory()}
            DashboardTile("البحث في السجلات",Icons.Default.Search,
                modifier=Modifier.weight(1f)){tap();onSearch()}
        }

        RaghifSectionTitle("آخر سجل تم إدخاله")
        RaghifCreamPanel(Modifier.fillMaxWidth()) {
            if(latest==null) {
                Text("لم تُسجّل بيانات لهذا المخبز بعد. اختر إدخال البيانات اليومية.",
                    color=RaghifPalette.TextBrown,modifier=Modifier.padding(14.dp))
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(vertical=10.dp),
                    horizontalArrangement=Arrangement.spacedBy(6.dp),
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    DashboardValue("التاريخ",latest!!.date,Icons.Default.CalendarMonth,
                        modifier=Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(72.dp).background(RaghifPalette.Brown))
                    DashboardValue("عدد الحبات",currency.format(latest!!.pieces)+" حبة",
                        Icons.Default.BakeryDining,modifier=Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(72.dp).background(RaghifPalette.Brown))
                    DashboardValue("الإيراد",currency.format(latest!!.revenue)+" ريال",
                        Icons.Default.Paid,modifier=Modifier.weight(1f))
                }
            }
        }

    }
}

@Composable
private fun DashboardTile(
    label: String,
    icon: ImageVector,
    highlighted: Boolean = false,
    modifier: Modifier=Modifier,
    onClick: () -> Unit
) {
    val rounded=RoundedCornerShape(23.dp)
    Box(
        modifier=modifier
            .shadow(if(highlighted) 5.dp else 3.dp,rounded)
            .background(
                Brush.verticalGradient(
                    if(highlighted)
                        listOf(Color(0xFFFFEAB4),Color(0xFFFFCF7A))
                    else listOf(Color(0xFFFFFFFF),Color(0xFFFFEAD0))
                ),rounded
            )
            .border(BorderStroke(if(highlighted) 2.dp else 1.dp,
                if(highlighted) RaghifPalette.Golden else RaghifPalette.GoldBorder),rounded)
            .clickable(onClick=onClick)
            .padding(vertical=12.dp,horizontal=5.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().height(116.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.SpaceBetween
        ) {
            Icon(icon,contentDescription=null,tint=RaghifPalette.Brown,
                modifier=Modifier.size(56.dp))
            Text(label,color=RaghifPalette.TextBrown,
                fontWeight=FontWeight.Bold,fontSize=13.sp,
                lineHeight=16.sp,maxLines=2,textAlign=TextAlign.Center)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                Icon(Icons.Default.ChevronLeft,contentDescription=null,
                    tint=RaghifPalette.DeepBrown,modifier=Modifier.size(20.dp))
            }
        }
    }
}

@Composable private fun DashboardValue(
    label:String,value:String,icon:ImageVector,
    modifier: Modifier=Modifier
) {
    Column(
        modifier=modifier,
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon,contentDescription=null,tint=RaghifPalette.Brown,
            modifier=Modifier.size(27.dp))
        Text(label,color=RaghifPalette.TextBrown,fontSize=12.sp,
            textAlign=TextAlign.Center)
        Text(value,fontWeight=FontWeight.Bold,color=RaghifPalette.Brown,
            fontSize=12.sp,textAlign=TextAlign.Center,maxLines=2)
    }
}
