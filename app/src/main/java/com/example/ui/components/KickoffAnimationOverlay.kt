package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Kickoff Animation Overlay:
 * Renders a curved parabolic trajectory with ball rotation and motion trail.
 * Total duration: 1100ms (<1.5s).
 */
@Composable
fun KickoffAnimationOverlay(
    onComplete: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val rotationAngle = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                textAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
            launch {
                rotationAngle.animateTo(
                    targetValue = 720f,
                    animationSpec = tween(durationMillis = 1100, easing = LinearEasing)
                )
            }
            launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
                )
                onComplete()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Canvas showing Curved Spinning Football Trajectory
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Bezier curve trajectory points
            val startX = w * 0.15f
            val startY = h * 0.70f
            val controlX = w * 0.50f
            val controlY = h * 0.20f
            val endX = w * 0.85f
            val endY = h * 0.55f

            val t = progress.value

            // Quadratic Bezier formula: B(t) = (1-t)^2 * P0 + 2*(1-t)*t * P1 + t^2 * P2
            val ballX = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * controlX + t * t * endX
            val ballY = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * controlY + t * t * endY

            // Draw motion trail
            val trailPath = Path().apply {
                moveTo(startX, startY)
                val steps = 30
                val curStep = (steps * t).toInt()
                for (i in 1..curStep) {
                    val stepT = i.toFloat() / steps
                    val px = (1 - stepT) * (1 - stepT) * startX + 2 * (1 - stepT) * stepT * controlX + stepT * stepT * endX
                    val py = (1 - stepT) * (1 - stepT) * startY + 2 * (1 - stepT) * stepT * controlY + stepT * stepT * endY
                    lineTo(px, py)
                }
            }
            drawPath(
                path = trailPath,
                color = Color(0xFFF59E0B).copy(alpha = 0.45f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw spinning football at (ballX, ballY)
            val ballRadius = 24.dp.toPx()
            rotate(degrees = rotationAngle.value, pivot = Offset(ballX, ballY)) {
                drawBall(ballX, ballY, ballRadius)
            }
        }

        // Central Kickoff Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 60.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF10B981).copy(alpha = textAlpha.value),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "¡SAQUE INICIAL!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Comienza el Partido Oficial",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = textAlpha.value * 0.9f)
            )
        }
    }
}

private fun DrawScope.drawBall(cx: Float, cy: Float, radius: Float) {
    // White ball base
    drawCircle(
        color = Color.White,
        radius = radius,
        center = Offset(cx, cy)
    )
    // Dark outer border
    drawCircle(
        color = Color(0xFF1E293B),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx())
    )

    // Central pentagon
    val pentagonRadius = radius * 0.38f
    val path = Path()
    for (i in 0 until 5) {
        val angle = (i * 72.0 - 90.0) * PI / 180.0
        val px = (cx + pentagonRadius * cos(angle)).toFloat()
        val py = (cy + pentagonRadius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path = path, color = Color(0xFF1E293B))

    // Seams from pentagon vertices to outer edge
    for (i in 0 until 5) {
        val angle = (i * 72.0 - 90.0) * PI / 180.0
        val p1x = (cx + pentagonRadius * cos(angle)).toFloat()
        val p1y = (cy + pentagonRadius * sin(angle)).toFloat()
        val p2x = (cx + radius * cos(angle)).toFloat()
        val p2y = (cy + radius * sin(angle)).toFloat()
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(p1x, p1y),
            end = Offset(p2x, p2y),
            strokeWidth = 2.dp.toPx()
        )
    }
}
