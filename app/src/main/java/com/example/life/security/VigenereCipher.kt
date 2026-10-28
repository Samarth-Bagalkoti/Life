package com.example.life.security

object VigenereCipher {
    fun encrypt(text: String, key: String): String {
        if (key.isEmpty()) return text
        val result = StringBuilder()
        var keyIndex = 0
        for (char in text) {
            if (char.isLetter()) {
                val base = if (char.isUpperCase()) 'A' else 'a'
                val keyChar = key[keyIndex % key.length]
                val shift = if (keyChar.isUpperCase()) keyChar - 'A' else keyChar - 'a'
                val encryptedChar = ((char - base + shift) % 26 + base.code).toChar()
                result.append(encryptedChar)
                keyIndex++
            } else {
                result.append(char)
            }
        }
        return result.toString()
    }

    fun decrypt(text: String, key: String): String {
        if (key.isEmpty()) return text
        val result = StringBuilder()
        var keyIndex = 0
        for (char in text) {
            if (char.isLetter()) {
                val base = if (char.isUpperCase()) 'A' else 'a'
                val keyChar = key[keyIndex % key.length]
                val shift = if (keyChar.isUpperCase()) keyChar - 'A' else keyChar - 'a'
                val decryptedChar = ((char - base - shift + 26) % 26 + base.code).toChar()
                result.append(decryptedChar)
                keyIndex++
            } else {
                result.append(char)
            }
        }
        return result.toString()
    }
}
