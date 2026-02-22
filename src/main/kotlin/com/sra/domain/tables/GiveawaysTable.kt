package com.sra.domain.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object GiveawaysTable : Table("giveaways") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UsersTable.id)
    val title = varchar("title", 255)
    val prizeDescription = text("prize_description")
    val entryRules = text("entry_rules").nullable()
    val status = varchar("status", 50).default("active")
    val totalEntries = integer("total_entries").default(0)
    val winnerEntryId = integer("winner_entry_id").nullable()
    val startDate = datetime("start_date").default(LocalDateTime.now())
    val endDate = datetime("end_date").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}