package com.sra.repository

import com.sra.domain.tables.UsersTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

data class UsageData(
    val planName:        String,
    val callsUsed:       Int,
    val callsLimit:      Int,     // -1 = unlimited
    val callsRemaining:  Int,
    val percentUsed:     Double,
    val resetDate:       String,
    val daysUntilReset:  Long,
    val warningLevel:    String,  // "none" | "warning" | "critical" | "exceeded"
    val todayCalls:      Int,
    val dailyHistory:    List<Int> // last 7 days, newest last
)

class UsageRepository {

    // Plan limits
    private fun planLimit(plan: String): Int = when (plan.lowercase()) {
        "starter"    -> 1_000
        "business"   -> 100_000
        "enterprise" -> -1        // unlimited
        else         -> 1_000     // free = same as starter
    }

    // ── INCREMENT CALL COUNTER ────────────────────────────────────
    fun incrementApiCalls(userId: Int) {
        transaction {
            // Auto-reset if past reset date
            val row = UsersTable.select { UsersTable.id eq userId }.singleOrNull()
            if (row != null) {
                val resetDate = row[UsersTable.apiCallsResetDate]
                if (LocalDateTime.now().isAfter(resetDate)) {
                    // Reset counter and push reset date forward 1 month
                    UsersTable.update({ UsersTable.id eq userId }) {
                        it[apiCallsUsed]      = 1
                        it[apiCallsResetDate] = LocalDateTime.now().plusMonths(1)
                        it[updatedAt]         = LocalDateTime.now()
                    }
                    return@transaction
                }
            }
            // Normal increment
            UsersTable.update({ UsersTable.id eq userId }) {
                it[apiCallsUsed] = UsersTable.apiCallsUsed + 1
                it[updatedAt]    = LocalDateTime.now()
            }
        }
    }

    // ── GET USAGE DATA ────────────────────────────────────────────
    fun getUsage(userId: Int): UsageData {
        return transaction {
            val row = UsersTable.select { UsersTable.id eq userId }.singleOrNull()
                ?: return@transaction defaultUsage()

            val plan      = row[UsersTable.plan]
            val used      = row[UsersTable.apiCallsUsed]
            val limit     = planLimit(plan)
            val resetDate = row[UsersTable.apiCallsResetDate]
            val now       = LocalDateTime.now()

            val remaining = if (limit == -1) -1 else maxOf(0, limit - used)
            val percent   = if (limit == -1) 0.0 else (used.toDouble() / limit) * 100
            val daysLeft  = java.time.Duration.between(now, resetDate).toDays()

            val warning = when {
                limit == -1          -> "none"
                percent >= 100.0     -> "exceeded"
                percent >= 95.0      -> "critical"
                percent >= 80.0      -> "warning"
                else                 -> "none"
            }

            // Fake daily history based on total (we'll make real later)
            // For now distribute used calls across 7 days realistically
            val daily = generateDailyHistory(used)

            UsageData(
                planName       = plan,
                callsUsed      = used,
                callsLimit     = limit,
                callsRemaining = remaining,
                percentUsed    = String.format("%.2f", percent).toDouble(),
                resetDate      = resetDate.toLocalDate().toString(),
                daysUntilReset = maxOf(0, daysLeft),
                warningLevel   = warning,
                todayCalls     = daily.last(),
                dailyHistory   = daily
            )
        }
    }

    private fun generateDailyHistory(total: Int): List<Int> {
        // Spread total across 7 days, most recent day has most
        if (total == 0) return List(7) { 0 }
        val days = MutableList(7) { 0 }
        days[6] = total  // all in today for now (will be replaced with real daily tracking later)
        return days
    }

    private fun defaultUsage() = UsageData(
        planName       = "free",
        callsUsed      = 0,
        callsLimit     = 1000,
        callsRemaining = 1000,
        percentUsed    = 0.0,
        resetDate      = LocalDateTime.now().plusMonths(1).toLocalDate().toString(),
        daysUntilReset = 30,
        warningLevel   = "none",
        todayCalls     = 0,
        dailyHistory   = List(7) { 0 }
    )
}