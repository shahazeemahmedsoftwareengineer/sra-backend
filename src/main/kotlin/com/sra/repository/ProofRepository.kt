package com.sra.repository

import com.sra.domain.models.Proof
import com.sra.domain.tables.ProofsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class ProofRepository {

    fun create(
        giveawayId: Int,
        winnerEntryId: Int,
        publicCode: String,
        seedGenerated: String,
        sourcesUsed: String,
        calculationDetails: String
    ): Proof {
        return transaction {
            val id = ProofsTable.insert {
                it[ProofsTable.giveawayId] = giveawayId
                it[ProofsTable.winnerEntryId] = winnerEntryId
                it[ProofsTable.publicCode] = publicCode
                it[ProofsTable.seedGenerated] = seedGenerated
                it[ProofsTable.sourcesUsed] = sourcesUsed
                it[ProofsTable.calculationDetails] = calculationDetails
                it[ProofsTable.isPublic] = true
                it[ProofsTable.createdAt] = LocalDateTime.now()
            }[ProofsTable.id]
            findById(id)!!
        }
    }

    fun findById(id: Int): Proof? {
        return transaction {
            ProofsTable.select { ProofsTable.id eq id }
                .singleOrNull()
                ?.toProof()
        }
    }

    fun findByPublicCode(code: String): Proof? {
        return transaction {
            ProofsTable.select { ProofsTable.publicCode eq code }
                .singleOrNull()
                ?.toProof()
        }
    }

    fun findByGiveawayId(giveawayId: Int): Proof? {
        return transaction {
            ProofsTable.select { ProofsTable.giveawayId eq giveawayId }
                .singleOrNull()
                ?.toProof()
        }
    }

    private fun ResultRow.toProof() = Proof(
        id = this[ProofsTable.id],
        giveawayId = this[ProofsTable.giveawayId],
        winnerEntryId = this[ProofsTable.winnerEntryId],
        publicCode = this[ProofsTable.publicCode],
        seedGenerated = this[ProofsTable.seedGenerated],
        sourcesUsed = this[ProofsTable.sourcesUsed],
        calculationDetails = this[ProofsTable.calculationDetails],
        isPublic = this[ProofsTable.isPublic],
        createdAt = this[ProofsTable.createdAt].toString(),
        publicUrl = "/proof/${this[ProofsTable.publicCode]}"
    )
}
