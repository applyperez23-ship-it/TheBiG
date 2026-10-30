package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Team
import com.example.data.repository.TheBiGRepository
import com.example.service.LiveTimerState
import com.example.service.MatchTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MatchViewModel(private val repository: TheBiGRepository) : ViewModel() {

    val matches: StateFlow<List<Match>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveMatches: StateFlow<List<Match>> = repository.liveMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timerState: StateFlow<LiveTimerState> = MatchTimerService.timerState

    private val _selectedMatchId = MutableStateFlow<Long?>(null)
    val selectedMatchId: StateFlow<Long?> = _selectedMatchId.asStateFlow()

    fun selectMatch(id: Long?) {
        _selectedMatchId.value = id
    }

    suspend fun createMatch(
        tournamentId: Long,
        homeTeamId: Long,
        awayTeamId: Long,
        roundName: String = "Jornada"
    ): Long {
        val m = Match(
            tournamentId = tournamentId,
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId,
            roundName = roundName.trim(),
            status = "SCHEDULED"
        )
        return repository.saveMatch(m)
    }

    fun deleteMatch(id: Long) {
        viewModelScope.launch {
            repository.deleteMatch(id)
            if (_selectedMatchId.value == id) {
                _selectedMatchId.value = null
            }
        }
    }

    fun startLiveMatch(
        context: Context,
        match: Match,
        homeTeam: Team,
        awayTeam: Team,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            repository.updateMatch(match.copy(status = "LIVE"))
            val intent = Intent(context, MatchTimerService::class.java).apply {
                action = MatchTimerService.ACTION_START
                putExtra(MatchTimerService.EXTRA_MATCH_ID, match.id)
                putExtra(MatchTimerService.EXTRA_HOME_NAME, homeTeam.name)
                putExtra(MatchTimerService.EXTRA_AWAY_NAME, awayTeam.name)
                putExtra(MatchTimerService.EXTRA_DURATION, durationMinutes)
                putExtra(MatchTimerService.EXTRA_ELAPSED, match.elapsedSeconds)
                putExtra(MatchTimerService.EXTRA_PERIOD, match.currentPeriod)
            }
            context.startForegroundService(intent)
        }
    }

    fun pauseLiveMatch(context: Context) {
        val intent = Intent(context, MatchTimerService::class.java).apply {
            action = MatchTimerService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeLiveMatch(context: Context) {
        val intent = Intent(context, MatchTimerService::class.java).apply {
            action = MatchTimerService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun startSecondHalf(context: Context) {
        val intent = Intent(context, MatchTimerService::class.java).apply {
            action = MatchTimerService.ACTION_START_SECOND_HALF
        }
        context.startService(intent)
    }

    fun finishMatch(context: Context, matchId: Long, elapsedSeconds: Int) {
        viewModelScope.launch {
            val m = repository.getMatchOnce(matchId) ?: return@launch
            repository.updateMatch(m.copy(status = "FINISHED", elapsedSeconds = elapsedSeconds))
            val intent = Intent(context, MatchTimerService::class.java).apply {
                action = MatchTimerService.ACTION_STOP
            }
            context.startService(intent)
        }
    }

    fun addGoal(
        matchId: Long,
        teamId: Long,
        playerId: Long,
        assistPlayerId: Long?,
        isOwnGoal: Boolean,
        isPenalty: Boolean,
        minute: Int
    ) {
        viewModelScope.launch {
            val eventType = when {
                isOwnGoal -> "OWN_GOAL"
                isPenalty -> "PENALTY_GOAL"
                else -> "GOAL"
            }
            val event = MatchEvent(
                matchId = matchId,
                teamId = teamId,
                playerId = playerId,
                assistPlayerId = assistPlayerId,
                eventType = eventType,
                minute = minute
            )
            repository.addMatchEvent(event)
        }
    }

    fun addPenaltyMissed(matchId: Long, teamId: Long, playerId: Long, minute: Int) {
        viewModelScope.launch {
            val event = MatchEvent(
                matchId = matchId,
                teamId = teamId,
                playerId = playerId,
                eventType = "PENALTY_MISSED",
                minute = minute,
                extraInfo = "Penalti fallado/atajado"
            )
            repository.addMatchEvent(event)
        }
    }

    fun addCard(matchId: Long, teamId: Long, playerId: Long, isRed: Boolean, minute: Int) {
        viewModelScope.launch {
            val event = MatchEvent(
                matchId = matchId,
                teamId = teamId,
                playerId = playerId,
                eventType = if (isRed) "RED_CARD" else "YELLOW_CARD",
                minute = minute
            )
            repository.addMatchEvent(event)
        }
    }

    fun addSubstitution(matchId: Long, teamId: Long, playerOutId: Long, playerInId: Long, minute: Int) {
        viewModelScope.launch {
            val event = MatchEvent(
                matchId = matchId,
                teamId = teamId,
                playerId = playerInId,
                assistPlayerId = playerOutId,
                eventType = "SUBSTITUTION",
                minute = minute,
                extraInfo = "Cambio táctico"
            )
            repository.addMatchEvent(event)
        }
    }

    fun recordPenaltyShootoutScore(matchId: Long, homePenalties: Int, awayPenalties: Int) {
        viewModelScope.launch {
            val m = repository.getMatchOnce(matchId) ?: return@launch
            repository.updateMatch(m.copy(homePenalties = homePenalties, awayPenalties = awayPenalties))
        }
    }

    fun setMatchMvp(matchId: Long, teamId: Long, playerId: Long) {
        viewModelScope.launch {
            val m = repository.getMatchOnce(matchId) ?: return@launch
            repository.updateMatch(m.copy(mvpPlayerId = playerId))
            val event = MatchEvent(
                matchId = matchId,
                teamId = teamId,
                playerId = playerId,
                eventType = "MVP",
                extraInfo = "Jugador MVP del Partido"
            )
            repository.addMatchEvent(event)
        }
    }

    fun deleteEvent(event: MatchEvent) {
        viewModelScope.launch {
            repository.deleteMatchEvent(event)
        }
    }
}

class MatchViewModelFactory(private val repository: TheBiGRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MatchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
