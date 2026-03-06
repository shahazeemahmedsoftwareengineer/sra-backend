package com.sra.domain.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ShieldKeysTable : Table("shield_keys") {
    val id = integer("id").autoIncrement()
    val publicKey = varchar("public_key", 255).uniqueIndex()
    val encryptedPrivateKey = text("encrypted_private_key")
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}