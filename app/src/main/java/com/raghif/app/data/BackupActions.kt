package com.raghif.app.data

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun BackupActions(database: AppDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingBackup by remember { mutableStateOf<ByteArray?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val data = pendingBackup
        pendingBackup = null
        if (uri != null && data != null) {
            scope.launch {
                busy = true
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                            ?: error("تعذر فتح الملف للحفظ")
                    }
                }.onSuccess {
                    notice = "تم حفظ نسخة احتياطية لجميع المخابز وسجلاتها."
                }.onFailure {
                    notice = "فشل حفظ النسخة: " + (it.localizedMessage ?: "خطأ غير معروف")
                }
                busy = false
            }
        }
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("النسخ الاحتياطي للبيانات")
        Button(
            onClick = {
                scope.launch {
                    busy = true
                    runCatching {
                        withContext(Dispatchers.IO) { RaghifBackup.export(database) }
                    }.onSuccess {
                        pendingBackup = it
                        createDocument.launch("raghif-backup-" + LocalDate.now() + ".json")
                    }.onFailure {
                        notice = "فشل إنشاء النسخة: " + (it.localizedMessage ?: "خطأ غير معروف")
                    }
                    busy = false
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("إنشاء نسخة احتياطية") }
        OutlinedButton(
            onClick = { openDocument.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("استعادة البيانات من نسخة احتياطية") }
        Text("الاستعادة تستبدل بيانات هذا الجهاز بالكامل بعد تأكيدك. أنشئ نسخة احتياطية قبل الاستعادة.")
    }

    if (pendingRestore != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("تأكيد استعادة البيانات") },
            text = { Text("سيتم استبدال جميع المخابز والسجلات الحالية ببيانات الملف المختار. لا يمكن التراجع عن ذلك بدون نسخة احتياطية. هل تريد المتابعة؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingRestore
                        pendingRestore = null
                        if (uri != null) scope.launch {
                            busy = true
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                        ?: error("تعذر فتح النسخة الاحتياطية")
                                    RaghifBackup.restore(database, bytes)
                                }
                            }.onSuccess { (bakeries, records) ->
                                notice = "تمت الاستعادة بنجاح: " + bakeries + " مخابز و" + records + " سجلات."
                            }.onFailure {
                                notice = "لم تتم الاستعادة وبقيت البيانات دون تغيير: " + (it.localizedMessage ?: "خطأ غير معروف")
                            }
                            busy = false
                        }
                    }
                ) { Text("استعادة واستبدال") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("إلغاء") }
            }
        )
    }

    if (notice != null) {
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text("بيانات رغيف") },
            text = { Text(notice.orEmpty()) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("موافق") } }
        )
    }
}
