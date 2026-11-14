package com.raghif.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Writes a fresh JSON backup to a user-approved Storage Access Framework folder. */
object RaghifAutoBackup {
    suspend fun saveOnExit(context: Context, database: AppDatabase): Uri =
        withContext(Dispatchers.IO) {
            val saved = RaghifPreferences.backupFolder(context)
                ?: error("لم يتم تحديد مجلد النسخ الاحتياطي التلقائي.")
            val folder = Uri.parse(saved)
            val resolver = context.contentResolver
            require(resolver.persistedUriPermissions.any {
                it.uri == folder && it.isWritePermission
            }) { "فقد التطبيق إذن الوصول إلى مجلد النسخ الاحتياطي. اختر المجلد مجددًا من الإعدادات." }

            // Export is unchanged: all bakeries and records, with historical prices.
            val bytes = RaghifBackup.export(database)
            val stamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.US)
            )
            val name = "raghif-backup-$stamp-${UUID.randomUUID().toString().take(8)}.json"
            val parent = DocumentsContract.buildDocumentUriUsingTree(
                folder, DocumentsContract.getTreeDocumentId(folder)
            )
            val document = DocumentsContract.createDocument(
                resolver, parent, "application/json", name
            ) ?: error("تعذر إنشاء ملف النسخة الاحتياطية داخل المجلد المختار.")
            try {
                resolver.openOutputStream(document, "w")?.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                } ?: error("تعذر فتح ملف النسخة الاحتياطية للكتابة.")
                document
            } catch (error: Exception) {
                runCatching { DocumentsContract.deleteDocument(resolver, document) }
                throw error
            }
        }
}
