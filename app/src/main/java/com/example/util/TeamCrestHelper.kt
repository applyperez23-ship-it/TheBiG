package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.entity.Team
import java.io.File
import java.io.FileOutputStream

object TeamCrestHelper {

    val PRESET_NAMES = listOf(
        "Escudo Real Ibérico",
        "Blasón Premier Point",
        "Escudo Chevron Galo",
        "Calcio Aerodinámico",
        "Ojival Gótico",
        "Fortaleza Imperial",
        "Diamante Dinastía",
        "Insignia Nórdica",
        "Laurel Centenario",
        "Vanguardia Moderna"
    )

    fun parseColor(hex: String, default: Color): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            default
        }
    }

    fun copyShieldUriToInternal(context: Context, uri: Uri): String? {
        return try {
            // Attempt to take persistable read permission if granted
            try {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {
                // Not all URIs support persistable permissions (e.g. tree/document vs content)
            }

            val dir = File(context.filesDir, "team_shields").apply { if (!exists()) mkdirs() }
            val destFile = File(dir, "shield_${System.currentTimeMillis()}.png")

            // Decode sampled bitmap safely to prevent OOM
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            val maxDim = 512
            while (options.outWidth / inSampleSize > maxDim || options.outHeight / inSampleSize > maxDim) {
                inSampleSize *= 2
            }

            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            if (bitmap != null) {
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                }
                bitmap.recycle()
                Uri.fromFile(destFile).toString()
            } else {
                // Direct stream copy fallback
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(destFile).toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try { uri.toString() } catch (_: Exception) { null }
        }
    }

    /**
     * Draws the team shield on an android.graphics.Canvas (used for image export).
     */
    fun drawTeamShieldOnAndroidCanvas(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        team: Team,
        context: Context? = null
    ) {
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // If custom URI exists, attempt to load bitmap
        var loadedCustom: android.graphics.Bitmap? = null
        if (!team.customShieldUri.isNullOrBlank() && context != null) {
            try {
                val uri = Uri.parse(team.customShieldUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    loadedCustom = android.graphics.BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (loadedCustom != null) {
            val bmp = loadedCustom!!
            val saveCount = canvas.save()
            val clipPath = android.graphics.Path().apply {
                addCircle(cx, cy, radius, android.graphics.Path.Direction.CCW)
            }
            canvas.clipPath(clipPath)
            val srcRect = android.graphics.Rect(0, 0, bmp.width, bmp.height)
            val dstRect = android.graphics.RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawBitmap(bmp, srcRect, dstRect, p)
            canvas.restoreToCount(saveCount)

            // Border
            p.style = android.graphics.Paint.Style.STROKE
            p.strokeWidth = radius * 0.12f
            p.color = try { android.graphics.Color.parseColor(team.borderColorHex) } catch (e: Exception) { android.graphics.Color.WHITE }
            canvas.drawCircle(cx, cy, radius, p)
            return
        }

        // Otherwise draw vector preset on canvas
        val primary = try { android.graphics.Color.parseColor(team.shieldPrimaryColorHex) } catch (e: Exception) { android.graphics.Color.parseColor("#F59E0B") }
        val secondary = try { android.graphics.Color.parseColor(team.secondaryColorHex) } catch (e: Exception) { android.graphics.Color.parseColor("#0F172A") }
        val border = try { android.graphics.Color.parseColor(team.borderColorHex) } catch (e: Exception) { android.graphics.Color.WHITE }

        val path = android.graphics.Path()
        val w = radius * 2
        val h = radius * 2
        val left = cx - radius
        val top = cy - radius
        val right = cx + radius
        val bottom = cy + radius

        path.moveTo(cx, top)
        path.lineTo(right, top + h * 0.2f)
        path.lineTo(right, top + h * 0.55f)
        path.quadTo(right, bottom, cx, bottom + h * 0.1f)
        path.quadTo(left, bottom, left, top + h * 0.55f)
        path.lineTo(left, top + h * 0.2f)
        path.close()

        p.style = android.graphics.Paint.Style.FILL
        p.color = primary
        canvas.drawPath(path, p)

        // Split
        val halfPath = android.graphics.Path().apply {
            moveTo(cx, top)
            lineTo(right, top + h * 0.2f)
            lineTo(right, top + h * 0.55f)
            quadTo(right, bottom, cx, bottom + h * 0.1f)
            close()
        }
        p.color = secondary
        canvas.drawPath(halfPath, p)

        // Stroke border
        p.style = android.graphics.Paint.Style.STROKE
        p.strokeWidth = radius * 0.14f
        p.color = border
        canvas.drawPath(path, p)

        // Initials
        p.style = android.graphics.Paint.Style.FILL
        p.color = border
        p.textSize = radius * 0.7f
        p.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        p.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText(team.shortName.take(3).uppercase(), cx, cy + (radius * 0.25f), p)
    }
}

@Composable
fun TeamCrestView(
    team: Team?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    if (team != null) {
        TeamCrestView(
            shieldIndex = team.shieldVectorIndex,
            primaryColorHex = team.shieldPrimaryColorHex,
            secondaryColorHex = team.secondaryColorHex,
            borderColorHex = team.borderColorHex,
            shortName = team.shortName,
            customShieldUri = team.customShieldUri,
            size = size,
            modifier = modifier
        )
    } else {
        TeamCrestView(
            shieldIndex = 0,
            primaryColorHex = "#F59E0B",
            secondaryColorHex = "#0F172A",
            borderColorHex = "#FFFFFF",
            shortName = "BIG",
            size = size,
            modifier = modifier
        )
    }
}

@Composable
fun TeamCrestView(
    shieldIndex: Int,
    primaryColorHex: String,
    secondaryColorHex: String,
    borderColorHex: String,
    shortName: String = "",
    customShieldUri: String? = null,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val primary = TeamCrestHelper.parseColor(primaryColorHex, Color(0xFFF59E0B))
    val secondary = TeamCrestHelper.parseColor(secondaryColorHex, Color(0xFF0F172A))
    val border = TeamCrestHelper.parseColor(borderColorHex, Color(0xFFFFFFFF))
    val context = LocalContext.current

    if (!customShieldUri.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border((size.value * 0.05f).coerceAtLeast(1.5f).dp, border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(customShieldUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Escudo $shortName",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height

                when (shieldIndex % 10) {
                    0 -> drawIberianCrownCrest(w, h, primary, secondary, border)
                    1 -> drawPremierPointCrest(w, h, primary, secondary, border)
                    2 -> drawFrenchChevronCrest(w, h, primary, secondary, border)
                    3 -> drawCalcioAerodynamicCrest(w, h, primary, secondary, border)
                    4 -> drawGothicChevronCrest(w, h, primary, secondary, border)
                    5 -> drawBattlementFortressCrest(w, h, primary, secondary, border)
                    6 -> drawDynastyDiamondCrest(w, h, primary, secondary, border)
                    7 -> drawNordicCrossCrest(w, h, primary, secondary, border)
                    8 -> drawCentenaryLaurelCrest(w, h, primary, secondary, border)
                    else -> drawVanguardModernCrest(w, h, primary, secondary, border)
                }
            }

            if (shortName.isNotBlank() && size >= 32.dp) {
                // Renders monogram in the designated crest plate
                Text(
                    text = shortName.take(3).uppercase(),
                    color = border,
                    fontSize = (size.value * 0.26f).sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = (size.value * 0.08f).dp)
                )
            }
        }
    }
}

// 1. Escudo Real Ibérico
private fun DrawScope.drawIberianCrownCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.12f, h * 0.18f)
        lineTo(w * 0.88f, h * 0.18f)
        lineTo(w * 0.88f, h * 0.58f)
        quadraticBezierTo(w * 0.88f, h * 0.90f, w * 0.50f, h * 0.98f)
        quadraticBezierTo(w * 0.12f, h * 0.90f, w * 0.12f, h * 0.58f)
        close()
    }
    drawPath(path, primary, style = Fill)

    // Vertical striped halves
    val halfPath = Path().apply {
        moveTo(w * 0.50f, h * 0.18f)
        lineTo(w * 0.88f, h * 0.18f)
        lineTo(w * 0.88f, h * 0.58f)
        quadraticBezierTo(w * 0.88f, h * 0.90f, w * 0.50f, h * 0.98f)
        close()
    }
    drawPath(halfPath, secondary, style = Fill)

    // Upper Crown / Monogram Bar
    val bar = Path().apply {
        moveTo(w * 0.12f, h * 0.18f)
        lineTo(w * 0.88f, h * 0.18f)
        lineTo(w * 0.88f, h * 0.44f)
        lineTo(w * 0.50f, h * 0.50f)
        lineTo(w * 0.12f, h * 0.44f)
        close()
    }
    drawPath(bar, secondary.copy(alpha = 0.85f), style = Fill)
    drawPath(bar, border, style = Stroke(width = w * 0.035f))
    drawPath(path, border, style = Stroke(width = w * 0.065f))
}

