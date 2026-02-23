package com.sra

import com.sra.config.DatabaseConfig
import com.sra.entropy.EntropyPool
import com.sra.plugins.*
import com.sra.repository.*
import com.sra.service.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.util.TimeZone

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    System.setProperty("user.timezone", "UTC")

    val httpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    DatabaseConfig.init()

    val userRepository = UserRepository()
    val giveawayRepository = GiveawayRepository()
    val entryRepository = EntryRepository()
    val proofRepository = ProofRepository()

    val entropyPool = EntropyPool(httpClient)

    // Start background entropy collection
    val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    entropyPool.startBackgroundCollection(applicationScope)

    // Stop cleanly on shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        entropyPool.stopBackgroundCollection()
        applicationScope.cancel()
    }

    val authService = AuthService(userRepository)
    val entropyService = EntropyService(entropyPool)
    val proofService = ProofService(proofRepository, giveawayRepository, entryRepository)
    val giveawayService = GiveawayService(giveawayRepository)
    val entryService = EntryService(entryRepository, giveawayRepository)
    val winnerService = WinnerService(giveawayRepository, entryRepository, entropyService, proofService)

    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureSecurity()
    configureRouting(authService, giveawayService, entryService, winnerService, proofService)

    log.info("SRA Backend started successfully")
}
