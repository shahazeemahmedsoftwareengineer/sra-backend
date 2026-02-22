package com.sra.plugins

import com.sra.config.SecurityConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    authentication {
        jwt("auth-jwt") {
            verifier(SecurityConfig.getVerifier())
            validate { credential ->
                val userId = credential.payload.getClaim("userId")?.asInt()
                val email = credential.payload.getClaim("email")?.asString()
                if (userId != null && email != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}