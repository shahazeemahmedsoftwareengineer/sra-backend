package com.sra.service

import com.sra.domain.models.*
import com.sra.repository.EntryRepository
import com.sra.repository.GiveawayRepository
import com.sra.repository.ProofRepository
import com.sra.utils.HashUtils
import com.sra.utils.NotFoundException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ProofService(
    private val proofRepository: ProofRepository,
    private val giveawayRepository: GiveawayRepository,
    private val entryRepository: EntryRepository
) {

    private val logger = LoggerFactory.getLogger(ProofService::class.java)
    private val json = Json { prettyPrint = true }

    fun create(
        giveawayId: Int,
        winnerEntryId: Int,
        entropyResult: EntropyResult,
        calculationDetails: String
    ): Proof {
        val publicCode = HashUtils.generateProofCode()
        val sourcesJson = json.encodeToString(entropyResult.sources)

        val proof = proofRepository.create(
            giveawayId = giveawayId,
            winnerEntryId = winnerEntryId,
            publicCode = publicCode,
            seedGenerated = entropyResult.seed,
            sourcesUsed = sourcesJson,
            calculationDetails = calculationDetails
        )

        logger.info("Proof created: ${proof.publicCode} for giveaway: $giveawayId")
        return proof
    }

    fun getPublicProof(code: String): PublicProof {
        val proof = proofRepository.findByPublicCode(code)
            ?: throw NotFoundException("Proof not found")

        val giveaway = giveawayRepository.findById(proof.giveawayId)
            ?: throw NotFoundException("Giveaway not found")

        val winner = entryRepository.findById(proof.winnerEntryId)
            ?: throw NotFoundException("Winner entry not found")

        val totalParticipants = entryRepository.countByGiveawayId(proof.giveawayId)

        return PublicProof(
            giveawayTitle = giveaway.title,
            winnerName = winner.participantName,
            totalParticipants = totalParticipants,
            winnerPosition = extractWinnerPosition(proof.calculationDetails),
            seedGenerated = proof.seedGenerated,
            sourcesUsed = proof.sourcesUsed,
            calculationDetails = proof.calculationDetails,
            drawnAt = proof.createdAt,
            verificationCode = proof.publicCode
        )
    }

    fun getProofByGiveaway(giveawayId: Int): Proof? {
        return proofRepository.findByGiveawayId(giveawayId)
    }

    private fun extractWinnerPosition(calculationDetails: String): Int {
        return try {
            val line = calculationDetails.lines().find { it.contains("WINNER_POSITION") }
            line?.substringAfter(":")?.trim()?.toInt() ?: 0
        } catch (e: Exception) { 0 }
    }
}