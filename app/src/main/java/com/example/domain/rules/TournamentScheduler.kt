package com.example.domain.rules

import com.example.data.local.entity.Match
import com.example.data.local.entity.Team
import com.example.data.local.entity.TournamentModes
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

object TournamentScheduler {

    /**
     * Generates a complete mathematical fixture based on the tournament type:
     * 1. LEAGUE (Todos contra Todos - Algoritmo Berger / Round-Robin)
     * 2. CUP (Eliminatoria Directa / Knockout)
     * 3. GROUPS_CUP (Fase de Grupos + Playoffs - Top 2 clasifican)
     * 4. MIXED (Liga + Eliminatoria)
     */
    fun generateSchedule(
        tournamentId: Long,
        tournamentType: String,
        teams: List<Team>,
        homeAndAway: Boolean = false // Ida y Vuelta
    ): List<Match> {
        if (teams.size < 2) return emptyList()

        return when (tournamentType) {
            TournamentModes.LEAGUE -> generateLeagueSchedule(tournamentId, teams, homeAndAway)
            TournamentModes.CUP -> generateCupSchedule(tournamentId, teams)
            TournamentModes.GROUPS_CUP -> generateGroupsAndCupSchedule(tournamentId, teams)
            TournamentModes.MIXED -> generateMixedSchedule(tournamentId, teams)
            else -> generateLeagueSchedule(tournamentId, teams, homeAndAway)
        }
    }

    /**
     * Generates exact Round-Robin schedule using Berger Tables.
     * Handles odd number of teams by inserting a bye round (descanso).
     */
    fun generateLeagueSchedule(
        tournamentId: Long,
        teams: List<Team>,
        homeAndAway: Boolean = false
    ): List<Match> {
        val n = teams.size
        val hasDummy = n % 2 != 0
        val totalTeams = if (hasDummy) n + 1 else n
        val rounds = totalTeams - 1
        val matchesPerRound = totalTeams / 2

        // List of team IDs, using -1L for dummy BYE team
        val teamIds = teams.map { it.id }.toMutableList()
        if (hasDummy) {
            teamIds.add(-1L) // Dummy Bye
        }

        val firstLegMatches = mutableListOf<Match>()

        for (round in 0 until rounds) {
            val roundLabel = "Jornada ${round + 1}"
            for (m in 0 until matchesPerRound) {
                val homeIdx = (round + m) % (totalTeams - 1)
                var awayIdx = (totalTeams - 1 - m + round) % (totalTeams - 1)
                if (m == 0) {
                    awayIdx = totalTeams - 1
                }

                val homeId = teamIds[homeIdx]
                val awayId = teamIds[awayIdx]

                // Skip dummy BYE matches
                if (homeId != -1L && awayId != -1L) {
                    val isEvenRound = round % 2 == 0
                    val (finalHome, finalAway) = if (isEvenRound && m == 0) Pair(awayId, homeId) else Pair(homeId, awayId)

                    firstLegMatches.add(
                        Match(
                            tournamentId = tournamentId,
                            homeTeamId = finalHome,
                            awayTeamId = finalAway,
                            roundName = roundLabel,
                            status = "SCHEDULED"
                        )
                    )
                }
            }
        }

        if (!homeAndAway) {
            return firstLegMatches
        }

        // Second Leg (Vuelta) with reversed home and away
        val secondLegMatches = firstLegMatches.map { match ->
            val roundNum = match.roundName.filter { it.isDigit() }.toIntOrNull() ?: 1
            val secondLegRound = "Jornada ${roundNum + rounds} (Vuelta)"
            match.copy(
                id = 0,
                homeTeamId = match.awayTeamId,
                awayTeamId = match.homeTeamId,
                roundName = secondLegRound,
                status = "SCHEDULED"
            )
        }

        return firstLegMatches + secondLegMatches
    }

