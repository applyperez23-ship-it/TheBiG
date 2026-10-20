package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.TheBiGDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MatchTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null
    private lateinit var notificationManager: NotificationManager
    private val notificationId = 1001
    private val channelId = "thebig_match_timer_channel"

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> {
                val matchId = intent?.getLongExtra(EXTRA_MATCH_ID, 0L) ?: 0L
                val homeName = intent?.getStringExtra(EXTRA_HOME_NAME) ?: "Local"
                val awayName = intent?.getStringExtra(EXTRA_AWAY_NAME) ?: "Visitante"
                val homeScore = intent?.getIntExtra(EXTRA_HOME_SCORE, 0) ?: 0
                val awayScore = intent?.getIntExtra(EXTRA_AWAY_SCORE, 0) ?: 0
                val period = intent?.getStringExtra(EXTRA_PERIOD) ?: "PRIMER_TIEMPO"
                val elapsed = intent?.getIntExtra(EXTRA_ELAPSED, 0) ?: 0

                MatchTimerManager.updateState(
                    MatchTimerState(
                        matchId = matchId,
                        homeTeamName = homeName,
                        awayTeamName = awayName,
                        homeScore = homeScore,
                        awayScore = awayScore,
                        period = period,
                        elapsedSeconds = elapsed,
                        isRunning = true
                    )
                )

                startForegroundServiceWithNotification()
                startTicker()
            }
            ACTION_PAUSE -> {
                MatchTimerManager.setRunning(false)
                updateNotification()
            }
            ACTION_RESUME -> {
                MatchTimerManager.setRunning(true)
                updateNotification()
            }
            ACTION_STOP -> {
                stopTicker()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            var counter = 0
            while (isActive) {
                delay(1000)
                MatchTimerManager.tick()
                counter++
                // Update notification every second
                updateNotification()

                // Save elapsed time to Room DB every 5 seconds
                if (counter % 5 == 0) {
                    val current = MatchTimerManager.timerState.value
                    if (current.matchId > 0) {
                        try {
                            val db = TheBiGDatabase.getDatabase(applicationContext)
                            val match = db.matchDao().getMatchByIdOnce(current.matchId)
                            if (match != null) {
                                db.matchDao().updateMatch(
                                    match.copy(
                                        elapsedSeconds = current.elapsedSeconds,
                                        homeScore = current.homeScore,
                                        awayScore = current.awayScore,
                                        currentPeriod = current.period
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun buildNotification(): Notification {
        val state = MatchTimerManager.timerState.value
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val periodLabel = formatPeriodLabel(state.period)
        val title = "${state.homeTeamName} ${state.homeScore} - ${state.awayScore} ${state.awayTeamName}"
        val text = "Tiempo: ${state.formattedTime} | $periodLabel"

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(state.isRunning)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification() {
        notificationManager.notify(notificationId, buildNotification())
    }

    private fun formatPeriodLabel(period: String): String {
        return when (period) {
            "PRIMER_TIEMPO" -> "1T"
            "DESCANSO" -> "Descanso"
            "SEGUNDO_TIEMPO" -> "2T"
            "TIEMPO_EXTRA_1" -> "1TE"
            "TIEMPO_EXTRA_2" -> "2TE"
            "PENALTIS" -> "Penaltis"
            "FINALIZADO" -> "Final"
            else -> period
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TheBiG Cronometro de Partido",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el tiempo y marcador en vivo durante el partido"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopTicker()
    }

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_MATCH_ID = "EXTRA_MATCH_ID"
        const val EXTRA_HOME_NAME = "EXTRA_HOME_NAME"
        const val EXTRA_AWAY_NAME = "EXTRA_AWAY_NAME"
        const val EXTRA_HOME_SCORE = "EXTRA_HOME_SCORE"
        const val EXTRA_AWAY_SCORE = "EXTRA_AWAY_SCORE"
        const val EXTRA_PERIOD = "EXTRA_PERIOD"
        const val EXTRA_ELAPSED = "EXTRA_ELAPSED"

        fun start(
            context: Context,
            matchId: Long,
            homeName: String,
            awayName: String,
            homeScore: Int,
            awayScore: Int,
            period: String,
            elapsed: Int
        ) {
            val intent = Intent(context, MatchTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MATCH_ID, matchId)
                putExtra(EXTRA_HOME_NAME, homeName)
                putExtra(EXTRA_AWAY_NAME, awayName)
                putExtra(EXTRA_HOME_SCORE, homeScore)
                putExtra(EXTRA_AWAY_SCORE, awayScore)
                putExtra(EXTRA_PERIOD, period)
                putExtra(EXTRA_ELAPSED, elapsed)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, MatchTimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, MatchTimerService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MatchTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
