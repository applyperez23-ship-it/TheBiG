package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
}

@Composable
fun TeamJerseyView(
    team: Team,
    isGoalkeeper: Boolean = false,
    dorsal: Int? = null,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    TeamJerseyView(
        primaryColorHex = team.primaryColorHex,
        secondaryColorHex = team.secondaryColorHex,
        goalkeeperColorHex = team.goalkeeperColorHex,
        jerseyPattern = team.jerseyPattern,
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
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val mainColor = if (isGoalkeeper) {
        TeamCrestHelper.parseColor(goalkeeperColorHex, Color(0xFF10B981))
    } else {
        TeamCrestHelper.parseColor(primaryColorHex, Color(0xFFF59E0B))
    }

    val accentColor = if (isGoalkeeper) {
        Color(0xFF064E3B)
    } else {
        TeamCrestHelper.parseColor(secondaryColorHex, Color(0xFF0F172A))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            val bodyPath = Path().apply {
                moveTo(w * 0.35f, h * 0.12f)
                lineTo(w * 0.65f, h * 0.12f)
                lineTo(w * 0.88f, h * 0.28f)
                lineTo(w * 0.78f, h * 0.42f)
                lineTo(w * 0.68f, h * 0.35f)
                lineTo(w * 0.68f, h * 0.88f)
                lineTo(w * 0.32f, h * 0.88f)
                lineTo(w * 0.32f, h * 0.35f)
                lineTo(w * 0.22f, h * 0.42f)
                lineTo(w * 0.12f, h * 0.28f)
                close()
            }

            drawPath(bodyPath, mainColor, style = Fill)

            // Pattern accents
            if (!isGoalkeeper) {
                when (jerseyPattern) {
                    JerseyPatternType.STRIPES_VERTICAL -> {
                        val stripe1 = Path().apply {
                            moveTo(w * 0.42f, h * 0.18f)
                            lineTo(w * 0.48f, h * 0.18f)
                            lineTo(w * 0.48f, h * 0.88f)
                            lineTo(w * 0.42f, h * 0.88f)
                            close()
                        }
                        val stripe2 = Path().apply {
                            moveTo(w * 0.52f, h * 0.18f)
                            lineTo(w * 0.58f, h * 0.18f)
                            lineTo(w * 0.58f, h * 0.88f)
                            lineTo(w * 0.52f, h * 0.88f)
                            close()
                        }
                        drawPath(stripe1, accentColor, style = Fill)
                        drawPath(stripe2, accentColor, style = Fill)
                    }
                    JerseyPatternType.STRIPES_HORIZONTAL -> {
                        val bar = Path().apply {
                            moveTo(w * 0.32f, h * 0.42f)
                            lineTo(w * 0.68f, h * 0.42f)
                            lineTo(w * 0.68f, h * 0.58f)
                            lineTo(w * 0.32f, h * 0.58f)
                            close()
                        }
                        drawPath(bar, accentColor, style = Fill)
                    }
                    JerseyPatternType.SASH -> {
                        val sash = Path().apply {
                            moveTo(w * 0.35f, h * 0.12f)
                            lineTo(w * 0.45f, h * 0.12f)
                            lineTo(w * 0.68f, h * 0.78f)
                            lineTo(w * 0.68f, h * 0.88f)
                            lineTo(w * 0.58f, h * 0.88f)
                            lineTo(w * 0.32f, h * 0.28f)
                            close()
                        }
                        drawPath(sash, accentColor, style = Fill)
                    }
                    JerseyPatternType.HALF -> {
                        val halfPath = Path().apply {
                            moveTo(w * 0.5f, h * 0.12f)
                            lineTo(w * 0.65f, h * 0.12f)
                            lineTo(w * 0.88f, h * 0.28f)
                            lineTo(w * 0.78f, h * 0.42f)
                            lineTo(w * 0.68f, h * 0.35f)
                            lineTo(w * 0.68f, h * 0.88f)
                            lineTo(w * 0.5f, h * 0.88f)
                            close()
                        }
                        drawPath(halfPath, accentColor, style = Fill)
                    }
                }
            }

            // Outline & Collar
            drawPath(bodyPath, Color.White.copy(alpha = 0.7f), style = Stroke(width = (w * 0.04f).coerceAtLeast(1.5f)))

            // Neck collar V-shape
            val neck = Path().apply {
                moveTo(w * 0.42f, h * 0.12f)
                lineTo(w * 0.5f, h * 0.22f)
                lineTo(w * 0.58f, h * 0.12f)
            }
            drawPath(neck, accentColor, style = Stroke(width = 2.5f))
        }

        if (dorsal != null) {
            Text(
                text = "$dorsal",
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.30f).sp,
                color = Color.White
            )
        }
    }
}
