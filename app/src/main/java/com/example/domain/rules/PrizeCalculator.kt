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

enum class PrizeCategory(val id: String, val label: String) {
    CHAMPION("champion", "Campeón"),
    RUNNER_UP("runner_up", "Subcampeón"),
    TOP_SCORER("top_scorer", "Goleador"),
    BEST_GK("best_gk", "Mejor Portero")
}

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

        val feePerTeam = tournament.totalTeamFee
        val feePerPlayer = tournament.feePerPlayer

        val estimatedRev = roundToTen(enrolledCount * feePerTeam)
        val actualRev = roundToTen(paidCount * feePerTeam)
        val pendingRev = roundToTen(pendingCount * feePerTeam)

        // Group players by team to automate roster-based calculations
        val playersByTeam = players.groupBy { it.teamId }
        val teamQuotasMap = crossRefs.associate { cr ->
            val teamPlayers = playersByTeam[cr.teamId] ?: emptyList()
            val rCount = teamPlayers.size
            val effectivePerPlayer = if (tournament.financialModel == "BY_PLAYER") {
                tournament.entryFee
            } else if (rCount > 0) {
                feePerTeam / rCount
            } else if (tournament.playersPerTeam > 0) {
                feePerTeam / tournament.playersPerTeam
            } else {
                0.0
            }
            cr.teamId to TeamQuotaInfo(
                teamId = cr.teamId,
                rosterCount = rCount,
                feePerPlayer = roundToTen(effectivePerPlayer),
                totalTeamFee = roundToTen(feePerTeam),
                feePaid = cr.feePaid
            )
        }

        val totalRoster = players.count { p -> crossRefs.any { it.teamId == p.teamId } }
        val avgFeePerPlayer = if (tournament.financialModel == "BY_PLAYER") {
            roundToTen(tournament.entryFee)
        } else if (totalRoster > 0) {
            roundToTen(estimatedRev / totalRoster)
        } else if (tournament.playersPerTeam > 0) {
            roundToTen(feePerTeam / tournament.playersPerTeam)
        } else {
            0.0
        }

        // Sanctions Pool calculation
        val yellowCount = events.count { it.eventType == "YELLOW_CARD" }
        val redCount = events.count { it.eventType == "RED_CARD" }
        val yellowFineTotal = roundToTen(yellowCount * tournament.yellowCardFine)
        val redFineTotal = roundToTen(redCount * tournament.redCardFine)
        val sanctionsPoolTotal = roundToTen(yellowFineTotal + redFineTotal)

        val mvpPrize = if (tournament.prizeMvpEnabled) {
            if (tournament.prizeMvpAmount > 0) roundToTen(tournament.prizeMvpAmount) else sanctionsPoolTotal
        } else 0.0

        val prizeList = listOf(
            PrizeItem(
                id = "champion",
                name = "Campeón",
                enabled = tournament.prizeChampionEnabled,
                amount = if (tournament.prizeChampionEnabled) roundToTen(tournament.prizeChampionAmount) else 0.0,
                winnerNote = "Equipo campeón del torneo"
            ),
            PrizeItem(
                id = "runner_up",
                name = "Subcampeón",
                enabled = tournament.prizeRunnerUpEnabled,
                amount = if (tournament.prizeRunnerUpEnabled) roundToTen(tournament.prizeRunnerUpAmount) else 0.0,
                winnerNote = "Equipo subcampeón del torneo"
            ),
            PrizeItem(
                id = "top_scorer",
                name = "Máximo Goleador (Pichichi)",
                enabled = tournament.prizeTopScorerEnabled,
                amount = if (tournament.prizeTopScorerEnabled) roundToTen(tournament.prizeTopScorerAmount) else 0.0,
                winnerNote = "Aplica a cualquier jugador registrado aunque su equipo haya sido eliminado"
            ),
            PrizeItem(
                id = "top_assister",
                name = "Máximo Asistidor",
                enabled = tournament.prizeTopAssisterEnabled,
                amount = if (tournament.prizeTopAssisterEnabled) roundToTen(tournament.prizeTopAssisterAmount) else 0.0,
                winnerNote = "Aplica a cualquier jugador registrado aunque su equipo haya sido eliminado"
            ),
            PrizeItem(
                id = "best_gk",
                name = "Mejor Portero (Valla Menos Vencida)",
                enabled = tournament.prizeBestGkEnabled,
                amount = if (tournament.prizeBestGkEnabled) roundToTen(tournament.prizeBestGkAmount) else 0.0,
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

        val totalPrizes = roundToTen(prizeList.sumOf { it.amount })
        val estimatedNet = roundToTen(estimatedRev + sanctionsPoolTotal - totalPrizes)
        val actualNet = roundToTen(actualRev + sanctionsPoolTotal - totalPrizes)

        return TournamentFinancialSummary(
            entryFee = roundToTen(tournament.entryFee),
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

    /**
     * Strictly rounds monetary amounts to nearest multiples of 10 (e.g. 2004.24 -> 2000.0)
     */
    fun roundToTen(amount: Double): Double {
        return (Math.round(amount / 10.0) * 10.0)
    }

    /**
     * Rebalances percentages dynamically enforcing hierarchy:
     * Campeón >= Subcampeón >= Resto.
     * When adjusting secondary categories (Goleador, Mejor Portero), Campeón and Subcampeón
     * remain fixed and are NOT altered.
     */
    fun rebalanceHierarchicalPercentages(
        changedCategory: PrizeCategory,
        newValue: Float,
        currentMap: Map<PrizeCategory, Float>
    ): Map<PrizeCategory, Float> {
        val result = currentMap.toMutableMap()
        val clampedVal = newValue.coerceIn(0f, 100f)

        when (changedCategory) {
            PrizeCategory.CHAMPION -> {
                result[PrizeCategory.CHAMPION] = clampedVal
                // Ensure Subcampeón does not exceed Campeón
                val currentRu = result[PrizeCategory.RUNNER_UP] ?: 25f
                if (currentRu > clampedVal) {
                    result[PrizeCategory.RUNNER_UP] = clampedVal
                }
                // Distribute remaining budget among others
                val locked = (result[PrizeCategory.CHAMPION] ?: 0f) + (result[PrizeCategory.RUNNER_UP] ?: 0f)
                val remaining = (100f - locked).coerceAtLeast(0f)
                val secondaries = listOf(PrizeCategory.TOP_SCORER, PrizeCategory.BEST_GK)
                val secSum = secondaries.sumOf { (result[it] ?: 0f).toDouble() }.toFloat()
                if (secSum > 0f) {
                    secondaries.forEach {
                        val ratio = (result[it] ?: 0f) / secSum
                        result[it] = (remaining * ratio)
                    }
                } else {
                    val half = remaining / 2f
                    secondaries.forEach { result[it] = half }
                }
            }
            PrizeCategory.RUNNER_UP -> {
                val champVal = result[PrizeCategory.CHAMPION] ?: 50f
                // Subcampeón cannot exceed Campeón
                val ruVal = clampedVal.coerceAtMost(champVal)
                result[PrizeCategory.RUNNER_UP] = ruVal
                val locked = champVal + ruVal
                val remaining = (100f - locked).coerceAtLeast(0f)
                val secondaries = listOf(PrizeCategory.TOP_SCORER, PrizeCategory.BEST_GK)
                val secSum = secondaries.sumOf { (result[it] ?: 0f).toDouble() }.toFloat()
                if (secSum > 0f) {
                    secondaries.forEach {
                        val ratio = (result[it] ?: 0f) / secSum
                        result[it] = (remaining * ratio)
                    }
                } else {
                    val half = remaining / 2f
                    secondaries.forEach { result[it] = half }
                }
            }
            PrizeCategory.TOP_SCORER, PrizeCategory.BEST_GK -> {
                // Secondary slider: Campeón and Subcampeón remain locked!
                val champVal = result[PrizeCategory.CHAMPION] ?: 50f
                val ruVal = result[PrizeCategory.RUNNER_UP] ?: 25f
                val budgetForSecondaries = (100f - (champVal + ruVal)).coerceAtLeast(0f)
                val otherSecondary = if (changedCategory == PrizeCategory.TOP_SCORER) PrizeCategory.BEST_GK else PrizeCategory.TOP_SCORER

                // Clamped so secondary cannot exceed Subcampeón or available budget
                val finalChanged = clampedVal.coerceIn(0f, budgetForSecondaries.coerceAtMost(ruVal))
                result[changedCategory] = finalChanged
                result[otherSecondary] = (budgetForSecondaries - finalChanged).coerceAtLeast(0f)
            }
        }

        // Final normalization to ensure strict 100.0% sum
        val sum = result.values.sum()
        if (Math.abs(sum - 100f) > 0.01f) {
            val lastKey = PrizeCategory.BEST_GK
            val others = result.keys.filter { it != lastKey }.sumOf { (result[it] ?: 0f).toDouble() }.toFloat()
            result[lastKey] = (100f - others).coerceAtLeast(0f)
        }

        return result
    }

    fun rebalancePercentages(
        changedCategory: PrizeCategory,
        newValue: Float,
        currentMap: Map<PrizeCategory, Float>
    ): Map<PrizeCategory, Float> {
        return rebalanceHierarchicalPercentages(changedCategory, newValue, currentMap)
    }
}
