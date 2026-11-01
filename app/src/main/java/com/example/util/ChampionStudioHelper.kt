package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import com.example.R
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChampionAwardInfo(
    val team: Team,
    val tournament: Tournament,
    val bestGkPlayerName: String? = null,
    val topScorerPlayerName: String? = null,
    val topAssisterPlayerName: String? = null,
    val dateText: String = getCurrentMonthYearSpanish()
) {
    companion object {
        fun getCurrentMonthYearSpanish(): String {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            return sdf.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }
}

object ChampionStudioHelper {

    /**
     * Decodes an image URI with inSampleSize to safely bound max memory usage and avoid OOM.
     * Automatically corrects EXIF rotation orientation and applies lighting enhancement.
     */
    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        imageUri: Uri,
        reqWidth: Int = 1920,
        reqHeight: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val decoded = context.contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: return@withContext null

            // Read EXIF orientation to prevent upside-down or sideways camera photos
            val orientation = try {
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    val exif = android.media.ExifInterface(input)
                    exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
                } ?: android.media.ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                android.media.ExifInterface.ORIENTATION_NORMAL
            }

            val matrix = android.graphics.Matrix()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            val orientedBitmap = if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (rotated != decoded) decoded.recycle()
                rotated
            } else {
                decoded
            }

            // Professional illumination and clarity boost so photos never look dark
            ImageEnhancer.enhanceCrestOrPhoto(orientedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Generates champion composite bitmap safely in the background on Dispatchers.IO.
     */
    suspend fun generateChampionCompositeBitmap(
        context: Context,
        imageUri: Uri?,
        info: ChampionAwardInfo,
        titleOffsetYPercent: Float, // 0.05f to 0.40f
        titleOffsetXPercent: Float, // 0.1f to 0.9f
        overlayOpacity: Float // 0.2f to 1.0f
    ): Bitmap = withContext(Dispatchers.IO) {
        var baseBitmap: Bitmap? = null
        if (imageUri != null) {
            baseBitmap = decodeSampledBitmapFromUri(context, imageUri, 1920, 1080)
        }

        if (baseBitmap == null) {
            try {
                baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.thebig_champion_bg_1792365669962)
            } catch (e: Exception) {
                // Fallback blank canvas
                baseBitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(android.graphics.Color.parseColor("#0F172A"))
                }
            }
        }

        val targetWidth = 1920
        val targetHeight = 1080
        val composite = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)

        // Draw Base Image scaled to fit targetWidth x targetHeight
        val srcRect = Rect(0, 0, baseBitmap.width, baseBitmap.height)
        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        val imgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(baseBitmap, srcRect, dstRect, imgPaint)

        // Subtle gradient vignette around bottom for badge contrast
        val vignettePaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 150).toInt(), 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, targetHeight - 340f, targetWidth.toFloat(), targetHeight.toFloat(), vignettePaint)

        // 1. Watermark in top-right corner
        val watermarkPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 210).toInt(), 255, 255, 255)
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("TheBiG", targetWidth - 210f, 70f, watermarkPaint)

        val officialPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 180).toInt(), 245, 158, 11)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Oficial", targetWidth - 210f, 100f, officialPaint)

        // 2. Team Name Header with adjustable position
        val headerX = (targetWidth * titleOffsetXPercent).coerceIn(100f, targetWidth - 700f)
        val headerY = (targetHeight * titleOffsetYPercent).coerceIn(90f, targetHeight - 200f)

        val textStr = "CAMPEÓN: ${info.team.name.uppercase()}"
        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val textWidth = textPaint.measureText(textStr)

        val pillPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 220).toInt(), 18, 27, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(headerX - 30f, headerY - 70f, headerX + textWidth + 30f, headerY + 25f),
            20f,
            20f,
            pillPaint
        )

        // Golden accent border
        val borderPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 255).toInt(), 245, 158, 11)
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(headerX - 30f, headerY - 70f, headerX + textWidth + 30f, headerY + 25f),
            20f,
            20f,
            borderPaint
        )

        canvas.drawText(textStr, headerX, headerY, textPaint)

        // Subtitle: Tournament
        val tourneyPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 240).toInt(), 245, 158, 11)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(info.tournament.name.uppercase(), headerX, headerY - 85f, tourneyPaint)

        // 3. Side & Bottom Award Badges (Portero, Goleador, Asistidor)
        val badgeCardPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 220).toInt(), 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(60f, targetHeight - 280f, targetWidth - 60f, targetHeight - 60f),
            24f,
            24f,
            badgeCardPaint
        )
        canvas.drawRoundRect(
            RectF(60f, targetHeight - 280f, targetWidth - 60f, targetHeight - 60f),
            24f,
            24f,
            borderPaint
        )

        val datePaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 230).toInt(), 148, 163, 184)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("FECHA: ${info.dateText.uppercase()}", 100f, targetHeight - 230f, datePaint)

        val awardTitlePaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 255).toInt(), 245, 158, 11)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val awardValuePaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 255).toInt(), 255, 255, 255)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var colX = 100f
        if (info.bestGkPlayerName != null) {
            canvas.drawText("MEJOR PORTERO (Valla Menos Vencida)", colX, targetHeight - 160f, awardTitlePaint)
            canvas.drawText(info.bestGkPlayerName, colX, targetHeight - 110f, awardValuePaint)
            colX += 560f
        }

        if (info.topScorerPlayerName != null) {
            canvas.drawText("MÁXIMO GOLEADOR (Pichichi)", colX, targetHeight - 160f, awardTitlePaint)
            canvas.drawText(info.topScorerPlayerName, colX, targetHeight - 110f, awardValuePaint)
            colX += 560f
        }

        if (info.topAssisterPlayerName != null) {
            canvas.drawText("MÁXIMO ASISTIDOR", colX, targetHeight - 160f, awardTitlePaint)
            canvas.drawText(info.topAssisterPlayerName, colX, targetHeight - 110f, awardValuePaint)
        }

        if (info.bestGkPlayerName == null && info.topScorerPlayerName == null && info.topAssisterPlayerName == null) {
            canvas.drawText("EQUIPO CAMPEÓN DEL TORNEO", 100f, targetHeight - 140f, awardValuePaint)
        }

        composite
    }
}
