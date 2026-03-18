package com.sra.domain.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UsersTable : Table("users") {
    val id            = integer("id").autoIncrement()
    val email         = varchar("email", 255).uniqueIndex()
    val password      = varchar("password", 255)
    val name          = varchar("name", 255)
    val plan          = varchar("plan", 50).default("free")
    val apiKey        = varchar("api_key", 64).uniqueIndex().nullable()
    val isActive      = bool("is_active").default(true)

    // ── USAGE TRACKING ────────────────────────────────────────────
    val apiCallsUsed  = integer("api_calls_used").default(0)
    val apiCallsResetDate = datetime("api_calls_reset_date").default(LocalDateTime.now().plusMonths(1))

    val createdAt     = datetime("created_at").default(LocalDateTime.now())
    val updatedAt     = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}