package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "match_events",
    foreignKeys = [
        ForeignKey(
            entity = Match::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["assistPlayerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("matchId"), Index("teamId"), Index("playerId"), Index("assistPlayerId")]
)
data class MatchEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matchId: Long,
    val teamId: Long,
    val playerId: Long?,
    val assistPlayerId: Long? = null,
    val eventType: String, // GOAL, OWN_GOAL, PENALTY_GOAL, PENALTY_MISSED, YELLOW_CARD, RED_CARD, SUBSTITUTION
    val minute: Int = 0,
    val extraInfo: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
