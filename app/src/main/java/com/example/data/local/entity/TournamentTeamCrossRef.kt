package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tournament_teams",
    primaryKeys = ["tournamentId", "teamId"],
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
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tournamentId"), Index("teamId")]
)
data class TournamentTeamCrossRef(
    val tournamentId: Long,
    val teamId: Long,
    val feePaid: Boolean = false,
    val enrolledAt: Long = System.currentTimeMillis()
)
