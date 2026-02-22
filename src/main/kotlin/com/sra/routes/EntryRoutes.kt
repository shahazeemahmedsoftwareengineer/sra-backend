package com.sra.routes

import com.sra.domain.models.SubmitEntryRequest
import com.sra.service.EntryService
import com.sra.utils.ApiResponse
import com.sra.utils.ResponseMessages
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.entryRoutes(entryService: EntryService) {

    route("/api/v1") {

        post("/giveaways/{id}/enter") {
            val giveawayId = call.parameters["id"]!!.toInt()
            val request = call.receive<SubmitEntryRequest>()
            val entry = entryService.submitEntry(giveawayId, request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                message = "Entry submitted successfully",
                data = entry
            ))
        }

        get("/giveaways/{id}/entries") {
            val giveawayId = call.parameters["id"]!!.toInt()
            val entries = entryService.getGiveawayEntries(giveawayId)
            call.respond(HttpStatusCode.OK, ApiResponse(
                success = true,
                message = ResponseMessages.SUCCESS,
                data = entries
            ))
        }
    }
}