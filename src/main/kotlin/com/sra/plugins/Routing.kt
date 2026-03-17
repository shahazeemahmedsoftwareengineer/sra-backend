package com.sra.plugins

import com.sra.repository.ShieldRepository
import com.sra.routes.*
import com.sra.security.IntrusionDetector
import com.sra.service.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    authService: AuthService,
    giveawayService: GiveawayService,
    entryService: EntryService,
    winnerService: WinnerService,
    proofService: ProofService,
    entropyService: EntropyService
) {
    val shieldRepository = ShieldRepository()
    val shieldService = ShieldService(entropyService, shieldRepository)

    routing {

        // ── FIX 9: INTRUSION DETECTION ────────────────────────────
        // Runs on EVERY incoming request before it hits any route
        // Checks for port scanning, suspicious paths, known scanners
        intercept(io.ktor.server.routing.RoutingRoot.Phases.Call) {
            val ip        = call.request.local.remoteAddress
            val path      = call.request.local.uri
            val userAgent = call.request.headers["User-Agent"]
            IntrusionDetector.recordRequest(ip, path, userAgent)
        }

        authRoutes(authService)
        giveawayRoutes(giveawayService)
        entryRoutes(entryService)
        winnerRoutes(winnerService)
        proofService.let { proofRoutes(it) }
        webhookRoutes(authService)
        shieldRoutes(shieldService, shieldRepository)
    }
}