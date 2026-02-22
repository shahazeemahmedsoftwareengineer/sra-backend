package com.sra.plugins

import com.sra.routes.*
import com.sra.service.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    authService: AuthService,
    giveawayService: GiveawayService,
    entryService: EntryService,
    winnerService: WinnerService,
    proofService: ProofService
) {
    routing {
        authRoutes(authService)
        giveawayRoutes(giveawayService)
        entryRoutes(entryService)
        winnerRoutes(winnerService)
        proofRoutes(proofService)
    }
}