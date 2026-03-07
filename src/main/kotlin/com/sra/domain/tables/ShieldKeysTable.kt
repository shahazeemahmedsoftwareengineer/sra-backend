package com.sra.domain.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

// Stores every encryption key generated for every company
// This table IS the audit trail — never delete rows
object ShieldKeysTable : Table("shield_keys") {
    val id                  = integer("id").autoIncrement()
    val userId              = integer("user_id").references(UsersTable.id)

    // The actual AES-256 key (first 32 bytes of entropy seed = 256 bits)
    val encryptionKey       = varchar("encryption_key", 64)

    // Entropy proof — what real world data was used
    val seed                = varchar("seed", 128)
    val btcPrice            = varchar("btc_price", 500).nullable()
    val ethBlockHash        = varchar("eth_block_hash", 500).nullable()
    val seismicData         = varchar("seismic_data", 1000).nullable()
    val serverTiming        = varchar("server_timing", 500).nullable()
    val rawEntropyString    = text("raw_entropy_string")

    // Verification — public proof code anyone can check
    val verificationCode    = varchar("verification_code", 32).uniqueIndex()

    // Key lifecycle
    val tier                = varchar("tier", 20)  // "fast" | "medium" | "full"
    val isActive            = bool("is_active").default(true)
    val rotatedAt           = datetime("rotated_at").nullable()
    val expiresAt           = datetime("expires_at").nullable()

    val createdAt           = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
