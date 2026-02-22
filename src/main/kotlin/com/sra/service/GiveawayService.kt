package com.sra.service

import com.sra.domain.models.*
import com.sra.repository.GiveawayRepository
import com.sra.utils.BadRequestException
import com.sra.utils.ForbiddenException
import com.sra.utils.NotFoundException
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GiveawayService(private val giveawayRepository: GiveawayRepository) {

    private val logger = LoggerFactory.getLogger(GiveawayService::class.java)

    fun create(userId: Int, request: CreateGiveawayRequest): Giveaway {
        if (request.title.isBlank()) throw BadRequestException("Title is required")
        if (request.prizeDescription.isBlank()) throw BadRequestException("Prize description is required")

        val endDate = request.endDate?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }

        val giveaway = giveawayRepository.create(
            userId = userId,
            title = request.title.trim(),
            prizeDescription = request.prizeDescription.trim(),
            entryRules = request.entryRules?.trim(),
            endDate = endDate
        )

        logger.info("Giveaway created: ${giveaway.id} by user: $userId")
        return giveaway
    }

    fun getById(id: Int, userId: Int): Giveaway {
        val giveaway = giveawayRepository.findById(id)
            ?: throw NotFoundException("Giveaway not found")
        if (giveaway.userId != userId) throw ForbiddenException("Access denied")
        return giveaway
    }

    fun getPublicById(id: Int): Giveaway {
        return giveawayRepository.findById(id)
            ?: throw NotFoundException("Giveaway not found")
    }

    fun getUserGiveaways(userId: Int): List<Giveaway> {
        return giveawayRepository.findByUserId(userId)
    }

    fun getStatus(id: Int, userId: Int, baseUrl: String): GiveawayStatus {
        val giveaway = getById(id, userId)
        return GiveawayStatus(
            id = giveaway.id,
            title = giveaway.title,
            status = giveaway.status,
            totalEntries = giveaway.totalEntries,
            entryLink = "$baseUrl/enter/${giveaway.id}"
        )
    }
}