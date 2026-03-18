package com.sra.repository

import com.sra.domain.tables.ActivityLogsTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

@Serializable
data class ActivityEntry(
    val id:        Int,
    val action:    String,
    val status:    String,
    val details:   String?,
    val createdAt: String,
    val icon:      String,
    val label:     String
)

@Serializable
data class ActivityResponse(
    val totalCalls: Int,
    val todayCalls: Int,
    val entries:    List<ActivityEntry>
)

class ActivityRepository {

    fun log(userId: Int, action: String, status: String = "SUCCESS", details: String? = null) {
        try {
            transaction {
                ActivityLogsTable.insert {
                    it[ActivityLogsTable.userId]    = userId
                    it[ActivityLogsTable.action]    = action
                    it[ActivityLogsTable.status]    = status
                    it[ActivityLogsTable.details]   = details
                    it[ActivityLogsTable.createdAt] = LocalDateTime.now()
                }
            }
        } catch (e: Exception) {
            // Never let activity logging break the main request
        }
    }

    fun getActivity(userId: Int, limit: Int = 50): ActivityResponse {
        return transaction {
            val rows = ActivityLogsTable
                .select { ActivityLogsTable.userId eq userId }
                .orderBy(ActivityLogsTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    val action  = row[ActivityLogsTable.action]
                    ActivityEntry(
                        id        = row[ActivityLogsTable.id],
                        action    = action,
                        status    = row[ActivityLogsTable.status],
                        details   = row[ActivityLogsTable.details],
                        createdAt = row[ActivityLogsTable.createdAt].toString(),
                        icon      = iconFor(action),
                        label     = labelFor(action)
                    )
                }

            val today = LocalDateTime.now().toLocalDate()
            val todayCalls = ActivityLogsTable
                .select { ActivityLogsTable.userId eq userId }
                .count { row ->
                    row[ActivityLogsTable.createdAt].toLocalDate() == today
                }

            val total = ActivityLogsTable
                .select { ActivityLogsTable.userId eq userId }
                .count().toInt()

            ActivityResponse(
                totalCalls = total,
                todayCalls = todayCalls,
                entries    = rows
            )
        }
    }

    private fun iconFor(action: String) = when (action) {
        "ENCRYPT"       -> "🔒"
        "DECRYPT"       -> "🔓"
        "KEY_GENERATED" -> "⚿"
        "KEY_ROTATED"   -> "🔄"
        else            -> "⚡"
    }

    private fun labelFor(action: String) = when (action) {
        "ENCRYPT"       -> "Data encrypted"
        "DECRYPT"       -> "Data decrypted"
        "KEY_GENERATED" -> "Encryption key generated"
        "KEY_ROTATED"   -> "Key rotated"
        else            -> action
    }
}