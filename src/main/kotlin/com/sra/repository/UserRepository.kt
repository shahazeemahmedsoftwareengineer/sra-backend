package com.sra.repository

import com.sra.domain.models.User
import com.sra.domain.tables.UsersTable
import com.sra.utils.HashUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class UserRepository {

    fun create(email: String, password: String, name: String): User {
        return transaction {
            val hashedPassword = HashUtils.hashPassword(password)
            val id = UsersTable.insert {
                it[UsersTable.email] = email
                it[UsersTable.password] = hashedPassword
                it[UsersTable.name] = name
                it[UsersTable.plan] = "free"
                it[UsersTable.createdAt] = LocalDateTime.now()
                it[UsersTable.updatedAt] = LocalDateTime.now()
            }[UsersTable.id]
            findById(id)!!
        }
    }

    fun findById(id: Int): User? {
        return transaction {
            UsersTable.select { UsersTable.id eq id }
                .singleOrNull()
                ?.toUser()
        }
    }

    fun findByEmail(email: String): Triple<User, String, Boolean>? {
        return transaction {
            UsersTable.select { UsersTable.email eq email }
                .singleOrNull()
                ?.let {
                    Triple(
                        it.toUser(),
                        it[UsersTable.password],
                        it[UsersTable.isActive]
                    )
                }
        }
    }

    fun findUserByEmail(email: String): User? {
        return transaction {
            UsersTable.select { UsersTable.email eq email }
                .singleOrNull()
                ?.toUser()
        }
    }

    // ── UPDATE PLAN BY EMAIL (called from Stripe webhook) ──
    fun updatePlanByEmail(email: String, plan: String): Boolean {
        return transaction {
            val updated = UsersTable.update({ UsersTable.email eq email }) {
                it[UsersTable.plan] = plan
                it[UsersTable.updatedAt] = LocalDateTime.now()
            }
            updated > 0
        }
    }

    // ── UPDATE PLAN BY USER ID ──
    fun updatePlanById(userId: Int, plan: String): Boolean {
        return transaction {
            val updated = UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.plan] = plan
                it[UsersTable.updatedAt] = LocalDateTime.now()
            }
            updated > 0
        }
    }

    fun generateApiKey(userId: Int): String {
        val apiKey = "sra_${UUID.randomUUID().toString().replace("-", "")}"
        transaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.apiKey] = apiKey
                it[UsersTable.updatedAt] = LocalDateTime.now()
            }
        }
        return apiKey
    }

    fun findByApiKey(apiKey: String): User? {
        return transaction {
            UsersTable.select { UsersTable.apiKey eq apiKey }
                .singleOrNull()
                ?.toUser()
        }
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        name = this[UsersTable.name],
        plan = this[UsersTable.plan],
        apiKey = this[UsersTable.apiKey],
        isActive = this[UsersTable.isActive],
        createdAt = this[UsersTable.createdAt].toString()
    )
}