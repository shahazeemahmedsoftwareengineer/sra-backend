package com.sra.routes

import com.sra.domain.models.LoginRequest
import com.sra.domain.models.RegisterRequest
import com.sra.security.IPBlocker
import com.sra.security.InputValidator
import com.sra.security.IntrusionDetector
import com.sra.service.AuthService
import com.sra.utils.ApiResponse
import com.sra.utils.ResponseMessages
import com.sra.utils.requireUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {

    route("/api/v1/auth") {

        rateLimit(RateLimitName("auth")) {
            post("/register") {
                val request = call.receive<RegisterRequest>()

                // Validate all inputs
                InputValidator.validateEmail(request.email)
                InputValidator.validatePassword(request.password)
                InputValidator.validate(request.name, "name")
                InputValidator.validateLength(request.name, "name", 100)

                val response = authService.register(request)
                call.respond(
                    HttpStatusCode.Created, ApiResponse(
                        success = true,
                        message = ResponseMessages.CREATED,
                        data = response
                    )
                )
            }

            post("/login") {
                val ip = call.request.local.remoteAddress

                if (IPBlocker.isBlocked(ip)) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ApiResponse(success = false, message = "Too many failed attempts. Try again later.", data = null)
                    )
                    return@post
                }

                val request = call.receive<LoginRequest>()

                // Validate inputs before hitting database
                InputValidator.validateEmail(request.email)
                InputValidator.validateLength(request.password, "password", 128)

                val result = authService.login(request)
                IPBlocker.recordSuccessfulLogin(ip)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(success = true, message = "Login successful", data = result)
                )
            }
        }

        authenticate("auth-jwt") {
            post("/api-key") {
                val userId = call.requireUserId()
                val apiKey = authService.generateApiKey(userId)
                call.respond(
                    HttpStatusCode.OK, ApiResponse(
                        success = true,
                        message = "API key generated",
                        data = mapOf("apiKey" to apiKey)
                    )
                )
            }
        }
    }

    route("/api/v1/admin") {
        get("/security/blocked-ips") {
            call.respond(HttpStatusCode.OK, IPBlocker.getStats())
        }
        get("/security/alerts") {
            call.respond(HttpStatusCode.OK, IntrusionDetector.getAlertsResponse())
        }
    }
}