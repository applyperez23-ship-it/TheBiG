package com.example.domain.rules

import com.example.data.local.entity.Match
import com.example.data.local.entity.Team

data class TeamStanding(
    val team: Team,
    val position: Int = 0,
    val pj: Int = 0,
    val pg: Int = 0,
    val pe: Int = 0,
    val pp: Int = 0,
    val gf: Int = 0,
    val gc: Int = 0,
    val dg: Int = 0,
    val pts: Int = 0
)

object StandingsCalculator {

    fun calculate(teams: List<Team>, matches: List<Match>): List<TeamStanding> {
        val statsMap = teams.associate { it.id to MutableStanding(it) }

        for (m in matches) {
            if (m.status != "FINISHED") continue
            val home = statsMap[m.homeTeamId]
            val away = statsMap[m.awayTeamId]
            if (home == null || away == null) continue

            home.pj++
            away.pj++
            home.gf += m.homeScore
            home.gc += m.awayScore
            away.gf += m.awayScore
            away.gc += m.homeScore

            when {
                m.homeScore > m.awayScore -> {
                    home.pg++
                    home.pts += 3
                    away.pp++
                }
                m.homeScore < m.awayScore -> {
                    away.pg++
                    away.pts += 3
                    home.pp++
                }
                else -> {
                    home.pe++
                    home.pts += 1
                    away.pe++
                    away.pts += 1
                }
            }
        }

        val sorted = statsMap.values.sortedWith { a, b ->
            when {
                b.pts != a.pts -> b.pts.compareTo(a.pts)
                (b.gf - b.gc) != (a.gf - a.gc) -> (b.gf - b.gc).compareTo(a.gf - a.gc)
                b.gf != a.gf -> b.gf.compareTo(a.gf)
                else -> a.team.shortName.compareTo(b.team.shortName)
            }
        }

        return sorted.mapIndexed { index, ms ->
            TeamStanding(
                team = ms.team,
                position = index + 1,
                pj = ms.pj,
                pg = ms.pg,
                pe = ms.pe,
                pp = ms.pp,
                gf = ms.gf,
                gc = ms.gc,
                dg = ms.gf - ms.gc,
                pts = ms.pts
            )
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
    }
}
