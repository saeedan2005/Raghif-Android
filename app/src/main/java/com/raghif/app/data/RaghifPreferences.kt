package com.raghif.app.data

import android.content.Context

/** UI/report options only. No Room schema changes and no changes to stored financial data. */
object RaghifPreferences {
    private const val FILE = "raghif_settings"
    const val DEFAULT_CREDIT_LABEL = "المباع آجل"
    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun showBagAverage(context: Context): Boolean =
        prefs(context).getBoolean("pdf_show_average", true)

    fun setShowBagAverage(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("pdf_show_average", enabled).apply()
    }

    fun showPiecesDifference(context: Context): Boolean =
        prefs(context).getBoolean("pdf_show_difference", true)

    fun setShowPiecesDifference(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("pdf_show_difference", enabled).apply()
    }

    fun creditLabel(context: Context): String =
        prefs(context).getString("credit_label", DEFAULT_CREDIT_LABEL)
            ?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_CREDIT_LABEL

    fun setCreditLabel(context: Context, label: String) {
        require(label.isNotBlank() && label.trim().length <= 50) {
            "يجب أن يكون المسمى بين حرف واحد و50 حرفًا."
        }
        prefs(context).edit().putString("credit_label", label.trim()).apply()
    }

    fun autoBackupOnExit(context: Context): Boolean =
        prefs(context).getBoolean("auto_backup_on_exit", false)

    fun setAutoBackupOnExit(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("auto_backup_on_exit", enabled).apply()
    }

    fun backupFolder(context: Context): String? =
        prefs(context).getString("auto_backup_folder", null)

    fun setBackupFolder(context: Context, uri: String) {
        prefs(context).edit().putString("auto_backup_folder", uri).apply()
    }
}
