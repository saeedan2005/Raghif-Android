package com.raghif.app

import com.raghif.app.data.Bakery
import com.raghif.app.data.DailyRecord
import com.raghif.app.data.DailyRecordStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DailyRecordStoreTest {
    private val bakery = Bakery(
        id = 12L,
        number = "12",
        normalizedNumber = "12",
        name = "مخبز الرحمة",
        normalizedName = "مخبز الرحمة",
        unitRevenue = 500,
        piecesPerBag = 9,
        flourBagWeight = 50.0
    )

    private fun record(date: String, id: Long, revenue: Long = 500) = DailyRecord(
        id = id,
        bakeryId = bakery.id,
        date = date,
        flourKg = 50,
        pieces = 200,
        revenue = revenue,
        unitRevenueUsed = 400,
        piecesPerBagUsed = 8,
        flourBagWeightUsed = 25.0
    )

    @Test fun newPastDateGetsNewIdentityAndCurrentBakeryConfiguration() {
        val todayOpened = record("2026-11-11", 74L)
        val pastDateEntered = todayOpened.copy(date = "2026-11-08", revenue = 250L)
        val result = DailyRecordStore.forSelectedDate(bakery, pastDateEntered, null)
        assertEquals("2026-11-08", result.date)
        assertEquals(0L, result.id)
        assertNotEquals(todayOpened.id, result.id)
        assertEquals(500L, result.unitRevenueUsed)
        assertEquals(9L, result.piecesPerBagUsed)
        assertEquals(50.0, result.flourBagWeightUsed, 0.00001)
    }

    @Test fun existingSelectedDateUsesOnlyMatchedDateIdentityAndOriginalPrice() {
        val originallyOpen = record("2026-11-11", 74L)
        val matchingPast = record("2026-11-08", 18L)
        val changedFields = originallyOpen.copy(date = "2026-11-08", revenue = 888L)
        val result = DailyRecordStore.forSelectedDate(bakery, changedFields, matchingPast)
        assertEquals(matchingPast.id, result.id)
        assertEquals(matchingPast.date, result.date)
        assertEquals(888L, result.revenue)
        assertEquals(400L, result.unitRevenueUsed)
        assertEquals(8L, result.piecesPerBagUsed)
        assertNotEquals(originallyOpen.id, result.id)
    }

    @Test fun sameDateEditPreservesItsIdAndHistoricalPrice() {
        val existing = record("2026-11-11", 74L)
        val edited = existing.copy(revenue = 670L)
        val result = DailyRecordStore.forSelectedDate(bakery, edited, existing)
        assertEquals(74L, result.id)
        assertEquals(400L, result.unitRevenueUsed)
        assertEquals(670L, result.revenue)
    }

    @Test fun cannotOverwriteAnotherBakeryOrDate() {
        val selected = record("2026-11-08", 0L)
        val wrongDate = record("2026-11-11", 74L)
        assertThrows(IllegalArgumentException::class.java) {
            DailyRecordStore.forSelectedDate(bakery, selected, wrongDate)
        }
    }
}
