package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Official Unified TheBiG Logo:
 * Dark navy blue (#0F172A) background with a gleaming gold (#F59E0B / #FBBF24) championship trophy
 * cleanly traversed by two parallel diagonal cutting lines (#0F172A).
 */
@Composable
fun TheBiGLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height

                // 1. Dark Navy Blue Rounded Square Background (#0F172A)
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(w * 0.24f, h * 0.24f)
                )

                // Gold Accent Border
                drawRoundRect(
                    color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(w * 0.24f, h * 0.24f),
                    style = Stroke(width = w * 0.035f)
                )

                // 2. Official Golden Championship Trophy
                // Cup Bowl
                val cupPath = Path().apply {
                    moveTo(w * 0.30f, h * 0.26f)
                    lineTo(w * 0.70f, h * 0.26f)
                    cubicTo(w * 0.70f, h * 0.48f, w * 0.65f, h * 0.58f, w * 0.50f, h * 0.62f)
                    cubicTo(w * 0.35f, h * 0.58f, w * 0.30f, h * 0.48f, w * 0.30f, h * 0.26f)
                    close()
                }
                drawPath(path = cupPath, color = Color(0xFFFBBF24), style = Fill)

                // Cup Rim Lip
                drawRoundRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(w * 0.28f, h * 0.24f),
                    size = Size(w * 0.44f, h * 0.05f),
                    cornerRadius = CornerRadius(w * 0.02f, h * 0.02f)
                )

                // Left Handle
                val leftHandle = Path().apply {
                    moveTo(w * 0.30f, h * 0.30f)
                    cubicTo(w * 0.16f, h * 0.30f, w * 0.16f, h * 0.46f, w * 0.30f, h * 0.50f)
                }
                drawPath(
                    path = leftHandle,
                    color = Color(0xFFF59E0B),
                    style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
                )

                // Right Handle
                val rightHandle = Path().apply {
                    moveTo(w * 0.70f, h * 0.30f)
                    cubicTo(w * 0.84f, h * 0.30f, w * 0.84f, h * 0.46f, w * 0.70f, h * 0.50f)
                }
                drawPath(
                    path = rightHandle,
                    color = Color(0xFFF59E0B),
                    style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
                )

                // Trophy Stem & Base
                drawRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(w * 0.46f, h * 0.62f),
                    size = Size(w * 0.08f, h * 0.09f)
                )
                // Base tier 1
                drawRoundRect(
                    color = Color(0xFFFBBF24),
                    topLeft = Offset(w * 0.38f, h * 0.71f),
                    size = Size(w * 0.24f, h * 0.04f),
                    cornerRadius = CornerRadius(w * 0.015f, h * 0.015f)
                )
                // Base tier 2 (Pedestal)
                drawRoundRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(w * 0.32f, h * 0.75f),
                    size = Size(w * 0.36f, h * 0.06f),
                    cornerRadius = CornerRadius(w * 0.02f, h * 0.02f)
                )

                // 3. Two Signature Diagonal Cutting Lines (#0F172A) traversing through the trophy
                drawLine(
                    color = Color(0xFF0F172A),
                    start = Offset(w * 0.22f, h * 0.60f),
                    end = Offset(w * 0.78f, h * 0.26f),
                    strokeWidth = w * 0.04f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF0F172A),
                    start = Offset(w * 0.28f, h * 0.68f),
                    end = Offset(w * 0.84f, h * 0.34f),
                    strokeWidth = w * 0.04f,
                    cap = StrokeCap.Round
                )
            }
        }

        if (showText) {
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "The",
                    fontWeight = FontWeight.Light,
                    fontSize = (size.value * 0.52f).sp,
                    color = Color.White
                )
                Text(
                    text = "BiG",
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.62f).sp,
                    color = Color(0xFFF59E0B)
                )
            }
        }
    }
}

@Composable
fun TheBiGLogoVector(size: Dp = 40.dp) {
    TheBiGLogo(size = size, showText = false)
}
