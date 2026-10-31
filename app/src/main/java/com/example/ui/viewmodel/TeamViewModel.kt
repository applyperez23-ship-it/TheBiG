package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.repository.TheBiGRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TeamViewModel(private val repository: TheBiGRepository) : ViewModel() {

    // Clean initial state: zero teams loaded
    val teams: StateFlow<List<Team>> = repository.allTeams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTeamId = MutableStateFlow<Long?>(null)
    val selectedTeamId: StateFlow<Long?> = _selectedTeamId.asStateFlow()

    fun selectTeam(id: Long?) {
        _selectedTeamId.value = id
    }

    suspend fun createTeam(
        name: String,
        shortName: String,
        shieldVectorIndex: Int,
        primaryColorHex: String,
        secondaryColorHex: String,
        borderColorHex: String,
        customShieldUri: String? = null,
        jerseyPattern: String = "SOLID",
        goalkeeperColorHex: String = "#10B981",
        shieldPrimaryColorHex: String = primaryColorHex
    ): Long {
        val team = Team(
            name = name.trim(),
            shortName = shortName.trim().uppercase(),
            shieldVectorIndex = shieldVectorIndex,
            primaryColorHex = primaryColorHex,
            secondaryColorHex = secondaryColorHex,
            borderColorHex = borderColorHex,
            customShieldUri = customShieldUri,
            jerseyPattern = jerseyPattern,
            goalkeeperColorHex = goalkeeperColorHex,
            shieldPrimaryColorHex = shieldPrimaryColorHex
        )
        return repository.saveTeam(team)
    }

    fun updateTeam(team: Team) {
        viewModelScope.launch {
            repository.updateTeam(team)
        }
    }

    fun deleteTeam(id: Long) {
        viewModelScope.launch {
            repository.deleteTeam(id)
            if (_selectedTeamId.value == id) {
                _selectedTeamId.value = null
            }
        }
    }

    fun getPlayersForTeam(teamId: Long): StateFlow<List<Player>> {
        return repository.getPlayersForTeam(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    suspend fun addPlayer(
        teamId: Long,
        name: String,
        dorsal: Int,
        isGoalkeeper: Boolean,
        feePaid: Boolean = false,
        isCaptain: Boolean = false
    ): Result<Long> {
        val currentPlayers = repository.getPlayersForTeamOnce(teamId)
        if (currentPlayers.size >= 22) {
            return Result.failure(Exception("El equipo ya alcanzó el límite reglamentario de 22 jugadores"))
        }

        val player = Player(
            teamId = teamId,
            name = name.trim(),
            dorsal = dorsal,
            isGoalkeeper = isGoalkeeper,
            feePaid = feePaid,
            isCaptain = isCaptain
        )
        val id = repository.savePlayer(player)
        return Result.success(id)
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch {
            repository.updatePlayer(player)
        }
    }

    fun deletePlayer(id: Long) {
        viewModelScope.launch {
            repository.deletePlayer(id)
        }
    }

    fun togglePlayerFee(playerId: Long, currentPaid: Boolean) {
        viewModelScope.launch {
            repository.updatePlayerFeeStatus(playerId, !currentPaid)
        }
    }

    fun toggleCaptain(teamId: Long, playerId: Long, currentIsCaptain: Boolean) {
        viewModelScope.launch {
            repository.setTeamCaptain(teamId, playerId, !currentIsCaptain)
        }
    }
}

class TeamViewModelFactory(private val repository: TheBiGRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeamViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
