package com.raghif.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raghif.app.R
import com.raghif.app.data.Bakery

/**
 * Interactive bakery selection modeled on the user's original "اختر المخبز"
 * screenshot. No hard-coded bakeries, phone numbers or illustrations labeled
 * with other bakeries' names: only actual Room records are rendered.
 */
@Composable
fun BakerySelectionScreen(
    bakeries: List<Bakery>,
    selectedBakeryId: Long?,
    onSettings: () -> Unit,
    onAddBakery: () -> Unit,
    onOpenBakery: (Bakery) -> Unit,
    onEditBakery: (Bakery) -> Unit,
    onDeleteBakery: (Bakery) -> Unit
) {
    LazyColumn(
        modifier=Modifier.fillMaxWidth(),
        verticalArrangement=Arrangement.spacedBy(12.dp),
        contentPadding=PaddingValues(start=8.dp,end=8.dp,top=4.dp,bottom=16.dp)
    ) {
        item {
            Column(
                modifier=Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(RaghifPalette.Cream,Color(0xFFFFFAF0),RaghifPalette.Paper)),
                    RoundedCornerShape(22.dp)
                ),
                horizontalAlignment=Alignment.CenterHorizontally
            ) {
                // RTL: Alignment.CenterEnd is the physical left side.
                // Keep the settings gear on the left per the approved mock-up.
                Box(Modifier.fillMaxWidth().padding(vertical=4.dp,horizontal=12.dp)) {
                    IconButton(
                        onClick=onSettings,
                        modifier=Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Settings,"الإعدادات",
                            tint=RaghifPalette.DeepBrown,modifier=Modifier.size(34.dp))
                    }
                }
                RaghifUncroppedImage(
                    drawableId=R.drawable.raghif_original_header,
                    description="رغيف، سنابل القمح والفرن",
                    modifier=Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(9.dp))
                Text("اختر المخبز",fontSize=27.sp,fontWeight=FontWeight.ExtraBold,
                    color=RaghifPalette.TextBrown)
                Text("اختر المخبز لعرض سجلاته وبياناته اليومية",
                    fontSize=13.sp,color=RaghifPalette.Brown,textAlign=TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                RaghifGoldWaves()
            }
        }

        if (bakeries.isEmpty()) {
            item {
                RaghifCreamPanel(Modifier.fillMaxWidth()) {
                    Text("لا توجد مخابز مسجلة. أضف مخبزًا جديدًا للبدء.",
                        modifier=Modifier.padding(15.dp),color=RaghifPalette.TextBrown)
                }
            }
        } else {
            items(bakeries,key={ it.id }) { bakery ->
                BakerySelectionCard(
                    bakery=bakery,
                    isSelected=bakery.id==selectedBakeryId,
                    onOpen={onOpenBakery(bakery)},
                    onEdit={onEditBakery(bakery)},
                    onDelete={onDeleteBakery(bakery)}
                )
            }
        }
        item {
            OutlinedButton(
                onClick=onAddBakery,
                modifier=Modifier.fillMaxWidth().height(60.dp),
                shape=RoundedCornerShape(32.dp),
                border=BorderStroke(2.dp,RaghifPalette.Orange),
                colors=ButtonDefaults.outlinedButtonColors(
                    containerColor=RaghifPalette.Paper,
                    contentColor=RaghifPalette.DeepBrown
                )
            ) {
                Text("إضافة مخبز جديد",fontSize=19.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Add,null,modifier=Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun BakerySelectionCard(
    bakery: Bakery,
    isSelected: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val rounded=RoundedCornerShape(23.dp)
    var menuOpen by remember(bakery.id) { mutableStateOf(false) }
    Column(
        modifier=Modifier.fillMaxWidth()
            .shadow(if(isSelected) 7.dp else 3.dp,rounded)
            .background(
                Brush.verticalGradient(
                    if(isSelected) listOf(Color(0xFFFFE5BA),Color(0xFFFFF2DB))
                    else listOf(Color(0xFFFFFCF6),Color(0xFFFFEFDA))
                ),rounded)
            .border(
                BorderStroke(if(isSelected) 2.dp else 1.dp,
                    if(isSelected) RaghifPalette.DeepBrown else RaghifPalette.GoldBorder),
                rounded
            ).clickable(onClick=onOpen).padding(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ) {
        Row(Modifier.fillMaxWidth(),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(60.dp)
                    .background(Color(0xFFFFDAA0),RoundedCornerShape(15.dp))
                    .border(1.dp,RaghifPalette.GoldBorder,RoundedCornerShape(15.dp)),
                contentAlignment=Alignment.Center
            ) {
                Icon(Icons.Default.Storefront,contentDescription="المخبز",
                    modifier=Modifier.size(42.dp),tint=RaghifPalette.DeepBrown)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(bakery.name,fontSize=19.sp,fontWeight=FontWeight.Bold,
                        color=RaghifPalette.TextBrown,maxLines=2,
                        overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f,fill=false))
                    if(isSelected) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.CheckCircle,"المخبز المحدد",
                            modifier=Modifier.size(21.dp),tint=RaghifPalette.DeepBrown)
                    }
                }
                Text(if(bakery.address.isBlank()) "المخبز رقم " + bakery.number else bakery.address,
                    color=RaghifPalette.Brown,fontSize=12.sp,maxLines=2,
                    overflow=TextOverflow.Ellipsis)
                if(bakery.phone.isNotBlank()) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone,null,tint=RaghifPalette.Brown,
                            modifier=Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(bakery.phone,fontSize=12.sp,color=RaghifPalette.TextBrown)
                    }
                }
            }
            Box {
                IconButton(onClick={menuOpen=true},modifier=Modifier.size(40.dp)) {
                    Icon(Icons.Default.MoreVert,"خيارات المخبز",tint=RaghifPalette.Brown)
                }
                DropdownMenu(expanded=menuOpen,onDismissRequest={menuOpen=false}) {
                    DropdownMenuItem(
                        text={Text("تعديل بيانات المخبز")},
                        leadingIcon={Icon(Icons.Default.Edit,null)},
                        onClick={menuOpen=false;onEdit()}
                    )
                    DropdownMenuItem(
                        text={Text("حذف المخبز",color=RaghifPalette.Danger)},
                        leadingIcon={Icon(Icons.Default.Delete,null,tint=RaghifPalette.Danger)},
                        onClick={menuOpen=false;onDelete()}
                    )
                }
            }
        }
        Button(onClick=onOpen,modifier=Modifier.fillMaxWidth().height(44.dp),
            shape=RoundedCornerShape(24.dp),
            colors=ButtonDefaults.buttonColors(
                containerColor=RaghifPalette.DeepBrown,contentColor=Color.White
            )
        ) {
            Text("فتح بيانات المخبز",fontSize=15.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Storefront,null,modifier=Modifier.size(19.dp))
        }
    }
}
