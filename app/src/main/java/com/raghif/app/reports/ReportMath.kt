package com.raghif.app.reports

import com.raghif.app.data.DailyRecord

/**
 * A daily record stores deferred sales in PIECES, while revenue is in riyals.
 * Convert the deferred pieces into money using the price/pack size saved on
 * the record (not the bakery's current price).
 */
object ReportMath {
    fun deferredValue(record: DailyRecord): Double {
        if (record.piecesPerBagUsed <= 0L || record.unitRevenueUsed < 0L) return 0.0
        return record.creditSold.toDouble() / record.piecesPerBagUsed * record.unitRevenueUsed
    }

    fun totalValue(record: DailyRecord): Double =
        record.revenue.toDouble() + deferredValue(record)

    fun cashPieces(record: DailyRecord): Double {
        if (record.unitRevenueUsed <= 0L) return 0.0
        return record.revenue.toDouble() / record.unitRevenueUsed * record.piecesPerBagUsed
    }

    fun productionDifference(record: DailyRecord): Double =
        record.pieces.toDouble() - record.workersMeal - record.waste - record.creditSold - cashPieces(record)

    fun averagePiecesPerFlourBag(record: DailyRecord): Double? {
        if (record.flourKg <= 0 || record.flourBagWeightUsed <= 0.0) return null
        return record.pieces.toDouble() / (record.flourKg / record.flourBagWeightUsed)
    }
}
