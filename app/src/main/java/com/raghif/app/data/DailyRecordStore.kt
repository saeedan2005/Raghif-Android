package com.raghif.app.data

import androidx.room.withTransaction
import java.time.LocalDate

/**
 * Stores exactly one daily record per (bakeryId, date).
 *
 * Critically, the id of the record previously shown on screen is NOT used to
 * select the row to update. Selecting another date never rewrites the
 * previously opened record.
 *
 * All calculations remain in the existing ReportMath class unchanged.
 */
object DailyRecordStore {
    fun forSelectedDate(
        bakery: Bakery,
        entered: DailyRecord,
        recordAtSelectedDate: DailyRecord?
    ): DailyRecord {
        require(entered.bakeryId == bakery.id) { "المخبز المحدد لا يطابق السجل" }
        require(runCatching { LocalDate.parse(entered.date) }.isSuccess) { "تاريخ السجل غير صالح" }
        require(recordAtSelectedDate == null ||
                (recordAtSelectedDate.bakeryId == bakery.id && recordAtSelectedDate.date == entered.date)
        ) { "لا يمكن تعديل سجل لمخبز أو تاريخ مختلف" }

        // New date => new id and current bakery configuration.
        // Existing date => keep the historical selling configuration and id.
        return entered.copy(
            id = recordAtSelectedDate?.id ?: 0L,
            unitRevenueUsed = recordAtSelectedDate?.unitRevenueUsed ?: bakery.unitRevenue,
            piecesPerBagUsed = recordAtSelectedDate?.piecesPerBagUsed ?: bakery.piecesPerBag,
            flourBagWeightUsed = recordAtSelectedDate?.flourBagWeightUsed ?: bakery.flourBagWeight
        )
    }

    /** Existing records may only be replaced after explicit user confirmation. */
    class ExistingRecordNeedsConfirmation : IllegalStateException("يوجد سجل سابق لهذا المخبز في هذا اليوم")

    fun needsConfirmation(recordAtSelectedDate: DailyRecord?): Boolean = recordAtSelectedDate != null

    suspend fun save(
        database: AppDatabase,
        bakery: Bakery,
        entered: DailyRecord,
        overwriteConfirmed: Boolean = false
    ): DailyRecord = database.withTransaction {
            val matchingDate = database.dailyDao().byDate(bakery.id, entered.date)
            if (needsConfirmation(matchingDate) && !overwriteConfirmed) {
                throw ExistingRecordNeedsConfirmation()
            }
            val target = forSelectedDate(bakery, entered, matchingDate)
            if (matchingDate == null) {
                target.copy(id = database.dailyDao().insert(target))
            } else {
                database.dailyDao().update(target)
                target
            }
        }
}
