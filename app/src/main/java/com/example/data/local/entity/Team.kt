package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val shortName: String,
    val shieldVectorIndex: Int = 0, // 0 to 9 preset shield shapes
    val primaryColorHex: String = "#F59E0B",
    val secondaryColorHex: String = "#0F172A",
    val borderColorHex: String = "#FFFFFF",
    val customShieldUri: String? = null,
    val jerseyPattern: String = "SOLID", // SOLID, STRIPES_VERTICAL, STRIPES_HORIZONTAL, SASH, HALF
    val goalkeeperColorHex: String = "#10B981", // Exclusivo para el portero
    val createdAt: Long = System.currentTimeMillis()
)
