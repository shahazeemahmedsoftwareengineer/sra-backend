package com.sra.repository

import com.sra.domain.tables.ActivityLogsTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(ActivityRepository::class.java)

    fun log(userId: Int, action: String, status: String = "SUCCESS", details: String? = null) {
        try {
            transaction {
                ActivityLogsTable.insert {
                    it[ActivityLogsTable.userId]    = userId
                    it[ActivityLogsTable.action]    = action
                    it[ActivityLogsTable.status]    = status
                    it[ActivityLogsTable.details]   = details?.take(255)
                    it[ActivityLogsTable.createdAt] = LocalDateTime.now()
                }
            }
            logger.info("Activity logged | userId=$userId | action=$action | status=$status")
        } catch (e: Exception) {
            // NOW we log the actual error instead of swallowing it
            logger.error("ACTIVITY LOG FAILED | userId=$userId | action=$action | error=${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun getActivity(userId: Int, limit: Int = 50): ActivityResponse {
        return try {
            transaction {
                val rows = ActivityLogsTable
                    .select { ActivityLogsTable.userId eq userId }
                    .orderBy(ActivityLogsTable.createdAt, SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        val action = row[ActivityLogsTable.action]
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
                val todayCalls = rows.count { entry ->
                    entry.createdAt.startsWith(today.toString())
                }

                ActivityResponse(
                    totalCalls = rows.size,
                    todayCalls = todayCalls,
                    entries    = rows
                )
            }
        } catch (e: Exception) {
            logger.error("GET ACTIVITY FAILED | userId=$userId | error=${e.message}")
            ActivityResponse(totalCalls = 0, todayCalls = 0, entries = emptyList())
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