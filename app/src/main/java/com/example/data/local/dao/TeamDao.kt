package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Team
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun getAllTeams(): Flow<List<Team>>

    @Query("SELECT * FROM teams WHERE id = :id")
    fun getTeamById(id: Long): Flow<Team?>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamByIdOnce(id: Long): Team?

    @Query("SELECT * FROM teams WHERE id IN (:ids)")
    suspend fun getTeamsByIds(ids: List<Long>): List<Team>

    @Query("SELECT t.* FROM teams t INNER JOIN tournament_teams tt ON t.id = tt.teamId WHERE tt.tournamentId = :tournamentId ORDER BY t.name ASC")
    fun getTeamsForTournament(tournamentId: Long): Flow<List<Team>>

    @Query("SELECT t.* FROM teams t INNER JOIN tournament_teams tt ON t.id = tt.teamId WHERE tt.tournamentId = :tournamentId ORDER BY t.name ASC")
    suspend fun getTeamsForTournamentOnce(tournamentId: Long): List<Team>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: Team): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTeams(teams: List<Team>)

    @Update
    suspend fun updateTeam(team: Team)

    @Delete
    suspend fun deleteTeam(team: Team)

    @Query("DELETE FROM teams WHERE id = :id")
    suspend fun deleteTeamById(id: Long)

    @Query("SELECT * FROM teams")
    suspend fun getAllTeamsOnce(): List<Team>
}
