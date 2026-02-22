package com.sra.config

import com.sra.domain.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseConfig {

    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)

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
    }
}