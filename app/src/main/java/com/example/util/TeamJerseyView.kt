package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Team

object JerseyPatternType {
    const val SOLID = "SOLID"
    const val STRIPES_VERTICAL = "STRIPES_VERTICAL"
    const val STRIPES_HORIZONTAL = "STRIPES_HORIZONTAL"
    const val SASH = "SASH"
    const val HALF = "HALF"

    val PATTERN_OPTIONS = listOf(
        SOLID to "Lisa (Sólida)",
        STRIPES_VERTICAL to "Franjas Verticales",
        STRIPES_HORIZONTAL to "Franjas Horizontales",
        SASH to "Banda Diagonal",
        HALF to "Mitad y Mitad"
    )
}

@Composable
fun TeamJerseyView(
    team: Team?,
    isGoalkeeper: Boolean = false,
    dorsal: Int? = null,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val primaryHex = team?.primaryColorHex ?: "#F59E0B"
    val secondaryHex = team?.secondaryColorHex ?: "#0F172A"
    val gkHex = team?.goalkeeperColorHex ?: "#10B981"
    val pattern = team?.jerseyPattern ?: JerseyPatternType.SOLID

    TeamJerseyView(
        primaryColorHex = primaryHex,
        secondaryColorHex = secondaryHex,
        goalkeeperColorHex = gkHex,
        jerseyPattern = pattern,
        isGoalkeeper = isGoalkeeper,
        dorsal = dorsal,
        size = size,
        modifier = modifier
    )
}

@Composable
fun TeamJerseyView(
    primaryColorHex: String,
    secondaryColorHex: String,
    goalkeeperColorHex: String = "#10B981",
    jerseyPattern: String = JerseyPatternType.SOLID,
    isGoalkeeper: Boolean = false,
    dorsal: Int? = null,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val primary = TeamCrestHelper.parseColor(primaryColorHex, Color(0xFFF59E0B))
    val secondary = TeamCrestHelper.parseColor(secondaryColorHex, Color(0xFF0F172A))
    val gkColor = TeamCrestHelper.parseColor(goalkeeperColorHex, Color(0xFF10B981))

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Full jersey outline path (sleeves + torso)
            val jerseyPath = Path().apply {
                // Collar top left
                moveTo(w * 0.35f, h * 0.12f)
                // Left shoulder to sleeve top
                lineTo(w * 0.04f, h * 0.28f)
                // Left sleeve cuff
                lineTo(w * 0.14f, h * 0.48f)
                // Left underarm
                lineTo(w * 0.26f, h * 0.38f)
                // Left waist down
                lineTo(w * 0.26f, h * 0.90f)
                // Bottom hem
                lineTo(w * 0.74f, h * 0.90f)
                // Right waist up
                lineTo(w * 0.74f, h * 0.38f)
                // Right underarm to sleeve cuff
                lineTo(w * 0.86f, h * 0.48f)
                // Right sleeve top
                lineTo(w * 0.96f, h * 0.28f)
                // Right shoulder to collar top right
                lineTo(w * 0.65f, h * 0.12f)
                // Neck opening curve
                quadraticBezierTo(w * 0.50f, h * 0.26f, w * 0.35f, h * 0.12f)
                close()
            }

            // Outline stroke style
            val strokeWidth = (w * 0.05f).coerceAtLeast(1.5f)

            if (isGoalkeeper) {
                // Goalkeeper jersey: Solid vibrant goalkeeper color with dark collar/accents
                drawPath(jerseyPath, gkColor, style = Fill)

                // Clip decorative accents inside jersey
                clipPath(jerseyPath) {
                    // Padded shoulders / side panels
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.25f),
                        radius = w * 0.35f,
                        center = Offset(w * 0.5f, -h * 0.05f)
                    )
                    // Arm accent stripes
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(w * 0.08f, h * 0.32f),
                        end = Offset(w * 0.17f, h * 0.46f),
                        strokeWidth = strokeWidth * 1.2f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(w * 0.92f, h * 0.32f),
                        end = Offset(w * 0.83f, h * 0.46f),
                        strokeWidth = strokeWidth * 1.2f
                    )
                }

                // Collar neckline
                val collar = Path().apply {
                    moveTo(w * 0.34f, h * 0.12f)
                    quadraticBezierTo(w * 0.50f, h * 0.28f, w * 0.66f, h * 0.12f)
                }
                drawPath(collar, Color(0xFF0F172A), style = Stroke(width = strokeWidth * 1.5f))
                drawPath(jerseyPath, Color(0xFF0F172A), style = Stroke(width = strokeWidth))
            } else {
                // Outfield player jersey with selected club pattern
                drawPath(jerseyPath, primary, style = Fill)

                clipPath(jerseyPath) {
                    when (jerseyPattern) {
                        JerseyPatternType.STRIPES_VERTICAL -> {
                            val stripeW = w * 0.10f
                            // Vertical stripes in secondary color
                            var x = w * 0.22f
                            while (x < w * 0.78f) {
                                drawRect(
                                    color = secondary,
                                    topLeft = Offset(x, 0f),
                                    size = Size(stripeW, h)
                                )
                                x += stripeW * 2f
                            }
                        }
                        JerseyPatternType.STRIPES_HORIZONTAL -> {
                            val stripeH = h * 0.12f
                            var y = h * 0.25f
                            while (y < h * 0.90f) {
                                drawRect(
                                    color = secondary,
                                    topLeft = Offset(0f, y),
                                    size = Size(w, stripeH)
                                )
                                y += stripeH * 2f
                            }
                        }
                        JerseyPatternType.SASH -> {
                            val sashPath = Path().apply {
                                moveTo(w * 0.20f, h * 0.15f)
                                lineTo(w * 0.42f, h * 0.15f)
                                lineTo(w * 0.80f, h * 0.90f)
                                lineTo(w * 0.58f, h * 0.90f)
                                close()
                            }
                            drawPath(sashPath, secondary, style = Fill)
                        }
                        JerseyPatternType.HALF -> {
                            drawRect(
                                color = secondary,
                                topLeft = Offset(w * 0.50f, 0f),
                                size = Size(w * 0.50f, h)
                            )
                        }
                        else -> {
                            // SOLID: Elegant subtle chest chevron / shadow
                            drawLine(
                                color = secondary.copy(alpha = 0.3f),
                                start = Offset(w * 0.26f, h * 0.40f),
                                end = Offset(w * 0.74f, h * 0.40f),
                                strokeWidth = strokeWidth * 0.8f
                            )
                        }
                    }

                    // Collar & sleeve cuffs in secondary color
                    drawCircle(
                        color = secondary,
                        radius = w * 0.18f,
                        center = Offset(w * 0.50f, h * 0.08f),
                        style = Stroke(width = strokeWidth * 1.5f)
                    )
                }

                // Border outline
                drawPath(jerseyPath, secondary.copy(alpha = 0.85f), style = Stroke(width = strokeWidth))
            }
        }

        // Optional dorsal number
        if (dorsal != null && dorsal > 0 && size >= 24.dp) {
            val numColor = if (isGoalkeeper) {
                Color.White
            } else {
                // Ensure contrast against primary color
                val lum = (primary.red * 0.299 + primary.green * 0.587 + primary.blue * 0.114)
                if (lum > 0.55) Color(0xFF0F172A) else Color.White
            }

            Text(
                text = "$dorsal",
                color = numColor,
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
