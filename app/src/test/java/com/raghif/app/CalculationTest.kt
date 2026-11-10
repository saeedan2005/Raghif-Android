package com.raghif.app

import com.raghif.app.data.DailyRecord
import com.raghif.app.reports.ReportMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculationTest {
    private fun record(
        pieces: Long = 1200,
        revenue: Long = 5000,
        creditSold: Long = 18,
        unitRevenue: Long = 500,
        piecesPerBag: Long = 9,
        flourKg: Long = 50
    ) = DailyRecord(
        bakeryId = 1,
        date = "2026-11-10",
        flourKg = flourKg,
        pieces = pieces,
        workersMeal = 5,
        waste = 4,
        creditSold = creditSold,
        revenue = revenue,
        unitRevenueUsed = unitRevenue,
        piecesPerBagUsed = piecesPerBag,
        flourBagWeightUsed = 50.0
    )

    @Test fun cashSoldAllowsFractions() {
        assertEquals(4.5, ReportMath.cashPieces(record(revenue = 250)), 0.0001)
    }

    @Test fun zeroRevenueMeansZeroCashPieces() {
        assertEquals(0.0, ReportMath.cashPieces(record(unitRevenue = 0)), 0.0001)
    }

    @Test fun deferredSalesPiecesConvertToRiyalsAtHistoricalPrice() {
        assertEquals(1000.0, ReportMath.deferredValue(record()), 0.0001)
        assertEquals(6000.0, ReportMath.totalValue(record()), 0.0001)
    }

    @Test fun deferredSalesAllowsFractionsAndZeroPackSize() {
        assertEquals(250.0, ReportMath.deferredValue(record(creditSold=3,unitRevenue=500,piecesPerBag=6)), 0.0001)
        assertEquals(0.0, ReportMath.deferredValue(record(piecesPerBag=0)), 0.0001)
    }

    @Test fun dailyDifferenceUsesPiecesNotMoney() {
        // 1200 - (5 meal + 4 waste + 18 credit + 90 cash pieces)
        assertEquals(1083.0, ReportMath.productionDifference(record()), 0.0001)
    }

    @Test fun averageHandlesMissingFlourWithoutDivideByZero() {
        assertEquals(1200.0, ReportMath.averagePiecesPerFlourBag(record())!!, 0.0001)
        assertNull(ReportMath.averagePiecesPerFlourBag(record(flourKg=0)))
    }
}
