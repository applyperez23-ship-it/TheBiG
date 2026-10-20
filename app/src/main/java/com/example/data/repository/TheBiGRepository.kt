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
        // Automatically sync match score if it's a goal
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
                // If own goal attributed to player in team A, team B gets the score point
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
        // Adjust score back
        val match = db.matchDao().getMatchByIdOnce(event.matchId) ?: return
        when (event.eventType) {
            "GOAL", "PENALTY_GOAL" -> {
                if (event.teamId == match.homeTeamId && match.homeScore > 0) {
                    db.matchDao().updateMatch(match.copy(homeScore = match.homeScore - 1))
                } else if (event.teamId == match.awayTeamId && match.awayScore > 0) {
                    db.matchDao().updateMatch(match.copy(awayScore = match.awayScore - 1))
                }
            }
            "OWN_GOAL" -> {
                if (event.teamId == match.homeTeamId && match.awayScore > 0) {
                    db.matchDao().updateMatch(match.copy(awayScore = match.awayScore - 1))
                } else if (event.teamId == match.awayTeamId && match.homeScore > 0) {
                    db.matchDao().updateMatch(match.copy(homeScore = match.homeScore - 1))
                }
            }
        }
    }

    // --- JSON Backup Export / Import ---
    suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "TheBiG")
        root.put("version", 1)
        root.put("author", "Daniel (el Grande)")
        root.put("timestamp", System.currentTimeMillis())

        val tournaments = db.tournamentDao().getAllTournamentsOnce()
        val tournamentsArray = JSONArray()
        for (t in tournaments) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("edition", t.edition)
            obj.put("entryFee", t.entryFee)
            obj.put("playersPerTeam", t.playersPerTeam)
            obj.put("matchDurationMinutes", t.matchDurationMinutes)
            obj.put("status", t.status)
            obj.put("championTeamId", t.championTeamId ?: JSONObject.NULL)
            obj.put("runnerUpTeamId", t.runnerUpTeamId ?: JSONObject.NULL)
            obj.put("prizeChampionEnabled", t.prizeChampionEnabled)
            obj.put("prizeChampionAmount", t.prizeChampionAmount)
            obj.put("prizeRunnerUpEnabled", t.prizeRunnerUpEnabled)
            obj.put("prizeRunnerUpAmount", t.prizeRunnerUpAmount)
            obj.put("prizeTopScorerEnabled", t.prizeTopScorerEnabled)
            obj.put("prizeTopScorerAmount", t.prizeTopScorerAmount)
            obj.put("prizeTopAssisterEnabled", t.prizeTopAssisterEnabled)
            obj.put("prizeTopAssisterAmount", t.prizeTopAssisterAmount)
            obj.put("prizeBestGkEnabled", t.prizeBestGkEnabled)
            obj.put("prizeBestGkAmount", t.prizeBestGkAmount)
            obj.put("minMatchesForBestGk", t.minMatchesForBestGk)
            obj.put("createdAt", t.createdAt)
            tournamentsArray.put(obj)
        }
        root.put("tournaments", tournamentsArray)

        val teams = db.teamDao().getAllTeamsOnce()
        val teamsArray = JSONArray()
        for (tm in teams) {
            val obj = JSONObject()
            obj.put("id", tm.id)
            obj.put("name", tm.name)
            obj.put("shortName", tm.shortName)
            obj.put("shieldVectorIndex", tm.shieldVectorIndex)
            obj.put("primaryColorHex", tm.primaryColorHex)
            obj.put("secondaryColorHex", tm.secondaryColorHex)
            obj.put("borderColorHex", tm.borderColorHex)
            obj.put("customShieldUri", tm.customShieldUri ?: JSONObject.NULL)
            obj.put("jerseyPattern", tm.jerseyPattern)
            obj.put("goalkeeperColorHex", tm.goalkeeperColorHex)
            obj.put("createdAt", tm.createdAt)
            teamsArray.put(obj)
        }
        root.put("teams", teamsArray)

        val players = db.playerDao().getAllPlayersOnce()
        val playersArray = JSONArray()
        for (p in players) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("teamId", p.teamId)
            obj.put("name", p.name)
            obj.put("dorsal", p.dorsal)
            obj.put("isGoalkeeper", p.isGoalkeeper)
            obj.put("feePaid", p.feePaid)
            obj.put("matchesPlayed", p.matchesPlayed)
            obj.put("matchesAsGoalkeeper", p.matchesAsGoalkeeper)
            obj.put("goalsConcededAsGk", p.goalsConcededAsGk)
            obj.put("createdAt", p.createdAt)
            playersArray.put(obj)
        }
        root.put("players", playersArray)

        val crossRefs = db.tournamentTeamDao().getAllCrossRefsOnce()
        val crossRefsArray = JSONArray()
        for (cr in crossRefs) {
            val obj = JSONObject()
            obj.put("tournamentId", cr.tournamentId)
            obj.put("teamId", cr.teamId)
            obj.put("feePaid", cr.feePaid)
            obj.put("enrolledAt", cr.enrolledAt)
            crossRefsArray.put(obj)
        }
        root.put("tournamentTeams", crossRefsArray)

        val matches = db.matchDao().getAllMatchesOnce()
        val matchesArray = JSONArray()
        for (m in matches) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("tournamentId", m.tournamentId)
            obj.put("homeTeamId", m.homeTeamId)
            obj.put("awayTeamId", m.awayTeamId)
            obj.put("homeScore", m.homeScore)
            obj.put("awayScore", m.awayScore)
            obj.put("roundName", m.roundName)
            obj.put("status", m.status)
            obj.put("currentPeriod", m.currentPeriod)
            obj.put("elapsedSeconds", m.elapsedSeconds)
            obj.put("homePenalties", m.homePenalties ?: JSONObject.NULL)
            obj.put("awayPenalties", m.awayPenalties ?: JSONObject.NULL)
            obj.put("matchDate", m.matchDate)
            obj.put("matchTime", m.matchTime)
            obj.put("isKnockout", m.isKnockout)
            obj.put("homeGoalkeeperPlayerId", m.homeGoalkeeperPlayerId ?: JSONObject.NULL)
            obj.put("awayGoalkeeperPlayerId", m.awayGoalkeeperPlayerId ?: JSONObject.NULL)
            obj.put("createdAt", m.createdAt)
            matchesArray.put(obj)
        }
        root.put("matches", matchesArray)

        val events = db.matchEventDao().getAllEventsOnce()
        val eventsArray = JSONArray()
        for (e in events) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("matchId", e.matchId)
            obj.put("teamId", e.teamId)
            obj.put("playerId", e.playerId ?: JSONObject.NULL)
            obj.put("assistPlayerId", e.assistPlayerId ?: JSONObject.NULL)
            obj.put("eventType", e.eventType)
            obj.put("minute", e.minute)
            obj.put("extraInfo", e.extraInfo)
            obj.put("timestamp", e.timestamp)
            eventsArray.put(obj)
        }
        root.put("matchEvents", eventsArray)

        root.toString(2)
    }

    suspend fun importDatabaseFromJson(jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonContent)

            // Parse teams first
            if (root.has("teams")) {
                val array = root.getJSONArray("teams")
                val list = mutableListOf<Team>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(
                        Team(
                            id = o.optLong("id", 0),
                            name = o.getString("name"),
                            shortName = o.optString("shortName", ""),
                            shieldVectorIndex = o.optInt("shieldVectorIndex", 0),
                            primaryColorHex = o.optString("primaryColorHex", "#F59E0B"),
                            secondaryColorHex = o.optString("secondaryColorHex", "#0F172A"),
                            borderColorHex = o.optString("borderColorHex", "#FFFFFF"),
                            customShieldUri = if (o.isNull("customShieldUri")) null else o.optString("customShieldUri"),
                            jerseyPattern = o.optString("jerseyPattern", "SOLID"),
                            goalkeeperColorHex = o.optString("goalkeeperColorHex", "#10B981"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                db.teamDao().insertAllTeams(list)
            }

            // Parse players
            if (root.has("players")) {
                val array = root.getJSONArray("players")
                val list = mutableListOf<Player>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(
                        Player(
                            id = o.optLong("id", 0),
                            teamId = o.getLong("teamId"),
                            name = o.getString("name"),
                            dorsal = o.optInt("dorsal", 1),
                            isGoalkeeper = o.optBoolean("isGoalkeeper", false),
                            feePaid = o.optBoolean("feePaid", false),
                            matchesPlayed = o.optInt("matchesPlayed", 0),
                            matchesAsGoalkeeper = o.optInt("matchesAsGoalkeeper", 0),
                            goalsConcededAsGk = o.optInt("goalsConcededAsGk", 0),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                db.playerDao().insertAllPlayers(list)
            }

            // Parse tournaments
            if (root.has("tournaments")) {
                val array = root.getJSONArray("tournaments")
                val list = mutableListOf<Tournament>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(
                        Tournament(
                            id = o.optLong("id", 0),
                            name = o.getString("name"),
                            edition = o.optString("edition", "1"),
                            entryFee = o.optDouble("entryFee", 0.0),
                            playersPerTeam = o.optInt("playersPerTeam", 7),
                            matchDurationMinutes = o.optInt("matchDurationMinutes", 20),
                            status = o.optString("status", "PLANNING"),
                            championTeamId = if (o.isNull("championTeamId")) null else o.optLong("championTeamId"),
                            runnerUpTeamId = if (o.isNull("runnerUpTeamId")) null else o.optLong("runnerUpTeamId"),
                            prizeChampionEnabled = o.optBoolean("prizeChampionEnabled", true),
                            prizeChampionAmount = o.optDouble("prizeChampionAmount", 0.0),
                            prizeRunnerUpEnabled = o.optBoolean("prizeRunnerUpEnabled", true),
                            prizeRunnerUpAmount = o.optDouble("prizeRunnerUpAmount", 0.0),
                            prizeTopScorerEnabled = o.optBoolean("prizeTopScorerEnabled", true),
                            prizeTopScorerAmount = o.optDouble("prizeTopScorerAmount", 0.0),
                            prizeTopAssisterEnabled = o.optBoolean("prizeTopAssisterEnabled", true),
                            prizeTopAssisterAmount = o.optDouble("prizeTopAssisterAmount", 0.0),
                            prizeBestGkEnabled = o.optBoolean("prizeBestGkEnabled", true),
                            prizeBestGkAmount = o.optDouble("prizeBestGkAmount", 0.0),
                            minMatchesForBestGk = o.optInt("minMatchesForBestGk", 2),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                db.tournamentDao().insertAllTournaments(list)
            }

            // Parse tournament teams
            if (root.has("tournamentTeams")) {
                val array = root.getJSONArray("tournamentTeams")
                val list = mutableListOf<TournamentTeamCrossRef>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(
                        TournamentTeamCrossRef(
                            tournamentId = o.getLong("tournamentId"),
                            teamId = o.getLong("teamId"),
                            feePaid = o.optBoolean("feePaid", false),
                            enrolledAt = o.optLong("enrolledAt", System.currentTimeMillis())
                        )
                    )
                }
                db.tournamentTeamDao().insertAllCrossRefs(list)
            }

            // Parse matches
            if (root.has("matches")) {
                val array = root.getJSONArray("matches")
                val list = mutableListOf<Match>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(
                        Match(
                            id = o.optLong("id", 0),
                            tournamentId = o.getLong("tournamentId"),
                            homeTeamId = o.getLong("homeTeamId"),
                            awayTeamId = o.getLong("awayTeamId"),
                            homeScore = o.optInt("homeScore", 0),
                            awayScore = o.optInt("awayScore", 0),
                            roundName = o.optString("roundName", "Jornada 1"),
                            status = o.optString("status", "SCHEDULED"),
                            currentPeriod = o.optString("currentPeriod", "NO_INICIADO"),
                            elapsedSeconds = o.optInt("elapsedSeconds", 0),
                            homePenalties = if (o.isNull("homePenalties")) null else o.optInt("homePenalties"),
                            awayPenalties = if (o.isNull("awayPenalties")) null else o.optInt("awayPenalties"),
                            matchDate = o.optString("matchDate", ""),
                            matchTime = o.optString("matchTime", ""),
                            isKnockout = o.optBoolean("isKnockout", false),
                            homeGoalkeeperPlayerId = if (o.isNull("homeGoalkeeperPlayerId")) null else o.optLong("homeGoalkeeperPlayerId"),
                            awayGoalkeeperPlayerId = if (o.isNull("awayGoalkeeperPlayerId")) null else o.optLong("awayGoalkeeperPlayerId"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                db.matchDao().insertAllMatches(list)
            }

            // Parse match events
            if (root.has("matchEvents")) {
                val array = root.getJSONArray("matchEvents")
                val list = mutableListOf<MatchEvent>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    list.add(
                        MatchEvent(
                            id = o.optLong("id", 0),
                            matchId = o.getLong("matchId"),
                            teamId = o.getLong("teamId"),
                            playerId = if (o.isNull("playerId")) null else o.optLong("playerId"),
                            assistPlayerId = if (o.isNull("assistPlayerId")) null else o.optLong("assistPlayerId"),
                            eventType = o.getString("eventType"),
                            minute = o.optInt("minute", 0),
                            extraInfo = o.optString("extraInfo", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                db.matchEventDao().insertAllEvents(list)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
