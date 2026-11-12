package com.raghif.app

import com.raghif.app.reports.ReportFileNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class ReportFileNamesTest {
    @Test fun dailyUsesBakeryNameAndSelectedDate() {
        assertEquals(
            "سجل_رغيف_اليومي_مخبز_الرحمة_2026-11-12",
            ReportFileNames.daily("مخبز الرحمة", "2026-11-12")
        )
    }
    @Test fun financialUsesSelectedRange() {
        assertEquals(
            "الايرادات_المالية_للفترة_من_2026-11-01_الى_2026-11-30",
            ReportFileNames.financial("2026-11-01", "2026-11-30")
        )
    }
    @Test fun safeBakeryNamesCannotContainSlashes() {
        val name=ReportFileNames.daily("مخبز الخير / شمال", "2026-11-12")
        assertFalse(name.contains("/"))
    }
    @Test fun reversedDatesAreInvalid() {
        assertThrows(IllegalArgumentException::class.java) {
            ReportFileNames.financial("2026-12-01", "2026-11-01")
        }
    }
}
