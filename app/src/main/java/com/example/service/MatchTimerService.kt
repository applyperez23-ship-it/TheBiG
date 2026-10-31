package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveTimerState(
    val matchId: Long = 0,
    val homeTeamName: String = "",
    val awayTeamName: String = "",
    val elapsedSeconds: Int = 0,
    val totalDurationMinutes: Int = 20,
    val isRunning: Boolean = false,
    val currentPeriod: String = "1er Tiempo" // "1er Tiempo", "Descanso", "2do Tiempo", "Tiempo Extra", "Penaltis"
) {
    val halfTimeSeconds: Int
        get() = (totalDurationMinutes * 60) / 2

    val remainingSeconds: Int
        get() = ((totalDurationMinutes * 60) - elapsedSeconds).coerceAtLeast(0)

    val formattedElapsed: String
        get() = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    val formattedRemaining: String
        get() = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)

    val formattedTotal: String
        get() = "%02d:00".format(totalDurationMinutes)

    /**
     * Complete referee clock format: "04:37 / 20:00 - Restan 15:23"
     */
    val fullClockDisplay: String
        get() = "$formattedElapsed / $formattedTotal - Restan $formattedRemaining"
}

class MatchTimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val matchId = intent.getLongExtra(EXTRA_MATCH_ID, 0)
                val home = intent.getStringExtra(EXTRA_HOME_NAME) ?: ""
                val away = intent.getStringExtra(EXTRA_AWAY_NAME) ?: ""
                val duration = intent.getIntExtra(EXTRA_DURATION, 20)
                val elapsed = intent.getIntExtra(EXTRA_ELAPSED, 0)
                val halfTime = (duration * 60) / 2
                val period = when {
                    elapsed >= duration * 60 -> "Finalizado"
                    elapsed > halfTime -> "2do Tiempo"
                    elapsed == halfTime -> "Descanso"
                    else -> "1er Tiempo"
                }

                _timerState.value = LiveTimerState(
                    matchId = matchId,
                    homeTeamName = home,
                    awayTeamName = away,
                    totalDurationMinutes = duration,
                    elapsedSeconds = elapsed,
                    isRunning = true,
                    currentPeriod = period
                )
                startForeground(NOTIFICATION_ID, buildNotification(_timerState.value))
                startTimer()
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_RESUME -> {
                resumeTimer()
            }
            ACTION_START_SECOND_HALF -> {
                val current = _timerState.value
                val halfTime = current.halfTimeSeconds
                _timerState.value = current.copy(
                    elapsedSeconds = if (current.elapsedSeconds < halfTime) halfTime else current.elapsedSeconds,
                    currentPeriod = "2do Tiempo",
                    isRunning = true
                )
                startTimer()
            }
            ACTION_STOP -> {
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_timerState.value.isRunning) {
                delay(1000)
                val current = _timerState.value
                val newElapsed = current.elapsedSeconds + 1
                val halfTime = current.halfTimeSeconds

                // Automatically switch to Descanso (Half-time) when reaching halfTime
                if (current.currentPeriod == "1er Tiempo" && newElapsed >= halfTime) {
                    _timerState.value = current.copy(
                        elapsedSeconds = halfTime,
                        currentPeriod = "Descanso",
                        isRunning = false
                    )
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(_timerState.value))
                    break
                }

                val period = if (newElapsed > halfTime) "2do Tiempo" else "1er Tiempo"
                _timerState.value = current.copy(elapsedSeconds = newElapsed, currentPeriod = period)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(_timerState.value))
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(_timerState.value))
    }

    private fun resumeTimer() {
        val current = _timerState.value
        val period = if (current.currentPeriod == "Descanso") "2do Tiempo" else current.currentPeriod
        _timerState.value = current.copy(isRunning = true, currentPeriod = period)
        startTimer()
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TheBiG Cronómetro de Partido",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Cronómetro oficial de partido en vivo de TheBiG"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: LiveTimerState): Notification {
        val title = "${state.homeTeamName} vs ${state.awayTeamName}"
        val content = "${state.fullClockDisplay} (${state.currentPeriod})"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "thebig_match_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_START_SECOND_HALF = "com.example.service.ACTION_START_SECOND_HALF"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_MATCH_ID = "extra_match_id"
        const val EXTRA_HOME_NAME = "extra_home_name"
        const val EXTRA_AWAY_NAME = "extra_away_name"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_ELAPSED = "extra_elapsed"
        const val EXTRA_PERIOD = "extra_period"

        private val _timerState = MutableStateFlow(LiveTimerState())
        val timerState: StateFlow<LiveTimerState> = _timerState.asStateFlow()
    }
}
