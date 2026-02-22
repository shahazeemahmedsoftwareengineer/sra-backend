package com.sra

import com.sra.config.DatabaseConfig
import com.sra.entropy.EntropyPool
import com.sra.plugins.*
import com.sra.repository.*
import com.sra.service.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import java.util.TimeZone

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Force a valid timezone name for PostgreSQL startup packets.
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    System.setProperty("user.timezone", "UTC")

    // HTTP Client for external APIs
    val httpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // Database
    DatabaseConfig.init()

    // Repositories
    val userRepository = UserRepository()
    val giveawayRepository = GiveawayRepository()
    val entryRepository = EntryRepository()
    val proofRepository = ProofRepository()

    // Entropy
    val entropyPool = EntropyPool(httpClient)

    // Services
    val authService = AuthService(userRepository)
    val entropyService = EntropyService(entropyPool)
    val proofService = ProofService(proofRepository, giveawayRepository, entryRepository)
    val giveawayService = GiveawayService(giveawayRepository)
    val entryService = EntryService(entryRepository, giveawayRepository)
    val winnerService = WinnerService(giveawayRepository, entryRepository, entropyService, proofService)

    // Plugins
    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureSecurity()
    configureRouting(authService, giveawayService, entryService, winnerService, proofService)

    log.info("SRA Backend started successfully")
}
