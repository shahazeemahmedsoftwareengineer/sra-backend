package com.sra.service

import com.sra.domain.models.*
import com.sra.repository.EntryRepository
import com.sra.repository.GiveawayRepository
import com.sra.utils.BadRequestException
import com.sra.utils.NotFoundException
import org.slf4j.LoggerFactory

class EntryService(
    private val entryRepository: EntryRepository,
    private val giveawayRepository: GiveawayRepository
) {

    private val logger = LoggerFactory.getLogger(EntryService::class.java)

    fun submitEntry(giveawayId: Int, request: SubmitEntryRequest): Entry {
        if (request.participantName.isBlank()) throw BadRequestException("Name is required")

        val giveaway = giveawayRepository.findById(giveawayId)
            ?: throw NotFoundException("Giveaway not found")

        if (giveaway.status != "active") throw BadRequestException("Giveaway is not active")

        if (request.participantEmail != null) {
            val alreadyEntered = entryRepository.existsByEmail(giveawayId, request.participantEmail)
            if (alreadyEntered) throw BadRequestException("This email has already entered")
        }

        val entry = entryRepository.create(
            giveawayId = giveawayId,
            participantName = request.participantName.trim(),
            participantEmail = request.participantEmail?.lowercase()?.trim(),
            socialHandle = request.socialHandle?.trim()
        )

        giveawayRepository.incrementEntryCount(giveawayId)
        logger.info("Entry submitted for giveaway: $giveawayId by: ${request.participantName}")

        return entry
    }

    fun getGiveawayEntries(giveawayId: Int): List<Entry> {
        return entryRepository.findByGiveawayId(giveawayId)
    }

    fun getEntryCount(giveawayId: Int): Int {
        return entryRepository.countByGiveawayId(giveawayId)
    }
}