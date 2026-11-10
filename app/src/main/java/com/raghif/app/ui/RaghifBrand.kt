package com.raghif.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A lightweight visual theme matching the approved amber/brown bakery direction.
 * Assets from the user's reference images must still be installed separately
 * before claiming an exact pixel match.
 */
object RaghifPalette {
    val DeepBrown = Color(0xFF713000)
    val Brown = Color(0xFF9C4706)
    val Orange = Color(0xFFE68A09)
    val Golden = Color(0xFFFFB52F)
    val Cream = Color(0xFFFFF4DA)
    val Paper = Color(0xFFFFF9EF)
}

@Composable
fun RaghifHeader(title: String, subtitle: String = "إدارة البيانات اليومية للمخابز") {
    val shape=RoundedCornerShape(22.dp)
    val deep=RaghifPalette.DeepBrown
    Box(
        modifier=Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(deep,RaghifPalette.Orange,deep)),shape)
            .border(1.dp,RaghifPalette.Golden,shape)
            .padding(vertical=15.dp,horizontal=14.dp)
    ) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("🌾",fontSize=34.sp)
                Icon(Icons.Default.BakeryDining,contentDescription=null,tint=RaghifPalette.Cream)
                Text("رغيف",fontWeight=FontWeight.ExtraBold,color=Color.White,fontSize=30.sp)
                Text("🌾",fontSize=34.sp)
            }
            Text(subtitle,fontSize=12.sp,color=RaghifPalette.Cream)
            Spacer(Modifier.height(8.dp))
            Text(title,fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color.White)
        }
    }
}

@Composable
fun RaghifSectionTitle(title:String) {
    val shape=RoundedCornerShape(15.dp)
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(RaghifPalette.DeepBrown,RaghifPalette.Brown,RaghifPalette.DeepBrown)),shape)
            .border(1.dp,RaghifPalette.Golden,shape)
            .padding(10.dp),
        contentAlignment=Alignment.Center
    ) {
        Text(title,fontWeight=FontWeight.Bold,color=Color.White,fontSize=18.sp)
    }
}
