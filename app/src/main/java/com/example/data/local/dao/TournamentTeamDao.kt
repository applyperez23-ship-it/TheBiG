package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TournamentTeamCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentTeamDao {
    @Query("SELECT * FROM tournament_teams WHERE tournamentId = :tournamentId")
    fun getCrossRefsForTournament(tournamentId: Long): Flow<List<TournamentTeamCrossRef>>

    @Query("SELECT * FROM tournament_teams WHERE tournamentId = :tournamentId")
    suspend fun getCrossRefsForTournamentOnce(tournamentId: Long): List<TournamentTeamCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: TournamentTeamCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCrossRefs(refs: List<TournamentTeamCrossRef>)

    @Update
    suspend fun updateCrossRef(ref: TournamentTeamCrossRef)

    @Delete
    suspend fun deleteCrossRef(ref: TournamentTeamCrossRef)

    @Query("DELETE FROM tournament_teams WHERE tournamentId = :tournamentId AND teamId = :teamId")
    suspend fun removeTeamFromTournament(tournamentId: Long, teamId: Long)

    @Query("UPDATE tournament_teams SET feePaid = :feePaid WHERE tournamentId = :tournamentId AND teamId = :teamId")
    suspend fun updateTournamentTeamFee(tournamentId: Long, teamId: Long, feePaid: Boolean)

    @Query("SELECT * FROM tournament_teams")
    suspend fun getAllCrossRefsOnce(): List<TournamentTeamCrossRef>
}
