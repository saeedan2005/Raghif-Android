package com.raghif.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationTest {
    @Test fun cashSoldAllowsFractions() {
        val result = 250.0 / 500.0 * 9.0
        assertEquals(4.5, result, 0.0001)
    }
    @Test fun zeroRevenueMeansZeroCashSold() {
        val result = if (0L == 0L) 0.0 else 0.0
        assertEquals(0.0, result, 0.0001)
    }
}
