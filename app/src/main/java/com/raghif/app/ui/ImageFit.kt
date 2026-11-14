package com.raghif.app.ui

import kotlin.math.min

/**
 * Fit an entire image inside a bounded destination, WITHOUT cropping, stretching
 * or changing the source image. Empty bands are intentional when ratios differ.
 * This same calculation is used by the A4 PDF exporter.
 */
data class FitBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

object ImageFit {
    fun contain(
        sourceWidth: Float,
        sourceHeight: Float,
        boxWidth: Float,
        boxHeight: Float
    ): FitBounds {
        require(sourceWidth > 0f && sourceHeight > 0f) { "Invalid image size" }
        require(boxWidth > 0f && boxHeight > 0f) { "Invalid target box" }
        val factor = min(boxWidth / sourceWidth, boxHeight / sourceHeight)
        val width = sourceWidth * factor
        val height = sourceHeight * factor
        val left = (boxWidth - width) / 2f
        val top = (boxHeight - height) / 2f
        return FitBounds(left, top, left + width, top + height)
    }
}
