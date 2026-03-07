package com.sra.config

import io.github.cdimascio.dotenv.dotenv

object AppConfig {

    private val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val port: Int get() = System.getenv("PORT")?.toIntOrNull() ?: dotenv.getOrDefault("PORT", "8080").toIntOrNull() ?: 8080
    val environment: String get() = System.getenv("ENVIRONMENT") ?: dotenv.getOrDefault("ENVIRONMENT", "development")
    val isDevelopment: Boolean get() = environment == "development"

    object Database {
        val url: String get() {
            val raw = System.getenv("DATABASE_URL") ?: dotenv.getOrDefault("DATABASE_URL", "")
            if (raw.isNotEmpty()) {
                return raw.replace("postgresql://", "jdbc:postgresql://")
                          .replace("postgres://", "jdbc:postgresql://")
            }
            val host = System.getenv("PGHOST") ?: dotenv.getOrDefault("PGHOST", "localhost")
            val port = System.getenv("PGPORT") ?: dotenv.getOrDefault("PGPORT", "5433")
            val name = System.getenv("PGDATABASE") ?: dotenv.getOrDefault("PGDATABASE", "sra_db")
            return "jdbc:postgresql://$host:$port/$name"
        }
        val user: String get() {
            val raw = System.getenv("DATABASE_URL") ?: dotenv.getOrDefault("DATABASE_URL", "")
            if (raw.isNotEmpty()) return ""
            return System.getenv("PGUSER") ?: dotenv.getOrDefault("PGUSER", "postgres")
        }
        val password: String get() {
            val raw = System.getenv("DATABASE_URL") ?: dotenv.getOrDefault("DATABASE_URL", "")
            if (raw.isNotEmpty()) return ""
            return System.getenv("PGPASSWORD") ?: dotenv.getOrDefault("PGPASSWORD", "")
        }
    }

    object Jwt {
        val secret: String get() = System.getenv("JWT_SECRET") ?: dotenv.getOrDefault("JWT_SECRET", "default-secret-change-this")
        val issuer: String get() = System.getenv("JWT_ISSUER") ?: dotenv.getOrDefault("JWT_ISSUER", "sra-platform")
        val audience: String get() = System.getenv("JWT_AUDIENCE") ?: dotenv.getOrDefault("JWT_AUDIENCE", "sra-users")
        val expiryDays: Long get() = System.getenv("JWT_EXPIRY_DAYS")?.toLongOrNull() ?: dotenv.getOrDefault("JWT_EXPIRY_DAYS", "30").toLongOrNull() ?: 30L
    }

    object Entropy {
        val alchemyApiKey: String get() = System.getenv("ALCHEMY_API_KEY") ?: dotenv.getOrDefault("ALCHEMY_API_KEY", "")
        val binanceApiUrl: String get() = System.getenv("BINANCE_API_URL") ?: dotenv.getOrDefault("BINANCE_API_URL", "https://api.binance.com/api/v3/ticker/price")
        val usgsApiUrl: String get() = System.getenv("USGS_API_URL") ?: dotenv.getOrDefault("USGS_API_URL", "https://earthquake.usgs.gov/fdsnws/event/1/query")
    }
}
