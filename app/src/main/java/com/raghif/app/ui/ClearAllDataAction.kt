package com.raghif.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.room.withTransaction
import com.raghif.app.data.AppDatabase
import kotlinx.coroutines.launch

/** Explicit confirmation; the two-table wipe is one database transaction. */
@Composable
fun ClearAllDataAction(db: AppDatabase) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Button(
        onClick={confirm=true},
        enabled=!busy,
        modifier=Modifier.fillMaxWidth(),
        colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFC32121))
    ) { Text("مسح جميع البيانات") }

    if(confirm) AlertDialog(
        onDismissRequest={confirm=false},
        title={Text("تأكيد مسح جميع البيانات")},
        text={Text("سيتم حذف كل المخابز وجميع سجلاتها نهائيًا من هذا الجهاز. يُفضّل إنشاء نسخة احتياطية أولًا. هل أنت متأكد؟")},
        dismissButton={TextButton(onClick={confirm=false}){Text("إلغاء")}},
        confirmButton={TextButton(onClick={
            confirm=false
            busy=true
            scope.launch {
                runCatching {
                    db.withTransaction {
                        db.dailyDao().clear()
                        db.bakeryDao().clear()
                    }
                }.onSuccess { notice="تم مسح جميع المخابز والسجلات من الجهاز." }
                    .onFailure { notice="فشل المسح: " + (it.localizedMessage ?: "خطأ غير معروف") }
                busy=false
            }
        }){Text("نعم، مسح الكل",color=Color.Red)}}
    )
    if(notice!=null) AlertDialog(
        onDismissRequest={notice=null},
        title={Text("إدارة البيانات")},
        text={Text(notice.orEmpty())},
        confirmButton={TextButton(onClick={notice=null}){Text("موافق")}}
    )
}
