package com.raghif.app.reports

import java.time.LocalDate

/**
 * Human-readable Arabic names for both PDF sharing and Android's document picker.
 * All dates are ISO yyyy-MM-dd so they sort reliably and cannot contain slash.
 */
object ReportFileNames {
    private fun safe(value: String): String = value.trim()
        .replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
        .trim('_')
        .take(80)

    fun daily(bakeryName: String, reportDate: String): String {
        val name = bakeryName.trim().replace(Regex("^مخبز\\s+"), "")
        require(name.isNotBlank()) { "اسم المخبز غير صالح" }
        val date = LocalDate.parse(reportDate).toString()
        return "سجل_رغيف_اليومي_مخبز_" + safe(name) + "_" + date
    }

    fun financial(startDate: String, endDate: String): String {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        require(!start.isAfter(end)) { "الفترة الزمنية غير صالحة" }
        return "الايرادات_المالية_للفترة_من_" + start + "_الى_" + end
    }
}
