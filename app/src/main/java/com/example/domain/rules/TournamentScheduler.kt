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
    /**
     * Generates a complete mathematical fixture based on tournament type:
     * 1. LEAGUE (Todos contra Todos - Algoritmo Berger / Round-Robin)
     * 2. CUP (Eliminatoria Directa / Knockout basado en potencias de 2)
     * 3. GROUPS_CUP (Fase de Grupos + Repechajes / Playoffs)
     * 4. MIXED (Fase regular de liga + Eliminatoria directa)
     */
    fun generateSchedule(
        tournamentId: Long,
        tournamentType: String,
        teams: List<Team>,
        homeAndAway: Boolean = false,
        startHour: Int = 9,
        startMinute: Int = 0,
        matchesPerDay: Int = 4,
        matchDurationMinutes: Int = 20,
        breakMinutes: Int = 1
    ): List<Match> {
        if (teams.size < 2) return emptyList()

        val rawMatches = when (tournamentType) {
            TournamentModes.LEAGUE -> generateLeagueSchedule(tournamentId, teams, homeAndAway)
            TournamentModes.CUP -> generateCupSchedule(tournamentId, teams)
            TournamentModes.GROUPS_CUP -> generateGroupsAndCupSchedule(tournamentId, teams)
            TournamentModes.MIXED -> generateMixedSchedule(tournamentId, teams)
            else -> generateLeagueSchedule(tournamentId, teams, homeAndAway)
        }

        return applyAutoScheduleTimestamps(
            matches = rawMatches,
            startHour = startHour,
            startMinute = startMinute,
            matchesPerDay = matchesPerDay,
            matchDurationMinutes = matchDurationMinutes,
            breakMinutes = breakMinutes
        )
    }

    /**
     * Automatically calculates and assigns match dates and times:
     * - Configurable matches per day
     * - Match duration (e.g. 20 min)
     * - Rest time between matches (e.g. 1 min)
     */
    fun applyAutoScheduleTimestamps(
        matches: List<Match>,
        startHour: Int = 9,
        startMinute: Int = 0,
        matchesPerDay: Int = 4,
        matchDurationMinutes: Int = 20,
        breakMinutes: Int = 1
    ): List<Match> {
        val totalSlotMinutes = (matchDurationMinutes + breakMinutes).coerceAtLeast(5)
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, startHour)
            set(java.util.Calendar.MINUTE, startMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        var matchesScheduledToday = 0

        return matches.mapIndexed { index, match ->
            if (matchesScheduledToday >= matchesPerDay) {
                // Advance to next day at initial start hour
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, startHour)
                calendar.set(java.util.Calendar.MINUTE, startMinute)
                matchesScheduledToday = 0
            }

            val timestamp = calendar.timeInMillis
            // Advance clock for next match in the same day
            calendar.add(java.util.Calendar.MINUTE, totalSlotMinutes)
            matchesScheduledToday++

            match.copy(scheduledTimestamp = timestamp)
        }
    }

    /**
     * Generates exact Round-Robin schedule using Berger Tables.
     * If rounds > 3, intelligently shuffles pairings between rounds for dynamic balance.
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

        val teamList = if (rounds > 3) teams.shuffled(java.util.Random(tournamentId + 42)) else teams
        val teamIds = teamList.map { it.id }.toMutableList()
        if (hasDummy) {
            teamIds.add(-1L) // BYE Dummy
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

        // Second Leg (Vuelta)
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
     * Generates single-elimination Cup schedule (potencias de 2).
     */
    fun generateCupSchedule(
        tournamentId: Long,
        teams: List<Team>
    ): List<Match> {
        val matches = mutableListOf<Match>()
        val count = teams.size

        val roundName = when {
            count > 8 -> "Octavos de Final"
            count > 4 -> "Cuartos de Final"
            count > 2 -> "Semifinal"
            else -> "Gran Final"
        }

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
     * Copa + PlayOffs (Repechajes):
     * - Fase previa de grupos / ligas cortas
     * - Cruces de repechaje entre 2dos y 3ros
     * - Posterior eliminación directa
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
        val fixturesA = generateLeagueSchedule(tournamentId, groupA, false).map {
            it.copy(roundName = "Grupo A - ${it.roundName}")
        }
        val fixturesB = generateLeagueSchedule(tournamentId, groupB, false).map {
            it.copy(roundName = "Grupo B - ${it.roundName}")
        }

        matches.addAll(fixturesA)
        matches.addAll(fixturesB)
        return matches
    }

    /**
     * Liga + Copa (Liga inicial con tabla general, seguida de eliminatorias).
     */
    fun generateMixedSchedule(
        tournamentId: Long,
        teams: List<Team>
    ): List<Match> {
        return generateLeagueSchedule(tournamentId, teams, false).map {
            it.copy(roundName = "Fase Regular - ${it.roundName}")
        }
    }

    /**
     * Determines whether the current phase has completed all matches and can transition.
     */
    fun canAdvanceToNextPhase(
        tournamentType: String,
        allMatches: List<Match>
    ): Boolean {
        if (allMatches.isEmpty()) return false
        val unfinished = allMatches.filter { it.status != "FINISHED" }
        if (unfinished.isNotEmpty()) return false

        return when (tournamentType) {
            TournamentModes.LEAGUE -> false // Finished after last round
            TournamentModes.CUP -> {
                // If Gran Final is finished, tournament is complete
                !allMatches.any { it.roundName == "Gran Final" }
            }
            TournamentModes.GROUPS_CUP -> {
                !allMatches.any { it.roundName == "Gran Final" }
            }
            TournamentModes.MIXED -> {
                !allMatches.any { it.roundName == "Gran Final" }
            }
            else -> false
        }
    }

    /**
     * Generates next stage matches dynamically based on current phase and standings:
     * - From Groups to Repechaje or Semifinals
     * - From League to PlayOffs
     * - From Cup rounds to next round
     */
    fun advanceToNextPhase(
        tournamentId: Long,
        tournamentType: String,
        allMatches: List<Match>,
        enrolledTeams: List<Team>
    ): List<Match> {
        return when (tournamentType) {
            TournamentModes.CUP -> {
                val currentRound = allMatches.lastOrNull()?.roundName ?: return emptyList()
                val currentRoundMatches = allMatches.filter { it.roundName == currentRound }
                computeNextKnockoutRound(tournamentId, currentRoundMatches, currentRound)
            }
            TournamentModes.GROUPS_CUP -> {
                val hasGranFinal = allMatches.any { it.roundName == "Gran Final" }
                if (hasGranFinal) return emptyList()

                val hasSemis = allMatches.any { it.roundName.startsWith("Semifinal") }
                val hasRepechaje = allMatches.any { it.roundName.startsWith("Repechaje") }

                if (!hasRepechaje && !hasSemis) {
                    // Compute standings for Group A and Group B
                    val groupAMatches = allMatches.filter { it.roundName.contains("Grupo A") }
                    val groupBMatches = allMatches.filter { it.roundName.contains("Grupo B") }
                    val standingsA = StandingsCalculator.calculate(enrolledTeams, groupAMatches).filter { s -> groupAMatches.any { it.homeTeamId == s.team.id || it.awayTeamId == s.team.id } }
                    val standingsB = StandingsCalculator.calculate(enrolledTeams, groupBMatches).filter { s -> groupBMatches.any { it.homeTeamId == s.team.id || it.awayTeamId == s.team.id } }

                    val nextMatches = mutableListOf<Match>()
                    // Repechaje: 2do Grupo A vs 3ro Grupo B, 2do Grupo B vs 3ro Grupo A
                    val a2 = standingsA.getOrNull(1)?.team?.id
                    val b3 = standingsB.getOrNull(2)?.team?.id
                    val b2 = standingsB.getOrNull(1)?.team?.id
                    val a3 = standingsA.getOrNull(2)?.team?.id

                    if (a2 != null && b3 != null) {
                        nextMatches.add(Match(tournamentId = tournamentId, homeTeamId = a2, awayTeamId = b3, roundName = "Repechaje 1 (2ºA vs 3ºB)", status = "SCHEDULED"))
                    }
                    if (b2 != null && a3 != null) {
                        nextMatches.add(Match(tournamentId = tournamentId, homeTeamId = b2, awayTeamId = a3, roundName = "Repechaje 2 (2ºB vs 3ºA)", status = "SCHEDULED"))
                    }

                    if (nextMatches.isEmpty() && standingsA.isNotEmpty() && standingsB.isNotEmpty()) {
                        // Direct to Semifinals (Top 2)
                        val a1 = standingsA.getOrNull(0)?.team?.id ?: enrolledTeams[0].id
                        val b2Team = standingsB.getOrNull(1)?.team?.id ?: enrolledTeams.getOrNull(1)?.id ?: enrolledTeams[0].id
                        val b1 = standingsB.getOrNull(0)?.team?.id ?: enrolledTeams.getOrNull(2)?.id ?: enrolledTeams[0].id
                        val a2Team = standingsA.getOrNull(1)?.team?.id ?: enrolledTeams.getOrNull(3)?.id ?: enrolledTeams[0].id

                        nextMatches.add(Match(tournamentId = tournamentId, homeTeamId = a1, awayTeamId = b2Team, roundName = "Semifinal 1 (1ºA vs 2ºB)", status = "SCHEDULED"))
                        nextMatches.add(Match(tournamentId = tournamentId, homeTeamId = b1, awayTeamId = a2Team, roundName = "Semifinal 2 (1ºB vs 2ºA)", status = "SCHEDULED"))
                    }
                    nextMatches
                } else if (hasRepechaje && !hasSemis) {
                    val repMatches = allMatches.filter { it.roundName.startsWith("Repechaje") }
                    val repWinners = repMatches.map { getMatchWinnerId(it) }

                    val groupAMatches = allMatches.filter { it.roundName.contains("Grupo A") }
                    val groupBMatches = allMatches.filter { it.roundName.contains("Grupo B") }
                    val a1 = StandingsCalculator.calculate(enrolledTeams, groupAMatches).firstOrNull()?.team?.id ?: enrolledTeams[0].id
                    val b1 = StandingsCalculator.calculate(enrolledTeams, groupBMatches).firstOrNull()?.team?.id ?: enrolledTeams.getOrNull(1)?.id ?: enrolledTeams[0].id

                    val w1 = repWinners.getOrNull(0) ?: enrolledTeams.getOrNull(2)?.id ?: enrolledTeams[0].id
                    val w2 = repWinners.getOrNull(1) ?: enrolledTeams.getOrNull(3)?.id ?: enrolledTeams[0].id

                    listOf(
                        Match(tournamentId = tournamentId, homeTeamId = a1, awayTeamId = w1, roundName = "Semifinal 1", status = "SCHEDULED"),
                        Match(tournamentId = tournamentId, homeTeamId = b1, awayTeamId = w2, roundName = "Semifinal 2", status = "SCHEDULED")
                    )
                } else if (hasSemis && !hasGranFinal) {
                    val semiMatches = allMatches.filter { it.roundName.startsWith("Semifinal") }
                    val semiWinners = semiMatches.map { getMatchWinnerId(it) }
                    if (semiWinners.size >= 2) {
                        listOf(Match(tournamentId = tournamentId, homeTeamId = semiWinners[0], awayTeamId = semiWinners[1], roundName = "Gran Final", status = "SCHEDULED"))
                    } else emptyList()
                } else emptyList()
            }
            TournamentModes.MIXED -> {
                val hasGranFinal = allMatches.any { it.roundName == "Gran Final" }
                if (hasGranFinal) return emptyList()

                val hasSemis = allMatches.any { it.roundName.startsWith("Semifinal") }
                if (!hasSemis) {
                    // Top 4 of league table advance to Semifinals
                    val standings = StandingsCalculator.calculate(enrolledTeams, allMatches)
                    val top1 = standings.getOrNull(0)?.team?.id ?: return emptyList()
                    val top2 = standings.getOrNull(1)?.team?.id ?: return emptyList()
                    val top3 = standings.getOrNull(2)?.team?.id ?: return emptyList()
                    val top4 = standings.getOrNull(3)?.team?.id ?: return emptyList()

                    listOf(
                        Match(tournamentId = tournamentId, homeTeamId = top1, awayTeamId = top4, roundName = "Semifinal 1 (1º vs 4º)", status = "SCHEDULED"),
                        Match(tournamentId = tournamentId, homeTeamId = top2, awayTeamId = top3, roundName = "Semifinal 2 (2º vs 3º)", status = "SCHEDULED")
                    )
                } else {
                    val semiMatches = allMatches.filter { it.roundName.startsWith("Semifinal") }
                    val semiWinners = semiMatches.map { getMatchWinnerId(it) }
                    if (semiWinners.size >= 2) {
                        listOf(Match(tournamentId = tournamentId, homeTeamId = semiWinners[0], awayTeamId = semiWinners[1], roundName = "Gran Final", status = "SCHEDULED"))
                    } else emptyList()
                }
            }
            else -> emptyList()
        }
    }

    fun getMatchWinnerId(m: Match): Long {
        return when {
            m.homeScore > m.awayScore -> m.homeTeamId
            m.awayScore > m.homeScore -> m.awayTeamId
            (m.homePenalties ?: 0) > (m.awayPenalties ?: 0) -> m.homeTeamId
            (m.awayPenalties ?: 0) > (m.homePenalties ?: 0) -> m.awayTeamId
            else -> m.homeTeamId
        }
    }

    /**
     * Dynamically advances winners from a finished knockout round into the next round.
     */
    fun computeNextKnockoutRound(
        tournamentId: Long,
        finishedRoundMatches: List<Match>,
        currentRoundName: String
    ): List<Match> {
        val winners = finishedRoundMatches.map { getMatchWinnerId(it) }

        if (winners.size < 2) return emptyList()

        val nextRoundName = when {
            currentRoundName.contains("Octavos") -> "Cuartos de Final"
            currentRoundName.contains("Cuartos") -> "Semifinal"
            currentRoundName.contains("Semifinal") -> "Gran Final"
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

    /**
     * If a team withdraws or is eliminated during a tournament,
     * remaining unplayed matches are automatically awarded as a 3-0 forfeit victory.
     */
    fun applyForfeitForTeam(matches: List<Match>, forfeitedTeamId: Long): List<Match> {
        return matches.map { match ->
            if (match.status != "FINISHED" && (match.homeTeamId == forfeitedTeamId || match.awayTeamId == forfeitedTeamId)) {
                val isHomeForfeited = match.homeTeamId == forfeitedTeamId
                match.copy(
                    status = "FINISHED",
                    homeScore = if (isHomeForfeited) 0 else 3,
                    awayScore = if (isHomeForfeited) 3 else 0
                )
            } else {
                match
            }
        }
    }
}