// 2. Blasón Premier Point
private fun DrawScope.drawPremierPointCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.06f)
        lineTo(w * 0.90f, h * 0.16f)
        lineTo(w * 0.84f, h * 0.62f)
        lineTo(w * 0.50f, h * 0.96f)
        lineTo(w * 0.16f, h * 0.62f)
        lineTo(w * 0.10f, h * 0.16f)
        close()
    }
    drawPath(path, primary, style = Fill)

    // Dual-chevron lower accent
    val chevron = Path().apply {
        moveTo(w * 0.16f, h * 0.50f)
        lineTo(w * 0.50f, h * 0.72f)
        lineTo(w * 0.84f, h * 0.50f)
        lineTo(w * 0.84f, h * 0.62f)
        lineTo(w * 0.50f, h * 0.96f)
        lineTo(w * 0.16f, h * 0.62f)
        close()
    }
    drawPath(chevron, secondary, style = Fill)

    // Upper monogram banner
    drawCircle(secondary, radius = w * 0.28f, center = Offset(w * 0.50f, h * 0.38f))
    drawCircle(border, radius = w * 0.28f, center = Offset(w * 0.50f, h * 0.38f), style = Stroke(width = w * 0.035f))
    drawPath(path, border, style = Stroke(width = w * 0.065f))
}

