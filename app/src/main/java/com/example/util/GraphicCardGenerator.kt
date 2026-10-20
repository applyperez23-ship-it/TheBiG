package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.local.entity.Tournament
import com.example.domain.rules.TeamStanding
import java.io.File
import java.io.FileOutputStream

object GraphicCardGenerator {

    fun generateStandingsBitmap(
        tournament: Tournament,
        standings: List<TeamStanding>
    ): Bitmap {
        val width = 1080
        val headerHeight = 260
        val rowHeight = 76
        val footerHeight = 120
        val height = headerHeight + (standings.size.coerceAtLeast(1) * rowHeight) + footerHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#090D16")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Header Background Card
        val cardPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#121B2A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, 40f, width - 40f, 220f), 24f, 24f, cardPaint)

        // Gold Accent Bar
        val goldPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#F59E0B")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, 40f, 56f, 220f), 8f, 8f, goldPaint)

        // Header Title
        val titlePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(tournament.name.uppercase(), 80f, 110f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Edición ${tournament.edition} • Tabla de Posiciones", 80f, 160f, subtitlePaint)

        val badgePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#F59E0B")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("TheBiG", width - 200f, 110f, badgePaint)

        // Table Header Row
        val colHeaderY = headerHeight.toFloat()
        val thPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#F59E0B")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("#", 60f, colHeaderY, thPaint)
        canvas.drawText("EQUIPO", 130f, colHeaderY, thPaint)
        canvas.drawText("PJ", 540f, colHeaderY, thPaint)
        canvas.drawText("PG", 630f, colHeaderY, thPaint)
        canvas.drawText("PE", 720f, colHeaderY, thPaint)
        canvas.drawText("PP", 810f, colHeaderY, thPaint)
        canvas.drawText("DG", 900f, colHeaderY, thPaint)
        canvas.drawText("PTS", 990f, colHeaderY, thPaint)

        // Divider
        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1E293B")
            strokeWidth = 2f
        }
        canvas.drawLine(40f, colHeaderY + 16f, width - 40f, colHeaderY + 16f, linePaint)

        // Rows
        val rowTextPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val statTextPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val ptsTextPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#F59E0B")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var currentY = colHeaderY + 24f

        for (s in standings) {
            currentY += rowHeight

            // Zebra background for alternating rows
            if (s.position % 2 == 0) {
                val rowBg = Paint().apply {
                    color = android.graphics.Color.parseColor("#0F172A")
                }
                canvas.drawRect(40f, currentY - 50f, width - 40f, currentY + 20f, rowBg)
            }

            canvas.drawText(s.position.toString(), 60f, currentY, thPaint)

            val name = if (s.team.name.length > 18) s.team.name.take(18) + "..." else s.team.name
            canvas.drawText(name, 130f, currentY, rowTextPaint)

            canvas.drawText(s.pj.toString(), 540f, currentY, statTextPaint)
            canvas.drawText(s.pg.toString(), 630f, currentY, statTextPaint)
            canvas.drawText(s.pe.toString(), 720f, currentY, statTextPaint)
            canvas.drawText(s.pp.toString(), 810f, currentY, statTextPaint)

            val dgStr = if (s.dg > 0) "+${s.dg}" else s.dg.toString()
            canvas.drawText(dgStr, 900f, currentY, statTextPaint)

            canvas.drawText(s.pts.toString(), 990f, currentY, ptsTextPaint)
        }

        // Footer
        val footerY = height - 40f
        val footerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("Generado por TheBiG • Desarrollado por Daniel (el Grande)", 60f, footerY, footerPaint)

        return bitmap
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, title: String = "Compartir Tabla") {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "thebig_standings_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "com.aistudio.thebig.ftbol.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
