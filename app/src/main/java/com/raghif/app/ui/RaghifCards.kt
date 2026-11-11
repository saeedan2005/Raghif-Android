package com.raghif.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RaghifMetricCard(
    label:String,
    value:String,
    unit:String,
    icon:ImageVector,
    modifier:Modifier=Modifier
) {
    RaghifCreamPanel(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical=7.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon,null,tint=RaghifPalette.Orange,modifier=Modifier.size(35.dp))
            Text(label,color=RaghifPalette.TextBrown,fontWeight=FontWeight.Bold,
                fontSize=12.sp,maxLines=2)
            Text(value,color=RaghifPalette.DeepBrown,fontWeight=FontWeight.ExtraBold,fontSize=20.sp)
            Text(unit,color=RaghifPalette.TextBrown,fontSize=11.sp)
        }
    }
}
