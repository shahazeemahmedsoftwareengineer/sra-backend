package com.sra.config

import io.github.cdimascio.dotenv.dotenv

object AppConfig {

    private val dotenv = dotenv {
        ignoreIfMissing = true
    }

    private fun env(key: String, default: String = ""): String =
        System.getenv(key) ?: try { dotenv[key] } catch (e: Exception) { default }

    val port: Int get() = env("PORT", "8080").toIntOrNull() ?: 8080
    val environment: String get() = env("ENVIRONMENT", "development")
    val isDevelopment: Boolean get() = environment == "development"

    object Database {
        val url: String get() {
            val raw = env("DATABASE_URL")
            if (raw.isNotEmpty()) {
                return raw.replace("postgresql://", "jdbc:postgresql://")
                          .replace("postgres://", "jdbc:postgresql://")
            }
            val host = env("PGHOST", "localhost")
            val port = env("PGPORT", "5433")
            val name = env("PGDATABASE", "sra_db")
            return "jdbc:postgresql://$host:$port/$name"
        }
        val user: String get() {
            val raw = env("DATABASE_URL")
            if (raw.isNotEmpty()) return ""
            return env("PGUSER", "postgres")
        }
        val password: String get() {
            val raw = env("DATABASE_URL")
            if (raw.isNotEmpty()) return ""
            return env("PGPASSWORD", "")
        }
    }

    object Jwt {
        val secret: String get() = env("JWT_SECRET", "default-secret-change-this")
        val issuer: String get() = env("JWT_ISSUER", "sra-platform")
        val audience: String get() = env("JWT_AUDIENCE", "sra-users")
        val expiryDays: Long get() = env("JWT_EXPIRY_DAYS", "30").toLongOrNull() ?: 30L
    }

    object Entropy {
        val alchemyApiKey: String get() = env("ALCHEMY_API_KEY", "")
        val binanceApiUrl: String get() = env("BINANCE_API_URL", "https://api.binance.com/api/v3/ticker/price")
        val usgsApiUrl: String get() = env("USGS_API_URL", "https://earthquake.usgs.gov/fdsnws/event/1/query")
    }
}