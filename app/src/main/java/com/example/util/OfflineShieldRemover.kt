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
     * Applies manual erase or paint along an interpolated line between two drag coordinates.
     */
    fun applyBrushLine(
        bitmap: Bitmap,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        radius: Float,
        isErase: Boolean = true,
        paintColor: Int = Color.BLACK
    ) {
        val canvas = Canvas(bitmap)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (isErase) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = paintColor
            }
            strokeWidth = radius * 2f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        canvas.drawLine(startX, startY, endX, endY, linePaint)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (isErase) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = paintColor
                style = Paint.Style.FILL
            }
        }
        canvas.drawCircle(startX, startY, radius, circlePaint)
        canvas.drawCircle(endX, endY, radius, circlePaint)
    }

    /**
     * Magic Wand tool: starts flood-fill at touch point (touchX, touchY) erasing
     * all connected pixels that match the tapped color within tolerance.
     */
    fun magicWandFloodFill(
        bitmap: Bitmap,
        touchX: Float,
        touchY: Float,
        tolerancePercentage: Int = 20
    ) {
        val width = bitmap.width
        val height = bitmap.height
        val startX = touchX.toInt().coerceIn(0, width - 1)
        val startY = touchY.toInt().coerceIn(0, height - 1)

        val targetColor = bitmap.getPixel(startX, startY)
        if (Color.alpha(targetColor) == 0) return

        val tr = Color.red(targetColor)
        val tg = Color.green(targetColor)
        val tb = Color.blue(targetColor)

        val maxDist = 441.67f
        val threshold = (tolerancePercentage / 100f) * maxDist

        fun isColorMatch(c: Int): Boolean {
            val a = Color.alpha(c)
            if (a == 0) return false
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

        val startIdx = startY * width + startX
        visited[startIdx] = true
        queue[tail++] = startIdx

        val dx = intArrayOf(1, -1, 0, 0)
        val dy = intArrayOf(0, 0, 1, -1)

        while (head < tail) {
            val curr = queue[head++]
            val cx = curr % width
            val cy = curr / width
            bitmap.setPixel(cx, cy, Color.TRANSPARENT)

            for (i in 0 until 4) {
                val nx = cx + dx[i]
                val ny = cy + dy[i]
                if (nx in 0 until width && ny in 0 until height) {
                    val nIdx = ny * width + nx
                    if (!visited[nIdx]) {
                        visited[nIdx] = true
                        val pixel = bitmap.getPixel(nx, ny)
                        if (isColorMatch(pixel)) {
                            queue[tail++] = nIdx
                        }
                    }
                }
            }
        }
    }

    /**
     * Pencil tool: restores exact original pixels from originalBitmap into the working bitmap.
     */
    fun restoreOriginalPixels(
        bitmap: Bitmap,
        originalBitmap: Bitmap,
        cx: Float,
        cy: Float,
        radius: Float
    ) {
        val width = bitmap.width
        val height = bitmap.height
        val minX = (cx - radius).toInt().coerceIn(0, width - 1)
        val maxX = (cx + radius).toInt().coerceIn(0, width - 1)
        val minY = (cy - radius).toInt().coerceIn(0, height - 1)
        val maxY = (cy + radius).toInt().coerceIn(0, height - 1)
        val r2 = radius * radius

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy <= r2) {
                    bitmap.setPixel(x, y, originalBitmap.getPixel(x, y))
                }
            }
        }
    }

    /**
     * Restores original pixels along a continuous interpolated line between two drag points.
     */
    fun restoreOriginalPixelsLine(
        bitmap: Bitmap,
        originalBitmap: Bitmap,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        radius: Float
    ) {
        val dist = sqrt(((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY)).toDouble()).toFloat()
        val steps = (dist / (radius * 0.5f)).toInt().coerceAtLeast(1)
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = startX + (endX - startX) * t
            val y = startY + (endY - startY) * t
            restoreOriginalPixels(bitmap, originalBitmap, x, y, radius)
        }
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
