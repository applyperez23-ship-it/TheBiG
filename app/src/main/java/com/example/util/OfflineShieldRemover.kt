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
     * Removes the background color of an image strictly from the EXTERIOR boundaries inward.
     * Uses a border-seeded BFS flood-fill algorithm so that interior design elements sharing
     * similar colors (e.g., white stripes, white football, yellow stars) remain completely intact.
     */
    fun removeExteriorContourBackground(
        source: Bitmap,
        tolerancePercentage: Int = 22
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = source.copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample border corner pixels to find representative background color
        val cornerColors = intArrayOf(
            pixels[0],
            pixels[width - 1],
            pixels[(height - 1) * width],
            pixels[width * height - 1]
        )
        // Dominant corner color
        val targetColor = cornerColors[0]
        val tr = Color.red(targetColor)
        val tg = Color.green(targetColor)
        val tb = Color.blue(targetColor)

        val maxDist = 441.67f
        val threshold = (tolerancePercentage / 100f) * maxDist

        fun isColorMatch(c: Int): Boolean {
            val a = Color.alpha(c)
            if (a == 0) return true
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val dist = sqrt(((r - tr) * (r - tr) + (g - tg) * (g - tg) + (b - tb) * (b - tb)).toDouble()).toFloat()
            return dist <= threshold
        }

        val visited = BooleanArray(width * height)
        val queue = IntArray(width * height)
        var head = 0
        var tail = 0

        // Seed from all 4 borders
        for (x in 0 until width) {
            val topIdx = x
            if (!visited[topIdx] && isColorMatch(pixels[topIdx])) {
                visited[topIdx] = true
                queue[tail++] = topIdx
            }
            val btmIdx = (height - 1) * width + x
            if (!visited[btmIdx] && isColorMatch(pixels[btmIdx])) {
                visited[btmIdx] = true
                queue[tail++] = btmIdx
            }
        }
        for (y in 0 until height) {
            val leftIdx = y * width
            if (!visited[leftIdx] && isColorMatch(pixels[leftIdx])) {
                visited[leftIdx] = true
                queue[tail++] = leftIdx
            }
            val rightIdx = y * width + (width - 1)
            if (!visited[rightIdx] && isColorMatch(pixels[rightIdx])) {
                visited[rightIdx] = true
                queue[tail++] = rightIdx
            }
        }

        // BFS traversal strictly confined to the exterior zone
        val dx = intArrayOf(1, -1, 0, 0)
        val dy = intArrayOf(0, 0, 1, -1)

        while (head < tail) {
            val curr = queue[head++]
            pixels[curr] = Color.TRANSPARENT

            val cx = curr % width
            val cy = curr / width

            for (i in 0 until 4) {
                val nx = cx + dx[i]
                val ny = cy + dy[i]

                if (nx in 0 until width && ny in 0 until height) {
                    val nIdx = ny * width + nx
                    if (!visited[nIdx]) {
                        visited[nIdx] = true
                        if (isColorMatch(pixels[nIdx])) {
                            queue[tail++] = nIdx
                        }
                    }
                }
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Backward-compatible alias directing to the smart contour algorithm.
     */
    fun removeColorBackground(
        source: Bitmap,
        targetColor: Int = source.getPixel(0, 0),
        tolerancePercentage: Int = 20
    ): Bitmap {
        return removeExteriorContourBackground(source, tolerancePercentage)
    }

    /**
     * Applies manual erase or paint on the mutable bitmap at coordinates (x, y) with radius.
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
     * Interpolates brush between previous and current touch points to produce
     * fluid continuous lines without gaps during fast drag motions.
     */
    fun applyBrushLine(
        bitmap: Bitmap,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        radius: Float,
        isErase: Boolean,
        paintColor: Int = Color.WHITE
    ) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = radius * 2f
            strokeCap = Paint.Cap.ROUND
            if (isErase) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = paintColor
                style = Paint.Style.STROKE
            }
        }
        canvas.drawLine(startX, startY, endX, endY, paint)
        // Also draw end circle
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (isErase) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = paintColor
                style = Paint.Style.FILL
            }
        }
        canvas.drawCircle(endX, endY, radius, circlePaint)
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
