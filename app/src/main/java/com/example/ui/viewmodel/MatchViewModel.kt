package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.repository.TheBiGRepository
import com.example.service.MatchTimerManager
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

    val timerState = MatchTimerManager.timerState

    private val _selectedMatchId = MutableStateFlow<Long?>(null)
    val selectedMatchId: StateFlow<Long?> = _selectedMatchId.asStateFlow()

    fun selectMatch(id: Long?) {
        _selectedMatchId.value = id
    }

    suspend fun createMatch(
        tournamentId: Long,
        homeTeamId: Long,
        awayTeamId: Long,
        roundName: String,
        matchDate: String = "",
        matchTime: String = "",
        isKnockout: Boolean = false
    ): Long {
        val match = Match(
            tournamentId = tournamentId,
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId,
            roundName = roundName.trim(),
            matchDate = matchDate,
            matchTime = matchTime,
            isKnockout = isKnockout,
            status = "SCHEDULED",
            currentPeriod = "NO_INICIADO"
        )
        return repository.saveMatch(match)
    }

    fun startLiveMatch(
        context: Context,
        match: Match,
        homeTeam: Team,
        awayTeam: Team
    ) {
        viewModelScope.launch {
            val updated = match.copy(
                status = "LIVE",
                currentPeriod = if (match.currentPeriod == "NO_INICIADO") "PRIMER_TIEMPO" else match.currentPeriod
            )
            repository.updateMatch(updated)
            MatchTimerService.start(
                context = context,
                matchId = updated.id,
                homeName = homeTeam.name,
                awayName = awayTeam.name,
                homeScore = updated.homeScore,
                awayScore = updated.awayScore,
                period = updated.currentPeriod,
                elapsed = updated.elapsedSeconds
            )
        }
    }

    fun pauseLiveMatch(context: Context) {
        MatchTimerService.pause(context)
    }

    fun resumeLiveMatch(context: Context) {
        MatchTimerService.resume(context)
    }

    fun updatePeriod(context: Context, matchId: Long, newPeriod: String) {
        viewModelScope.launch {
            MatchTimerManager.setPeriod(newPeriod)
            val match = repository.getMatchOnce(matchId) ?: return@launch
            val updated = match.copy(
                currentPeriod = newPeriod,
                elapsedSeconds = MatchTimerManager.timerState.value.elapsedSeconds
            )
            repository.updateMatch(updated)
        }
    }

    fun finishMatch(context: Context, matchId: Long) {
        viewModelScope.launch {
            val currentElapsed = MatchTimerManager.timerState.value.elapsedSeconds
            MatchTimerService.stop(context)
            MatchTimerManager.reset()

            val match = repository.getMatchOnce(matchId) ?: return@launch
            val updated = match.copy(
                status = "FINISHED",
                currentPeriod = "FINALIZADO",
                elapsedSeconds = currentElapsed
            )
            repository.updateMatch(updated)
        }
    }

    fun addGoal(
        matchId: Long,
        teamId: Long,
        playerId: Long?,
        assistPlayerId: Long? = null,
        isOwnGoal: Boolean = false,
        isPenalty: Boolean = false,
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

            // Sync with timer state
            val match = repository.getMatchOnce(matchId) ?: return@launch
            MatchTimerManager.setScore(match.homeScore, match.awayScore)
        }
    }

    fun addPenaltyMissed(matchId: Long, teamId: Long, playerId: Long?, minute: Int) {
        viewModelScope.launch {
            val event = MatchEvent(
                matchId = matchId,
                teamId = teamId,
                playerId = playerId,
                eventType = "PENALTY_MISSED",
                minute = minute
            )
            repository.addMatchEvent(event)
        }
    }

    fun addCard(matchId: Long, teamId: Long, playerId: Long?, isRed: Boolean, minute: Int) {
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

    fun addSubstitution(matchId: Long, teamId: Long, playerOutId: Long?, playerInId: Long?, minute: Int) {
        viewModelScope.launch {
            val event = MatchEvent(
                matchId = matchId,
                teamId = teamId,
                playerId = playerInId,
                assistPlayerId = playerOutId,
                eventType = "SUBSTITUTION",
                minute = minute
            )
            repository.addMatchEvent(event)
        }
    }

    fun setMatchMvp(matchId: Long, teamId: Long, playerId: Long) {
        viewModelScope.launch {
            val currentEvents = repository.getEventsForMatchOnce(matchId)
            val oldMvp = currentEvents.firstOrNull { it.eventType == "MVP" }
            if (oldMvp != null) {
                repository.deleteMatchEvent(oldMvp)
            }
            val event = MatchEvent(
                matchId = matchId,
                teamId = teamId,
                playerId = playerId,
                eventType = "MVP",
                minute = 0
            )
            repository.addMatchEvent(event)
        }
    }

    fun deleteEvent(event: MatchEvent) {
        viewModelScope.launch {
            repository.deleteMatchEvent(event)
            val match = repository.getMatchOnce(event.matchId) ?: return@launch
            MatchTimerManager.setScore(match.homeScore, match.awayScore)
        }
    }

    fun recordPenaltyShootoutScore(matchId: Long, homePenalties: Int, awayPenalties: Int) {
        viewModelScope.launch {
            val match = repository.getMatchOnce(matchId) ?: return@launch
            repository.updateMatch(
                match.copy(
                    homePenalties = homePenalties,
                    awayPenalties = awayPenalties
                )
            )
        }
    }

    fun updateGoalkeepers(matchId: Long, homeGkId: Long?, awayGkId: Long?) {
        viewModelScope.launch {
            val match = repository.getMatchOnce(matchId) ?: return@launch
            repository.updateMatch(
                match.copy(
                    homeGoalkeeperPlayerId = homeGkId,
                    awayGoalkeeperPlayerId = awayGkId
                )
            )
        }
    }

    fun deleteMatch(id: Long) {
        viewModelScope.launch {
            repository.deleteMatch(id)
            if (_selectedMatchId.value == id) {
                _selectedMatchId.value = null
            }
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
