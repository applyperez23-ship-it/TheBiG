package com.example.domain.rules

import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Tournament
import com.example.data.local.entity.TournamentTeamCrossRef

data class TeamQuotaInfo(
    val teamId: Long,
    val rosterCount: Int,
    val feePerPlayer: Double,
    val totalTeamFee: Double,
    val feePaid: Boolean
)

data class TournamentFinancialSummary(
    val entryFee: Double,
    val feePerPlayer: Double,
    val enrolledTeamsCount: Int,
    val paidTeamsCount: Int,
    val pendingTeamsCount: Int,
    val totalRosterPlayers: Int,
    val averageFeePerPlayer: Double,
    val teamQuotas: Map<Long, TeamQuotaInfo>,
    val estimatedRevenue: Double,
    val actualRevenue: Double,
    val pendingRevenue: Double,
    val yellowCardsCount: Int = 0,
    val redCardsCount: Int = 0,
    val yellowCardFineTotal: Double = 0.0,
    val redCardFineTotal: Double = 0.0,
    val totalSanctionsPool: Double = 0.0,
    val mvpPrizeFundedBySanctions: Double = 0.0,
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
        crossRefs: List<TournamentTeamCrossRef>,
        events: List<MatchEvent> = emptyList(),
        players: List<Player> = emptyList()
    ): TournamentFinancialSummary {
        val enrolledCount = crossRefs.size
        val paidCount = crossRefs.count { it.feePaid }
        val pendingCount = enrolledCount - paidCount

        val fee = tournament.entryFee
        val estimatedRev = enrolledCount * fee
        val actualRev = paidCount * fee
        val pendingRev = pendingCount * fee

        // Group players by team to automate roster-based calculations
        val playersByTeam = players.groupBy { it.teamId }
        val teamQuotasMap = crossRefs.associate { cr ->
            val teamPlayers = playersByTeam[cr.teamId] ?: emptyList()
            val rCount = teamPlayers.size
            val effectivePerPlayer = if (rCount > 0) {
                fee / rCount
            } else if (tournament.playersPerTeam > 0) {
                fee / tournament.playersPerTeam
            } else {
                0.0
            }
            cr.teamId to TeamQuotaInfo(
                teamId = cr.teamId,
                rosterCount = rCount,
                feePerPlayer = effectivePerPlayer,
                totalTeamFee = fee,
                feePaid = cr.feePaid
            )
        }

        val totalRoster = players.count { p -> crossRefs.any { it.teamId == p.teamId } }
        val avgFeePerPlayer = if (totalRoster > 0) {
            estimatedRev / totalRoster
        } else if (tournament.playersPerTeam > 0) {
            fee / tournament.playersPerTeam
        } else {
            0.0
        }

        // Sanctions Pool calculation
        val yellowCount = events.count { it.eventType == "YELLOW_CARD" }
        val redCount = events.count { it.eventType == "RED_CARD" }
        val yellowFineTotal = yellowCount * tournament.yellowCardFine
        val redFineTotal = redCount * tournament.redCardFine
        val sanctionsPoolTotal = yellowFineTotal + redFineTotal

        val mvpPrize = if (tournament.prizeMvpEnabled) {
            if (tournament.prizeMvpAmount > 0) tournament.prizeMvpAmount else sanctionsPoolTotal
        } else 0.0

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
            ),
            PrizeItem(
                id = "mvp",
                name = "Premio Económico MVP",
                enabled = tournament.prizeMvpEnabled,
                amount = mvpPrize,
                winnerNote = "Financiado por el Pozo de Sanciones acumuladas por Tarjetas Amarillas y Rojas ($${sanctionsPoolTotal.toInt()})"
            )
        )

        val totalPrizes = prizeList.sumOf { it.amount }
        val estimatedNet = estimatedRev + sanctionsPoolTotal - totalPrizes
        val actualNet = actualRev + sanctionsPoolTotal - totalPrizes

        return TournamentFinancialSummary(
            entryFee = fee,
            feePerPlayer = avgFeePerPlayer,
            enrolledTeamsCount = enrolledCount,
            paidTeamsCount = paidCount,
            pendingTeamsCount = pendingCount,
            totalRosterPlayers = totalRoster,
            averageFeePerPlayer = avgFeePerPlayer,
            teamQuotas = teamQuotasMap,
            estimatedRevenue = estimatedRev,
            actualRevenue = actualRev,
            pendingRevenue = pendingRev,
            yellowCardsCount = yellowCount,
            redCardsCount = redCount,
            yellowCardFineTotal = yellowFineTotal,
            redCardFineTotal = redFineTotal,
            totalSanctionsPool = sanctionsPoolTotal,
            mvpPrizeFundedBySanctions = mvpPrize,
            prizes = prizeList,
            totalPrizesAllocated = totalPrizes,
            estimatedNetBalance = estimatedNet,
            actualNetBalance = actualNet
        )
    }
}
