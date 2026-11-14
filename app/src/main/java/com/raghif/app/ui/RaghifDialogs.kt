package com.raghif.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Branded, consistent Android confirmations instead of generic grey dialogs. */
@Composable
fun RaghifNoticeDialog(message:String,onDismiss:()->Unit) {
    val saved=message.contains("تم حفظ")
    AlertDialog(
        onDismissRequest=onDismiss,
        shape=RoundedCornerShape(24.dp),
        containerColor=RaghifPalette.Paper,
        icon={
            Icon(
                if(saved) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription=null,
                tint=RaghifPalette.Brown,
                modifier=Modifier.size(42.dp)
            )
        },
        title={Text(if(saved) "تم الحفظ بنجاح" else "تنبيه رغيف",
            fontWeight=FontWeight.Bold,color=RaghifPalette.DeepBrown)},
        text={Text(message,color=RaghifPalette.TextBrown)},
        confirmButton={
            Button(onClick=onDismiss,shape=RoundedCornerShape(15.dp),
                colors=ButtonDefaults.buttonColors(containerColor=RaghifPalette.Brown)) {
                Text("موافق",color=Color.White,fontWeight=FontWeight.Bold)
            }
        }
    )
}

@Composable
fun RaghifExitDialog(onConfirm:()->Unit,onDismiss:()->Unit) {
    AlertDialog(
        onDismissRequest=onDismiss,
        shape=RoundedCornerShape(24.dp),
        containerColor=RaghifPalette.Paper,
        icon={
            Icon(Icons.Default.ExitToApp,contentDescription=null,
                tint=RaghifPalette.Brown,modifier=Modifier.size(44.dp))
        },
        title={Text("الخروج من رغيف",
            fontWeight=FontWeight.Bold,color=RaghifPalette.DeepBrown)},
        text={Text("هل أنت متأكد من الخروج من التطبيق؟",
            color=RaghifPalette.TextBrown)},
        confirmButton={
            Button(onClick=onConfirm,modifier=Modifier.padding(horizontal=5.dp),shape=RoundedCornerShape(14.dp),
                colors=ButtonDefaults.buttonColors(containerColor=RaghifPalette.Brown)) {
                Text("نعم، خروج",color=Color.White)
            }
        },
        dismissButton={
            OutlinedButton(onClick=onDismiss,
                modifier=Modifier.padding(horizontal=5.dp),
                border=BorderStroke(1.dp,RaghifPalette.GoldBorder),
                shape=RoundedCornerShape(14.dp)) {
                Text("البقاء في التطبيق",color=RaghifPalette.TextBrown)
            }
        }
    )
}
