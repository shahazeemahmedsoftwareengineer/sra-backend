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
        logger.info("Initializing database connection...")

        Database.connect(
            url = AppConfig.Database.url,
            driver = "org.postgresql.Driver",
            user = AppConfig.Database.user,
            password = AppConfig.Database.password
        )

        createTables()
        logger.info("Database initialized successfully")
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
                        EntropyLogsTable
                    )
                }
                logger.info("Database tables verified/created")
                return
            } catch (e: Exception) {
                if (!isConnectionException(e) || attempt >= maxDbInitAttempts) {
                    throw e
                }
                logger.warn(
                    "Database is not ready (attempt {}/{}). Retrying in {} ms...",
                    attempt,
                    maxDbInitAttempts,
                    dbInitRetryDelayMs
                )
                Thread.sleep(dbInitRetryDelayMs)
                attempt++
            }
        }
    }

    private fun isConnectionException(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is ConnectException) {
                return true
            }
            if (current is SQLException && current.sqlState?.startsWith("08") == true) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
