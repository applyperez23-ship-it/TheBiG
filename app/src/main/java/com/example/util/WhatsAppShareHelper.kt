package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsAppShareHelper {

    private const val SUPPORT_PHONE_NUMBER = "+5363270465"

    fun openSupportWhatsApp(context: Context, initialMessage: String = "Hola Daniel (el Grande), te contacto desde TheBiG para un reporte o sugerencia:") {
        try {
            val encodedMessage = Uri.encode(initialMessage)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$SUPPORT_PHONE_NUMBER&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$SUPPORT_PHONE_NUMBER")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(genericIntent)
        }
    }
}
