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
import java.io.InputStream

data class ChampionAwardInfo(
    val team: Team,
    val tournament: Tournament,
    val bestGkPlayerName: String? = null,
    val topScorerPlayerName: String? = null,
    val topAssisterPlayerName: String? = null,
    val dateText: String = "OCTUBRE 2026"
)

object ChampionStudioHelper {

    fun generateChampionCompositeBitmap(
        context: Context,
        imageUri: Uri?,
        info: ChampionAwardInfo,
        titleOffsetYPercent: Float, // 0.05f to 0.40f
        titleOffsetXPercent: Float, // 0.1f to 0.9f
        overlayOpacity: Float // 0.2f to 1.0f
    ): Bitmap {
        var baseBitmap: Bitmap? = null
        if (imageUri != null) {
            try {
                val input: InputStream? = context.contentResolver.openInputStream(imageUri)
                baseBitmap = BitmapFactory.decodeStream(input)
                input?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (baseBitmap == null) {
            baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.thebig_champion_bg_1792365669962)
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
            color = android.graphics.Color.argb((overlayOpacity * 140).toInt(), 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, targetHeight - 340f, targetWidth.toFloat(), targetHeight.toFloat(), vignettePaint)

        // 1. Watermark in top-right corner
        val watermarkPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 200).toInt(), 255, 255, 255)
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("TheBiG", targetWidth - 200f, 70f, watermarkPaint)

        val creatorPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 160).toInt(), 245, 158, 11)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Oficial", targetWidth - 200f, 100f, creatorPaint)

        // 2. Team Name Header with adjustable position
        val headerX = (targetWidth * titleOffsetXPercent).coerceIn(100f, targetWidth - 600f)
        val headerY = (targetHeight * titleOffsetYPercent).coerceIn(80f, targetHeight - 200f)

        // Background pill for team name
        val textStr = "CAMPEON: ${info.team.name.uppercase()}"
        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val textWidth = textPaint.measureText(textStr)

        val pillPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 210).toInt(), 18, 27, 42)
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

        // Subtitle: Tournament & Edition
        val tourneyPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 230).toInt(), 245, 158, 11)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("${info.tournament.name} • EDICION ${info.tournament.edition}", headerX, headerY - 85f, tourneyPaint)

        // 3. Side & Bottom Award Badges (Portero, Goleador, Asistidor)
        val badgeCardPaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 210).toInt(), 15, 23, 42)
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
            color = android.graphics.Color.argb((overlayOpacity * 220).toInt(), 148, 163, 184)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("FECHA: ${info.dateText.uppercase()}", 100f, targetHeight - 230f, datePaint)

        val awardTitlePaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 255).toInt(), 245, 158, 11)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val awardValuePaint = Paint().apply {
            color = android.graphics.Color.argb((overlayOpacity * 255).toInt(), 255, 255, 255)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var colX = 100f
        if (info.bestGkPlayerName != null) {
            canvas.drawText("MEJOR PORTERO (Valla Menos Vencida)", colX, targetHeight - 160f, awardTitlePaint)
            canvas.drawText(info.bestGkPlayerName, colX, targetHeight - 110f, awardValuePaint)
            colX += 540f
        }

        if (info.topScorerPlayerName != null) {
            canvas.drawText("MAXIMO GOLEADOR (Pichichi)", colX, targetHeight - 160f, awardTitlePaint)
            canvas.drawText(info.topScorerPlayerName, colX, targetHeight - 110f, awardValuePaint)
            colX += 540f
        }

        if (info.topAssisterPlayerName != null) {
            canvas.drawText("MAXIMO ASISTIDOR", colX, targetHeight - 160f, awardTitlePaint)
            canvas.drawText(info.topAssisterPlayerName, colX, targetHeight - 110f, awardValuePaint)
        }

        if (info.bestGkPlayerName == null && info.topScorerPlayerName == null && info.topAssisterPlayerName == null) {
            canvas.drawText("EQUIPO CAMPEON DEL TORNEO", 100f, targetHeight - 140f, awardValuePaint)
        }

        return composite
    }
}
