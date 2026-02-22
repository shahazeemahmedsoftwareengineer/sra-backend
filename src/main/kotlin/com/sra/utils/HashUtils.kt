package com.sra.utils

import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest

object HashUtils {

    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(12))
    }

    fun verifyPassword(password: String, hashed: String): Boolean {
        return BCrypt.checkpw(password, hashed)
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun sha512(input: String): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun generateProofCode(): String {
        val timestamp = System.nanoTime().toString()
        val random = java.util.UUID.randomUUID().toString()
        return sha256("$timestamp-$random").take(16).uppercase()
    }
}