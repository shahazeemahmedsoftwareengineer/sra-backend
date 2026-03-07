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
        val url: String get() {
            // Railway provides DATABASE_URL directly
            val databaseUrl = System.getenv("DATABASE_URL")
                ?: dotenv.getOrDefault("DATABASE_URL", "")
            
            if (databaseUrl.isNotEmpty()) {
                // Railway format: postgresql://user:password@host:port/dbname
                // Convert to JDBC format
                return databaseUrl
                    .replace("postgresql://", "jdbc:postgresql://")
                    .replace("postgres://", "jdbc:postgresql://")
            }
            
            // Fallback to individual vars (local development)
            val host = dotenv.getOrDefault("PGHOST", "localhost")
            val port = dotenv.getOrDefault("PGPORT", "5433")
            val name = dotenv.getOrDefault("PGDATABASE", "sra_db")
            return "jdbc:postgresql://$host:$port/$name"
        }
        val user: String get() {
            val databaseUrl = System.getenv("DATABASE_URL") ?: ""
            if (databaseUrl.isNotEmpty()) return ""  // extracted from URL
            return dotenv.getOrDefault("PGUSER", "postgres")
        }
        val password: String get() {
            val databaseUrl = System.getenv("DATABASE_URL") ?: ""
            if (databaseUrl.isNotEmpty()) return ""  // extracted from URL
            return dotenv.getOrDefault("PGPASSWORD", "")
        }
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