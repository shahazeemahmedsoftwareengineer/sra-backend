package com.sra.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Proof(
    val id: Int,
    val giveawayId: Int,
    val winnerEntryId: Int,
    val publicCode: String,
    val seedGenerated: String,
    val sourcesUsed: String,
    val calculationDetails: String,
    val isPublic: Boolean,
    val createdAt: String,
    val publicUrl: String
)

@Serializable
data class PublicProof(
    val giveawayTitle: String,
    val winnerName: String,
    val totalParticipants: Int,
    val winnerPosition: Int,
    val seedGenerated: String,
    val sourcesUsed: String,
    val calculationDetails: String,
    val drawnAt: String,
    val verificationCode: String
)