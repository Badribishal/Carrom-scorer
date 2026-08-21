package com.example.carrom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_match")
data class ActiveMatchEntity(
    @PrimaryKey
    val id: Int = 1,
    val gameStateJson: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
