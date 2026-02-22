package com.sra.entropy

import com.sra.utils.HashUtils
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

object EntropyMixer {

    private val logger = LoggerFactory.getLogger(EntropyMixer::class.java)

    // Global atomic counter — never resets, never repeats
    // AtomicLong = thread safe, handles 1000 simultaneous draws
    private val drawCounter = AtomicLong(System.currentTimeMillis())

    fun mix(sources: List<String>): String {
        require(sources.isNotEmpty()) { "At least one entropy source required" }

        val counter = drawCounter.incrementAndGet()
        val drawId = UUID.randomUUID().toString()
        val nanoTime = System.nanoTime()

        val combined = sources.joinToString("|") +
                "|nanotime:$nanoTime" +
                "|counter:$counter" +
                "|drawid:$drawId"

        val hash = HashUtils.sha512(combined)
        logger.debug("Mixed ${sources.size} sources | counter:$counter | draw:$drawId")
        return hash
    }

    fun seedToWinnerPosition(seed: String, totalParticipants: Int): Int {
        require(totalParticipants > 0) { "Total participants must be greater than 0" }

        val bigInt = try {
            BigInteger(seed.take(32), 16)
        } catch (e: NumberFormatException) {
            BigInteger(seed.toByteArray())
        }

        val position = (bigInt.mod(BigInteger.valueOf(totalParticipants.toLong()))).toInt()
        logger.debug("Seed mapped to position $position of $totalParticipants")
        return position
    }

    fun getCurrentCounter(): Long = drawCounter.get()

    fun buildCalculationDetails(
        seed: String,
        totalParticipants: Int,
        winnerPosition: Int,
        winnerName: String,
        counter: Long,
        drawId: String
    ): String {
        return """
            SEED: $seed
            TOTAL_PARTICIPANTS: $totalParticipants
            CALCULATION: seed(first 32 chars) mod $totalParticipants = $winnerPosition
            WINNER_POSITION: $winnerPosition
            WINNER_NAME: $winnerName
            DRAW_COUNTER: $counter
            DRAW_ID: $drawId
            VERIFICATION: Anyone can verify using seed + participant list
        """.trimIndent()
    }
}