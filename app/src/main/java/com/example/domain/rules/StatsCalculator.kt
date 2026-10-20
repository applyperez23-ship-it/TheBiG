package com.example.domain.rules

import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team

data class ScorerStat(
    val player: Player,
    val team: Team?,
    val goals: Int
)

data class AssisterStat(
    val player: Player,
    val team: Team?,
    val assists: Int
)

data class CardStat(
    val player: Player,
    val team: Team?,
    val yellowCards: Int,
    val redCards: Int,
    val totalPoints: Int // red = 3, yellow = 1
)

data class MvpStat(
    val player: Player,
    val team: Team?,
    val mvpAwards: Int
)

data class GoalkeeperStat(
    val player: Player,
    val team: Team?,
    val matchesPlayedAsGk: Int,
    val goalsConceded: Int,
    val coefficient: Double,
    val qualifiesForAward: Boolean
)

object StatsCalculator {
    /**
     * Calculates top goal scorers.
     * Rule: Excludes OWN_GOAL (autogoles).
     */
    fun getTopScorers(
        events: List<MatchEvent>,
        players: List<Player>,
        teams: List<Team>
    ): List<ScorerStat> {
        val playerMap = players.associateBy { it.id }
        val teamMap = teams.associateBy { it.id }

        val goalEvents = events.filter {
            (it.eventType == "GOAL" || it.eventType == "PENALTY_GOAL") && it.playerId != null
        }

        val counts = mutableMapOf<Long, Int>()
        for (event in goalEvents) {
            val pid = event.playerId ?: continue
            counts[pid] = (counts[pid] ?: 0) + 1
        }

        return counts.entries
            .mapNotNull { (pid, count) ->
                val player = playerMap[pid] ?: return@mapNotNull null
                val team = teamMap[player.teamId]
                ScorerStat(player = player, team = team, goals = count)
            }
            .sortedByDescending { it.goals }
    }

    /**
     * Calculates top assists.
     */
    fun getTopAssisters(
        events: List<MatchEvent>,
        players: List<Player>,
        teams: List<Team>
    ): List<AssisterStat> {
        val playerMap = players.associateBy { it.id }
        val teamMap = teams.associateBy { it.id }

        val assistCounts = mutableMapOf<Long, Int>()
        for (event in events) {
            val aid = event.assistPlayerId ?: continue
            assistCounts[aid] = (assistCounts[aid] ?: 0) + 1
        }

        return assistCounts.entries
            .mapNotNull { (aid, count) ->
                val player = playerMap[aid] ?: return@mapNotNull null
                val team = teamMap[player.teamId]
                AssisterStat(player = player, team = team, assists = count)
            }
            .sortedByDescending { it.assists }
    }

    /**
     * Calculates top carded players (Tarjetas Amarillas / Rojas).
     */
    fun getTopCards(
        events: List<MatchEvent>,
        players: List<Player>,
        teams: List<Team>
    ): List<CardStat> {
        val playerMap = players.associateBy { it.id }
        val teamMap = teams.associateBy { it.id }

        val yellowMap = mutableMapOf<Long, Int>()
        val redMap = mutableMapOf<Long, Int>()

        for (event in events) {
            val pid = event.playerId ?: continue
            when (event.eventType) {
                "YELLOW_CARD" -> yellowMap[pid] = (yellowMap[pid] ?: 0) + 1
                "RED_CARD" -> redMap[pid] = (redMap[pid] ?: 0) + 1
            }
        }

        val allPlayerIds = (yellowMap.keys + redMap.keys).toSet()

        return allPlayerIds.mapNotNull { pid ->
            val player = playerMap[pid] ?: return@mapNotNull null
            val team = teamMap[player.teamId]
            val yellows = yellowMap[pid] ?: 0
            val reds = redMap[pid] ?: 0
            val points = (reds * 3) + yellows
            CardStat(
                player = player,
                team = team,
                yellowCards = yellows,
                redCards = reds,
                totalPoints = points
            )
        }.sortedWith(compareByDescending<CardStat> { it.totalPoints }
            .thenByDescending { it.redCards }
            .thenByDescending { it.yellowCards })
    }

