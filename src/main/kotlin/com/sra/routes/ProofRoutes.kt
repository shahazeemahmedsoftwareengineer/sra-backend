package com.sra.routes

import com.sra.service.ProofService
import com.sra.utils.ApiResponse
import com.sra.utils.ResponseMessages
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.proofRoutes(proofService: ProofService) {

    route("/api/v1/proof") {

        get("/{code}") {
            val code = call.parameters["code"]!!
            val proof = proofService.getPublicProof(code)
            call.respond(HttpStatusCode.OK, ApiResponse(
                success = true,
                message = ResponseMessages.SUCCESS,
                data = proof
            ))
        }
    }
}