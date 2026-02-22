package com.sra.config

import io.github.cdimascio.dotenv.dotenv

object AppConfig {

    private val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val port: Int get() = dotenv["PORT"].toIntOrNull() ?: 8080
    val environment: String get() = dotenv["ENVIRONMENT"] ?: "development"
    val isDevelopment: Boolean get() = environment == "development"

    object Database {
        val url: String get() = dotenv["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5432/sra_db"
        val user: String get() = dotenv["DATABASE_USER"] ?: "postgres"
        val password: String get() = dotenv["DATABASE_PASSWORD"] ?: ""
    }

    object Jwt {
        val secret: String get() = dotenv["JWT_SECRET"] ?: "default-secret-change-this"
        val issuer: String get() = dotenv["JWT_ISSUER"] ?: "sra-platform"
        val audience: String get() = dotenv["JWT_AUDIENCE"] ?: "sra-users"
        val expiryDays: Long get() = dotenv["JWT_EXPIRY_DAYS"].toLongOrNull() ?: 30L
    }

    object Entropy {
        val alchemyApiKey: String get() = dotenv["ALCHEMY_API_KEY"] ?: ""
        val binanceApiUrl: String get() = dotenv["BINANCE_API_URL"] ?: "https://api.binance.com/api/v3/ticker/price"
        val usgsApiUrl: String get() = dotenv["USGS_API_URL"] ?: "https://earthquake.usgs.gov/fdsnws/event/1/query"
    }
}