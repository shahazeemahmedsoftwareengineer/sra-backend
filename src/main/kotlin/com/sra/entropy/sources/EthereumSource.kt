package com.sra.entropy.sources

import com.sra.config.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class EthereumSource(private val client: HttpClient) {

    private val logger = LoggerFactory.getLogger(EthereumSource::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private var lastBlockHash: String = ""

    @Serializable
    private data class EthResponse(
        val result: EthBlock? = null
    )

    @Serializable
    private data class EthBlock(
        val hash: String,
        val number: String,
        val timestamp: String
    )

    suspend fun collect(): String {
        return try {
            val apiUrl = "https://eth-mainnet.g.alchemy.com/v2/${AppConfig.Entropy.alchemyApiKey}"
            val response: String = client.post(apiUrl) {
                header("Content-Type", "application/json")
                setBody("""{"jsonrpc":"2.0","method":"eth_getBlockByNumber","params":["latest",false],"id":1}""")
            }.body()

            val ethResponse = json.decodeFromString<EthResponse>(response)
            val block = ethResponse.result

            if (block != null && block.hash != lastBlockHash) {
                lastBlockHash = block.hash
                logger.debug("Ethereum block entropy collected: ${block.number}")
                "${block.hash}-${block.number}-${block.timestamp}"
            } else {
                "${lastBlockHash}-${System.nanoTime()}"
            }
        } catch (e: Exception) {
            logger.warn("Failed to collect Ethereum data, using fallback: ${e.message}")
            "ETH_FALLBACK-${System.nanoTime()}-${Math.random()}"
        }
    }

    fun getSourceName() = "ETHEREUM_BLOCK"
}