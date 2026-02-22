package com.sra.utils

import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.principal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun ApplicationCall.getUserId(): Int? {
    val principal = principal<JWTPrincipal>()
    return principal?.payload?.getClaim("userId")?.asInt()
}

fun ApplicationCall.requireUserId(): Int {
    return getUserId() ?: throw UnauthorizedException("Authentication required")
}

fun LocalDateTime.toIsoString(): String {
    return this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}

fun String.toBigIntegerSafe(): java.math.BigInteger {
    return try {
        java.math.BigInteger(this, 16)
    } catch (e: NumberFormatException) {
        java.math.BigInteger(this.toByteArray())
    }
}

class UnauthorizedException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class BadRequestException(message: String) : Exception(message)
class ForbiddenException(message: String) : Exception(message)