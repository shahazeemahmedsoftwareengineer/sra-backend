package com.sra.domain.models

import kotlinx.serialization.Serializable

// ── REQUEST MODELS ──────────────────────────────────────────────

@Serializable
data class GenerateKeyRequest(
    val tier: String = "full"   // "fast" | "medium" | "full"
)

@Serializable
data class RotateKeyRequest(
    val reason: String = "manual"
)

@Serializable
data class EncryptRequest(val plaintext: String)

@Serializable
data class DecryptRequest(val encrypted: String)

// ── RESPONSE MODELS ─────────────────────────────────────────────

@Serializable
data class ShieldKeyResponse(
    val encryptionKey: String,       // AES-256 key — company uses this to encrypt DB
    val verificationCode: String,    // Public proof code e.g. "SRA-K-X7K9M3"
    val verifyUrl: String,           // Anyone can check this URL
    val proof: ShieldKeyProof,
    val expiresAt: String,           // Key rotates after 24 hours
    val issuedAt: String
)

@Serializable
data class ShieldKeyProof(
    val seed: String,
    val tier: String,
    val sourcesUsed: List<String>,
    val btcPrice: String?,
    val ethBlockHash: String?,
    val seismicData: String?,
    val serverTiming: String?
)

@Serializable
data class ShieldAuditEntry(
    val id: Int,
    val verificationCode: String,
    val tier: String,
    val isActive: Boolean,
    val createdAt: String,
    val expiresAt: String?,
    val rotatedAt: String?,
    val proof: ShieldKeyProof
)

@Serializable
data class ShieldAuditResponse(
    val totalKeysGenerated: Int,
    val activeKeys: Int,
    val keys: List<ShieldAuditEntry>
)

@Serializable
data class ShieldVerifyResponse(
    val valid: Boolean,
    val verificationCode: String,
    val issuedAt: String,
    val tier: String,
    val proof: ShieldKeyProof,
    val explanation: String
)

// ── INTERNAL MODELS ─────────────────────────────────────────────

data class InternalActiveKey(
    val encryptionKey: String,
    val verificationCode: String
)