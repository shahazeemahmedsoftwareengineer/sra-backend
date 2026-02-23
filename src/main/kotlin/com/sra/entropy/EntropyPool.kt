package com.sra.entropy

import com.sra.domain.models.EntropyResult
import com.sra.domain.models.EntropySource
import com.sra.entropy.sources.*
import com.sra.utils.HashUtils
import io.ktor.client.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

class EntropyPool(private val httpClient: HttpClient) {

    private val logger = LoggerFactory.getLogger(EntropyPool::class.java)

    private val serverTimingSource = ServerTimingSource()
    private val cryptoPriceSource = CryptoPriceSource(httpClient)
    private val ethereumSource = EthereumSource(httpClient)
    private val seismicSource = SeismicSource(httpClient)

    // Rolling pool — accumulates entropy continuously
    private val rollingPool = AtomicReference<String>(
        HashUtils.sha512("SRA-INIT-${System.nanoTime()}-${java.util.UUID.randomUUID()}")
    )

    private var backgroundJob: Job? = null

    // Start background entropy collection
    fun startBackgroundCollection(scope: CoroutineScope) {
        logger.info("Starting background entropy collection...")

        backgroundJob = scope.launch {

            // Every 5 seconds — server timing
            launch {
                while (isActive) {
                    try {
                        val timing = serverTimingSource.collect()
                        mixIntoPool(timing, "SERVER_TIMING")
                    } catch (e: Exception) {
                        logger.warn("Background timing collection failed: ${e.message}")
                    }
                    delay(5_000)
                }
            }

            // Every 30 seconds — crypto price
            launch {
                while (isActive) {
                    try {
                        val price = cryptoPriceSource.collect()
                        mixIntoPool(price, "CRYPTO_PRICE")
                    } catch (e: Exception) {
                        logger.warn("Background crypto collection failed: ${e.message}")
                    }
                    delay(30_000)
                }
            }

            // Every 15 seconds — ethereum block
            launch {
                while (isActive) {
                    try {
                        val eth = ethereumSource.collect()
                        mixIntoPool(eth, "ETHEREUM")
                    } catch (e: Exception) {
                        logger.warn("Background ethereum collection failed: ${e.message}")
                    }
                    delay(15_000)
                }
            }

            // Every 5 minutes — seismic
            launch {
                while (isActive) {
                    try {
                        val seismic = seismicSource.collect()
                        mixIntoPool(seismic, "SEISMIC")
                    } catch (e: Exception) {
                        logger.warn("Background seismic collection failed: ${e.message}")
                    }
                    delay(300_000)
                }
            }
        }

        logger.info("Background entropy collection started")
    }

    fun stopBackgroundCollection() {
        backgroundJob?.cancel()
        logger.info("Background entropy collection stopped")
    }

    private fun mixIntoPool(newEntropy: String, source: String) {
        val current = rollingPool.get()
        val mixed = HashUtils.sha512(
            "$current|$newEntropy|$source|${System.nanoTime()}"
        )
        rollingPool.set(mixed)
        logger.debug("Pool updated from $source")
    }

    fun recordRequest() {
        serverTimingSource.recordTiming()
        // Each request also mixes into pool
        mixIntoPool(System.nanoTime().toString(), "REQUEST_TIMING")
    }

    suspend fun collectFast(): EntropyResult {
        logger.info("Collecting fast entropy using rolling pool")
        val timestamp = LocalDateTime.now().toString()

        val poolState = rollingPool.get()
        val serverTiming = serverTimingSource.collect()
        val cryptoPrice = cryptoPriceSource.collect()

        // Mix pool state with fresh sources
        val finalSeed = HashUtils.sha512(
            "$poolState|$serverTiming|$cryptoPrice|${System.nanoTime()}"
        )

        val sources = listOf(
            EntropySource("ROLLING_POOL", poolState.take(32) + "...", timestamp),
            EntropySource(serverTimingSource.getSourceName(), serverTiming, timestamp),
            EntropySource(cryptoPriceSource.getSourceName(), cryptoPrice, timestamp)
        )

        return EntropyResult(seed = finalSeed, sources = sources, timestamp = timestamp)
    }

    suspend fun collectMedium(): EntropyResult {
        logger.info("Collecting medium entropy using rolling pool")
        val timestamp = LocalDateTime.now().toString()

        val poolState = rollingPool.get()
        val serverTiming = serverTimingSource.collect()
        val cryptoPrice = cryptoPriceSource.collect()
        val ethereum = ethereumSource.collect()

        val finalSeed = HashUtils.sha512(
            "$poolState|$serverTiming|$cryptoPrice|$ethereum|${System.nanoTime()}"
        )

        val sources = listOf(
            EntropySource("ROLLING_POOL", poolState.take(32) + "...", timestamp),
            EntropySource(serverTimingSource.getSourceName(), serverTiming, timestamp),
            EntropySource(cryptoPriceSource.getSourceName(), cryptoPrice, timestamp),
            EntropySource(ethereumSource.getSourceName(), ethereum, timestamp)
        )

        return EntropyResult(seed = finalSeed, sources = sources, timestamp = timestamp)
    }

    suspend fun collectFull(): EntropyResult {
        logger.info("Collecting full entropy using rolling pool")
        val timestamp = LocalDateTime.now().toString()

        val poolState = rollingPool.get()
        val serverTiming = serverTimingSource.collect()
        val cryptoPrice = cryptoPriceSource.collect()
        val ethereum = ethereumSource.collect()
        val seismic = seismicSource.collect()

        val finalSeed = HashUtils.sha512(
            "$poolState|$serverTiming|$cryptoPrice|$ethereum|$seismic|${System.nanoTime()}"
        )

        val sources = listOf(
            EntropySource("ROLLING_POOL", poolState.take(32) + "...", timestamp),
            EntropySource(serverTimingSource.getSourceName(), serverTiming, timestamp),
            EntropySource(cryptoPriceSource.getSourceName(), cryptoPrice, timestamp),
            EntropySource(ethereumSource.getSourceName(), ethereum, timestamp),
            EntropySource(seismicSource.getSourceName(), seismic, timestamp)
        )

        return EntropyResult(seed = finalSeed, sources = sources, timestamp = timestamp)
    }
}