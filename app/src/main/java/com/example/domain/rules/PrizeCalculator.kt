package com.example.domain.rules

import com.example.data.local.entity.Tournament
import com.example.data.local.entity.TournamentTeamCrossRef

data class TournamentFinancialSummary(
    val entryFee: Double,
    val enrolledTeamsCount: Int,
    val paidTeamsCount: Int,
    val pendingTeamsCount: Int,
    val estimatedRevenue: Double,
    val actualRevenue: Double,
    val pendingRevenue: Double,
    val prizes: List<PrizeItem>,
    val totalPrizesAllocated: Double,
    val estimatedNetBalance: Double,
    val actualNetBalance: Double
)

data class PrizeItem(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val amount: Double,
    val winnerNote: String = ""
)

object PrizeCalculator {
    fun calculate(
        tournament: Tournament,
        crossRefs: List<TournamentTeamCrossRef>
    ): TournamentFinancialSummary {
        val enrolledCount = crossRefs.size
        val paidCount = crossRefs.count { it.feePaid }
        val pendingCount = enrolledCount - paidCount

        val fee = tournament.entryFee
        val estimatedRev = enrolledCount * fee
        val actualRev = paidCount * fee
        val pendingRev = pendingCount * fee

        val prizeList = listOf(
            PrizeItem(
                id = "champion",
                name = "Campeón",
                enabled = tournament.prizeChampionEnabled,
                amount = if (tournament.prizeChampionEnabled) tournament.prizeChampionAmount else 0.0,
                winnerNote = "Equipo campeón del torneo"
            ),
            PrizeItem(
                id = "runner_up",
                name = "Subcampeón",
                enabled = tournament.prizeRunnerUpEnabled,
                amount = if (tournament.prizeRunnerUpEnabled) tournament.prizeRunnerUpAmount else 0.0,
                winnerNote = "Equipo subcampeón del torneo"
            ),
            PrizeItem(
                id = "top_scorer",
                name = "Máximo Goleador (Pichichi)",
                enabled = tournament.prizeTopScorerEnabled,
                amount = if (tournament.prizeTopScorerEnabled) tournament.prizeTopScorerAmount else 0.0,
                winnerNote = "Aplica a cualquier jugador registrado aunque su equipo haya sido eliminado"
            ),
            PrizeItem(
                id = "top_assister",
                name = "Máximo Asistidor",
                enabled = tournament.prizeTopAssisterEnabled,
                amount = if (tournament.prizeTopAssisterEnabled) tournament.prizeTopAssisterAmount else 0.0,
                winnerNote = "Aplica a cualquier jugador registrado aunque su equipo haya sido eliminado"
            ),
            PrizeItem(
                id = "best_gk",
                name = "Mejor Portero (Valla Menos Vencida)",
                enabled = tournament.prizeBestGkEnabled,
                amount = if (tournament.prizeBestGkEnabled) tournament.prizeBestGkAmount else 0.0,
                winnerNote = "Regla de Justicia del Portero (mínimo ${tournament.minMatchesForBestGk} partidos en portería)"
            )
        )

        val totalPrizes = prizeList.sumOf { it.amount }
        val estimatedNet = estimatedRev - totalPrizes
        val actualNet = actualRev - totalPrizes

        return TournamentFinancialSummary(
            entryFee = fee,
            enrolledTeamsCount = enrolledCount,
            paidTeamsCount = paidCount,
            pendingTeamsCount = pendingCount,
            estimatedRevenue = estimatedRev,
            actualRevenue = actualRev,
            pendingRevenue = pendingRev,
            prizes = prizeList,
            totalPrizesAllocated = totalPrizes,
            estimatedNetBalance = estimatedNet,
            actualNetBalance = actualNet
        )
    }
}
