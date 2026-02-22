package com.sra.entropy.sources

import com.sra.config.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class CryptoPriceSource(private val client: HttpClient) {

    private val logger = LoggerFactory.getLogger(CryptoPriceSource::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class BinancePrice(
        val symbol: String,
        val price: String
    )

    suspend fun collect(): String {
        return try {
            val response: String = client.get(AppConfig.Entropy.binanceApiUrl) {
                parameter("symbol", "BTCUSDT")
            }.body()

            val priceData = json.decodeFromString<BinancePrice>(response)
            val result = "${priceData.symbol}-${priceData.price}-${System.nanoTime()}"
            logger.debug("Crypto price entropy collected: BTC = ${priceData.price}")
            result
        } catch (e: Exception) {
            logger.warn("Failed to collect crypto price, using fallback: ${e.message}")
            "CRYPTO_FALLBACK-${System.nanoTime()}-${Math.random()}"
        }
    }

    fun getSourceName() = "CRYPTO_PRICE_BINANCE"
}