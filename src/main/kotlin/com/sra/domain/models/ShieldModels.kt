package com.sra.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class ShieldKey(
    val id: Int,
    val publicKey: String,
    val encryptedPrivateKey: String,
    val createdAt: String
)

@Serializable
data class NewShieldKeyRequest(
    val identity: String // User-provided identifier
)

@Serializable
data class ShieldedData(
    val keyId: String,
    val ciphertext: String,
    val signature: String
)

@Serializable
data class UnshieldedDataRequest(
    val shieldedData: ShieldedData
)