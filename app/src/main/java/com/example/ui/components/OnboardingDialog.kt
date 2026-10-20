package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.WhatsAppShareHelper

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { /* Mandatory dialog - cannot dismiss by clicking outside */ },
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Bienvenido a TheBiG",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "La app fue desarrollada por Daniel (el Grande), con el propósito de un buen torneo de fútbol. Si encuentras algún bug (error) o tienes una sugerencia puedes contactarme por Whatsapp yendo a la configuración de la app a la función 'reportar'.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("onboarding_accept_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text(text = " Aceptar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    WhatsAppShareHelper.openSupportWhatsApp(context)
                },
                modifier = Modifier.testTag("onboarding_report_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Text(text = " Reportar")
            }
        }
    )
}
