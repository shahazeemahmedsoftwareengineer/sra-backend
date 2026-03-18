package com.sra.domain.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

// Stores every API call made by every user
// Used for the Activity tab in the dashboard
object ActivityLogsTable : Table("activity_logs") {
    val id          = integer("id").autoIncrement()
    val userId      = integer("user_id").references(UsersTable.id)
    val action      = varchar("action", 50)      // "ENCRYPT" | "DECRYPT" | "KEY_GENERATED" | "KEY_ROTATED"
    val status      = varchar("status", 20)      // "SUCCESS" | "FAILED"
    val details     = varchar("details", 255).nullable()  // e.g. "AES-256-GCM" or tier name
    val createdAt   = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}