package com.sra.repository

import com.sra.domain.models.Giveaway
import com.sra.domain.tables.GiveawaysTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class GiveawayRepository {

    fun create(
        userId: Int,
        title: String,
        prizeDescription: String,
        entryRules: String?,
        endDate: LocalDateTime?
    ): Giveaway {
        return transaction {
            val id = GiveawaysTable.insert {
                it[GiveawaysTable.userId] = userId
                it[GiveawaysTable.title] = title
                it[GiveawaysTable.prizeDescription] = prizeDescription
                it[GiveawaysTable.entryRules] = entryRules
                it[GiveawaysTable.status] = "active"
                it[GiveawaysTable.endDate] = endDate
                it[GiveawaysTable.createdAt] = LocalDateTime.now()
                it[GiveawaysTable.updatedAt] = LocalDateTime.now()
            }[GiveawaysTable.id]
            findById(id)!!
        }
    }

    fun findById(id: Int): Giveaway? {
        return transaction {
            GiveawaysTable.select { GiveawaysTable.id eq id }
                .singleOrNull()
                ?.toGiveaway()
        }
    }

    fun findByUserId(userId: Int): List<Giveaway> {
        return transaction {
            GiveawaysTable.select { GiveawaysTable.userId eq userId }
                .orderBy(GiveawaysTable.createdAt, SortOrder.DESC)
                .map { it.toGiveaway() }
        }
    }

    fun updateStatus(id: Int, status: String) {
        transaction {
            GiveawaysTable.update({ GiveawaysTable.id eq id }) {
                it[GiveawaysTable.status] = status
                it[GiveawaysTable.updatedAt] = LocalDateTime.now()
            }
        }
    }

    fun updateWinner(id: Int, winnerEntryId: Int) {
        transaction {
            GiveawaysTable.update({ GiveawaysTable.id eq id }) {
                it[GiveawaysTable.winnerEntryId] = winnerEntryId
                it[GiveawaysTable.status] = "completed"
                it[GiveawaysTable.updatedAt] = LocalDateTime.now()
            }
        }
    }

    fun incrementEntryCount(id: Int) {
        transaction {
            GiveawaysTable.update({ GiveawaysTable.id eq id }) {
                with(SqlExpressionBuilder) {
                    it.update(GiveawaysTable.totalEntries, GiveawaysTable.totalEntries + 1)
                }
                it[GiveawaysTable.updatedAt] = LocalDateTime.now()
            }
        }
    }

    private fun ResultRow.toGiveaway() = Giveaway(
        id = this[GiveawaysTable.id],
        userId = this[GiveawaysTable.userId],
        title = this[GiveawaysTable.title],
        prizeDescription = this[GiveawaysTable.prizeDescription],
        entryRules = this[GiveawaysTable.entryRules],
        status = this[GiveawaysTable.status],
        totalEntries = this[GiveawaysTable.totalEntries],
        winnerEntryId = this[GiveawaysTable.winnerEntryId],
        startDate = this[GiveawaysTable.startDate].toString(),
        endDate = this[GiveawaysTable.endDate]?.toString(),
        createdAt = this[GiveawaysTable.createdAt].toString()
    )
}
