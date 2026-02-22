package com.sra.entropy

import com.sra.domain.models.EntropyResult
import com.sra.domain.models.EntropySource
import com.sra.entropy.sources.*
import io.ktor.client.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class EntropyPool(private val httpClient: HttpClient) {

    private val logger = LoggerFactory.getLogger(EntropyPool::class.java)

    private val serverTimingSource = ServerTimingSource()
    private val cryptoPriceSource = CryptoPriceSource(httpClient)
    private val ethereumSource = EthereumSource(httpClient)
    private val seismicSource = SeismicSource(httpClient)

    fun recordRequest() {
        serverTimingSource.recordTiming()
    }

    suspend fun collectFast(): EntropyResult {
        logger.info("Collecting fast entropy (1 second tier)")
        val timestamp = LocalDateTime.now().toString()

        val serverTiming = serverTimingSource.collect()
        val cryptoPrice = cryptoPriceSource.collect()

        val sources = listOf(
            EntropySource(serverTimingSource.getSourceName(), serverTiming, timestamp),
            EntropySource(cryptoPriceSource.getSourceName(), cryptoPrice, timestamp)
        )

        val seed = EntropyMixer.mix(listOf(serverTiming, cryptoPrice))

        return EntropyResult(
            seed = seed,
            sources = sources,
            timestamp = timestamp
        )
    }

    suspend fun collectMedium(): EntropyResult {
        logger.info("Collecting medium entropy (12 second tier)")
        val timestamp = LocalDateTime.now().toString()

        val serverTiming = serverTimingSource.collect()
        val cryptoPrice = cryptoPriceSource.collect()
        val ethereum = ethereumSource.collect()

        val sources = listOf(
            EntropySource(serverTimingSource.getSourceName(), serverTiming, timestamp),
            EntropySource(cryptoPriceSource.getSourceName(), cryptoPrice, timestamp),
            EntropySource(ethereumSource.getSourceName(), ethereum, timestamp)
        )

        val seed = EntropyMixer.mix(listOf(serverTiming, cryptoPrice, ethereum))

        return EntropyResult(
            seed = seed,
            sources = sources,
            timestamp = timestamp
        )
    }

    suspend fun collectFull(): EntropyResult {
        logger.info("Collecting full entropy (1 minute tier)")
        val timestamp = LocalDateTime.now().toString()

        val serverTiming = serverTimingSource.collect()
        val cryptoPrice = cryptoPriceSource.collect()
        val ethereum = ethereumSource.collect()
        val seismic = seismicSource.collect()

        val sources = listOf(
            EntropySource(serverTimingSource.getSourceName(), serverTiming, timestamp),
            EntropySource(cryptoPriceSource.getSourceName(), cryptoPrice, timestamp),
            EntropySource(ethereumSource.getSourceName(), ethereum, timestamp),
            EntropySource(seismicSource.getSourceName(), seismic, timestamp)
        )

        val seed = EntropyMixer.mix(listOf(serverTiming, cryptoPrice, ethereum, seismic))

        return EntropyResult(
            seed = seed,
            sources = sources,
            timestamp = timestamp
        )
    }
}