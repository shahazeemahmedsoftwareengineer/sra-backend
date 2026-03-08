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
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.TimeZone

private const val defaultPort = 8080
private const val maxPort = 65535
private val startupLogger = LoggerFactory.getLogger("ApplicationStartup")

fun main(args: Array<String>) {
    val requestedPort = System.getenv("PORT")?.toIntOrNull() ?: defaultPort
    val isDevelopment = System.getProperty("io.ktor.development")?.equals("true", ignoreCase = true) == true

    if (!canBind(requestedPort)) {
        if (!isDevelopment) {
            throw IllegalStateException("Port $requestedPort is already in use. Stop the running instance or set PORT to a free value.")
        }

        val fallbackPort = findAvailablePort(requestedPort + 1)
            ?: throw IllegalStateException("No free port found starting from ${requestedPort + 1}.")

        System.setProperty("ktor.deployment.port", fallbackPort.toString())
        startupLogger.warn("Port {} is busy, using fallback port {} in development mode.", requestedPort, fallbackPort)
    }

    EngineMain.main(args)
}

fun Application.module() {
    ensureConfiguredPortIsAvailable()

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
    environment.monitor.subscribe(ApplicationStarted) {
        log.info("SRA Backend started successfully")
    }

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
    configureRateLimiting()
    configureSecurityHeaders()
    configureIntrusionDetection()
    configureRouting(authService, giveawayService, entryService, winnerService, proofService, entropyService)
}

private fun Application.ensureConfiguredPortIsAvailable() {
    val configuredPort = environment.config.propertyOrNull("ktor.deployment.port")
        ?.getString()
        ?.toIntOrNull()
        ?: return

    if (configuredPort <= 0 || canBind(configuredPort)) return

    throw IllegalStateException(
        "Port $configuredPort is already in use. Stop the running instance or set PORT to a free value."
    )
}

private fun canBind(port: Int): Boolean {
    return try {
        ServerSocket().use { socket ->
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(port))
        }
        true
    } catch (_: Exception) {
        false
    }
}

private fun findAvailablePort(startPort: Int): Int? {
    var port = startPort.coerceAtLeast(1)
    while (port <= maxPort) {
        if (canBind(port)) {
            return port
        }
        port++
    }
    return null
}


