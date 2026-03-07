package com.sra.plugins

import com.sra.security.IPBlocker
import com.sra.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<UnauthorizedException> { call, cause ->
            // Record failed login attempt for IP blocking
            val ip = call.request.local.remoteAddress
            IPBlocker.recordFailedLogin(ip)
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(message = cause.message ?: "Unauthorized")
            )
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(message = cause.message ?: "Not found"))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(message = cause.message ?: "Bad request"))
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(message = cause.message ?: "Forbidden"))
        }
        exception<Exception> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(message = "Internal server error"))
        }
    }
}