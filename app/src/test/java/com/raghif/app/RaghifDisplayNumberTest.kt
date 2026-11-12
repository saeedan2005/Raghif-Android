package com.raghif.app

import com.raghif.app.ui.RaghifDisplayNumber
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class RaghifDisplayNumberTest {
    @Test fun integerShowsNoDecimal() {
        assertEquals("10",RaghifDisplayNumber.format(10.0))
    }
    @Test fun trailingZeroIsRemoved() {
        assertEquals("12.5",RaghifDisplayNumber.format(12.50))
    }
    @Test fun moreThanOneDecimalRoundsForDisplayOnly() {
        val precise = 15.75
        assertEquals("15.8",RaghifDisplayNumber.format(precise))
        assertEquals(15.75,precise,0.0)
    }
    @Test fun negativeAndGroupedAmountsHaveOneDecimalAtMost() {
        assertEquals("-1,234.6",RaghifDisplayNumber.format(-1234.56))
    }
    @Test fun arabicLocaleIsSupported() {
        val value=RaghifDisplayNumber.format(18.75,Locale("ar"))
        assertEquals(1,value.count { it == '.' || it == '٫' })
    }
}
