package com.sra.service

import com.sra.domain.models.*
import com.sra.repository.ShieldRepository
import com.sra.utils.HashUtils
import org.slf4j.LoggerFactory

class ShieldService(
    private val entropyService: EntropyService,
    private val shieldRepository: ShieldRepository
) {

    private val logger = LoggerFactory.getLogger(ShieldService::class.java)

    // ── GENERATE ENCRYPTION KEY ───────────────────────────────────
    suspend fun generateKey(userId: Int, tier: String): ShieldKeyResponse {
        logger.info("Generating Shield key for userId=$userId tier=$tier")

        // Step 1: Collect real world entropy
        val entropyResult = when (tier) {
            "fast"   -> entropyService.collectFast()
            "medium" -> entropyService.collectMedium()
            else     -> entropyService.collectFull()
        }

        // Step 2: Derive proper AES-256 key (32 bytes) from entropy seed
        // SHA-256(seed) → exactly 256 bits, proper key material
        val keyBytes      = HashUtils.deriveAES256Key(entropyResult.seed)
        val encryptionKey = HashUtils.bytesToHex(keyBytes)  // 64-char hex for storage

        // Step 3: Verification code for audit trail
        val verificationCode = generateVerificationCode()

        // Step 4: Deactivate previous keys (rotation)
        shieldRepository.deactivatePreviousKeys(userId)

        // Step 5: Save to audit log
        shieldRepository.saveKey(
            userId           = userId,
            encryptionKey    = encryptionKey,
            verificationCode = verificationCode,
            tier             = tier,
            entropyResult    = entropyResult
        )

        logger.info("Shield key generated | userId=$userId | code=$verificationCode | algo=AES-256-GCM")

        return ShieldKeyResponse(
            encryptionKey    = encryptionKey,
            verificationCode = verificationCode,
            verifyUrl        = "https://sra.com/shield/verify/$verificationCode",
            proof = ShieldKeyProof(
                seed         = entropyResult.seed.take(64) + "...",
                tier         = tier,
                sourcesUsed  = entropyResult.sources.map { it.name },
                btcPrice     = entropyResult.sources.find { it.name.contains("CRYPTO") }?.value,
                ethBlockHash = entropyResult.sources.find { it.name.contains("ETHEREUM") }?.value,
                seismicData  = entropyResult.sources.find { it.name.contains("SEISMIC") }?.value,
                serverTiming = entropyResult.sources.find { it.name.contains("TIMING") }?.value
            ),
            expiresAt = java.time.LocalDateTime.now().plusHours(24).toString(),
            issuedAt  = entropyResult.timestamp
        )
    }

    // ── ENCRYPT DATA ──────────────────────────────────────────────
    // Client calls this with their plaintext + active key
    // Returns AES-256-GCM encrypted ciphertext
    fun encrypt(userId: Int, plaintext: String): ShieldEncryptResponse {
        logger.info("Encrypting data for userId=$userId")

        // Get user's active key from database
        val activeKey = shieldRepository.getActiveKey(userId)
            ?: throw IllegalStateException("No active key found. Generate a key first.")

        // Convert stored hex key back to bytes
        val keyBytes = HashUtils.hexToBytes(activeKey.encryptionKey)

        // Encrypt using AES-256-GCM
        val encrypted = HashUtils.encryptAES256GCM(plaintext, keyBytes)

        logger.info("Data encrypted | userId=$userId | algo=AES-256-GCM")

        return ShieldEncryptResponse(
            encrypted       = encrypted,
            algorithm       = "AES-256-GCM",
            keyCode         = activeKey.verificationCode,
            encryptedAt     = java.time.LocalDateTime.now().toString()
        )
    }

    // ── DECRYPT DATA ──────────────────────────────────────────────
    // Client calls this with ciphertext + same key used to encrypt
    // GCM authentication tag automatically detects tampering
    fun decrypt(userId: Int, encrypted: String): ShieldDecryptResponse {
        logger.info("Decrypting data for userId=$userId")

        val activeKey = shieldRepository.getActiveKey(userId)
            ?: throw IllegalStateException("No active key found.")

        val keyBytes = HashUtils.hexToBytes(activeKey.encryptionKey)

        return try {
            val plaintext = HashUtils.decryptAES256GCM(encrypted, keyBytes)
            logger.info("Data decrypted successfully | userId=$userId")
            ShieldDecryptResponse(
                plaintext   = plaintext,
                algorithm   = "AES-256-GCM",
                verified    = true,
                decryptedAt = java.time.LocalDateTime.now().toString()
            )
        } catch (e: Exception) {
            // GCM auth failure = data was tampered or wrong key
            logger.warn("Decryption failed for userId=$userId — possible tampering: ${e.message}")
            ShieldDecryptResponse(
                plaintext   = "",
                algorithm   = "AES-256-GCM",
                verified    = false,
                decryptedAt = java.time.LocalDateTime.now().toString()
            )
        }
    }

    // ── ROTATE KEY ────────────────────────────────────────────────
    suspend fun rotateKey(userId: Int): ShieldKeyResponse {
        logger.info("Rotating Shield key for userId=$userId")
        return generateKey(userId, "full")
    }

    // ── GET AUDIT LOG ─────────────────────────────────────────────
    fun getAuditLog(userId: Int): ShieldAuditResponse {
        val keys = shieldRepository.getAuditLog(userId)
        return ShieldAuditResponse(
            totalKeysGenerated = keys.size,
            activeKeys         = keys.count { it.isActive },
            keys               = keys
        )
    }

    // ── PUBLIC VERIFY ─────────────────────────────────────────────
    fun verifyKey(verificationCode: String): ShieldVerifyResponse {
        val entry = shieldRepository.getByVerificationCode(verificationCode)
            ?: return ShieldVerifyResponse(
                valid            = false,
                verificationCode = verificationCode,
                issuedAt         = "",
                tier             = "",
                proof            = ShieldKeyProof("", "", emptyList(), null, null, null, null),
                explanation      = "Verification code not found"
            )

        return ShieldVerifyResponse(
            valid            = true,
            verificationCode = entry.verificationCode,
            issuedAt         = entry.createdAt,
            tier             = entry.tier,
            proof            = entry.proof,
            explanation      = buildExplanation(entry)
        )
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────

    private fun generateVerificationCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = (1..8).map { chars.random() }.joinToString("")
        return "SRA-K-$random"
    }

    private fun buildExplanation(entry: ShieldAuditEntry): String {
        val sources = entry.proof.sourcesUsed.joinToString(" + ")
        return buildString {
            appendLine("KEY GENERATION PROOF")
            appendLine("====================")
            appendLine("Verification Code : ${entry.verificationCode}")
            appendLine("Generated At      : ${entry.createdAt}")
            appendLine("Entropy Tier      : ${entry.tier.uppercase()}")
            appendLine("Algorithm         : AES-256-GCM")
            appendLine("")
            appendLine("ENTROPY SOURCES USED:")
            entry.proof.btcPrice?.let     { appendLine("  Bitcoin Price   : $it") }
            entry.proof.ethBlockHash?.let { appendLine("  Ethereum Block  : ${it.take(20)}...") }
            entry.proof.seismicData?.let  { appendLine("  Seismic Data    : $it") }
            entry.proof.serverTiming?.let { appendLine("  Server Timing   : $it") }
            appendLine("")
            appendLine("METHOD:")
            appendLine("  SHA-256($sources) → 32-byte AES-256 key")
            appendLine("  AES-256-GCM with random 96-bit IV per encryption")
            appendLine("  GCM authentication tag detects any data tampering")
            appendLine("")
            appendLine("COMPLIANCE NOTE:")
            appendLine("  AES-256-GCM satisfies DPDP Act 2023 and NIST SP 800-38D.")
        }
    }
}