package com.example.data.repository

import com.example.data.local.TheBiGDatabase
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.data.local.entity.TournamentTeamCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TheBiGRepository(private val db: TheBiGDatabase) {

    // --- Tournaments ---
    val allTournaments: Flow<List<Tournament>> = db.tournamentDao().getAllTournaments()

    fun getTournament(id: Long): Flow<Tournament?> = db.tournamentDao().getTournamentById(id)
    suspend fun getTournamentOnce(id: Long): Tournament? = db.tournamentDao().getTournamentByIdOnce(id)
    suspend fun saveTournament(tournament: Tournament): Long = db.tournamentDao().insertTournament(tournament)
    suspend fun updateTournament(tournament: Tournament) = db.tournamentDao().updateTournament(tournament)
    suspend fun deleteTournament(id: Long) = db.tournamentDao().deleteTournamentById(id)

    // --- Teams ---
    val allTeams: Flow<List<Team>> = db.teamDao().getAllTeams()

    fun getTeam(id: Long): Flow<Team?> = db.teamDao().getTeamById(id)
    suspend fun getTeamOnce(id: Long): Team? = db.teamDao().getTeamByIdOnce(id)
    fun getTeamsForTournament(tournamentId: Long): Flow<List<Team>> = db.teamDao().getTeamsForTournament(tournamentId)
    suspend fun getTeamsForTournamentOnce(tournamentId: Long): List<Team> = db.teamDao().getTeamsForTournamentOnce(tournamentId)
    suspend fun saveTeam(team: Team): Long = db.teamDao().insertTeam(team)
    suspend fun updateTeam(team: Team) = db.teamDao().updateTeam(team)
    suspend fun deleteTeam(id: Long) = db.teamDao().deleteTeamById(id)

    // --- Players ---
    fun getPlayersForTeam(teamId: Long): Flow<List<Player>> = db.playerDao().getPlayersForTeam(teamId)
    suspend fun getPlayersForTeamOnce(teamId: Long): List<Player> = db.playerDao().getPlayersForTeamOnce(teamId)
    suspend fun getPlayersForTeamsOnce(teamIds: List<Long>): List<Player> = db.playerDao().getPlayersForTeamsOnce(teamIds)
    suspend fun savePlayer(player: Player): Long = db.playerDao().insertPlayer(player)
    suspend fun updatePlayer(player: Player) = db.playerDao().updatePlayer(player)
    suspend fun deletePlayer(id: Long) = db.playerDao().deletePlayerById(id)
    suspend fun updatePlayerFeeStatus(playerId: Long, feePaid: Boolean) = db.playerDao().updatePlayerFeeStatus(playerId, feePaid)
    suspend fun setTeamCaptain(teamId: Long, playerId: Long, isCaptain: Boolean) {
        db.playerDao().clearCaptainForTeam(teamId)
        if (isCaptain) {
            db.playerDao().setPlayerCaptainStatus(playerId, true)
        }
    }

    // --- Tournament Teams (CrossRef) ---
    fun getCrossRefsForTournament(tournamentId: Long): Flow<List<TournamentTeamCrossRef>> =
        db.tournamentTeamDao().getCrossRefsForTournament(tournamentId)

    suspend fun enrollTeam(tournamentId: Long, teamId: Long, feePaid: Boolean = false) {
        db.tournamentTeamDao().insertCrossRef(
            TournamentTeamCrossRef(
                tournamentId = tournamentId,
                teamId = teamId,
                feePaid = feePaid
            )
        )
    }

    suspend fun removeTeamFromTournament(tournamentId: Long, teamId: Long) {
        db.tournamentTeamDao().removeTeamFromTournament(tournamentId, teamId)
    }

    suspend fun updateTournamentTeamFee(tournamentId: Long, teamId: Long, feePaid: Boolean) {
        db.tournamentTeamDao().updateTournamentTeamFee(tournamentId, teamId, feePaid)
    }

    // --- Matches ---
    val allMatches: Flow<List<Match>> = db.matchDao().getAllMatches()
    val liveMatches: Flow<List<Match>> = db.matchDao().getLiveMatches()

    fun getMatchesForTournament(tournamentId: Long): Flow<List<Match>> = db.matchDao().getMatchesForTournament(tournamentId)
    suspend fun getMatchesForTournamentOnce(tournamentId: Long): List<Match> = db.matchDao().getMatchesForTournamentOnce(tournamentId)
    fun getMatch(id: Long): Flow<Match?> = db.matchDao().getMatchById(id)
    suspend fun getMatchOnce(id: Long): Match? = db.matchDao().getMatchByIdOnce(id)
    suspend fun saveMatch(match: Match): Long = db.matchDao().insertMatch(match)
    suspend fun updateMatch(match: Match) = db.matchDao().updateMatch(match)
    suspend fun deleteMatch(id: Long) = db.matchDao().deleteMatchById(id)

    // --- Match Events ---
    fun getEventsForMatch(matchId: Long): Flow<List<MatchEvent>> = db.matchEventDao().getEventsForMatch(matchId)
    suspend fun getEventsForMatchOnce(matchId: Long): List<MatchEvent> = db.matchEventDao().getEventsForMatchOnce(matchId)
    fun getAllEventsForTournament(tournamentId: Long): Flow<List<MatchEvent>> = db.matchEventDao().getAllEventsForTournament(tournamentId)
    suspend fun getAllEventsForTournamentOnce(tournamentId: Long): List<MatchEvent> = db.matchEventDao().getAllEventsForTournamentOnce(tournamentId)

    suspend fun addMatchEvent(event: MatchEvent) {
        db.matchEventDao().insertEvent(event)
        val match = db.matchDao().getMatchByIdOnce(event.matchId) ?: return
        when (event.eventType) {
            "GOAL", "PENALTY_GOAL" -> {
                if (event.teamId == match.homeTeamId) {
                    db.matchDao().updateMatch(match.copy(homeScore = match.homeScore + 1))
                } else if (event.teamId == match.awayTeamId) {
                    db.matchDao().updateMatch(match.copy(awayScore = match.awayScore + 1))
                }
            }
            "OWN_GOAL" -> {
                if (event.teamId == match.homeTeamId) {
                    db.matchDao().updateMatch(match.copy(awayScore = match.awayScore + 1))
                } else if (event.teamId == match.awayTeamId) {
                    db.matchDao().updateMatch(match.copy(homeScore = match.homeScore + 1))
                }
            }
        }
    }

    suspend fun deleteMatchEvent(event: MatchEvent) {
        db.matchEventDao().deleteEvent(event)
        val match = db.matchDao().getMatchByIdOnce(event.matchId) ?: return
        when (event.eventType) {
            "GOAL", "PENALTY_GOAL" -> {
                if (event.teamId == match.homeTeamId) {
                    db.matchDao().updateMatch(match.copy(homeScore = (match.homeScore - 1).coerceAtLeast(0)))
                } else if (event.teamId == match.awayTeamId) {
                    db.matchDao().updateMatch(match.copy(awayScore = (match.awayScore - 1).coerceAtLeast(0)))
                }
            }
            "OWN_GOAL" -> {
                if (event.teamId == match.homeTeamId) {
                    db.matchDao().updateMatch(match.copy(awayScore = (match.awayScore - 1).coerceAtLeast(0)))
                } else if (event.teamId == match.awayTeamId) {
                    db.matchDao().updateMatch(match.copy(homeScore = (match.homeScore - 1).coerceAtLeast(0)))
                }
            }
        }
    }

    suspend fun generateRoundRobinFixture(tournamentId: Long): List<Match> {
        val teams = db.teamDao().getTeamsForTournamentOnce(tournamentId)
        if (teams.size < 2) return emptyList()

        val createdMatches = mutableListOf<Match>()
        val n = teams.size
        var roundCount = 1

        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val match = Match(
                    tournamentId = tournamentId,
                    homeTeamId = teams[i].id,
                    awayTeamId = teams[j].id,
                    roundName = "Jornada $roundCount",
                    status = "SCHEDULED"
                )
                val id = db.matchDao().insertMatch(match)
                createdMatches.add(match.copy(id = id))
                roundCount++
            }
        }
        return createdMatches
    }

    // --- Backup & Restore via JSON ---
    suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "TheBiG")
        root.put("author", "Daniel (el Grande)")
        root.put("version", 4)
        root.put("timestamp", System.currentTimeMillis())

        val tList = db.tournamentDao().getAllTournamentsOnce()
        val tArr = JSONArray()
        for (t in tList) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("edition", t.edition)
            obj.put("entryFee", t.entryFee)
            obj.put("playersPerTeam", t.playersPerTeam)
            obj.put("matchDurationMinutes", t.matchDurationMinutes)
            obj.put("status", t.status)
            obj.put("tournamentType", t.tournamentType)
            obj.put("awardTiming", t.awardTiming)
            obj.put("prizeChampionAmount", t.prizeChampionAmount)
            obj.put("prizeRunnerUpAmount", t.prizeRunnerUpAmount)
            obj.put("prizeTopScorerAmount", t.prizeTopScorerAmount)
            obj.put("prizeTopAssisterAmount", t.prizeTopAssisterAmount)
            obj.put("prizeBestGkAmount", t.prizeBestGkAmount)
            obj.put("prizeMvpAmount", t.prizeMvpAmount)
            obj.put("yellowCardFine", t.yellowCardFine)
            obj.put("redCardFine", t.redCardFine)
            tArr.put(obj)
        }
        root.put("tournaments", tArr)

        val teamList = db.teamDao().getAllTeamsOnce()
        val teamArr = JSONArray()
        for (team in teamList) {
            val obj = JSONObject()
            obj.put("id", team.id)
            obj.put("name", team.name)
            obj.put("shortName", team.shortName)
            obj.put("shieldVectorIndex", team.shieldVectorIndex)
            obj.put("primaryColorHex", team.primaryColorHex)
            obj.put("secondaryColorHex", team.secondaryColorHex)
            obj.put("borderColorHex", team.borderColorHex)
            obj.put("jerseyPattern", team.jerseyPattern)
            obj.put("goalkeeperColorHex", team.goalkeeperColorHex)
            teamArr.put(obj)
        }
        root.put("teams", teamArr)

        val playerList = db.playerDao().getAllPlayersOnce()
        val playerArr = JSONArray()
        for (p in playerList) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("teamId", p.teamId)
            obj.put("name", p.name)
            obj.put("dorsal", p.dorsal)
            obj.put("isGoalkeeper", p.isGoalkeeper)
            obj.put("isCaptain", p.isCaptain)
            obj.put("feePaid", p.feePaid)
            playerArr.put(obj)
        }
        root.put("players", playerArr)

        root.toString(2)
    }

    suspend fun importDatabaseFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            val tArr = root.optJSONArray("tournaments") ?: JSONArray()
            val teamArr = root.optJSONArray("teams") ?: JSONArray()
            val playerArr = root.optJSONArray("players") ?: JSONArray()

            for (i in 0 until tArr.length()) {
                val obj = tArr.getJSONObject(i)
                val t = Tournament(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    edition = obj.optString("edition", "1"),
                    entryFee = obj.optDouble("entryFee", 0.0),
                    playersPerTeam = obj.optInt("playersPerTeam", 7),
                    matchDurationMinutes = obj.optInt("matchDurationMinutes", 20),
                    status = obj.optString("status", "PLANNING"),
                    tournamentType = obj.optString("tournamentType", "LEAGUE"),
                    awardTiming = obj.optString("awardTiming", "END_OF_LEAGUE"),
                    prizeChampionAmount = obj.optDouble("prizeChampionAmount", 0.0),
                    prizeRunnerUpAmount = obj.optDouble("prizeRunnerUpAmount", 0.0),
                    prizeTopScorerAmount = obj.optDouble("prizeTopScorerAmount", 0.0),
                    prizeTopAssisterAmount = obj.optDouble("prizeTopAssisterAmount", 0.0),
                    prizeBestGkAmount = obj.optDouble("prizeBestGkAmount", 0.0),
                    prizeMvpAmount = obj.optDouble("prizeMvpAmount", 0.0),
                    yellowCardFine = obj.optDouble("yellowCardFine", 5.0),
                    redCardFine = obj.optDouble("redCardFine", 10.0)
                )
                db.tournamentDao().insertTournament(t)
            }

            for (i in 0 until teamArr.length()) {
                val obj = teamArr.getJSONObject(i)
                val tm = Team(
                    id = obj.optLong("id", 0),
                    name = obj.getString("name"),
                    shortName = obj.optString("shortName", "BIG"),
                    shieldVectorIndex = obj.optInt("shieldVectorIndex", 0),
                    primaryColorHex = obj.optString("primaryColorHex", "#F59E0B"),
                    secondaryColorHex = obj.optString("secondaryColorHex", "#0F172A"),
                    borderColorHex = obj.optString("borderColorHex", "#FFFFFF"),
                    jerseyPattern = obj.optString("jerseyPattern", "SOLID"),
                    goalkeeperColorHex = obj.optString("goalkeeperColorHex", "#10B981")
                )
                db.teamDao().insertTeam(tm)
            }

            for (i in 0 until playerArr.length()) {
                val obj = playerArr.getJSONObject(i)
                val p = Player(
                    id = obj.optLong("id", 0),
                    teamId = obj.getLong("teamId"),
                    name = obj.getString("name"),
                    dorsal = obj.getInt("dorsal"),
                    isGoalkeeper = obj.optBoolean("isGoalkeeper", false),
                    isCaptain = obj.optBoolean("isCaptain", false),
                    feePaid = obj.optBoolean("feePaid", false)
                )
                db.playerDao().insertPlayer(p)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
