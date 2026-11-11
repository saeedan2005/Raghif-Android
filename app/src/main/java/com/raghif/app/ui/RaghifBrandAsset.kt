package com.raghif.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * An optional full-resolution image can replace the compressed sample
 * banner bundled in the APK without ever cropping or stretching it.
 *
 * Existing bakery records, prices, and financial calculations are untouched.
 */
object RaghifBrandAsset {
    private const val MAX_BYTES = 15 * 1024 * 1024
    private const val FILE_NAME = "raghif_original_header_full_res.img"
    var revision by mutableIntStateOf(0)
        private set

    private fun path(context: Context): File = File(context.filesDir,FILE_NAME)

    fun isInstalled(context: Context): Boolean = path(context).isFile

    fun decode(context: Context): Bitmap? {
        val file=path(context)
        if(!file.isFile) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    fun importOriginal(context: Context, bytes: ByteArray) {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) {
            "الصورة كبيرة جدًا؛ الحد الأقصى 15 ميجابايت."
        }
        val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
        require(options.outWidth >= 800 && options.outHeight >= 120) {
            "الصورة صغيرة. اختر صورة الترويسة الأصلية عالية الدقة."
        }
        require(options.outWidth.toFloat()/options.outHeight.toFloat() >= 2f) {
            "اختر ترويسة أفقية وليست لقطة شاشة طولية."
        }
        val target=path(context)
        val temporary=File(target.parentFile,"$FILE_NAME.tmp")
        try {
            temporary.writeBytes(bytes)
            require(temporary.renameTo(target)) { "تعذر حفظ الترويسة." }
            revision += 1
        } finally {
            if(temporary.exists()) temporary.delete()
        }
    }
}
