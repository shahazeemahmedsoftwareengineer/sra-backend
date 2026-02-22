package com.sra.domain.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ProofsTable : Table("proofs") {
    val id = integer("id").autoIncrement()
    val giveawayId = integer("giveaway_id").references(GiveawaysTable.id)
    val winnerEntryId = integer("winner_entry_id").references(EntriesTable.id)
    val publicCode = varchar("public_code", 32).uniqueIndex()
    val seedGenerated = varchar("seed_generated", 128)
    val sourcesUsed = text("sources_used")
    val calculationDetails = text("calculation_details")
    val isPublic = bool("is_public").default(true)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}