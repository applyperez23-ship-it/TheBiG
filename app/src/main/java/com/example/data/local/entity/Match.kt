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
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["awayTeamId"],
            onDelete = ForeignKey.RESTRICT
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
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val roundName: String = "Jornada 1", // "Jornada 1", "Cuartos", "Semifinal", "Final", etc.
    val status: String = "SCHEDULED", // SCHEDULED, LIVE, FINISHED
    val currentPeriod: String = "NO_INICIADO", // NO_INICIADO, PRIMER_TIEMPO, DESCANSO, SEGUNDO_TIEMPO, TIEMPO_EXTRA, PENALTIS, FINALIZADO
    val elapsedSeconds: Int = 0,
    val homePenalties: Int? = null,
    val awayPenalties: Int? = null,
    val matchDate: String = "",
    val matchTime: String = "",
    val isKnockout: Boolean = false,
    val homeGoalkeeperPlayerId: Long? = null,
    val awayGoalkeeperPlayerId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
