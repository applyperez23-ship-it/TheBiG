package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Match
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY createdAt DESC, id DESC")
    fun getAllMatches(): Flow<List<Match>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY id ASC")
    fun getMatchesForTournament(tournamentId: Long): Flow<List<Match>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY id ASC")
    suspend fun getMatchesForTournamentOnce(tournamentId: Long): List<Match>

    @Query("SELECT * FROM matches WHERE id = :id")
    fun getMatchById(id: Long): Flow<Match?>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchByIdOnce(id: Long): Match?

    @Query("SELECT * FROM matches WHERE status = 'LIVE' ORDER BY id DESC")
    fun getLiveMatches(): Flow<List<Match>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMatches(matches: List<Match>)

    @Update
    suspend fun updateMatch(match: Match)

    @Delete
    suspend fun deleteMatch(match: Match)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteMatchById(id: Long)

    @Query("SELECT * FROM matches")
    suspend fun getAllMatchesOnce(): List<Match>
}
