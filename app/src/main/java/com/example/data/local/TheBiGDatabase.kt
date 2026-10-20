package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.MatchDao
import com.example.data.local.dao.MatchEventDao
import com.example.data.local.dao.PlayerDao
import com.example.data.local.dao.TeamDao
import com.example.data.local.dao.TournamentDao
import com.example.data.local.dao.TournamentTeamDao
import com.example.data.local.entity.Match
import com.example.data.local.entity.MatchEvent
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.data.local.entity.TournamentTeamCrossRef

@Database(
    entities = [
        Tournament::class,
        Team::class,
        Player::class,
        TournamentTeamCrossRef::class,
        Match::class,
        MatchEvent::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TheBiGDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun tournamentTeamDao(): TournamentTeamDao
    abstract fun matchDao(): MatchDao
    abstract fun matchEventDao(): MatchEventDao

    companion object {
        @Volatile
        private var INSTANCE: TheBiGDatabase? = null

        fun getDatabase(context: Context): TheBiGDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TheBiGDatabase::class.java,
                    "thebig_football.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
