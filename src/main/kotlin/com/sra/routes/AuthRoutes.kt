package com.sra.routes

import com.sra.domain.models.LoginRequest
import com.sra.domain.models.RegisterRequest
import com.sra.service.AuthService
import com.sra.utils.ApiResponse
import com.sra.utils.ResponseMessages
import com.sra.utils.requireUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {

    route("/api/v1/auth") {

        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = authService.register(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                message = ResponseMessages.CREATED,
                data = response
            ))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.login(request)
            call.respond(HttpStatusCode.OK, ApiResponse(
                success = true,
                message = ResponseMessages.SUCCESS,
                data = response
            ))
        }

        authenticate("auth-jwt") {
            post("/api-key") {
                val userId = call.requireUserId()
                val apiKey = authService.generateApiKey(userId)
                call.respond(HttpStatusCode.OK, ApiResponse(
                    success = true,
                    message = "API key generated",
                    data = mapOf("apiKey" to apiKey)
                ))
            }
        }
    }
}