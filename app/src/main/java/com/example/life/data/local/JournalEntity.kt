package com.example.life.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val encryptedContent: String,
    val verificationString: String, // "LIFE" encrypted with the same key
    val timestamp: Long
)
/*
Note: We store encryptedContent as ByteArray for AES-GCM.
*/