// 3. Escudo Chevron Galo
private fun DrawScope.drawFrenchChevronCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.14f, h * 0.12f)
        lineTo(w * 0.86f, h * 0.12f)
        lineTo(w * 0.86f, h * 0.60f)
        quadraticBezierTo(w * 0.80f, h * 0.86f, w * 0.50f, h * 0.96f)
        quadraticBezierTo(w * 0.20f, h * 0.86f, w * 0.14f, h * 0.60f)
        close()
    }
    drawPath(path, primary, style = Fill)

    // Diagonal Sash
    val sash = Path().apply {
        moveTo(w * 0.14f, h * 0.25f)
        lineTo(w * 0.40f, h * 0.12f)
        lineTo(w * 0.86f, h * 0.65f)
        lineTo(w * 0.60f, h * 0.82f)
        close()
    }
    drawPath(sash, secondary, style = Fill)

    // Upper monogram badge
    val topPlate = Path().apply {
        moveTo(w * 0.22f, h * 0.12f)
        lineTo(w * 0.78f, h * 0.12f)
        lineTo(w * 0.70f, h * 0.36f)
        lineTo(w * 0.30f, h * 0.36f)
        close()
    }
    drawPath(topPlate, secondary, style = Fill)
    drawPath(path, border, style = Stroke(width = w * 0.065f))
}

// 4. Calcio Aerodinámico
private fun DrawScope.drawCalcioAerodynamicCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.08f)
        quadraticBezierTo(w * 0.90f, h * 0.18f, w * 0.86f, h * 0.60f)
        quadraticBezierTo(w * 0.82f, h * 0.86f, w * 0.50f, h * 0.96f)
        quadraticBezierTo(w * 0.18f, h * 0.86f, w * 0.14f, h * 0.60f)
        quadraticBezierTo(w * 0.10f, h * 0.18f, w * 0.50f, h * 0.08f)
        close()
    }
    drawPath(path, primary, style = Fill)

    // Lower pointed oval accent
    val inner = Path().apply {
        moveTo(w * 0.50f, h * 0.35f)
        lineTo(w * 0.80f, h * 0.60f)
        lineTo(w * 0.50f, h * 0.88f)
        lineTo(w * 0.20f, h * 0.60f)
        close()
    }
    drawPath(inner, secondary, style = Fill)

    // Top Stars accent
    drawCircle(border, radius = w * 0.035f, center = Offset(w * 0.36f, h * 0.20f))
    drawCircle(border, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.16f))
    drawCircle(border, radius = w * 0.035f, center = Offset(w * 0.64f, h * 0.20f))

    drawPath(path, border, style = Stroke(width = w * 0.065f))
}

