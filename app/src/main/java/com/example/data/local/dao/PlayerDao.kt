package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY isCaptain DESC, dorsal ASC, name ASC")
    fun getPlayersForTeam(teamId: Long): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY isCaptain DESC, dorsal ASC, name ASC")
    suspend fun getPlayersForTeamOnce(teamId: Long): List<Player>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): Player?

    @Query("SELECT * FROM players WHERE teamId IN (:teamIds) ORDER BY isCaptain DESC, dorsal ASC")
    suspend fun getPlayersForTeamsOnce(teamIds: List<Long>): List<Player>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPlayers(players: List<Player>)

    @Update
    suspend fun updatePlayer(player: Player)

    @Delete
    suspend fun deletePlayer(player: Player)

    @Query("DELETE FROM players WHERE id = :id")
    suspend fun deletePlayerById(id: Long)

    @Query("UPDATE players SET feePaid = :feePaid WHERE id = :playerId")
    suspend fun updatePlayerFeeStatus(playerId: Long, feePaid: Boolean)

    @Query("UPDATE players SET isCaptain = 0 WHERE teamId = :teamId")
    suspend fun clearCaptainForTeam(teamId: Long)

    @Query("UPDATE players SET isCaptain = :isCaptain WHERE id = :playerId")
    suspend fun setPlayerCaptainStatus(playerId: Long, isCaptain: Boolean)

    @Query("SELECT COUNT(*) FROM players WHERE teamId = :teamId")
    suspend fun getPlayerCountForTeam(teamId: Long): Int

    @Query("SELECT * FROM players")
    suspend fun getAllPlayersOnce(): List<Player>
}
