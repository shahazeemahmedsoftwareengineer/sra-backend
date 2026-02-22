package com.sra.service

import com.sra.domain.models.*
import com.sra.entropy.EntropyMixer
import com.sra.repository.EntryRepository
import com.sra.repository.GiveawayRepository
import com.sra.utils.BadRequestException
import com.sra.utils.ForbiddenException
import com.sra.utils.NotFoundException
import org.slf4j.LoggerFactory
import java.util.UUID

class WinnerService(
    private val giveawayRepository: GiveawayRepository,
    private val entryRepository: EntryRepository,
    private val entropyService: EntropyService,
    private val proofService: ProofService
) {

    private val logger = LoggerFactory.getLogger(WinnerService::class.java)

    suspend fun drawWinner(giveawayId: Int, userId: Int, tier: String = "fast"): Proof {
        val giveaway = giveawayRepository.findById(giveawayId)
            ?: throw NotFoundException("Giveaway not found")

        if (giveaway.userId != userId) throw ForbiddenException("Access denied")
        if (giveaway.status != "active") throw BadRequestException("Giveaway is not active")

        val entries = entryRepository.findByGiveawayId(giveawayId)
        if (entries.isEmpty()) throw BadRequestException("No entries found for this giveaway")

        // Unique draw ID for this specific draw
        val drawId = UUID.randomUUID().toString()
        val currentCounter = EntropyMixer.getCurrentCounter()

        logger.info("Drawing winner | giveaway:$giveawayId | entries:${entries.size} | tier:$tier | drawId:$drawId")

        val entropyResult = when (tier) {
            "medium" -> entropyService.collectMedium()
            "full"   -> entropyService.collectFull()
            else     -> entropyService.collectFast()
        }

        entropyService.logToDatabase(giveawayId, entropyResult)

        val winnerPosition = EntropyMixer.seedToWinnerPosition(
            seed = entropyResult.seed,
            totalParticipants = entries.size
        )

        val winner = entries[winnerPosition]
        giveawayRepository.updateWinner(giveawayId, winner.id)

        val calculationDetails = EntropyMixer.buildCalculationDetails(
            seed = entropyResult.seed,
            totalParticipants = entries.size,
            winnerPosition = winnerPosition,
            winnerName = winner.participantName,
            counter = currentCounter,
            drawId = drawId
        )

        val proof = proofService.create(
            giveawayId = giveawayId,
            winnerEntryId = winner.id,
            entropyResult = entropyResult,
            calculationDetails = calculationDetails
        )

        logger.info("Winner selected | giveaway:$giveawayId | winner:${winner.participantName} | position:$winnerPosition")
        return proof
    }
}
