package com.sra.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class EntropyLog(
    val id: Int,
    val giveawayId: Int? = null,
    val bitcoinPrice: String? = null,
    val serverTimingData: String? = null,
    val ethereumBlockHash: String? = null,
    val seismicData: String? = null,
    val rawCombinedString: String,
    val sha256Hash: String,
    val seedGenerated: String,
    val createdAt: String
)

@Serializable
data class EntropyResult(
    val seed: String,
    val sources: List<EntropySource>,
    val timestamp: String
)

@Serializable
data class EntropySource(
    val name: String,
    val value: String,
    val collectedAt: String
)