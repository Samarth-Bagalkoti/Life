package com.example.life.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.life.data.model.Mood

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val situation: String,
    val mood: String, // Storing enum name
    val reason: String?,
    val nextAction: String
)
