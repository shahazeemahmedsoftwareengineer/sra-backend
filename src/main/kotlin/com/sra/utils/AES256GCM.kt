package com.sra.utils

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AES256GCM {

    private const val GCM_TAG_LENGTH = 128  // bits
    private const val GCM_IV_LENGTH  = 12   // bytes

    // Encrypt plaintext using AES-256-GCM
    // keyHex = 64 hex chars = 32 bytes = 256 bits
    fun encrypt(plaintext: String, keyHex: String): String {
        require(keyHex.length == 64) { "Key must be 64 hex chars (256 bits)" }

        val keyBytes = hexToBytes(keyHex)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val paramSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Combine IV + ciphertext for storage
        val combined = iv + ciphertext
        return Base64.getUrlEncoder().withoutPadding().encodeToString(combined)
    }

    // Decrypt ciphertext using AES-256-GCM
    fun decrypt(encrypted: String, keyHex: String): String {
        require(keyHex.length == 64) { "Key must be 64 hex chars (256 bits)" }

        val keyBytes = hexToBytes(keyHex)
        val combined = Base64.getUrlDecoder().decode(encrypted)

        val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = combined.sliceArray(GCM_IV_LENGTH until combined.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val paramSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    fun hexToBytes(hex: String): ByteArray {
        check(hex.length % 2 == 0) { "Hex must have even length" }
        return ByteArray(hex.length / 2) {
            hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }
}