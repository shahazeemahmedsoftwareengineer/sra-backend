package com.sra.repository

import com.sra.domain.models.*
import com.sra.domain.tables.ShieldKeysTable
import com.sra.domain.models.EntropyResult
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ShieldRepository {

    private val logger = LoggerFactory.getLogger(ShieldRepository::class.java)

    // ── SAVE new key generation to audit log ──────────────────────
    fun saveKey(
        userId: Int,
        encryptionKey: String,
        verificationCode: String,
        tier: String,
        entropyResult: EntropyResult
    ): Int {
        return transaction {
            ShieldKeysTable.insert {
                it[ShieldKeysTable.userId]           = userId
                it[ShieldKeysTable.encryptionKey]    = encryptionKey
                it[ShieldKeysTable.verificationCode] = verificationCode
                it[ShieldKeysTable.tier]             = tier
                it[ShieldKeysTable.seed]             = entropyResult.seed.take(128)
                it[ShieldKeysTable.rawEntropyString] = entropyResult.sources
                    .joinToString("|") { s -> "${s.name}:${s.value}" }
                it[ShieldKeysTable.btcPrice]         = entropyResult.sources
                    .find { s -> s.name.contains("CRYPTO") }?.value
                it[ShieldKeysTable.ethBlockHash]     = entropyResult.sources
                    .find { s -> s.name.contains("ETHEREUM") }?.value
                it[ShieldKeysTable.seismicData]      = entropyResult.sources
                    .find { s -> s.name.contains("SEISMIC") }?.value
                it[ShieldKeysTable.serverTiming]     = entropyResult.sources
                    .find { s -> s.name.contains("TIMING") }?.value
                it[ShieldKeysTable.isActive]         = true
                it[ShieldKeysTable.expiresAt]        = LocalDateTime.now().plusHours(24)
                it[ShieldKeysTable.createdAt]        = LocalDateTime.now()
            }[ShieldKeysTable.id]
        }
    }

    // ── DEACTIVATE all previous keys for this user (rotation) ────
    fun deactivatePreviousKeys(userId: Int) {
        transaction {
            ShieldKeysTable.update(
                where = {
                    (ShieldKeysTable.userId eq userId) and
                            (ShieldKeysTable.isActive eq true)
                }
            ) {
                it[isActive]   = false
                it[rotatedAt]  = LocalDateTime.now()
            }
        }
        logger.info("Deactivated previous keys for userId=$userId")
    }

    // ── GET audit log for a user ──────────────────────────────────
    fun getAuditLog(userId: Int): List<ShieldAuditEntry> {
        return transaction {
            ShieldKeysTable
                .select { ShieldKeysTable.userId eq userId }
                .orderBy(ShieldKeysTable.createdAt, SortOrder.DESC)
                .map { row -> rowToAuditEntry(row) }
        }
    }

    // ── GET single key by verification code (public endpoint) ─────
    fun getByVerificationCode(code: String): ShieldAuditEntry? {
        return transaction {
            ShieldKeysTable
                .select { ShieldKeysTable.verificationCode eq code }
                .singleOrNull()
                ?.let { row -> rowToAuditEntry(row) }
        }
    }

    // ── GET active key for a user (for audit) ─────────────────────
    fun getActiveKey(userId: Int): ShieldAuditEntry? {
        return transaction {
            ShieldKeysTable
                .select {
                    (ShieldKeysTable.userId eq userId) and
                            (ShieldKeysTable.isActive eq true)
                }
                .orderBy(ShieldKeysTable.createdAt, SortOrder.DESC)
                .firstOrNull()
                ?.let { row -> rowToAuditEntry(row) }
        }
    }

    // ── GET active key for a user (for encryption) ──────────────
    fun getInternalActiveKey(userId: Int): InternalActiveKey? {
        return transaction {
            ShieldKeysTable
                .select {
                    (ShieldKeysTable.userId eq userId) and
                            (ShieldKeysTable.isActive eq true)
                }
                .orderBy(ShieldKeysTable.createdAt, SortOrder.DESC)
                .firstOrNull()
                ?.let {
                    InternalActiveKey(
                        encryptionKey = it[ShieldKeysTable.encryptionKey],
                        verificationCode = it[ShieldKeysTable.verificationCode]
                    )
                }
        }
    }

    // ── COUNT keys for a user ─────────────────────────────────────
    fun countKeys(userId: Int): Int {
        return transaction {
            ShieldKeysTable
                .select { ShieldKeysTable.userId eq userId }
                .count()
                .toInt()
        }
    }

    // ── MAP database row to model ─────────────────────────────────
    private fun rowToAuditEntry(row: ResultRow): ShieldAuditEntry {
        val sourcesUsed = mutableListOf<String>()
        if (row[ShieldKeysTable.btcPrice] != null)     sourcesUsed.add("BITCOIN_PRICE")
        if (row[ShieldKeysTable.ethBlockHash] != null) sourcesUsed.add("ETHEREUM_BLOCK")
        if (row[ShieldKeysTable.seismicData] != null)  sourcesUsed.add("SEISMIC_DATA")
        if (row[ShieldKeysTable.serverTiming] != null) sourcesUsed.add("SERVER_TIMING")
        sourcesUsed.add("ROLLING_POOL")

        return ShieldAuditEntry(
            id                = row[ShieldKeysTable.id],
            verificationCode  = row[ShieldKeysTable.verificationCode],
            tier              = row[ShieldKeysTable.tier],
            isActive          = row[ShieldKeysTable.isActive],
            createdAt         = row[ShieldKeysTable.createdAt].toString(),
            expiresAt         = row[ShieldKeysTable.expiresAt]?.toString(),
            rotatedAt         = row[ShieldKeysTable.rotatedAt]?.toString(),
            proof = ShieldKeyProof(
                seed          = row[ShieldKeysTable.seed],
                tier          = row[ShieldKeysTable.tier],
                sourcesUsed   = sourcesUsed,
                btcPrice      = row[ShieldKeysTable.btcPrice],
                ethBlockHash  = row[ShieldKeysTable.ethBlockHash],
                seismicData   = row[ShieldKeysTable.seismicData],
                serverTiming  = row[ShieldKeysTable.serverTiming]
            )
        )
    }
}