// 5. Ojival Gótico
private fun DrawScope.drawGothicChevronCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.06f)
        lineTo(w * 0.88f, h * 0.22f)
        lineTo(w * 0.88f, h * 0.60f)
        lineTo(w * 0.50f, h * 0.96f)
        lineTo(w * 0.12f, h * 0.60f)
        lineTo(w * 0.12f, h * 0.22f)
        close()
    }
    drawPath(path, primary, style = Fill)

    // Left half contrast
    val leftHalf = Path().apply {
        moveTo(w * 0.50f, h * 0.06f)
        lineTo(w * 0.50f, h * 0.96f)
        lineTo(w * 0.12f, h * 0.60f)
        lineTo(w * 0.12f, h * 0.22f)
        close()
    }
    drawPath(leftHalf, secondary, style = Fill)

    // Central Monogram Chevron
    val chev = Path().apply {
        moveTo(w * 0.20f, h * 0.45f)
        lineTo(w * 0.50f, h * 0.60f)
        lineTo(w * 0.80f, h * 0.45f)
        lineTo(w * 0.50f, h * 0.30f)
        close()
    }
    drawPath(chev, primary.copy(alpha = 0.9f), style = Fill)
    drawPath(chev, border, style = Stroke(width = w * 0.03f))

    drawPath(path, border, style = Stroke(width = w * 0.065f))
}

// 6. Fortaleza Imperial
private fun DrawScope.drawBattlementFortressCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.14f, h * 0.12f)
        lineTo(w * 0.32f, h * 0.12f)
        lineTo(w * 0.32f, h * 0.20f)
        lineTo(w * 0.44f, h * 0.20f)
        lineTo(w * 0.44f, h * 0.12f)
        lineTo(w * 0.56f, h * 0.12f)
        lineTo(w * 0.56f, h * 0.20f)
        lineTo(w * 0.68f, h * 0.20f)
        lineTo(w * 0.68f, h * 0.12f)
        lineTo(w * 0.86f, h * 0.12f)
        lineTo(w * 0.86f, h * 0.58f)
        quadraticBezierTo(w * 0.86f, h * 0.86f, w * 0.50f, h * 0.96f)
        quadraticBezierTo(w * 0.14f, h * 0.86f, w * 0.14f, h * 0.58f)
        close()
    }
    drawPath(path, primary, style = Fill)

    // Right half in secondary
    val rightHalf = Path().apply {
        moveTo(w * 0.50f, h * 0.12f)
        lineTo(w * 0.56f, h * 0.12f)
        lineTo(w * 0.56f, h * 0.20f)
        lineTo(w * 0.68f, h * 0.20f)
        lineTo(w * 0.68f, h * 0.12f)
        lineTo(w * 0.86f, h * 0.12f)
        lineTo(w * 0.86f, h * 0.58f)
        quadraticBezierTo(w * 0.86f, h * 0.86f, w * 0.50f, h * 0.96f)
        close()
    }
    drawPath(rightHalf, secondary, style = Fill)

    // Central Monogram Shield
    drawCircle(secondary, radius = w * 0.24f, center = Offset(w * 0.50f, h * 0.48f))
    drawPath(path, border, style = Stroke(width = w * 0.065f))
}

// 7. Diamante Dinastía
private fun DrawScope.drawDynastyDiamondCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.08f)
        lineTo(w * 0.90f, h * 0.32f)
        lineTo(w * 0.78f, h * 0.88f)
        lineTo(w * 0.50f, h * 0.96f)
        lineTo(w * 0.22f, h * 0.88f)
        lineTo(w * 0.10f, h * 0.32f)
        close()
    }
    drawPath(path, primary, style = Fill)

    // Monogram Upper Hexagon
    val plate = Path().apply {
        moveTo(w * 0.50f, h * 0.14f)
        lineTo(w * 0.82f, h * 0.32f)
        lineTo(w * 0.72f, h * 0.60f)
        lineTo(w * 0.28f, h * 0.60f)
        lineTo(w * 0.18f, h * 0.32f)
        close()
    }
    drawPath(plate, secondary, style = Fill)
    drawPath(plate, border, style = Stroke(width = w * 0.035f))
    drawPath(path, border, style = Stroke(width = w * 0.065f))
}

