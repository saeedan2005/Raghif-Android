package com.raghif.app

import com.raghif.app.ui.ImageFit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageFitTest {
    @Test fun wideBannerKeepsNaturalRatio() {
        val fit = ImageFit.contain(1024f, 310f, 360f, 140f)
        assertEquals(360f, fit.width, 0.001f)
        assertEquals(360f * 310f / 1024f, fit.height, 0.001f)
        assertEquals(0f, fit.left, 0.001f)
        assertEquals((140f - fit.height) / 2f, fit.top, 0.001f)
    }

    @Test fun portraitMockupIsNeverCroppedWhenAspectDoesNotMatch() {
        val fit = ImageFit.contain(1024f, 1536f, 412f, 915f)
        assertEquals(412f, fit.width, 0.001f)
        assertEquals(618f, fit.height, 0.001f)
        assertEquals((915f - 618f) / 2f, fit.top, 0.001f)
    }

    @Test fun landscapePdfReferenceFitsPortraitPaperWithoutDistortion() {
        val fit = ImageFit.contain(1536f, 1024f, 595f, 400f)
        assertEquals(595f, fit.width, 0.001f)
        assertEquals(595f * 2f / 3f, fit.height, 0.001f)
    }

    @Test fun invalidSizesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageFit.contain(0f, 100f, 100f, 100f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ImageFit.contain(100f, 100f, 0f, 100f)
        }
    }
}
