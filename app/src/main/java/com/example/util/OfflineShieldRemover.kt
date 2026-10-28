package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object OfflineShieldRemover {

    /**
     * Removes the background color of an image based on a target color and tolerance [0..100].
     * Tolerance is converted to Euclidean RGB color distance.
     */
    fun removeColorBackground(
        source: Bitmap,
        targetColor: Int = source.getPixel(0, 0),
        tolerancePercentage: Int = 20
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = source.copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        val tr = Color.red(targetColor)
        val tg = Color.green(targetColor)
        val tb = Color.blue(targetColor)

        // Max possible Euclidean distance in RGB is sqrt(255^2 * 3) ~ 441.67
        val maxDist = 441.67f
        val threshold = (tolerancePercentage / 100f) * maxDist

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)

            val dist = sqrt(
                ((r - tr) * (r - tr) + (g - tg) * (g - tg) + (b - tb) * (b - tb)).toDouble()
            ).toFloat()

            if (dist <= threshold) {
                // Set pixel transparent
                pixels[i] = Color.TRANSPARENT
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Applies manual erase or paint on the mutable bitmap at coordinates (x, y) with radius.
     * isErase = true: clears pixels to transparent
     * isErase = false: draws with paintColor
     */
    fun applyBrush(
        bitmap: Bitmap,
        x: Float,
        y: Float,
        radius: Float,
        isErase: Boolean,
        paintColor: Int = Color.BLACK
    ) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (isErase) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = paintColor
                style = Paint.Style.FILL
            }
        }
        canvas.drawCircle(x, y, radius, paint)
    }

    /**
     * Scales bitmap proportionally so its largest dimension is at most maxDimension,
     * protecting against OutOfMemory crashes on large camera photos.
     */
    fun scaleBitmapPreservingRatio(source: Bitmap, maxDimension: Int = 512): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source

        val ratio = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val dstWidth = (width * ratio).toInt().coerceAtLeast(1)
        val dstHeight = (height * ratio).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(source, dstWidth, dstHeight, true)
    }
}
