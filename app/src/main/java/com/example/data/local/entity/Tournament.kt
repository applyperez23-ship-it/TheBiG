package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object TournamentModes {
    const val LEAGUE = "LEAGUE" // Liga Tradicional
    const val CUP = "CUP" // Copa (Eliminatoria Directa)
    const val GROUPS_CUP = "GROUPS_CUP" // Fase de Grupos + Eliminatoria
    const val MIXED = "MIXED" // Liga + Copa Simultánea
}

object AwardTimingModes {
    const val END_OF_LEAGUE = "END_OF_LEAGUE" // Al terminar la Liga
    const val END_OF_CUP = "END_OF_CUP" // Al finalizar la eliminatoria de Copa
}

@Entity(tableName = "tournaments")
data class Tournament(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val edition: String = "1",
    val entryFee: Double = 0.0,
    val playersPerTeam: Int = 7,
    val matchDurationMinutes: Int = 20,
    val status: String = "PLANNING", // PLANNING, IN_PROGRESS, FINISHED
    val tournamentType: String = TournamentModes.LEAGUE,
    val awardTiming: String = AwardTimingModes.END_OF_LEAGUE,
    val championTeamId: Long? = null,
    val runnerUpTeamId: Long? = null,
    val prizeChampionEnabled: Boolean = true,
    val prizeChampionAmount: Double = 0.0,
    val prizeRunnerUpEnabled: Boolean = true,
    val prizeRunnerUpAmount: Double = 0.0,
    val prizeTopScorerEnabled: Boolean = true,
    val prizeTopScorerAmount: Double = 0.0,
    val prizeTopAssisterEnabled: Boolean = true,
    val prizeTopAssisterAmount: Double = 0.0,
    val prizeBestGkEnabled: Boolean = true,
    val prizeBestGkAmount: Double = 0.0,
    val prizeMvpEnabled: Boolean = true,
    val prizeMvpAmount: Double = 0.0,
    val yellowCardFine: Double = 5.0,
    val redCardFine: Double = 10.0,
    val minMatchesForBestGk: Int = 2,
    val financialModel: String = "BY_TEAM", // BY_TEAM or BY_PLAYER
    val createdAt: Long = System.currentTimeMillis()
) {
    val feePerPlayer: Double
        get() = if (financialModel == "BY_PLAYER") entryFee else (if (playersPerTeam > 0) entryFee / playersPerTeam else 0.0)

    val totalTeamFee: Double
        get() = if (financialModel == "BY_PLAYER") entryFee * playersPerTeam else entryFee
}
