package com.raghif.app.ui

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Presentation-only numeric formatting.
 * Values are rounded only for display, never before calculations, storage,
 * or report aggregation. All displayed quantities have at most one decimal.
 */
object RaghifDisplayNumber {
    fun format(value: Number, locale: Locale = Locale.US): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
            roundingMode = RoundingMode.HALF_UP
            isGroupingUsed = true
        }.format(value)
}
