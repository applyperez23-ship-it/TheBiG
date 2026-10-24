package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
        "Escudo Clásico",
        "Corona Real",
        "Estrella de Oro",
        "Diamante Elite",
        "Hexágono Moderno",
        "Insignia Circular",
        "Estandarte Victorioso",
        "Fortaleza Defensiva",
        "Rayo Dinámico",
        "Laurel de Campeón"
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
            val dir = File(context.filesDir, "team_shields").apply { if (!exists()) mkdirs() }
            val destFile = File(dir, "shield_${System.currentTimeMillis()}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            uri.toString()
        }
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
            primaryColorHex = team.primaryColorHex,
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
            primaryColorHex = "#64748B",
            secondaryColorHex = "#334155",
            borderColorHex = "#94A3B8",
            shortName = "TB",
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
                0 -> drawClassicShield(w, h, primary, secondary, border)
                1 -> drawCrownShield(w, h, primary, secondary, border)
                2 -> drawStarShield(w, h, primary, secondary, border)
                3 -> drawDiamondShield(w, h, primary, secondary, border)
                4 -> drawHexagonShield(w, h, primary, secondary, border)
                5 -> drawCircularBadge(w, h, primary, secondary, border)
                6 -> drawBannerPennant(w, h, primary, secondary, border)
                7 -> drawFortressShield(w, h, primary, secondary, border)
                8 -> drawLightningShield(w, h, primary, secondary, border)
                else -> drawLaurelShield(w, h, primary, secondary, border)
            }
        }

        if (shortName.isNotBlank() && size >= 40.dp) {
            Text(
                text = shortName.take(3).uppercase(),
                color = border,
                fontSize = (size.value * 0.28f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
}

private fun DrawScope.drawClassicShield(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.1f, h * 0.15f)
        lineTo(w * 0.9f, h * 0.15f)
        lineTo(w * 0.9f, h * 0.55f)
        quadraticBezierTo(w * 0.9f, h * 0.85f, w * 0.5f, h * 0.95f)
        quadraticBezierTo(w * 0.1f, h * 0.85f, w * 0.1f, h * 0.55f)
        close()
    }
    drawPath(path, primary, style = Fill)
    // Vertical half split
    val halfPath = Path().apply {
        moveTo(w * 0.5f, h * 0.15f)
        lineTo(w * 0.9f, h * 0.15f)
        lineTo(w * 0.9f, h * 0.55f)
        quadraticBezierTo(w * 0.9f, h * 0.85f, w * 0.5f, h * 0.95f)
        close()
    }
    drawPath(halfPath, secondary, style = Fill)
    drawPath(path, border, style = Stroke(width = w * 0.06f))
}

private fun DrawScope.drawCrownShield(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.15f, h * 0.25f)
        lineTo(w * 0.35f, h * 0.1f)
        lineTo(w * 0.5f, h * 0.25f)
        lineTo(w * 0.65f, h * 0.1f)
        lineTo(w * 0.85f, h * 0.25f)
        lineTo(w * 0.85f, h * 0.6f)
        lineTo(w * 0.5f, h * 0.92f)
        lineTo(w * 0.15f, h * 0.6f)
        close()
    }
    drawPath(path, primary, style = Fill)
    drawCircle(secondary, radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.52f))
    drawPath(path, border, style = Stroke(width = w * 0.06f))
}

private fun DrawScope.drawStarShield(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.08f)
        lineTo(w * 0.9f, h * 0.25f)
        lineTo(w * 0.75f, h * 0.88f)
        lineTo(w * 0.5f, h * 0.95f)
        lineTo(w * 0.25f, h * 0.88f)
        lineTo(w * 0.1f, h * 0.25f)
        close()
    }
    drawPath(path, secondary, style = Fill)
    drawCircle(primary, radius = w * 0.28f, center = Offset(w * 0.5f, h * 0.5f))
    drawPath(path, border, style = Stroke(width = w * 0.06f))
}

private fun DrawScope.drawDiamondShield(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.08f)
        lineTo(w * 0.92f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.92f)
        lineTo(w * 0.08f, h * 0.5f)
        close()
    }
    drawPath(path, primary, style = Fill)
    val inner = Path().apply {
        moveTo(w * 0.5f, h * 0.22f)
        lineTo(w * 0.78f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.78f)
        lineTo(w * 0.22f, h * 0.5f)
        close()
    }
    drawPath(inner, secondary, style = Fill)
    drawPath(path, border, style = Stroke(width = w * 0.06f))
}

