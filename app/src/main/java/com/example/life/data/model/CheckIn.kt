package com.example.life.data.model

import java.util.Date
import java.util.UUID

enum class Mood(val label: String, val emoji: String, val score: Int) {
    Happy("Happy", "😊", 5),
    Good("Good", "🙂", 4),
    Okay("Okay", "😐", 3),
    Sad("Sad", "😔", 2),
    Angry("Angry", "😠", 1),
    Anxious("Anxious", "😰", 2),
    Stressed("Stressed", "😫", 1),
    Tired("Tired", "😴", 2),
    Excited("Excited", "🤩", 5)
}

data class CheckIn(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Date = Date(),
    val situation: String,
    val mood: Mood,
    val reason: String? = null,
    val nextAction: String
)
