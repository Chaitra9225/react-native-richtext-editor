package com.richtext.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan

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
    private val bitmap: Bitmap? = null
) : ReplacementSpan() {

    fun toMediaAttachmentData(): MediaAttachmentData = data

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val widthPx = (data.widthDp * density).toInt()
        if (fm != null) {
            val heightPx = (data.heightDp * density).toInt()
            fm.ascent = -heightPx
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
        val widthPx = data.widthDp * density
        val heightPx = data.heightDp * density
        val oldColor = paint.color
        val oldStyle = paint.style
        val rectTop = y - heightPx

        if (bitmap != null) {
            val destRect = RectF(x, rectTop, x + widthPx, rectTop + heightPx)
            canvas.drawBitmap(bitmap, null, destRect, null)
        }

        paint.color = oldColor
        paint.style = oldStyle
    }
}
