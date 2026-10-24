package com.example.domain.rules

import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team

data class ScorerStat(
    val player: Player,
    val team: Team,
    val goals: Int
)

data class AssisterStat(
    val player: Player,
    val team: Team,
    val assists: Int
)

data class GoalkeeperStat(
    val player: Player,
    val team: Team,
    val matchesPlayed: Int,
    val goalsConceded: Int,
    val coefficient: Double
)

data class CardStat(
    val player: Player,
    val team: Team,
    val yellowCards: Int,
    val redCards: Int
)

data class MvpStat(
    val player: Player,
    val team: Team,
    val mvpCount: Int
)

object StatsCalculator {

    fun getTopScorers(
        events: List<MatchEvent>,
        players: List<Player>,
        teams: List<Team>
    ): List<ScorerStat> {
        val playerMap = players.associateBy { it.id }
        val teamMap = teams.associateBy { it.id }

        val goalCounts = mutableMapOf<Long, Int>()
        for (e in events) {
            if ((e.eventType == "GOAL" || e.eventType == "PENALTY_GOAL") && e.playerId != null) {
                goalCounts[e.playerId] = (goalCounts[e.playerId] ?: 0) + 1
            }
        }

        return goalCounts.mapNotNull { (playerId, count) ->
            val player = playerMap[playerId] ?: return@mapNotNull null
            val team = teamMap[player.teamId] ?: return@mapNotNull null
            ScorerStat(player = player, team = team, goals = count)
        }.sortedByDescending { it.goals }
    }

    fun getTopAssisters(
        events: List<MatchEvent>,
        players: List<Player>,
        teams: List<Team>
    ): List<AssisterStat> {
        val playerMap = players.associateBy { it.id }
        val teamMap = teams.associateBy { it.id }

        val assistCounts = mutableMapOf<Long, Int>()
        for (e in events) {
            if (e.assistPlayerId != null) {
                assistCounts[e.assistPlayerId] = (assistCounts[e.assistPlayerId] ?: 0) + 1
            }
        }

        return assistCounts.mapNotNull { (playerId, count) ->
            val player = playerMap[playerId] ?: return@mapNotNull null
            val team = teamMap[player.teamId] ?: return@mapNotNull null
            AssisterStat(player = player, team = team, assists = count)
        }.sortedByDescending { it.assists }
    }

    fun getGoalkeeperRankings(
        matches: List<Match>,
        players: List<Player>,
        teams: List<Team>,
        minMatchesRequired: Int = 2
    ): List<GoalkeeperStat> {
        val teamMap = teams.associateBy { it.id }
        val goalkeepers = players.filter { it.isGoalkeeper }

        val stats = mutableListOf<GoalkeeperStat>()
        for (gk in goalkeepers) {
            val team = teamMap[gk.teamId] ?: continue
            val teamMatches = matches.filter { it.status == "FINISHED" && (it.homeTeamId == team.id || it.awayTeamId == team.id) }
            val pj = teamMatches.size
            if (pj < minMatchesRequired) continue

            var goalsConceded = 0
            for (m in teamMatches) {
                goalsConceded += if (m.homeTeamId == team.id) m.awayScore else m.homeScore
            }

            val coef = if (pj > 0) goalsConceded.toDouble() / pj.toDouble() else 0.0
            stats.add(GoalkeeperStat(player = gk, team = team, matchesPlayed = pj, goalsConceded = goalsConceded, coefficient = coef))
        }

        return stats.sortedBy { it.coefficient }
    }

    fun getTopCards(
        events: List<MatchEvent>,
        players: List<Player>,
        teams: List<Team>
    ): List<CardStat> {
        val playerMap = players.associateBy { it.id }
        val teamMap = teams.associateBy { it.id }

        val yellowMap = mutableMapOf<Long, Int>()
        val redMap = mutableMapOf<Long, Int>()

        for (e in events) {
            val pId = e.playerId ?: continue
            when (e.eventType) {
                "YELLOW_CARD" -> yellowMap[pId] = (yellowMap[pId] ?: 0) + 1
                "RED_CARD" -> redMap[pId] = (redMap[pId] ?: 0) + 1
            }
        }

        val allPlayerIds = yellowMap.keys + redMap.keys
        return allPlayerIds.mapNotNull { pId ->
            val player = playerMap[pId] ?: return@mapNotNull null
            val team = teamMap[player.teamId] ?: return@mapNotNull null
            CardStat(
                player = player,
                team = team,
                yellowCards = yellowMap[pId] ?: 0,
                redCards = redMap[pId] ?: 0
            )
        }.sortedWith(compareByDescending<CardStat> { it.redCards }.thenByDescending { it.yellowCards })
    }

    fun getTopMvps(
        events: List<MatchEvent>,
        players: List<Player>,
        teams: List<Team>
    ): List<MvpStat> {
        val playerMap = players.associateBy { it.id }
        val teamMap = teams.associateBy { it.id }

        val mvpMap = mutableMapOf<Long, Int>()
        for (e in events) {
            if (e.eventType == "MVP" && e.playerId != null) {
                mvpMap[e.playerId] = (mvpMap[e.playerId] ?: 0) + 1
            }
        }

        return mvpMap.mapNotNull { (pId, count) ->
            val player = playerMap[pId] ?: return@mapNotNull null
            val team = teamMap[player.teamId] ?: return@mapNotNull null
            MvpStat(player = player, team = team, mvpCount = count)
        }.sortedByDescending { it.mvpCount }
    }
}
