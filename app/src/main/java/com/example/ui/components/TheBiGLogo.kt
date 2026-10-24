package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.WinnerGold

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
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            val shieldPath = Path().apply {
                moveTo(w * 0.5f, h * 0.04f)
                lineTo(w * 0.94f, h * 0.22f)
                lineTo(w * 0.94f, h * 0.65f)
                lineTo(w * 0.5f, h * 0.98f)
                lineTo(w * 0.06f, h * 0.65f)
                lineTo(w * 0.06f, h * 0.22f)
                close()
            }
            drawPath(
                path = shieldPath,
                color = Color(0xFF0F172A),
                style = Fill
            )
            drawPath(
                path = shieldPath,
                color = WinnerGold,
                style = Stroke(width = w * 0.06f)
            )

            val chevronPath = Path().apply {
                moveTo(w * 0.25f, h * 0.32f)
                lineTo(w * 0.55f, h * 0.32f)
                lineTo(w * 0.75f, h * 0.50f)
                lineTo(w * 0.55f, h * 0.68f)
                lineTo(w * 0.25f, h * 0.68f)
                lineTo(w * 0.45f, h * 0.50f)
                close()
            }
            drawPath(
                path = chevronPath,
                color = WinnerGold,
                style = Fill
            )

            drawCircle(
                color = PitchGreen,
                radius = w * 0.10f,
                center = Offset(w * 0.48f, h * 0.50f)
            )
        }

        if (showText) {
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "The",
                    fontWeight = FontWeight.Light,
                    fontSize = (size.value * 0.55f).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "BiG",
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.65f).sp,
                    color = WinnerGold
                )
            }
        }
    }
}
