package com.sra.plugins

import com.sra.repository.ShieldRepository
import com.sra.routes.*
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
        authRoutes(authService)
        giveawayRoutes(giveawayService)
        entryRoutes(entryService)
        winnerRoutes(winnerService)
        proofRoutes(proofService)
        webhookRoutes(authService)
        shieldRoutes(shieldService, shieldRepository)
    }
}