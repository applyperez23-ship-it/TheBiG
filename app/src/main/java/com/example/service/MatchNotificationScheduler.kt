package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.entity.Match
import com.example.data.local.entity.Team

object MatchNotificationScheduler {

    const val CHANNEL_ID = "thebig_match_reminders"
    const val EXTRA_MATCH_ID = "thebig_extra_match_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Partidos TheBiG",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos 1 hora antes del inicio de los partidos oficiales"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a notification for an upcoming match that opens MatchLiveSheetScreen via MainActivity.
     */
    fun showMatchReminder(
        context: Context,
        match: Match,
        homeTeam: Team?,
        awayTeam: Team?,
        tournamentName: String = "Torneo TheBiG"
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MATCH_ID, match.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            match.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val homeName = homeTeam?.name ?: "Local"
        val awayName = awayTeam?.name ?: "Visitante"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Próximo Partido en 1 Hora - $tournamentName")
            .setContentText("$homeName vs $awayName • ${match.roundName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("El encuentro entre $homeName y $awayName (${match.roundName}) está programado para comenzar en 1 hora. Toca aquí para abrir el acta arbitral en vivo."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(match.id.toInt() + 1000, notification)
    }
}