    /**
     * Generates single-elimination Cup schedule.
     * Rounds: Octavos de Final, Cuartos de Final, Semifinales, Gran Final.
     */
    fun generateCupSchedule(
        tournamentId: Long,
        teams: List<Team>
    ): List<Match> {
        val matches = mutableListOf<Match>()
        val count = teams.size

        // Determine initial round name
        val roundName = when {
            count > 8 -> "Octavos de Final"
            count > 4 -> "Cuartos de Final"
            count > 2 -> "Semifinal"
            else -> "Gran Final"
        }

        // Pair teams for the first round
        for (i in 0 until count step 2) {
            if (i + 1 < count) {
                matches.add(
                    Match(
                        tournamentId = tournamentId,
                        homeTeamId = teams[i].id,
                        awayTeamId = teams[i + 1].id,
                        roundName = roundName,
                        status = "SCHEDULED"
                    )
                )
            }
        }

        return matches
    }

    /**
     * Generates Hybrid Groups + Knockout:
     * - Teams are distributed into Group A and Group B.
     * - Round-robin played within each group.
     * - Top 2 from each group will advance to Semifinals (A1 vs B2, B1 vs A2).
     */
    fun generateGroupsAndCupSchedule(
        tournamentId: Long,
        teams: List<Team>
    ): List<Match> {
        val groupA = mutableListOf<Team>()
        val groupB = mutableListOf<Team>()

        teams.forEachIndexed { index, team ->
            if (index % 2 == 0) groupA.add(team) else groupB.add(team)
        }

        val matches = mutableListOf<Match>()

        // Group A fixtures
        val fixturesA = generateLeagueSchedule(tournamentId, groupA, false).map {
            it.copy(roundName = "Grupo A - ${it.roundName}")
        }
        // Group B fixtures
        val fixturesB = generateLeagueSchedule(tournamentId, groupB, false).map {
            it.copy(roundName = "Grupo B - ${it.roundName}")
        }

        matches.addAll(fixturesA)
        matches.addAll(fixturesB)

        return matches
    }

    /**
     * Mixed mode: Full League + Playoffs.
     */
    fun generateMixedSchedule(
        tournamentId: Long,
        teams: List<Team>
    ): List<Match> {
        val leagueFixtures = generateLeagueSchedule(tournamentId, teams, false).map {
            it.copy(roundName = "Fase Regular - ${it.roundName}")
        }
        return leagueFixtures
    }

    /**
     * Dynamically advances winners from a finished knockout round into the next round.
     * E.g., Semifinal 1 winner vs Semifinal 2 winner -> Gran Final.
     */
    fun computeNextKnockoutRound(
        tournamentId: Long,
        finishedRoundMatches: List<Match>,
        currentRoundName: String
    ): List<Match> {
        val winners = finishedRoundMatches.mapNotNull { m ->
            when {
                m.homeScore > m.awayScore -> m.homeTeamId
                m.awayScore > m.homeScore -> m.awayTeamId
                (m.homePenalties ?: 0) > (m.awayPenalties ?: 0) -> m.homeTeamId
                (m.awayPenalties ?: 0) > (m.homePenalties ?: 0) -> m.awayTeamId
                else -> m.homeTeamId // Fallback
            }
        }

        if (winners.size < 2) return emptyList()

        val nextRoundName = when (currentRoundName) {
            "Octavos de Final" -> "Cuartos de Final"
            "Cuartos de Final" -> "Semifinal"
            "Semifinal" -> "Gran Final"
            else -> "Ronda Siguiente"
        }

        val nextMatches = mutableListOf<Match>()
        for (i in 0 until winners.size step 2) {
            if (i + 1 < winners.size) {
                nextMatches.add(
                    Match(
                        tournamentId = tournamentId,
                        homeTeamId = winners[i],
                        awayTeamId = winners[i + 1],
                        roundName = nextRoundName,
                        status = "SCHEDULED"
                    )
                )
            }
        }
        return nextMatches
    }
}
