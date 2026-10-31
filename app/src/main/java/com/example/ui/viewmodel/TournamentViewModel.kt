package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.data.local.entity.TournamentModes
import com.example.data.local.entity.AwardTimingModes
import com.example.data.local.entity.TournamentTeamCrossRef
import com.example.data.repository.TheBiGRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TournamentViewModel(private val repository: TheBiGRepository) : ViewModel() {

    // Clean initial state: zero tournaments loaded
    val tournaments: StateFlow<List<Tournament>> = repository.allTournaments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTeams: StateFlow<List<Team>> = repository.allTeams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTournamentId = MutableStateFlow<Long?>(null)
    val selectedTournamentId: StateFlow<Long?> = _selectedTournamentId.asStateFlow()

    private val _selectedSubTab = MutableStateFlow(0)
    val selectedSubTab: StateFlow<Int> = _selectedSubTab.asStateFlow()

    fun selectTournament(id: Long?) {
        _selectedTournamentId.value = id
        _selectedSubTab.value = 0
    }

    fun selectSubTab(tabIndex: Int) {
        _selectedSubTab.value = tabIndex
    }

    suspend fun createTournament(
        name: String,
        edition: String,
        entryFee: Double,
        playersPerTeam: Int = 7,
        matchDurationMinutes: Int = 20,
        tournamentType: String = TournamentModes.LEAGUE,
        awardTiming: String = AwardTimingModes.END_OF_LEAGUE,
        prizeChampion: Double,
        prizeRunnerUp: Double,
        prizeScorer: Double,
        prizeAssister: Double,
        prizeBestGk: Double,
        prizeMvp: Double = 0.0,
        yellowCardFine: Double = 5.0,
        redCardFine: Double = 10.0,
        minMatchesGk: Int = 2,
        financialModel: String = "BY_TEAM"
    ): Long {
        val t = Tournament(
            name = name.trim(),
            edition = edition.trim(),
            entryFee = entryFee,
            playersPerTeam = playersPerTeam,
            matchDurationMinutes = matchDurationMinutes,
            tournamentType = tournamentType,
            awardTiming = awardTiming,
            prizeChampionEnabled = prizeChampion > 0,
            prizeChampionAmount = prizeChampion,
            prizeRunnerUpEnabled = prizeRunnerUp > 0,
            prizeRunnerUpAmount = prizeRunnerUp,
            prizeTopScorerEnabled = prizeScorer > 0,
            prizeTopScorerAmount = prizeScorer,
            prizeTopAssisterEnabled = prizeAssister > 0,
            prizeTopAssisterAmount = prizeAssister,
            prizeBestGkEnabled = prizeBestGk > 0,
            prizeBestGkAmount = prizeBestGk,
            prizeMvpEnabled = true,
            prizeMvpAmount = prizeMvp,
            yellowCardFine = yellowCardFine,
            redCardFine = redCardFine,
            minMatchesForBestGk = minMatchesGk,
            financialModel = financialModel
        )
        return repository.saveTournament(t)
    }

    fun updateTournament(tournament: Tournament) {
        viewModelScope.launch {
            repository.updateTournament(tournament)
        }
    }

    fun deleteTournament(id: Long) {
        viewModelScope.launch {
            repository.deleteTournament(id)
            if (_selectedTournamentId.value == id) {
                _selectedTournamentId.value = null
            }
        }
    }

    fun enrollTeam(tournamentId: Long, teamId: Long, feePaid: Boolean) {
        viewModelScope.launch {
            repository.enrollTeam(tournamentId, teamId, feePaid)
        }
    }

    fun removeTeamFromTournament(tournamentId: Long, teamId: Long) {
        viewModelScope.launch {
            repository.removeTeamFromTournament(tournamentId, teamId)
        }
    }

    fun updateTeamFeeStatus(tournamentId: Long, teamId: Long, feePaid: Boolean) {
        viewModelScope.launch {
            repository.updateTournamentTeamFee(tournamentId, teamId, feePaid)
        }
    }

    fun generateFixture(tournamentId: Long) {
        viewModelScope.launch {
            repository.generateRoundRobinFixture(tournamentId)
        }
    }

    fun setChampion(tournamentId: Long, champTeamId: Long, runnerUpId: Long?) {
        viewModelScope.launch {
            val t = repository.getTournamentOnce(tournamentId) ?: return@launch
            repository.updateTournament(
                t.copy(
                    championTeamId = champTeamId,
                    runnerUpTeamId = runnerUpId,
                    status = "FINISHED"
                )
            )
        }
    }
}

class TournamentViewModelFactory(private val repository: TheBiGRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TournamentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TournamentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
