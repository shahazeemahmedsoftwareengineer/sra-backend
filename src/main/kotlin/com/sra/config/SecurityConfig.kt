package com.sra.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object SecurityConfig {

    private val algorithm = Algorithm.HMAC256(AppConfig.Jwt.secret)

    fun generateToken(userId: Int, email: String): String {
        return JWT.create()
            .withIssuer(AppConfig.Jwt.issuer)
            .withAudience(AppConfig.Jwt.audience)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(
                Date(System.currentTimeMillis() + AppConfig.Jwt.expiryDays * 24 * 60 * 60 * 1000)
            )
            .sign(algorithm)
    }

    fun getVerifier() = JWT.require(algorithm)
        .withIssuer(AppConfig.Jwt.issuer)
        .withAudience(AppConfig.Jwt.audience)
        .build()
}