// 8. Insignia Nórdica
private fun DrawScope.drawNordicCrossCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.14f, h * 0.14f)
        lineTo(w * 0.86f, h * 0.14f)
        lineTo(w * 0.86f, h * 0.56f)
        quadraticBezierTo(w * 0.86f, h * 0.88f, w * 0.50f, h * 0.96f)
        quadraticBezierTo(w * 0.14f, h * 0.88f, w * 0.14f, h * 0.56f)
        close()
    }
    drawPath(path, primary, style = Fill)

    // Cross bars
    drawLine(secondary, Offset(w * 0.14f, h * 0.44f), Offset(w * 0.86f, h * 0.44f), strokeWidth = w * 0.18f)
    drawLine(secondary, Offset(w * 0.50f, h * 0.14f), Offset(w * 0.50f, h * 0.90f), strokeWidth = w * 0.18f)

    // Monogram center badge
    drawCircle(primary, radius = w * 0.22f, center = Offset(w * 0.50f, h * 0.44f))
    drawCircle(border, radius = w * 0.22f, center = Offset(w * 0.50f, h * 0.44f), style = Stroke(width = w * 0.035f))
    drawPath(path, border, style = Stroke(width = w * 0.065f))
}

// 9. Laurel Centenario
private fun DrawScope.drawCentenaryLaurelCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.18f, h * 0.16f)
        lineTo(w * 0.82f, h * 0.16f)
        lineTo(w * 0.82f, h * 0.56f)
        quadraticBezierTo(w * 0.82f, h * 0.86f, w * 0.50f, h * 0.95f)
        quadraticBezierTo(w * 0.18f, h * 0.86f, w * 0.18f, h * 0.56f)
        close()
    }
    drawPath(path, secondary, style = Fill)

    // Inner shield
    val inner = Path().apply {
        moveTo(w * 0.26f, h * 0.22f)
        lineTo(w * 0.74f, h * 0.22f)
        lineTo(w * 0.74f, h * 0.54f)
        quadraticBezierTo(w * 0.74f, h * 0.78f, w * 0.50f, h * 0.86f)
        quadraticBezierTo(w * 0.26f, h * 0.78f, w * 0.26f, h * 0.54f)
        close()
    }
    drawPath(inner, primary, style = Fill)

    // Upper monogram header
    val head = Path().apply {
        moveTo(w * 0.22f, h * 0.16f)
        lineTo(w * 0.78f, h * 0.16f)
        lineTo(w * 0.68f, h * 0.40f)
        lineTo(w * 0.32f, h * 0.40f)
        close()
    }
    drawPath(head, secondary, style = Fill)
    drawPath(head, border, style = Stroke(width = w * 0.035f))

    drawPath(path, border, style = Stroke(width = w * 0.065f))
}

// 10. Vanguardia Moderna
private fun DrawScope.drawVanguardModernCrest(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.06f)
        lineTo(w * 0.90f, h * 0.24f)
        lineTo(w * 0.80f, h * 0.86f)
        lineTo(w * 0.50f, h * 0.98f)
        lineTo(w * 0.20f, h * 0.86f)
        lineTo(w * 0.10f, h * 0.24f)
        close()
    }
    drawPath(path, primary, style = Fill)

    // Center diagonal cut
    val diag = Path().apply {
        moveTo(w * 0.50f, h * 0.06f)
        lineTo(w * 0.90f, h * 0.24f)
        lineTo(w * 0.50f, h * 0.65f)
        lineTo(w * 0.10f, h * 0.24f)
        close()
    }
    drawPath(diag, secondary, style = Fill)

    // Center monogram circle
    drawCircle(primary, radius = w * 0.24f, center = Offset(w * 0.50f, h * 0.46f))
    drawCircle(border, radius = w * 0.24f, center = Offset(w * 0.50f, h * 0.46f), style = Stroke(width = w * 0.035f))

    drawPath(path, border, style = Stroke(width = w * 0.065f))
}
