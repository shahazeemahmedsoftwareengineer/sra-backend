package com.sra.config

import com.sra.domain.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.sql.SQLException

object DatabaseConfig {
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)
    private const val maxDbInitAttempts = 10
    private const val dbInitRetryDelayMs = 1000L

    fun init() {
        val dbUrl = buildJdbcUrl()
        logger.info("Connecting to DB...")
        Database.connect(url = dbUrl, driver = "org.postgresql.Driver")
        createTables()
        logger.info("Database initialized successfully")
    }

    private fun buildJdbcUrl(): String {
        val rawUrl = System.getenv("DATABASE_URL") ?: ""
        if (rawUrl.isNotEmpty()) {
            logger.info("Using DATABASE_URL")
            // Parse postgresql://user:password@host:port/db
            // Convert to jdbc:postgresql://host:port/db?user=user&password=pass
            val withoutScheme = rawUrl
                .removePrefix("postgresql://")
                .removePrefix("postgres://")
            val atIndex = withoutScheme.lastIndexOf("@")
            val credentials = withoutScheme.substring(0, atIndex)
            val hostAndDb = withoutScheme.substring(atIndex + 1)
            val colonIndex = credentials.indexOf(":")
            val user = credentials.substring(0, colonIndex)
            val pass = credentials.substring(colonIndex + 1)
            return "jdbc:postgresql://$hostAndDb?user=$user&password=$pass&sslmode=require"
        }

        val host = System.getenv("PGHOST") ?: "localhost"
        val port = System.getenv("PGPORT") ?: "5433"
        val db   = System.getenv("PGDATABASE") ?: "sra_db"
        val user = System.getenv("PGUSER") ?: "postgres"
        val pass = System.getenv("PGPASSWORD") ?: ""
        logger.info("Using PGHOST={} PGPORT={} PGDATABASE={}", host, port, db)
        return "jdbc:postgresql://$host:$port/$db?user=$user&password=$pass"
    }

    private fun createTables() {
        var attempt = 1
        while (true) {
            try {
                transaction {
                    SchemaUtils.createMissingTablesAndColumns(
                        UsersTable,
                        GiveawaysTable,
                        EntriesTable,
                        ProofsTable,
                        EntropyLogsTable,
                        ShieldKeysTable
                    )
                }
                logger.info("Database tables verified/created")
                return
            } catch (e: Exception) {
                if (!isConnectionException(e) || attempt >= maxDbInitAttempts) throw e
                logger.warn("Database not ready (attempt {}/{}). Retrying...", attempt, maxDbInitAttempts)
                Thread.sleep(dbInitRetryDelayMs)
                attempt++
            }
        }
    }

    private fun isConnectionException(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is ConnectException) return true
            if (current is SQLException && current.sqlState?.startsWith("08") == true) return true
            current = current.cause
        }
        return false
    }
}


