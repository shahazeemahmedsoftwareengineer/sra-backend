package com.sra.repository

import com.sra.domain.models.Entry
import com.sra.domain.tables.EntriesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class EntryRepository {

    fun create(
        giveawayId: Int,
        participantName: String,
        participantEmail: String?,
        socialHandle: String?
    ): Entry {
        return transaction {
            val id = EntriesTable.insert {
                it[EntriesTable.giveawayId] = giveawayId
                it[EntriesTable.participantName] = participantName
                it[EntriesTable.participantEmail] = participantEmail
                it[EntriesTable.socialHandle] = socialHandle
                it[EntriesTable.createdAt] = LocalDateTime.now()
            }[EntriesTable.id]
            findById(id)!!
        }
    }

    fun findById(id: Int): Entry? {
        return transaction {
            EntriesTable.select { EntriesTable.id eq id }
                .singleOrNull()
                ?.toEntry()
        }
    }

    fun findByGiveawayId(giveawayId: Int): List<Entry> {
        return transaction {
            EntriesTable.select { EntriesTable.giveawayId eq giveawayId }
                .orderBy(EntriesTable.createdAt, SortOrder.ASC)
                .map { it.toEntry() }
        }
    }

    fun countByGiveawayId(giveawayId: Int): Int {
        return transaction {
            EntriesTable.select { EntriesTable.giveawayId eq giveawayId }.count().toInt()
        }
    }

    fun existsByEmail(giveawayId: Int, email: String): Boolean {
        return transaction {
            EntriesTable.select {
                (EntriesTable.giveawayId eq giveawayId) and
                        (EntriesTable.participantEmail eq email)
            }.count() > 0
        }
    }

    private fun ResultRow.toEntry() = Entry(
        id = this[EntriesTable.id],
        giveawayId = this[EntriesTable.giveawayId],
        participantName = this[EntriesTable.participantName],
        participantEmail = this[EntriesTable.participantEmail],
        socialHandle = this[EntriesTable.socialHandle],
        entryCount = this[EntriesTable.entryCount],
        createdAt = this[EntriesTable.createdAt].toString()
    )
}
