package com.example.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

/**
 * 3D Lottie Soccer Ball Kickoff Animation:
 * Uses official Lottie JSON animation from LottieFiles running in under 1.5 seconds,
 * completely replacing any flat static PNGs.
 */
@Composable
fun KickoffAnimationOverlay(
    onComplete: () -> Unit
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://assets5.lottiefiles.com/packages/lf20_w98ccncd.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 1.5f
    )

    // Complete transition when Lottie finishes its single iteration
    LaunchedEffect(progress) {
        if (progress >= 1f) {
            onComplete()
        }
    }

    // Safety fallback: ensure guaranteed clean transition in < 1.5s (1350ms) even if network is slow/offline
    LaunchedEffect(Unit) {
        delay(1350)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16).copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Kickoff Badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF10B981),
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "TheBiG • Partido Oficial en Vivo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B)
            )
        }
    }
}
