package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive Kickoff Animation Overlay:
 * A photorealistic 3D soccer ball shoots from a screen corner and explodes with a
 * powerful "bomba" expanding shockwave covering the entire UI in < 1.4 seconds,
 * cleanly transitioning into the live referee match sheet.
 */
@Composable
fun KickoffAnimationOverlay(
    onComplete: () -> Unit
) {
    val travelProgress = remember { Animatable(0f) }
    val rotationAngle = remember { Animatable(0f) }
    val bombScale = remember { Animatable(1f) }
    val bombFlashAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                textAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            }
            launch {
                rotationAngle.animateTo(
                    targetValue = 1080f,
                    animationSpec = tween(durationMillis = 1300, easing = LinearEasing)
                )
            }
            // Step 1: Ball travels from bottom-left corner to center (0 - 650ms)
            launch {
                travelProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
                // Step 2: "Bomba" explosive expansion covering entire UI (650 - 1300ms)
                launch {
                    bombFlashAlpha.animateTo(
                        targetValue = 0.95f,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                    )
                }
                bombScale.animateTo(
                    targetValue = 28f,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
                onComplete()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Start coordinate: bottom-left corner
        val startX = -screenWidth.value * 0.45f
        val startY = screenHeight.value * 0.45f
        // Control point curving upwards ("efecto comba")
        val controlX = -screenWidth.value * 0.15f
        val controlY = -screenHeight.value * 0.35f

        val t = travelProgress.value
        val u = 1f - t
        val currentX = (u * u * startX + 2 * u * t * controlX + t * t * 0f).dp
        val currentY = (u * u * startY + 2 * u * t * controlY + t * t * 0f).dp

        // Explosive energy shockwave ring behind the ball
        if (travelProgress.value > 0.8f) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(bombScale.value * 1.2f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF59E0B).copy(alpha = 0.8f),
                                Color(0xFF10B981).copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Photorealistic 3D Soccer Ball
        Image(
            painter = painterResource(id = R.drawable.thebig_ball_realistic),
            contentDescription = "Balón Oficial TheBiG",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .offset(x = currentX, y = currentY)
                .rotate(rotationAngle.value)
                .scale(if (travelProgress.value < 1f) (0.6f + travelProgress.value * 0.6f) else bombScale.value)
                .size(110.dp)
        )

        // Flash overlay covering whole interface on blast impact
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(bombFlashAlpha.value)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.95f),
                            Color(0xFF0F172A).copy(alpha = 0.98f)
                        )
                    )
                )
        )

        // Central Kickoff Badge & Message
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
                .alpha(textAlpha.value * (1f - bombFlashAlpha.value))
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF10B981),
                shadowElevation = 10.dp
            ) {
                Text(
                    text = "¡SAQUE INICIAL!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Balón Oficial TheBiG en Juego",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B)
            )
        }
    }
}
