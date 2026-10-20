package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.data.local.entity.TournamentTeamCrossRef
import com.example.data.repository.TheBiGRepository
import com.example.domain.rules.AssisterStat
import com.example.domain.rules.GoalkeeperStat
import com.example.domain.rules.PrizeCalculator
import com.example.domain.rules.ScorerStat
import com.example.domain.rules.StandingsCalculator
import com.example.domain.rules.StatsCalculator
import com.example.domain.rules.TeamStanding
import com.example.domain.rules.TournamentFinancialSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TournamentDetailUiState(
    val tournament: Tournament? = null,
    val enrolledTeams: List<Team> = emptyList(),
    val crossRefs: List<TournamentTeamCrossRef> = emptyList(),
    val matches: List<Match> = emptyList(),
    val events: List<MatchEvent> = emptyList(),
    val players: List<Player> = emptyList(),
    val standings: List<TeamStanding> = emptyList(),
    val topScorers: List<ScorerStat> = emptyList(),
    val topAssisters: List<AssisterStat> = emptyList(),
    val goalkeeperRankings: List<GoalkeeperStat> = emptyList(),
    val financialSummary: TournamentFinancialSummary? = null,
    val selectedSubTab: Int = 0 // 0: Tablas, 1: Partidos, 2: Estadísticas, 3: Finanzas
)

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
        prizeChampion: Double,
        prizeRunnerUp: Double,
        prizeScorer: Double,
        prizeAssister: Double,
        prizeBestGk: Double,
        minMatchesGk: Int
    ): Long {
        val t = Tournament(
            name = name.trim(),
            edition = edition.trim(),
            entryFee = entryFee,
            playersPerTeam = playersPerTeam,
            matchDurationMinutes = matchDurationMinutes,
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
            minMatchesForBestGk = minMatchesGk
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

    fun enrollTeam(tournamentId: Long, teamId: Long, feePaid: Boolean = false) {
        viewModelScope.launch {
            repository.enrollTeam(tournamentId, teamId, feePaid)
        }
    }

    fun removeTeam(tournamentId: Long, teamId: Long) {
        viewModelScope.launch {
            repository.removeTeamFromTournament(tournamentId, teamId)
        }
    }

    fun toggleTeamFee(tournamentId: Long, teamId: Long, currentPaid: Boolean) {
        viewModelScope.launch {
            repository.updateTournamentTeamFee(tournamentId, teamId, !currentPaid)
        }
    }

    fun generateRoundRobinFixture(tournamentId: Long, teams: List<Team>) {
        if (teams.size < 2) return
        viewModelScope.launch {
            val matchList = mutableListOf<Match>()
            val count = teams.size
            var roundNum = 1
            for (i in 0 until count) {
                for (j in i + 1 until count) {
                    matchList.add(
                        Match(
                            tournamentId = tournamentId,
                            homeTeamId = teams[i].id,
                            awayTeamId = teams[j].id,
                            roundName = "Jornada $roundNum",
                            status = "SCHEDULED",
                            currentPeriod = "NO_INICIADO"
                        )
                    )
                    roundNum++
                }
            }
            for (m in matchList) {
                repository.saveMatch(m)
            }
        }
    }

    fun setTournamentChampion(tournamentId: Long, championTeamId: Long, runnerUpTeamId: Long?) {
        viewModelScope.launch {
            val t = repository.getTournamentOnce(tournamentId) ?: return@launch
            repository.updateTournament(
                t.copy(
                    championTeamId = championTeamId,
                    runnerUpTeamId = runnerUpTeamId,
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
