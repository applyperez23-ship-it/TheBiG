package com.example

import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.data.local.entity.TournamentTeamCrossRef
import com.example.domain.rules.PrizeCalculator
import com.example.domain.rules.StandingsCalculator
import com.example.domain.rules.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TheBiGUnitTests {

    @Test
    fun standingsCalculator_correctPointsAndTiebreaker() {
        val teamA = Team(id = 1, name = "Club Alfa", shortName = "ALF")
        val teamB = Team(id = 2, name = "Club Beta", shortName = "BET")
        val teamC = Team(id = 3, name = "Club Gamma", shortName = "GAM")

        val teams = listOf(teamA, teamB, teamC)

        // Matches:
        // Alfa 2 - 0 Beta -> Alfa 3pts (DG +2), Beta 0pts (DG -2)
        // Beta 3 - 1 Gamma -> Beta 3pts (DG 0), Gamma 0pts (DG -2)
        // Alfa 1 - 1 Gamma -> Alfa 1pt (DG +2, tot 4pts), Gamma 1pt (DG -2, tot 1pt)
        val matches = listOf(
            Match(id = 1, tournamentId = 1, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0, status = "FINISHED"),
            Match(id = 2, tournamentId = 1, homeTeamId = 2, awayTeamId = 3, homeScore = 3, awayScore = 1, status = "FINISHED"),
            Match(id = 3, tournamentId = 1, homeTeamId = 1, awayTeamId = 3, homeScore = 1, awayScore = 1, status = "FINISHED")
        )

        val standings = StandingsCalculator.calculate(teams, matches)

        assertEquals(3, standings.size)

        // 1st: Alfa (4 pts)
        assertEquals(1L, standings[0].team.id)
        assertEquals(4, standings[0].pts)
        assertEquals(1, standings[0].position)

        // 2nd: Beta (3 pts)
        assertEquals(2L, standings[1].team.id)
        assertEquals(3, standings[1].pts)
        assertEquals(2, standings[1].position)

        // 3rd: Gamma (1 pt)
        assertEquals(3L, standings[2].team.id)
        assertEquals(1, standings[2].pts)
        assertEquals(3, standings[2].position)
    }

    @Test
    fun statsCalculator_excludesOwnGoalsFromScorers() {
        val p1 = Player(id = 10, teamId = 1, name = "Delantero Estrella", dorsal = 9)
        val p2 = Player(id = 11, teamId = 1, name = "Defensa Desafortunado", dorsal = 4)
        val players = listOf(p1, p2)
        val team = Team(id = 1, name = "Club Alfa", shortName = "ALF")

        val events = listOf(
            MatchEvent(id = 1, matchId = 1, teamId = 1, playerId = 10, eventType = "GOAL", minute = 15),
            MatchEvent(id = 2, matchId = 1, teamId = 1, playerId = 10, eventType = "PENALTY_GOAL", minute = 44),
            MatchEvent(id = 3, matchId = 1, teamId = 1, playerId = 11, eventType = "OWN_GOAL", minute = 80) // Should be excluded!
        )

        val scorers = StatsCalculator.getTopScorers(events, players, listOf(team))

        assertEquals(1, scorers.size)
        assertEquals(10L, scorers[0].player.id)
        assertEquals(2, scorers[0].goals)
    }

    @Test
    fun statsCalculator_goalkeeperFairRuleThreshold() {
        val gk1 = Player(id = 20, teamId = 1, name = "Portero Titular", dorsal = 1, isGoalkeeper = true)
        val gk2 = Player(id = 21, teamId = 2, name = "Portero Suplente 1 Partido", dorsal = 1, isGoalkeeper = true)
        val players = listOf(gk1, gk2)
        val teams = listOf(
            Team(id = 1, name = "Club Alfa", shortName = "ALF"),
            Team(id = 2, name = "Club Beta", shortName = "BET")
        )

        val matches = listOf(
            Match(id = 1, tournamentId = 1, homeTeamId = 1, awayTeamId = 2, homeScore = 1, awayScore = 0, status = "FINISHED", homeGoalkeeperPlayerId = 20, awayGoalkeeperPlayerId = 21),
            Match(id = 2, tournamentId = 1, homeTeamId = 1, awayTeamId = 2, homeScore = 0, awayScore = 0, status = "FINISHED", homeGoalkeeperPlayerId = 20, awayGoalkeeperPlayerId = 99)
        )

        val rankings = StatsCalculator.getGoalkeeperRankings(matches, players, teams, minMatchesThreshold = 2)

        // gk1 played 2 matches and conceded 0 -> coefficient 0.0 -> qualifies!
        val statGk1 = rankings.first { it.player.id == 20L }
        assertTrue(statGk1.qualifiesForAward)
        assertEquals(2, statGk1.matchesPlayedAsGk)
        assertEquals(0, statGk1.goalsConceded)

        // gk2 only played 1 match (less than 2) -> does NOT qualify for award
        val statGk2 = rankings.first { it.player.id == 21L }
        assertFalse(statGk2.qualifiesForAward)
    }

    @Test
    fun prizeCalculator_calculatesEstimatedAndActualRevenues() {
        val tournament = Tournament(
            id = 1,
            name = "Copa Oro",
            entryFee = 50.0,
            prizeChampionEnabled = true,
            prizeChampionAmount = 150.0,
            prizeRunnerUpEnabled = true,
            prizeRunnerUpAmount = 50.0
        )

        val crossRefs = listOf(
            TournamentTeamCrossRef(tournamentId = 1, teamId = 101, feePaid = true),
            TournamentTeamCrossRef(tournamentId = 1, teamId = 102, feePaid = true),
            TournamentTeamCrossRef(tournamentId = 1, teamId = 103, feePaid = false),
            TournamentTeamCrossRef(tournamentId = 1, teamId = 104, feePaid = false)
        )

        val summary = PrizeCalculator.calculate(tournament, crossRefs)

        assertEquals(4, summary.enrolledTeamsCount)
        assertEquals(2, summary.paidTeamsCount)
        assertEquals(2, summary.pendingTeamsCount)
        assertEquals(200.0, summary.estimatedRevenue, 0.01)
        assertEquals(100.0, summary.actualRevenue, 0.01)
        assertEquals(200.0, summary.totalPrizesAllocated, 0.01)
        assertEquals(0.0, summary.estimatedNetBalance, 0.01)
        assertEquals(-100.0, summary.actualNetBalance, 0.01)
    }

    @Test
    fun smartFinancials_calculatesPlayerAndTeamFees() {
        val tournament = Tournament(
            id = 2,
            name = "Torneo Futbol 7",
            entryFee = 70.0,
            playersPerTeam = 7,
            matchDurationMinutes = 20
        )

        assertEquals(10.0, tournament.feePerPlayer, 0.001)
        assertEquals(7, tournament.playersPerTeam)
        assertEquals(20, tournament.matchDurationMinutes)
    }
}
