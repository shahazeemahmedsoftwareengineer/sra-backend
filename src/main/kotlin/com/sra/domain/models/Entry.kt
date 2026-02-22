package com.sra.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Entry(
    val id: Int,
    val giveawayId: Int,
    val participantName: String,
    val participantEmail: String? = null,
    val socialHandle: String? = null,
    val entryCount: Int,
    val createdAt: String
)

@Serializable
data class SubmitEntryRequest(
    val participantName: String,
    val participantEmail: String? = null,
    val socialHandle: String? = null
)