package com.richtext.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan
import kotlin.math.ceil

data class MediaAttachmentData(
    val kind: String,
    val uri: String,
    val widthDp: Int,
    val heightDp: Int,
    val alt: String
)

class MediaAttachmentSpan(
    private val data: MediaAttachmentData,
    private val density: Float,
    private val bitmap: Bitmap? = null,
    private val lineSpacingMultiplier: Float = 1f
) : ReplacementSpan() {

    fun toMediaAttachmentData(): MediaAttachmentData = data

    private fun getWidthPx(): Int {
        return bitmap?.width ?: (data.widthDp * density).toInt().coerceAtLeast(1)
    }

    private fun getHeightPx(): Int {
        return bitmap?.height ?: (data.heightDp * density).toInt().coerceAtLeast(1)
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val widthPx = getWidthPx()
        if (fm != null) {
            val imageHeightPx = getHeightPx()
            val metricsHeightPx = if (lineSpacingMultiplier > 1f) {
                ceil(imageHeightPx / lineSpacingMultiplier).toInt().coerceAtLeast(1)
            } else {
                imageHeightPx
            }
            fm.ascent = -metricsHeightPx
            fm.descent = 0
            fm.top = fm.ascent
            fm.bottom = 0
        }
        return widthPx
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val widthPx = getWidthPx().toFloat()
        val heightPx = getHeightPx().toFloat()
        val oldColor = paint.color
        val oldStyle = paint.style
        val lineHeight = (bottom - top).toFloat().coerceAtLeast(heightPx)
        val rectTop = top + ((lineHeight - heightPx) / 2f)

        if (bitmap != null) {
            val destRect = RectF(x, rectTop, x + widthPx, rectTop + heightPx)
            canvas.drawBitmap(bitmap, null, destRect, null)
        }

        paint.color = oldColor
        paint.style = oldStyle
    }
}
