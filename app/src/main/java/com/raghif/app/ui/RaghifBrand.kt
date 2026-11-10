package com.raghif.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Native recreation of the reference palette: warm cream, amber/gold, baked brown, rounded bars. */
object RaghifPalette {
    val coffee = Color(0xFF65300E)
    val deep = Color(0xFF3E1C08)
    val orange = Color(0xFFAD4A00)
    val gold = Color(0xFFFFB833)
    val paleGold = Color(0xFFFFE3A5)
    val ivory = Color(0xFFFFF2D9)
}

@Composable
fun RaghifBrandHeader(title: String, isRoot: Boolean, onBack: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.25f
    val context=LocalContext.current
    val referenceArtwork=context.resources.getIdentifier("raghif_header","drawable",context.packageName)
    val topColors = if (isDark) listOf(Color(0xFF35200E),Color(0xFF52300F)) else
        listOf(Color(0xFFFFEACC),Color(0xFFFFC866),Color(0xFFFFECCC))
    Column(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(topColors)).padding(horizontal=13.dp,vertical=9.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(4.dp)
    ) {
        if(referenceArtwork != 0) {
            Box(Modifier.fillMaxWidth()) {
                Image(
                    painter=painterResource(referenceArtwork),
                    contentDescription="الهوية الأصلية لرغيف",
                    modifier=Modifier.fillMaxWidth().aspectRatio(1024f/193f),
                    contentScale=ContentScale.FillBounds
                )
                IconButton(onClick=onBack,modifier=Modifier.align(Alignment.TopEnd)) {
                    Icon(if(isRoot) Icons.Filled.ExitToApp else Icons.Filled.ArrowBack,
                        contentDescription=if(isRoot)"الخروج" else "رجوع",tint=RaghifPalette.coffee)
                }
            }
        } else Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text("🌾",fontSize=29.sp)
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.BakeryDining,contentDescription=null,modifier=Modifier.size(36.dp),tint=RaghifPalette.orange)
                    Text("رغيف",fontSize=36.sp,fontWeight=FontWeight.Black,color=if(isDark) Color(0xFFFFCD78) else RaghifPalette.deep)
                }
                Text("إدارة البيانات اليومية للمخابز",fontSize=12.sp,fontWeight=FontWeight.Bold,color=if(isDark) Color.White else RaghifPalette.deep)
            }
            IconButton(onClick=onBack) {
                Icon(if(isRoot) Icons.Filled.ExitToApp else Icons.Filled.ArrowBack,contentDescription=if(isRoot)"الخروج" else "رجوع",tint=if(isDark) Color.White else RaghifPalette.coffee)
            }
        }
        Surface(
            shape=RoundedCornerShape(50),
            border=BorderStroke(2.dp,RaghifPalette.gold),
            shadowElevation=5.dp,
            modifier=Modifier.fillMaxWidth().padding(horizontal=24.dp),
            color=Color.Transparent
        ) {
            Box(
                Modifier.background(Brush.horizontalGradient(listOf(RaghifPalette.deep,RaghifPalette.orange,RaghifPalette.coffee))).padding(vertical=10.dp,horizontal=12.dp),
                contentAlignment=Alignment.Center
            ) {
                Text(title,fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color.White,textAlign=TextAlign.Center)
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
fun RaghifSectionHeading(title: String) {
    Box(
        Modifier.fillMaxWidth().background(
            Brush.horizontalGradient(listOf(RaghifPalette.deep,RaghifPalette.orange)),
            RoundedCornerShape(topStart=16.dp,topEnd=16.dp)
        ).padding(vertical=8.dp,horizontal=14.dp),
        contentAlignment=Alignment.Center
    ) {
        Text(title,color=Color.White,fontWeight=FontWeight.Bold,fontSize=17.sp)
    }
}
