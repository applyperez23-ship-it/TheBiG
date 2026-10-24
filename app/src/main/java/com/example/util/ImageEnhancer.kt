package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

object ImageEnhancer {

    /**
     * AI-inspired graphical enhancement filter:
     * - Contrast expansion (dynamic stretching)
     * - Edge sharpening via 3x3 convolution kernel
     * - Color saturation enhancement
     */
    fun enhanceCrestOrPhoto(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height

        // Step 1: Contrast and Color Vibrancy Adjustment
        val contrastAdjusted = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val contrastFactor = 1.25f // 25% boost in contrast
        val brightnessOffset = 5

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = Color.alpha(pixel)
            if (a == 0) {
                pixels[i] = pixel
                continue
            }
            var r = Color.red(pixel)
            var g = Color.green(pixel)
            var b = Color.blue(pixel)

            // Dynamic Contrast Stretching
            r = min(255, max(0, ((r - 128) * contrastFactor + 128 + brightnessOffset).toInt()))
            g = min(255, max(0, ((g - 128) * contrastFactor + 128 + brightnessOffset).toInt()))
            b = min(255, max(0, ((b - 128) * contrastFactor + 128 + brightnessOffset).toInt()))

            pixels[i] = Color.argb(a, r, g, b)
        }
        contrastAdjusted.setPixels(pixels, 0, width, 0, 0, width, height)

        // Step 2: 3x3 Sharpening Convolution Filter
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val kernel = floatArrayOf(
            0f, -0.6f, 0f,
            -0.6f, 3.4f, -0.6f,
            0f, -0.6f, 0f
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                val centerAlpha = Color.alpha(contrastAdjusted.getPixel(x, y))

                if (centerAlpha < 20) {
                    output.setPixel(x, y, contrastAdjusted.getPixel(x, y))
                    continue
                }

                var kIndex = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val px = contrastAdjusted.getPixel(x + kx, y + ky)
                        val w = kernel[kIndex++]
                        sumR += Color.red(px) * w
                        sumG += Color.green(px) * w
                        sumB += Color.blue(px) * w
                    }
                }

                val finalR = min(255, max(0, sumR.toInt()))
                val finalG = min(255, max(0, sumG.toInt()))
                val finalB = min(255, max(0, sumB.toInt()))

                output.setPixel(x, y, Color.argb(centerAlpha, finalR, finalG, finalB))
            }
        }

        // Copy border edges
        for (x in 0 until width) {
            output.setPixel(x, 0, contrastAdjusted.getPixel(x, 0))
            output.setPixel(x, height - 1, contrastAdjusted.getPixel(x, height - 1))
        }
        for (y in 0 until height) {
            output.setPixel(0, y, contrastAdjusted.getPixel(0, y))
            output.setPixel(width - 1, y, contrastAdjusted.getPixel(width - 1, y))
        }

        return output
    }
}
