package com.sra.routes

import com.sra.service.AuthService
import com.sra.utils.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class UpdatePlanRequest(
    val email: String,
    val plan: String,           // "free" | "starter" | "pro"
    val webhookSecret: String   // internal secret to prevent abuse
)

fun Route.webhookRoutes(authService: AuthService) {

    val logger = LoggerFactory.getLogger("WebhookRoutes")
    val internalSecret = System.getenv("INTERNAL_WEBHOOK_SECRET") ?: "change_this_secret"

    route("/api/v1/internal") {

        // Called by Next.js Stripe webhook handler
        post("/update-plan") {
            val request = call.receive<UpdatePlanRequest>()

            // Verify internal secret
            if (request.webhookSecret != internalSecret) {
                logger.warn("Unauthorized plan update attempt for: ${request.email}")
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(
                    success = false,
                    message = "Unauthorized",
                    data = null
                ))
                return@post
            }

            // Validate plan value
            val validPlans = listOf("free", "starter", "pro")
            if (request.plan !in validPlans) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse(
                    success = false,
                    message = "Invalid plan: ${request.plan}",
                    data = null
                ))
                return@post
            }

            val updated = authService.updatePlanByEmail(request.email, request.plan)

            if (updated) {
                logger.info("Plan updated: ${request.email} → ${request.plan}")
                call.respond(HttpStatusCode.OK, ApiResponse(
                    success = true,
                    message = "Plan updated to ${request.plan}",
                    data = mapOf("email" to request.email, "plan" to request.plan)
                ))
            } else {
                logger.warn("User not found for plan update: ${request.email}")
                call.respond(HttpStatusCode.NotFound, ApiResponse(
                    success = false,
                    message = "User not found",
                    data = null
                ))
            }
        }

        // Get current user plan (called by frontend to sync localStorage)
        get("/user-plan/{email}") {
            val email = call.parameters["email"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ApiResponse(
                    success = false, message = "Email required", data = null
                ))
                return@get
            }

            val user = authService.getUserByEmail(email)
            if (user != null) {
                call.respond(HttpStatusCode.OK, ApiResponse(
                    success = true,
                    message = "OK",
                    data = mapOf("plan" to user.plan, "email" to user.email)
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse(
                    success = false, message = "User not found", data = null
                ))
            }
        }
    }
}