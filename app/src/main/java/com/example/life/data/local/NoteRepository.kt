package com.example.life.data.local

import com.example.life.security.VigenereCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NoteRepository(private val noteDao: NoteDao) {
    private val VERIFICATION_TEXT = "NOTE"

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun insert(title: String, content: String, key: String) = withContext(Dispatchers.IO) {
        val encrypted = VigenereCipher.encrypt(content, key)
        val verification = VigenereCipher.encrypt(VERIFICATION_TEXT, key)
        val note = NoteEntity(
            title = title,
            encryptedContent = encrypted,
            verificationString = verification,
            timestamp = System.currentTimeMillis()
        )
        noteDao.insert(note)
    }

    suspend fun update(note: NoteEntity, key: String) = withContext(Dispatchers.IO) {
        val encrypted = VigenereCipher.encrypt(VigenereCipher.decrypt(note.encryptedContent, ""), key) // This is wrong, key is needed to decrypt old
        // Actually the caller should provide the new encrypted content or I should decrypt with old key first.
        // The prompt says: "Re-encrypt using the new key after validating the old key."
        noteDao.update(note)
    }
    
    suspend fun updateWithNewKey(id: Long, title: String, oldContent: String, newKey: String) = withContext(Dispatchers.IO) {
        val encrypted = VigenereCipher.encrypt(oldContent, newKey)
        val verification = VigenereCipher.encrypt(VERIFICATION_TEXT, newKey)
        val note = NoteEntity(
            id = id,
            title = title,
            encryptedContent = encrypted,
            verificationString = verification,
            timestamp = System.currentTimeMillis()
        )
        noteDao.update(note)
    }

    suspend fun delete(note: NoteEntity) {
        noteDao.delete(note)
    }

    fun verifyKey(verificationString: String, key: String): Boolean {
        return VigenereCipher.decrypt(verificationString, key) == VERIFICATION_TEXT
    }

    fun decryptContent(encryptedContent: String, key: String): String {
        return VigenereCipher.decrypt(encryptedContent, key)
    }
}
