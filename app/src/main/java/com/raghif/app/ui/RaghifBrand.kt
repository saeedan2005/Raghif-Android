package com.raghif.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raghif.app.R

/**
 * Raghif visual palette from the supplied reference designs.
 * Never stretch or crop image assets. Editable UI content is composed separately.
 */
object RaghifPalette {
    val DeepBrown = Color(0xFF642800)
    val Brown = Color(0xFF8D3900)
    val Orange = Color(0xFFD96D00)
    val Golden = Color(0xFFFFB326)
    val LightGold = Color(0xFFFFDB7D)
    val Cream = Color(0xFFFFF0D1)
    val Paper = Color(0xFFFFF8E9)
    val TextBrown = Color(0xFF532809)
    val Danger = Color(0xFFB91414)
    val GoldBorder = Color(0xFFE98D18)
}

/**
 * Source aspect ratio is read from the actual bitmap, not assumed from the
 * phone or from the design mock-up. FIT preserves every original pixel of the
 * image's visible area; any extra space is an intentional letterbox.
 */
@Composable
fun RaghifUncroppedImage(
    @DrawableRes drawableId: Int,
    description: String?,
    modifier: Modifier = Modifier
) {
    val painter = painterResource(id = drawableId)
    val intrinsic = painter.intrinsicSize
    val ratio = if (
        intrinsic.width.isFinite() && intrinsic.height.isFinite() &&
        intrinsic.width > 0f && intrinsic.height > 0f
    ) intrinsic.width / intrinsic.height else 1f
    Image(
        painter = painter,
        contentDescription = description,
        modifier = modifier.fillMaxWidth().aspectRatio(ratio, matchHeightConstraintsFirst = false),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
}

@Composable
fun RaghifHeader(title: String, subtitle: String = "إدارة البيانات اليومية للمخابز") {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        RaghifUncroppedImage(
            drawableId = R.drawable.raghif_original_header,
            description = "الشعار مع سنابل القمح والخبز والفرن"
        )
        RaghifSectionTitle(title)
    }
}

@Composable
fun RaghifSectionTitle(title: String) {
    val shape = RoundedCornerShape(23.dp)
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 6.dp)
            .shadow(5.dp,shape)
            .background(
                Brush.verticalGradient(
                    listOf(RaghifPalette.Orange, RaghifPalette.Brown, RaghifPalette.DeepBrown)
                ), shape
            )
            .border(2.dp,RaghifPalette.Golden,shape)
            .padding(horizontal=14.dp,vertical=10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RaghifCreamPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .shadow(3.dp,shape)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFF9EC), RaghifPalette.Cream)),
                shape
            )
            .border(1.25.dp,RaghifPalette.GoldBorder,shape)
            .padding(10.dp)
    ) {
        content()
    }
}

@Composable
fun RaghifGoldWaves() {
    Canvas(modifier=Modifier.fillMaxWidth().height(24.dp)) {
        val w=size.width
        val h=size.height
        val orange=Path().apply {
            moveTo(0f,12f)
            cubicTo(w*0.2f,0f,w*0.4f,h*1.25f,w*0.6f,h*.52f)
            cubicTo(w*.8f,-h*.15f,w*.9f,h*.9f,w,h*.32f)
            lineTo(w,h);lineTo(0f,h);close()
        }
        drawPath(orange,RaghifPalette.Golden,style=Fill)
        val dark=Path().apply {
            moveTo(0f,h*.78f)
            cubicTo(w*.25f,h*.30f,w*.38f,h*1.1f,w*.55f,h*.65f)
            cubicTo(w*.77f,h*.3f,w*.92f,h*.8f,w,h*.6f)
            lineTo(w,h);lineTo(0f,h);close()
        }
        drawPath(dark,RaghifPalette.DeepBrown,style=Fill)
        drawLine(RaghifPalette.LightGold,Offset(0f,12f),Offset(w,12f),0.75.dp.toPx())
    }
}


@Composable
fun RaghifFooter(isHome: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        RaghifGoldWaves()
        Row(
            Modifier.fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(RaghifPalette.DeepBrown,RaghifPalette.Brown,RaghifPalette.DeepBrown)
                    )
                )
                .padding(horizontal=12.dp,vertical=5.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "إعداد التطبيق: عبدالعزيز سعيدان",
                fontSize=12.sp,
                color=RaghifPalette.Cream,
                modifier=Modifier.weight(1f)
            )
            OutlinedButton(
                onClick=onBack,
                border=androidx.compose.foundation.BorderStroke(1.5.dp,RaghifPalette.LightGold),
                shape=RoundedCornerShape(20.dp),
                colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White),
            ) {
                Text(if(isHome) "خروج" else "رجوع",fontWeight=FontWeight.Bold)
                Icon(
                    if(isHome) Icons.Default.Logout else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription=null,
                    modifier=Modifier.padding(start=5.dp)
                )
            }
        }
    }
}
