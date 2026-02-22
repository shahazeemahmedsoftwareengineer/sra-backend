package com.sra.routes

import com.sra.domain.models.CreateGiveawayRequest
import com.sra.service.GiveawayService
import com.sra.utils.ApiResponse
import com.sra.utils.ResponseMessages
import com.sra.utils.requireUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.giveawayRoutes(giveawayService: GiveawayService) {

    authenticate("auth-jwt") {
        route("/api/v1/giveaways") {

            post {
                val userId = call.requireUserId()
                val request = call.receive<CreateGiveawayRequest>()
                val giveaway = giveawayService.create(userId, request)
                call.respond(HttpStatusCode.Created, ApiResponse(
                    success = true,
                    message = ResponseMessages.CREATED,
                    data = giveaway
                ))
            }

            get {
                val userId = call.requireUserId()
                val giveaways = giveawayService.getUserGiveaways(userId)
                call.respond(HttpStatusCode.OK, ApiResponse(
                    success = true,
                    message = ResponseMessages.SUCCESS,
                    data = giveaways
                ))
            }

            get("/{id}") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]!!.toInt()
                val giveaway = giveawayService.getById(id, userId)
                call.respond(HttpStatusCode.OK, ApiResponse(
                    success = true,
                    message = ResponseMessages.SUCCESS,
                    data = giveaway
                ))
            }

            get("/{id}/status") {
                val userId = call.requireUserId()
                val id = call.parameters["id"]!!.toInt()
                val baseUrl = "${call.request.origin.scheme}://${call.request.host()}"
                val status = giveawayService.getStatus(id, userId, baseUrl)
                call.respond(HttpStatusCode.OK, ApiResponse(
                    success = true,
                    message = ResponseMessages.SUCCESS,
                    data = status
                ))
            }
        }
    }
}