package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MatchEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchEventDao {
    @Query("SELECT * FROM match_events WHERE matchId = :matchId ORDER BY minute ASC, timestamp ASC")
    fun getEventsForMatch(matchId: Long): Flow<List<MatchEvent>>

    @Query("SELECT * FROM match_events WHERE matchId = :matchId ORDER BY minute ASC, timestamp ASC")
    suspend fun getEventsForMatchOnce(matchId: Long): List<MatchEvent>

    @Query("SELECT me.* FROM match_events me INNER JOIN matches m ON me.matchId = m.id WHERE m.tournamentId = :tournamentId ORDER BY me.timestamp ASC")
    fun getAllEventsForTournament(tournamentId: Long): Flow<List<MatchEvent>>

    @Query("SELECT me.* FROM match_events me INNER JOIN matches m ON me.matchId = m.id WHERE m.tournamentId = :tournamentId ORDER BY me.timestamp ASC")
    suspend fun getAllEventsForTournamentOnce(tournamentId: Long): List<MatchEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MatchEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEvents(events: List<MatchEvent>)

    @Delete
    suspend fun deleteEvent(event: MatchEvent)

    @Query("DELETE FROM match_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("SELECT * FROM match_events")
    suspend fun getAllEventsOnce(): List<MatchEvent>
}
