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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Print
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.raghif.app.ui.RaghifPalette
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.raghif.app.ui.LocalRaghifSound
import java.io.File

/**
 * Four equally sized graphical PDF actions. The full Arabic description is
 * provided to TalkBack; a short caption also remains visible below each icon.
 */
@Composable
fun ReportActions(
    fileName: String,
    enabled: Boolean,
    createPdf: (Context) -> ByteArray
) {
    val context=LocalContext.current
    val clickSound=LocalRaghifSound.current
    var pending by remember { mutableStateOf<ByteArray?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val bytes=pending
        pending=null
        if(uri!=null && bytes!=null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: error("تعذر فتح مكان الحفظ")
            }.onSuccess { notice="تم حفظ التقرير بصيغة PDF." }
             .onFailure { notice="تعذر حفظ التقرير: "+(it.localizedMessage ?: "خطأ غير معروف") }
        }
    }
    fun makeFile(): File {
        val directory=File(context.cacheDir,"reports")
        if(!directory.exists() && !directory.mkdirs()) error("تعذر إنشاء مجلد التقارير")
        val safe=fileName.filter { it.isLetterOrDigit() || it=='_' || it=='-' }
            .ifBlank { "raghif-report" }
        return File(directory,safe+".pdf").apply { writeBytes(createPdf(context)) }
    }

    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        PdfActionIcon("حفظ","حفظ التقرير PDF",Icons.Default.Save,enabled,Modifier.weight(1f)) {
            clickSound()
            runCatching { createPdf(context) }
                .onSuccess { pending=it;save.launch(fileName+".pdf") }
                .onFailure { notice="تعذر إعداد PDF: "+(it.localizedMessage ?: "خطأ غير معروف") }
        }
        PdfActionIcon("معاينة","معاينة التقرير PDF",Icons.Default.Visibility,enabled,Modifier.weight(1f)) {
            clickSound()
            runCatching {
                val file=makeFile()
                val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri,"application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },"معاينة تقرير رغيف"))
            }.onFailure { notice="تعذرت معاينة PDF: "+(it.localizedMessage ?: "لا يوجد قارئ PDF") }
        }
        PdfActionIcon("مشاركة","مشاركة التقرير PDF",Icons.Default.Share,enabled,Modifier.weight(1f)) {
            clickSound()
            runCatching {
                val file=makeFile()
                val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type="application/pdf"
                    putExtra(Intent.EXTRA_STREAM,uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },"مشاركة تقرير رغيف"))
            }.onFailure { notice="تعذرت مشاركة PDF: "+(it.localizedMessage ?: "خطأ غير معروف") }
        }
        PdfActionIcon("طباعة","طباعة التقرير PDF",Icons.Default.Print,enabled,Modifier.weight(1f)) {
            clickSound()
            runCatching {
                val file=makeFile()
                val manager=context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                manager.print(
                    "رغيف - "+fileName,
                    ExistingPdfPrintAdapter(file),
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                )
            }.onFailure { notice="تعذرت الطباعة: "+(it.localizedMessage ?: "خطأ غير معروف") }
        }
    }
    if(notice!=null) {
        AlertDialog(
            onDismissRequest={notice=null},
            icon={Icon(Icons.Default.Save,contentDescription=null,tint=RaghifPalette.Brown)},
            title={Text("تقارير رغيف",fontWeight=FontWeight.Bold)},
            text={Text(notice.orEmpty())},
            shape=RoundedCornerShape(22.dp),
            containerColor=RaghifPalette.Paper,
            confirmButton={TextButton(onClick={notice=null}){Text("موافق")}}
        )
    }
}

@Composable
private fun PdfActionIcon(
    caption:String,
    description:String,
    image:ImageVector,
    enabled:Boolean,
    modifier:Modifier=Modifier,
    onClick:()->Unit
) {
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally) {
        Card(
            border=BorderStroke(1.dp,if(enabled) RaghifPalette.GoldBorder else Color.LightGray),
            shape=RoundedCornerShape(16.dp),
            colors=CardDefaults.cardColors(containerColor=RaghifPalette.Cream)
        ) {
            IconButton(onClick=onClick,enabled=enabled,modifier=Modifier.size(56.dp)) {
                Icon(image,contentDescription=description,
                    tint=if(enabled) RaghifPalette.DeepBrown else Color.Gray,
                    modifier=Modifier.size(30.dp))
            }
        }
        Text(caption,fontSize=11.sp,fontWeight=FontWeight.SemiBold,
            color=if(enabled) RaghifPalette.TextBrown else Color.Gray)
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
