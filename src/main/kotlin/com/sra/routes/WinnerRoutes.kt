package com.sra.routes

import com.sra.service.WinnerService
import com.sra.utils.ApiResponse
import com.sra.utils.requireUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.winnerRoutes(winnerService: WinnerService) {

    authenticate("auth-jwt") {
        route("/api/v1/giveaways/{id}") {

            post("/draw") {
                val userId = call.requireUserId()
                val giveawayId = call.parameters["id"]!!.toInt()
                val tier = call.request.queryParameters["tier"] ?: "fast"
                val proof = winnerService.drawWinner(giveawayId, userId, tier)
                call.respond(HttpStatusCode.OK, ApiResponse(
                    success = true,
                    message = "Winner selected successfully",
                    data = proof
                ))
            }
        }
    }
}