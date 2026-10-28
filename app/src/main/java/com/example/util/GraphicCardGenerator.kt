package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.local.entity.Match
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.domain.rules.ScorerStat
import com.example.domain.rules.TeamStanding
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object GraphicCardGenerator {

    private const val CARD_SIZE = 1080

    /**
     * Generates a 1080x1080 px Instagram feed card for the Standings Table.
     */
    fun generateStandingsBitmap(tournament: Tournament, standings: List<TeamStanding>): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_SIZE, CARD_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawInstagramBackground(canvas)
        drawWatermarkAndBranding(canvas, tournament.name, "TABLA DE POSICIONES • EDICION ${tournament.edition.uppercase()}")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Table Header Bar
        val headerY = 250f
        paint.color = Color.parseColor("#1E293B")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(60f, headerY, 1020f, headerY + 54f), 12f, 12f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("POS", 80f, headerY + 36f, paint)
        canvas.drawText("EQUIPO", 170f, headerY + 36f, paint)
        canvas.drawText("PJ", 600f, headerY + 36f, paint)
        canvas.drawText("PG", 670f, headerY + 36f, paint)
        canvas.drawText("PE", 740f, headerY + 36f, paint)
        canvas.drawText("PP", 810f, headerY + 36f, paint)
        canvas.drawText("DG", 880f, headerY + 36f, paint)
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawText("PTS", 950f, headerY + 36f, paint)

        // Draw Rows (up to 12 teams max for 1080 height)
        var rowY = headerY + 70f
        val displayStandings = standings.take(10)

        displayStandings.forEachIndexed { index, item ->
            // Row card background
            paint.color = if (index % 2 == 0) Color.parseColor("#151D2A") else Color.parseColor("#0F172A")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(60f, rowY, 1020f, rowY + 56f), 8f, 8f, paint)

            // Pos
            paint.color = if (index == 0) Color.parseColor("#F59E0B") else Color.WHITE
            paint.textSize = 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${index + 1}", 90f, rowY + 38f, paint)

            // Team Crest Color Dot
            val teamColor = try {
                Color.parseColor(item.team.primaryColorHex)
            } catch (e: Exception) {
                Color.parseColor("#0284C7")
            }
            paint.color = teamColor
            canvas.drawCircle(190f, rowY + 28f, 14f, paint)

            // Team Name
            paint.color = Color.WHITE
            paint.textSize = 25f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val truncatedName = if (item.team.name.length > 18) item.team.name.substring(0, 16) + ".." else item.team.name
            canvas.drawText(truncatedName, 220f, rowY + 37f, paint)

            // Stats
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#CBD5E1")
            paint.textSize = 23f
            canvas.drawText("${item.pj}", 605f, rowY + 37f, paint)
            canvas.drawText("${item.pg}", 675f, rowY + 37f, paint)
            canvas.drawText("${item.pe}", 745f, rowY + 37f, paint)
            canvas.drawText("${item.pp}", 815f, rowY + 37f, paint)
            canvas.drawText("${item.dg}", 885f, rowY + 37f, paint)

            // PTS
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#F59E0B")
            paint.textSize = 26f
            canvas.drawText("${item.pts}", 955f, rowY + 38f, paint)

            rowY += 66f
        }

        drawFooterBrand(canvas)
        return bitmap
    }

    /**
     * Generates a 1080x1080 px Instagram feed card for the Fixture / Matches.
     */
    fun generateFixtureBitmap(tournament: Tournament, matches: List<Match>, teams: List<Team>): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_SIZE, CARD_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val teamMap = teams.associateBy { it.id }

        drawInstagramBackground(canvas)
        drawWatermarkAndBranding(canvas, tournament.name, "FIXTURE OFICIAL • JORNADA")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var rowY = 240f
        val displayMatches = matches.take(7)

        displayMatches.forEach { match ->
            val home = teamMap[match.homeTeamId]
            val away = teamMap[match.awayTeamId]

            // Match Box
            paint.color = Color.parseColor("#151D2A")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(60f, rowY, 1020f, rowY + 86f), 14f, 14f, paint)

            // Home Team Name
            paint.color = Color.WHITE
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            val hName = home?.name?.take(16) ?: "Local"
            canvas.drawText(hName, 440f, rowY + 54f, paint)

            // Score or VS in Center
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.parseColor("#1E293B")
            canvas.drawRoundRect(RectF(480f, rowY + 16f, 600f, rowY + 70f), 10f, 10f, paint)

            if (match.status == "SCHEDULED") {
                paint.color = Color.parseColor("#38BDF8")
                paint.textSize = 24f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("VS", 540f, rowY + 52f, paint)
            } else {
                paint.color = Color.parseColor("#F59E0B")
                paint.textSize = 30f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${match.homeScore} - ${match.awayScore}", 540f, rowY + 54f, paint)
            }

            // Away Team Name
            paint.color = Color.WHITE
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.LEFT
            val aName = away?.name?.take(16) ?: "Visitante"
            canvas.drawText(aName, 640f, rowY + 54f, paint)

            // Round small label
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(match.roundName, 80f, rowY + 52f, paint)

            rowY += 100f
        }

        paint.textAlign = Paint.Align.LEFT
        drawFooterBrand(canvas)
        return bitmap
    }

    /**
     * Generates a 1080x1080 px Instagram feed card for the Top Scorers.
     */
    fun generateScorersBitmap(tournament: Tournament, scorers: List<ScorerStat>): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_SIZE, CARD_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawInstagramBackground(canvas)
        drawWatermarkAndBranding(canvas, tournament.name, "TABLA DE GOLEADORES")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var rowY = 250f
        val displayScorers = scorers.take(8)

        displayScorers.forEachIndexed { index, stat ->
            paint.color = if (index == 0) Color.parseColor("#1E2A3A") else Color.parseColor("#151D2A")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(80f, rowY, 1000f, rowY + 76f), 14f, 14f, paint)

            // Rank
            paint.color = if (index == 0) Color.parseColor("#F59E0B") else Color.WHITE
            paint.textSize = 32f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${index + 1}", 110f, rowY + 48f, paint)

            // Player Name + Captain Badge
            val pName = stat.player.name + if (stat.player.isCaptain) " (C)" else ""
            paint.color = Color.WHITE
            paint.textSize = 28f
            canvas.drawText(pName, 170f, rowY + 42f, paint)

            // Team Name
            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(stat.team?.name ?: "", 170f, rowY + 66f, paint)

            // Goals Badge
            paint.color = Color.parseColor("#F59E0B")
            paint.textSize = 36f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${stat.goals} G", 900f, rowY + 48f, paint)

            rowY += 90f
        }

        drawFooterBrand(canvas)
        return bitmap
    }

    /**
     * Generates the Official 1080x1080 px Champion Frame (Cuadro del Campeón):
     * Large team crest, squad roster, captain highlighted with (C) gold armband,
     * trophy badge, and "Desarrollado por Daniel (el Grande)" badge.
     */
    fun generateChampionFrameBitmap(
        tournament: Tournament,
        championTeam: Team,
        players: List<Player>,
        customPhoto: Bitmap? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_SIZE, CARD_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Luxury Champion Gold & Navy Gradient Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val gradient = LinearGradient(
            0f, 0f, CARD_SIZE.toFloat(), CARD_SIZE.toFloat(),
            intArrayOf(Color.parseColor("#0F172A"), Color.parseColor("#1E1B4B"), Color.parseColor("#090D16")),
            null,
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = gradient
        canvas.drawRect(0f, 0f, CARD_SIZE.toFloat(), CARD_SIZE.toFloat(), bgPaint)

        // Golden Outer Border Frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        framePaint.style = Paint.Style.STROKE
        framePaint.strokeWidth = 14f
        framePaint.color = Color.parseColor("#F59E0B")
        canvas.drawRoundRect(RectF(30f, 30f, 1050f, 1050f), 28f, 28f, framePaint)

        // Inner decorative gold line
        framePaint.strokeWidth = 3f
        framePaint.color = Color.parseColor("#B45309")
        canvas.drawRoundRect(RectF(44f, 44f, 1036f, 1036f), 20f, 20f, framePaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Trophy / Champion Header
        paint.color = Color.parseColor("#F59E0B")
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("★ CAMPEON OFICIAL ★", 540f, 110f, paint)

        // Tournament Name
        paint.color = Color.WHITE
        paint.textSize = 42f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(tournament.name.uppercase(), 540f, 160f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("EDICION ${tournament.edition.uppercase()} • THEBIG FTBOL", 540f, 195f, paint)

        // Large Crest or Custom Photo in Center
        val crestCenterY = 360f
        if (customPhoto != null) {
            val enhancedPhoto = ImageEnhancer.enhanceCrestOrPhoto(customPhoto)
            val destRect = RectF(400f, crestCenterY - 120f, 680f, crestCenterY + 140f)
            canvas.drawBitmap(enhancedPhoto, null, destRect, paint)
        } else {
            // Draw Large Team Crest Shield
            drawVectorShield(canvas, 540f, crestCenterY, 110f, championTeam)
        }

        // Team Name in Big Bold Letters
        paint.color = Color.parseColor("#F59E0B")
        paint.textSize = 46f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(championTeam.name.uppercase(), 540f, 550f, paint)

        // Squad List Header
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PLANTILLA CAMPEONA", 540f, 600f, paint)

        // Two columns of Players
        val col1X = 140f
        val col2X = 580f
        var pY = 645f
        val half = (players.size + 1) / 2

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 21f

        players.take(16).forEachIndexed { i, p ->
            val curX = if (i < half) col1X else col2X
            val curY = if (i < half) pY + (i * 38f) else pY + ((i - half) * 38f)

            if (p.isCaptain) {
                // Gold highlight for captain
                paint.color = Color.parseColor("#F59E0B")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("#${p.number} ${p.name} (C)", curX, curY, paint)
            } else {
                paint.color = Color.parseColor("#E2E8F0")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("#${p.number} ${p.name}", curX, curY, paint)
            }
        }

        // Mandatory Official Brand Badge
        val badgeY = 990f
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.parseColor("#F59E0B")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TheBiG • Torneo Oficial", 540f, badgeY, paint)

        return bitmap
    }

    private fun drawInstagramBackground(canvas: Canvas) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = LinearGradient(
            0f, 0f, 0f, CARD_SIZE.toFloat(),
            intArrayOf(Color.parseColor("#0B1120"), Color.parseColor("#0284C7").let { Color.argb(40, 2, 132, 199) }, Color.parseColor("#0B1120")),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = shader
        canvas.drawRect(0f, 0f, CARD_SIZE.toFloat(), CARD_SIZE.toFloat(), bgPaint)

        // Subtle tech grid lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint.color = Color.parseColor("#1E293B")
        linePaint.strokeWidth = 2f
        canvas.drawRoundRect(RectF(40f, 40f, 1040f, 1040f), 24f, 24f, linePaint)
    }

    private fun drawWatermarkAndBranding(canvas: Canvas, title: String, subtitle: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // "TheBiG" Top Logo
        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("TheBiG", 70f, 100f, paint)

        paint.color = Color.WHITE
        paint.textSize = 42f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title.uppercase(), 70f, 160f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(subtitle, 70f, 200f, paint)
    }

    private fun drawFooterBrand(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("TheBiG • Plataforma Oficial", 540f, 1010f, paint)
    }

    private fun drawVectorShield(canvas: Canvas, cx: Float, cy: Float, radius: Float, team: Team) {
        TeamCrestHelper.drawTeamShieldOnAndroidCanvas(canvas, cx, cy, radius, team)
    }

    /**
     * Saves bitmap to app cache and opens native Android Share sheet.
     */
    fun shareBitmap(context: Context, bitmap: Bitmap, title: String) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "thebig_share_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "com.aistudio.thebig.ftbol.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title\nTorneo oficial gestionado con TheBiG")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Imagen"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateStandingsInstagramBitmap(tournament: Tournament, standings: List<TeamStanding>): Bitmap =
        generateStandingsBitmap(tournament, standings)

    fun generateFixtureInstagramBitmap(tournament: Tournament, matches: List<Match>, teamMap: Map<Long, Team>): Bitmap =
        generateFixtureBitmap(tournament, matches, teamMap.values.toList())

    fun generateScorersInstagramBitmap(tournament: Tournament, scorers: List<ScorerStat>): Bitmap =
        generateScorersBitmap(tournament, scorers)
}
