package com.raghif.app.reports

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.raghif.app.ui.LocalRaghifSound
import java.io.File

/**
 * Three explicit PDF actions: save via Android document picker, share via
 * FileProvider, or send to Android's print service. No storage permission needed.
 */
@Composable
fun ReportActions(
    fileName: String,
    enabled: Boolean,
    createPdf: () -> ByteArray
) {
    val context = LocalContext.current
    val clickSound = LocalRaghifSound.current
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val bytes = pending
        pending = null
        if (uri != null && bytes != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: error("تعذر فتح مكان الحفظ")
            }.onSuccess {
                notice = "تم حفظ التقرير بصيغة PDF."
            }.onFailure {
                notice = "تعذر حفظ التقرير: " + (it.localizedMessage ?: "خطأ غير معروف")
            }
        }
    }

    fun makeFile(): File {
        val output = File(context.cacheDir, "reports")
        if (!output.exists() && !output.mkdirs()) error("تعذر إنشاء مجلد التقارير")
        val safe = fileName.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.ifBlank { "raghif-report" }
        return File(output, safe + ".pdf").apply { writeBytes(createPdf()) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.Dp(8f))) {
        Button(
            onClick = {
                clickSound()
                runCatching { createPdf() }.onSuccess {
                    pending = it
                    save.launch(fileName + ".pdf")
                }.onFailure { notice = "تعذر إعداد PDF: " + (it.localizedMessage ?: "خطأ غير معروف") }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) { Text("حفظ التقرير PDF") }

        OutlinedButton(
            onClick = {
                clickSound()
                runCatching {
                    val file = makeFile()
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "مشاركة تقرير رغيف"))
                }.onFailure { notice = "تعذرت مشاركة التقرير: " + (it.localizedMessage ?: "خطأ غير معروف") }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) { Text("مشاركة التقرير PDF") }

        OutlinedButton(
            onClick = {
                runCatching {
                    val file = makeFile()
                    val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    manager.print(
                        "رغيف - " + fileName,
                        ExistingPdfPrintAdapter(file),
                        PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                    )
                }.onFailure { notice = "تعذرت الطباعة: " + (it.localizedMessage ?: "خطأ غير معروف") }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) { Text("طباعة التقرير") }
    }

    if (notice != null) {
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text("تقارير رغيف") },
            text = { Text(notice.orEmpty()) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("موافق") } }
        )
    }
}

/** Bridges an already generated PDF to Android Print Framework. */
private class ExistingPdfPrintAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        callback?.onLayoutFinished(
            PrintDocumentInfo.Builder(file.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
        }
        if (destination == null) {
            callback?.onWriteFailed("لم يتم تحديد وجهة الطباعة")
            return
        }
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(destination).use { target ->
                file.inputStream().use { source -> source.copyTo(target) }
            }
            if (cancellationSignal?.isCanceled == true) callback?.onWriteCancelled()
            else callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (exception: Exception) {
            callback?.onWriteFailed(exception.localizedMessage ?: "فشلت الطباعة")
        }
    }
}