private fun DrawScope.drawHexagonShield(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.08f)
        lineTo(w * 0.88f, h * 0.28f)
        lineTo(w * 0.88f, h * 0.72f)
        lineTo(w * 0.5f, h * 0.92f)
        lineTo(w * 0.12f, h * 0.72f)
        lineTo(w * 0.12f, h * 0.28f)
        close()
    }
    drawPath(path, primary, style = Fill)
    drawLine(secondary, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), strokeWidth = w * 0.1f)
    drawPath(path, border, style = Stroke(width = w * 0.06f))
}

private fun DrawScope.drawCircularBadge(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    drawCircle(primary, radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f))
    drawCircle(secondary, radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.5f))
    drawCircle(border, radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = w * 0.06f))
}

private fun DrawScope.drawBannerPennant(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.15f, h * 0.1f)
        lineTo(w * 0.85f, h * 0.1f)
        lineTo(w * 0.85f, h * 0.85f)
        lineTo(w * 0.5f, h * 0.65f)
        lineTo(w * 0.15f, h * 0.85f)
        close()
    }
    drawPath(path, primary, style = Fill)
    val stripe = Path().apply {
        moveTo(w * 0.4f, h * 0.1f)
        lineTo(w * 0.6f, h * 0.1f)
        lineTo(w * 0.6f, h * 0.7f)
        lineTo(w * 0.4f, h * 0.7f)
        close()
    }
    drawPath(stripe, secondary, style = Fill)
    drawPath(path, border, style = Stroke(width = w * 0.06f))
}

private fun DrawScope.drawFortressShield(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.12f, h * 0.12f)
        lineTo(w * 0.32f, h * 0.12f)
        lineTo(w * 0.32f, h * 0.22f)
        lineTo(w * 0.48f, h * 0.22f)
        lineTo(w * 0.48f, h * 0.12f)
        lineTo(w * 0.68f, h * 0.12f)
        lineTo(w * 0.68f, h * 0.22f)
        lineTo(w * 0.88f, h * 0.22f)
        lineTo(w * 0.88f, h * 0.55f)
        lineTo(w * 0.5f, h * 0.9f)
        lineTo(w * 0.12f, h * 0.55f)
        close()
    }
    drawPath(path, secondary, style = Fill)
    drawCircle(primary, radius = w * 0.2f, center = Offset(w * 0.5f, h * 0.55f))
    drawPath(path, border, style = Stroke(width = w * 0.06f))
}

private fun DrawScope.drawLightningShield(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.1f, h * 0.1f)
        lineTo(w * 0.9f, h * 0.1f)
        lineTo(w * 0.75f, h * 0.85f)
        lineTo(w * 0.5f, h * 0.95f)
        lineTo(w * 0.25f, h * 0.85f)
        close()
    }
    drawPath(path, primary, style = Fill)
    val bolt = Path().apply {
        moveTo(w * 0.55f, h * 0.2f)
        lineTo(w * 0.35f, h * 0.52f)
        lineTo(w * 0.52f, h * 0.52f)
        lineTo(w * 0.42f, h * 0.82f)
        lineTo(w * 0.68f, h * 0.46f)
        lineTo(w * 0.52f, h * 0.46f)
        close()
    }
    drawPath(bolt, secondary, style = Fill)
    drawPath(path, border, style = Stroke(width = w * 0.06f))
}

private fun DrawScope.drawLaurelShield(w: Float, h: Float, primary: Color, secondary: Color, border: Color) {
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        quadraticBezierTo(w * 0.85f, h * 0.15f, w * 0.85f, h * 0.55f)
        quadraticBezierTo(w * 0.85f, h * 0.85f, w * 0.5f, h * 0.95f)
        quadraticBezierTo(w * 0.15f, h * 0.85f, w * 0.15f, h * 0.55f)
        quadraticBezierTo(w * 0.15f, h * 0.15f, w * 0.5f, h * 0.1f)
        close()
    }
    drawPath(path, primary, style = Fill)
    drawCircle(secondary, radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.5f))
    drawPath(path, border, style = Stroke(width = w * 0.06f))
}