    /**
     * Calculates top MVPs (Jugador del Partido).
     */
    fun getTopMvps(
        events: List<MatchEvent>,
        players: List<Player>,
        teams: List<Team>
    ): List<MvpStat> {
        val playerMap = players.associateBy { it.id }
        val teamMap = teams.associateBy { it.id }

        val mvpEvents = events.filter { it.eventType == "MVP" && it.playerId != null }
        val counts = mutableMapOf<Long, Int>()
        for (event in mvpEvents) {
            val pid = event.playerId ?: continue
            counts[pid] = (counts[pid] ?: 0) + 1
        }

        return counts.entries.mapNotNull { (pid, count) ->
            val player = playerMap[pid] ?: return@mapNotNull null
            val team = teamMap[player.teamId]
            MvpStat(player = player, team = team, mvpAwards = count)
        }.sortedByDescending { it.mvpAwards }
    }

    /**
     * "GoalkeeperFairRule" (Regla de Justicia del Portero):
     * Calculates coefficient of goals conceded per match played as goalkeeper.
     * Evaluates against minimum matches requirement to prevent unrepresented or single-game keepers from winning unfairly.
     */
    fun getGoalkeeperRankings(
        matches: List<Match>,
        players: List<Player>,
        teams: List<Team>,
        minMatchesThreshold: Int
    ): List<GoalkeeperStat> {
        val playerMap = players.associateBy { it.id }
        val teamMap = teams.associateBy { it.id }

        val finishedMatches = matches.filter { it.status == "FINISHED" }

        // GK ID -> Pair(MatchesInGoal, GoalsConceded)
        val gkMatches = mutableMapOf<Long, Int>()
        val gkConceded = mutableMapOf<Long, Int>()

        for (m in finishedMatches) {
            val homeGkId = m.homeGoalkeeperPlayerId
            if (homeGkId != null) {
                gkMatches[homeGkId] = (gkMatches[homeGkId] ?: 0) + 1
                gkConceded[homeGkId] = (gkConceded[homeGkId] ?: 0) + m.awayScore
            }
            val awayGkId = m.awayGoalkeeperPlayerId
            if (awayGkId != null) {
                gkMatches[awayGkId] = (gkMatches[awayGkId] ?: 0) + 1
                gkConceded[awayGkId] = (gkConceded[awayGkId] ?: 0) + m.homeScore
            }
        }

        // If no explicit GK was selected on matches, fall back to registered goalkeepers in teams
        val allGoalkeepers = players.filter { it.isGoalkeeper }
        val allGkIds = (gkMatches.keys + allGoalkeepers.map { it.id }).toSet()

        val results = mutableListOf<GoalkeeperStat>()

        for (gkId in allGkIds) {
            val player = playerMap[gkId] ?: continue
            val team = teamMap[player.teamId]
            val matchesAsGk = gkMatches[gkId] ?: player.matchesAsGoalkeeper
            val conceded = gkConceded[gkId] ?: player.goalsConcededAsGk

            val coefficient = if (matchesAsGk > 0) conceded.toDouble() / matchesAsGk.toDouble() else 999.0
            val qualifies = matchesAsGk >= minMatchesThreshold

            results.add(
                GoalkeeperStat(
                    player = player,
                    team = team,
                    matchesPlayedAsGk = matchesAsGk,
                    goalsConceded = conceded,
                    coefficient = coefficient,
                    qualifiesForAward = qualifies
                )
            )
        }

        // Sort: Qualifying first, then lowest coefficient (less conceded per match), then least goals conceded
        return results.sortedWith { a, b ->
            when {
                a.qualifiesForAward && !b.qualifiesForAward -> -1
                !a.qualifiesForAward && b.qualifiesForAward -> 1
                a.coefficient != b.coefficient -> a.coefficient.compareTo(b.coefficient)
                else -> a.goalsConceded.compareTo(b.goalsConceded)
            }
        }
    }
}
