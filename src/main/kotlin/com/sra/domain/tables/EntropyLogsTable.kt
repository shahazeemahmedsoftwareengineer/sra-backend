package com.sra.domain.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object EntropyLogsTable : Table("entropy_logs") {
    val id = integer("id").autoIncrement()
    val giveawayId = integer("giveaway_id").references(GiveawaysTable.id).nullable()
    val bitcoinPrice = varchar("bitcoin_price", 50).nullable()
    val serverTimingData = text("server_timing_data").nullable()
    val ethereumBlockHash = varchar("ethereum_block_hash", 128).nullable()
    val seismicData = text("seismic_data").nullable()
    val rawCombinedString = text("raw_combined_string")
    val sha256Hash = varchar("sha256_hash", 128)
    val seedGenerated = varchar("seed_generated", 128)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}