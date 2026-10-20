package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MatchTimerState(
    val matchId: Long = 0,
    val homeTeamName: String = "",
    val awayTeamName: String = "",
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val period: String = "PRIMER_TIEMPO",
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false
) {
    val formattedTime: String
        get() {
            val m = elapsedSeconds / 60
            val s = elapsedSeconds % 60
            return "%02d:%02d".format(m, s)
        }
}

object MatchTimerManager {
    private val _timerState = MutableStateFlow(MatchTimerState())
    val timerState: StateFlow<MatchTimerState> = _timerState.asStateFlow()

    fun updateState(state: MatchTimerState) {
        _timerState.value = state
    }

    fun tick() {
        val current = _timerState.value
        if (current.isRunning) {
            _timerState.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
        }
    }

    fun setScore(home: Int, away: Int) {
        _timerState.value = _timerState.value.copy(homeScore = home, awayScore = away)
    }

    fun setPeriod(period: String) {
        _timerState.value = _timerState.value.copy(period = period)
    }

    fun setRunning(running: Boolean) {
        _timerState.value = _timerState.value.copy(isRunning = running)
    }

    fun reset() {
        _timerState.value = MatchTimerState()
    }
}
