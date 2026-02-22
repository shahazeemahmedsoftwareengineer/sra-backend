package com.sra.entropy.sources

import com.sra.config.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.slf4j.LoggerFactory

class SeismicSource(private val client: HttpClient) {

    private val logger = LoggerFactory.getLogger(SeismicSource::class.java)

    suspend fun collect(): String {
        return try {
            val response: String = client.get(AppConfig.Entropy.usgsApiUrl) {
                parameter("format", "geojson")
                parameter("limit", "5")
                parameter("orderby", "time")
                parameter("minmagnitude", "0.1")
            }.body()

            val hash = response.hashCode().toString()
            val timestamp = System.nanoTime()
            val result = "SEISMIC-$hash-$timestamp"
            logger.debug("Seismic entropy collected")
            result
        } catch (e: Exception) {
            logger.warn("Failed to collect seismic data, using fallback: ${e.message}")
            "SEISMIC_FALLBACK-${System.nanoTime()}-${Math.random()}"
        }
    }

    fun getSourceName() = "USGS_SEISMIC"
}