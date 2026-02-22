package com.sra.service

import com.sra.domain.models.EntropyResult
import com.sra.domain.tables.EntropyLogsTable
import com.sra.entropy.EntropyPool
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class EntropyService(private val entropyPool: EntropyPool) {

    private val logger = LoggerFactory.getLogger(EntropyService::class.java)
    private val json = Json { prettyPrint = true }

    suspend fun collectFast(): EntropyResult {
        logger.info("Starting fast entropy collection")
        return entropyPool.collectFast()
    }

    suspend fun collectMedium(): EntropyResult {
        logger.info("Starting medium entropy collection — waiting 12 seconds")
        delay(12_000)
        return entropyPool.collectMedium()
    }

    suspend fun collectFull(): EntropyResult {
        logger.info("Starting full entropy collection — waiting 60 seconds")
        delay(60_000)
        return entropyPool.collectFull()
    }

    fun logToDatabase(giveawayId: Int, result: EntropyResult): Int {
        return transaction {
            EntropyLogsTable.insert {
                it[EntropyLogsTable.giveawayId] = giveawayId
                it[EntropyLogsTable.serverTimingData] = result.sources.find { s ->
                    s.name == "SERVER_TIMING"
                }?.value
                it[EntropyLogsTable.bitcoinPrice] = result.sources.find { s ->
                    s.name == "CRYPTO_PRICE_BINANCE"
                }?.value
                it[EntropyLogsTable.ethereumBlockHash] = result.sources.find { s ->
                    s.name == "ETHEREUM_BLOCK"
                }?.value
                it[EntropyLogsTable.seismicData] = result.sources.find { s ->
                    s.name == "USGS_SEISMIC"
                }?.value
                it[EntropyLogsTable.rawCombinedString] = result.sources.joinToString("|") { s -> s.value }
                it[EntropyLogsTable.sha256Hash] = result.seed.take(64)
                it[EntropyLogsTable.seedGenerated] = result.seed
                it[EntropyLogsTable.createdAt] = LocalDateTime.now()
            }[EntropyLogsTable.id]
        }
    }
}
