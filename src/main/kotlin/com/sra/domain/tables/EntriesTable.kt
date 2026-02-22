package com.sra.domain.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object EntriesTable : Table("entries") {
    val id = integer("id").autoIncrement()
    val giveawayId = integer("giveaway_id").references(GiveawaysTable.id)
    val participantName = varchar("participant_name", 255)
    val participantEmail = varchar("participant_email", 255).nullable()
    val socialHandle = varchar("social_handle", 255).nullable()
    val entryCount = integer("entry_count").default(1)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}