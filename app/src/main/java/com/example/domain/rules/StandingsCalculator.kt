package com.example.domain.rules

import com.example.data.local.entity.Match
import com.example.data.local.entity.Team

data class TeamStanding(
    val team: Team,
    val pj: Int = 0, // Partidos Jugados
    val pg: Int = 0, // Partidos Ganados
    val pe: Int = 0, // Partidos Empatados
    val pp: Int = 0, // Partidos Perdidos
    val gf: Int = 0, // Goles a Favor
    val gc: Int = 0, // Goles en Contra
    val dg: Int = 0, // Diferencia de Goles
    val pts: Int = 0, // Puntos
    val position: Int = 1
)

object StandingsCalculator {
    fun calculate(teams: List<Team>, matches: List<Match>): List<TeamStanding> {
        val statsMap = teams.associate { it.id to MutableStanding(it) }.toMutableMap()

        // Process only finished matches
        val finishedMatches = matches.filter { it.status == "FINISHED" }

        for (match in finishedMatches) {
            val home = statsMap[match.homeTeamId]
            val away = statsMap[match.awayTeamId]
            if (home == null || away == null) continue

            val hScore = match.homeScore
            val aScore = match.awayScore

            home.pj++
            away.pj++
            home.gf += hScore
            home.gc += aScore
            away.gf += aScore
            away.gc += hScore

            when {
                hScore > aScore -> {
                    home.pg++
                    home.pts += 3
                    away.pp++
                }
                aScore > hScore -> {
                    away.pg++
                    away.pts += 3
                    home.pp++
                }
                else -> {
                    home.pe++
                    away.pe++
                    home.pts += 1
                    away.pts += 1
                }
            }
        }

        val standings = statsMap.values.map { it.toTeamStanding() }

        // Sort with tie-breakers:
        // 1. Puntos (DESC)
        // 2. Diferencia de Goles (DESC)
        // 3. Enfrentamiento Directo (Head-to-head)
        // 4. Goles a Favor (DESC)
        // 5. Nombre (ASC)
        val sorted = standings.sortedWith { a, b ->
            when {
                b.pts != a.pts -> b.pts.compareTo(a.pts)
                b.dg != a.dg -> b.dg.compareTo(a.dg)
                else -> {
                    // Head-to-head tiebreaker
                    val h2h = getHeadToHead(a.team.id, b.team.id, finishedMatches)
                    if (h2h != 0) {
                        h2h
                    } else if (b.gf != a.gf) {
                        b.gf.compareTo(a.gf)
                    } else {
                        a.team.name.compareTo(b.team.name, ignoreCase = true)
                    }
                }
            }
        }

        return sorted.mapIndexed { index, standing ->
            standing.copy(position = index + 1)
        }
    }

    private fun getHeadToHead(teamAId: Long, teamBId: Long, finishedMatches: List<Match>): Int {
        var ptsA = 0
        var ptsB = 0
        var dgA = 0

        for (m in finishedMatches) {
            if (m.homeTeamId == teamAId && m.awayTeamId == teamBId) {
                if (m.homeScore > m.awayScore) ptsA += 3
                else if (m.homeScore < m.awayScore) ptsB += 3
                else { ptsA += 1; ptsB += 1 }
                dgA += (m.homeScore - m.awayScore)
            } else if (m.homeTeamId == teamBId && m.awayTeamId == teamAId) {
                if (m.homeScore > m.awayScore) ptsB += 3
                else if (m.homeScore < m.awayScore) ptsA += 3
                else { ptsA += 1; ptsB += 1 }
                dgA += (m.awayScore - m.homeScore)
            }
        }

        return when {
            ptsB != ptsA -> ptsB.compareTo(ptsA) // higher pts wins (b > a means b comes first, so return positive)
            dgA != 0 -> (-dgA).compareTo(0)
            else -> 0
        }
    }

    private class MutableStanding(val team: Team) {
        var pj = 0
        var pg = 0
        var pe = 0
        var pp = 0
        var gf = 0
        var gc = 0
        var pts = 0

        fun toTeamStanding(): TeamStanding {
            return TeamStanding(
                team = team,
                pj = pj,
                pg = pg,
                pe = pe,
                pp = pp,
                gf = gf,
                gc = gc,
                dg = gf - gc,
                pts = pts
            )
        }
    }
}
