package com.sra.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Giveaway(
    val id: Int,
    val userId: Int,
    val title: String,
    val prizeDescription: String,
    val entryRules: String? = null,
    val status: String,
    val totalEntries: Int,
    val winnerEntryId: Int? = null,
    val startDate: String,
    val endDate: String? = null,
    val createdAt: String
)

@Serializable
data class CreateGiveawayRequest(
    val title: String,
    val prizeDescription: String,
    val entryRules: String? = null,
    val endDate: String? = null
)

@Serializable
data class GiveawayStatus(
    val id: Int,
    val title: String,
    val status: String,
    val totalEntries: Int,
    val entryLink: String
)