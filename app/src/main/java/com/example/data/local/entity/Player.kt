package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId")]
)
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamId: Long,
    val name: String,
    val dorsal: Int,
    val isCaptain: Boolean = false,
    val isGoalkeeper: Boolean = false,
    val feePaid: Boolean = false,
    val matchesPlayed: Int = 0,
    val matchesAsGoalkeeper: Int = 0,
    val goalsConcededAsGk: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val number: Int get() = dorsal
}
