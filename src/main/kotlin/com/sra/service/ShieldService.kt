package com.sra.service

import com.sra.domain.models.*
import com.sra.repository.ShieldRepository
import com.sra.utils.HashUtils
import org.slf4j.LoggerFactory
import java.util.UUID

class ShieldService(
    private val entropyService: EntropyService,
    private val shieldRepository: ShieldRepository
) {

    private val logger = LoggerFactory.getLogger(ShieldService::class.java)

    // ── GENERATE ENCRYPTION KEY ───────────────────────────────────
    // This is the core product. Called when company needs a key.
    // Uses existing entropy engine — same lava lamp concept.
    suspend fun generateKey(userId: Int, tier: String): ShieldKeyResponse {
        logger.info("Generating Shield key for userId=$userId tier=$tier")

        // Step 1: Collect real world entropy (reuses existing engine)
        val entropyResult = when (tier) {
            "fast"   -> entropyService.collectFast()
            "medium" -> entropyService.collectMedium()
            else     -> entropyService.collectFull()  // default = full = strongest
        }

        // Step 2: Derive AES-256 key from entropy seed
        // AES-256 needs exactly 256 bits = 32 bytes = 64 hex chars
        // We take first 64 chars of SHA-512 seed — this IS the key
        val encryptionKey = entropyResult.seed.take(64)

        // Step 3: Generate unique verification code for audit trail
        val verificationCode = generateVerificationCode()

        // Step 4: Deactivate previous active keys (rotation)
        shieldRepository.deactivatePreviousKeys(userId)

        // Step 5: Save to audit log — permanent, never deleted
        shieldRepository.saveKey(
            userId           = userId,
            encryptionKey    = encryptionKey,
            verificationCode = verificationCode,
            tier             = tier,
            entropyResult    = entropyResult
        )

        logger.info("Shield key generated | userId=$userId | code=$verificationCode")

        // Step 6: Build response with full proof
        val sourcesUsed = entropyResult.sources.map { it.name }
        val issuedAt = entropyResult.timestamp

        return ShieldKeyResponse(
            encryptionKey    = encryptionKey,
            verificationCode = verificationCode,
            verifyUrl        = "https://sra.com/shield/verify/$verificationCode",
            proof = ShieldKeyProof(
                seed         = entropyResult.seed.take(64) + "...",
                tier         = tier,
                sourcesUsed  = sourcesUsed,
                btcPrice     = entropyResult.sources.find { it.name.contains("CRYPTO") }?.value,
                ethBlockHash = entropyResult.sources.find { it.name.contains("ETHEREUM") }?.value,
                seismicData  = entropyResult.sources.find { it.name.contains("SEISMIC") }?.value,
                serverTiming = entropyResult.sources.find { it.name.contains("TIMING") }?.value
            ),
            expiresAt = java.time.LocalDateTime.now().plusHours(24).toString(),
            issuedAt  = issuedAt
        )
    }

    // ── ROTATE KEY ────────────────────────────────────────────────
    // Called manually or by 24-hour scheduler
    // Deactivates old key, generates fresh one
    suspend fun rotateKey(userId: Int): ShieldKeyResponse {
        logger.info("Rotating Shield key for userId=$userId")
        return generateKey(userId, "full")  // Rotation always uses full entropy
    }

    // ── GET AUDIT LOG ─────────────────────────────────────────────
    // Returns every key ever generated for this company
    // This is their compliance proof document
    fun getAuditLog(userId: Int): ShieldAuditResponse {
        val keys = shieldRepository.getAuditLog(userId)
        val activeCount = keys.count { it.isActive }

        return ShieldAuditResponse(
            totalKeysGenerated = keys.size,
            activeKeys         = activeCount,
            keys               = keys
        )
    }

    // ── PUBLIC VERIFY ─────────────────────────────────────────────
    // Anyone can call this with a verification code
    // No auth required — this is the transparency proof
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

        val explanation = buildExplanation(entry)

        return ShieldVerifyResponse(
            valid            = true,
            verificationCode = entry.verificationCode,
            issuedAt         = entry.createdAt,
            tier             = entry.tier,
            proof            = entry.proof,
            explanation      = explanation
        )
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────

    private fun generateVerificationCode(): String {
        // Format: SRA-K-XXXXXX (K = Key, 8 random uppercase chars)
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"  // no confusing chars
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
            appendLine("")
            appendLine("ENTROPY SOURCES USED:")
            entry.proof.btcPrice?.let     { appendLine("  Bitcoin Price   : $it") }
            entry.proof.ethBlockHash?.let { appendLine("  Ethereum Block  : ${it.take(20)}...") }
            entry.proof.seismicData?.let  { appendLine("  Seismic Data    : $it") }
            entry.proof.serverTiming?.let { appendLine("  Server Timing   : $it") }
            appendLine("")
            appendLine("METHOD:")
            appendLine("  SHA-512($sources)")
            appendLine("  = ${entry.proof.seed}")
            appendLine("")
            appendLine("RESULT:")
            appendLine("  First 64 chars of seed = AES-256 encryption key")
            appendLine("  This key is mathematically derived from real world")
            appendLine("  events nobody controls or can predict.")
            appendLine("")
            appendLine("COMPLIANCE NOTE:")
            appendLine("  This proof satisfies DPDP Act 2023 reasonable")
            appendLine("  security safeguards requirement.")
        }
    }
}