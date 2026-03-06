package com.sra.repository

import com.sra.domain.models.ShieldKey
import com.sra.domain.tables.ShieldKeysTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.format.DateTimeFormatter

class ShieldRepository {

    fun create(publicKey: String, encryptedPrivateKey: String): ShieldKey {
        val newId = transaction {
            ShieldKeysTable.insert {
                it[ShieldKeysTable.publicKey] = publicKey
                it[ShieldKeysTable.encryptedPrivateKey] = encryptedPrivateKey
            } get ShieldKeysTable.id
        }
        return findById(newId)!!
    }

    fun findByPublicKey(publicKey: String): ShieldKey? = transaction {
        ShieldKeysTable.select { ShieldKeysTable.publicKey eq publicKey }
            .map { toShieldKey(it) }
            .singleOrNull()
    }

    fun findById(id: Int): ShieldKey? = transaction {
        ShieldKeysTable.select { ShieldKeysTable.id eq id }
            .map { toShieldKey(it) }
            .singleOrNull()
    }

    private fun toShieldKey(row: ResultRow): ShieldKey = ShieldKey(
        id = row[ShieldKeysTable.id],
        publicKey = row[ShieldKeysTable.publicKey],
        encryptedPrivateKey = row[ShieldKeysTable.encryptedPrivateKey],
        createdAt = row[ShieldKeysTable.createdAt].format(DateTimeFormatter.ISO_DATE_TIME)
    )
}