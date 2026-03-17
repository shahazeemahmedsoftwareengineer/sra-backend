package com.sra.utils

import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

object HashUtils {

    private val secureRandom = SecureRandom()
    private const val GCM_IV_LENGTH   = 12   // 96 bits — GCM standard
    private const val GCM_TAG_LENGTH  = 128  // 128 bits — strongest GCM tag

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

    // ── AES-256-GCM KEY DERIVATION ────────────────────────────────
    // Takes entropy seed (hex string of any length)
    // Returns exactly 32 bytes (256 bits) suitable for AES-256
    // Uses SHA-256 so output is always exactly the right size
    fun deriveAES256Key(entropySeed: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(entropySeed.toByteArray(Charsets.UTF_8))
        // SHA-256 always outputs exactly 32 bytes = AES-256 key ✅
    }

    // ── AES-256-GCM ENCRYPT ───────────────────────────────────────
    // plaintext  : any string you want to protect
    // keyBytes   : 32-byte key from deriveAES256Key()
    // Returns    : base64(iv[12] + ciphertext + gcmTag[16])
    //              IV is prepended so we can extract it on decrypt
    fun encryptAES256GCM(plaintext: String, keyBytes: ByteArray): String {
        require(keyBytes.size == 32) { "AES-256 key must be 32 bytes" }

        // Random 96-bit IV — never reuse IV with same key
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val paramSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Combine IV + ciphertext into one array then base64 encode
        val combined = iv + ciphertext
        return Base64.getEncoder().encodeToString(combined)
    }

    // ── AES-256-GCM DECRYPT ───────────────────────────────────────
    // encrypted  : base64 string from encryptAES256GCM()
    // keyBytes   : same 32-byte key used to encrypt
    // Returns    : original plaintext string
    // Throws     : exception if key is wrong or data tampered (GCM auth)
    fun decryptAES256GCM(encrypted: String, keyBytes: ByteArray): String {
        require(keyBytes.size == 32) { "AES-256 key must be 32 bytes" }

        val combined = Base64.getDecoder().decode(encrypted)

        // Extract IV from first 12 bytes
        val iv         = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val paramSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)

        // GCM automatically verifies authentication tag here
        // If data was tampered → throws AEADBadTagException
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    // ── KEY TO HEX ────────────────────────────────────────────────
    // Converts raw key bytes to hex string for storage/display
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ── HEX TO KEY ────────────────────────────────────────────────
    // Converts stored hex key back to bytes for crypto operations
    fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}