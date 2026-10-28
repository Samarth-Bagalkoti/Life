package com.example.life.data.local

import com.example.life.security.VigenereCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

data class JournalEntry(
    val id: Long,
    val title: String,
    val encryptedContent: String,
    val verificationString: String,
    val timestamp: Long
)

class JournalRepository(
    private val journalDao: JournalDao
) {
    private val VERIFICATION_TEXT = "LIFE"

    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries().map { entities ->
        entities.map { entity ->
            JournalEntry(entity.id, entity.title, entity.encryptedContent, entity.verificationString, entity.timestamp)
        }
    }

    suspend fun getEntryForToday(): JournalEntry? = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        // This is a bit inefficient without a specific DAO query, but let's see
        // For simplicity, I'll just check the first one in allEntries if I had a list, 
        // but let's assume we want to enforce it.
        null // I'll add a DAO method if needed, but for now I'll just check in ViewModel
    }

    suspend fun insert(title: String, content: String, key: String) = withContext(Dispatchers.IO) {
        val encrypted = VigenereCipher.encrypt(content, key)
        val verification = VigenereCipher.encrypt(VERIFICATION_TEXT, key)
        val entity = JournalEntity(
            title = title,
            encryptedContent = encrypted,
            verificationString = verification,
            timestamp = System.currentTimeMillis()
        )
        journalDao.insert(entity)
    }

    suspend fun update(id: Long, title: String, content: String, key: String) = withContext(Dispatchers.IO) {
        val encrypted = VigenereCipher.encrypt(content, key)
        val verification = VigenereCipher.encrypt(VERIFICATION_TEXT, key)
        val entity = JournalEntity(
            id = id,
            title = title,
            encryptedContent = encrypted,
            verificationString = verification,
            timestamp = System.currentTimeMillis()
        )
        journalDao.update(entity)
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val entity = journalDao.getEntryById(id)
        if (entity != null) {
            journalDao.delete(entity)
        }
    }
    
    fun verifyKey(verificationString: String, key: String): Boolean {
        return VigenereCipher.decrypt(verificationString, key) == VERIFICATION_TEXT
    }
    
    fun decryptContent(encryptedContent: String, key: String): String {
        return VigenereCipher.decrypt(encryptedContent, key)
    }
}
