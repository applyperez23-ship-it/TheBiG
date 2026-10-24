package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = Tournament::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["homeTeamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["awayTeamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tournamentId"), Index("homeTeamId"), Index("awayTeamId")]
)
data class Match(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tournamentId: Long,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val roundName: String = "Jornada 1",
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val homePenalties: Int? = null,
    val awayPenalties: Int? = null,
    val status: String = "SCHEDULED", // SCHEDULED, LIVE, PAUSED, FINISHED
    val currentPeriod: String = "FIRST_HALF", // FIRST_HALF, HALF_TIME, SECOND_HALF, EXTRA_TIME, PENALTIES
    val elapsedSeconds: Int = 0,
    val mvpPlayerId: Long? = null,
    val scheduledTimestamp: Long = System.currentTimeMillis()
)
