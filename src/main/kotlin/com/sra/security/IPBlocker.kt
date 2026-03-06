package com.sra.security

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.time.Instant
import org.slf4j.LoggerFactory

object IPBlocker {

    private val logger = LoggerFactory.getLogger(IPBlocker::class.java)
    private val failedAttempts = ConcurrentHashMap<String, AtomicInteger>()
    private val blockedIPs = ConcurrentHashMap<String, Instant>()

    fun recordFailedLogin(ip: String) {
        val count = failedAttempts
            .getOrPut(ip) { AtomicInteger(0) }
            .incrementAndGet()

        logger.warn("Failed login IP: $ip | Total attempts: $count")

        when {
            count >= 10 -> blockIP(ip, hours = 24)
            count >= 5  -> blockIP(ip, hours = 1)
        }
    }

    fun recordSuccessfulLogin(ip: String) {
        failedAttempts.remove(ip)
        logger.info("Successful login — cleared failed attempts for IP: $ip")
    }

    fun isBlocked(ip: String): Boolean {
        val blockedUntil = blockedIPs[ip] ?: return false
        return if (Instant.now().isBefore(blockedUntil)) {
            logger.warn("Blocked IP tried to access: $ip")
            true
        } else {
            blockedIPs.remove(ip)
            failedAttempts.remove(ip)
            false
        }
    }

    fun getStats(): Map<String, Any> {
        return mapOf(
            "totalBlockedIPs" to blockedIPs.size,
            "blockedIPs" to blockedIPs.keys.toList(),
            "suspiciousIPs" to failedAttempts
                .filter { it.value.get() >= 3 }
                .map { "${it.key} (${it.value.get()} attempts)" }
        )
    }

    private fun blockIP(ip: String, hours: Long) {
        blockedIPs[ip] = Instant.now().plusSeconds(hours * 3600)
        logger.error("IP BLOCKED: $ip for $hours hours")
    }
}