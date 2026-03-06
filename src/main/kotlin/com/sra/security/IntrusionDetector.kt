package com.sra.security

import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object IntrusionDetector {

    private val logger = LoggerFactory.getLogger(IntrusionDetector::class.java)

    // Track requests per IP per minute
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val requestTimestamps = ConcurrentHashMap<String, Instant>()
    private val alerts = mutableListOf<SecurityAlert>()

    data class SecurityAlert(
        val type: String,
        val ip: String,
        val details: String,
        val timestamp: Instant = Instant.now()
    )

    fun recordRequest(ip: String, path: String, userAgent: String?) {

        // Detect port scanning — too many different endpoints hit
        val count = requestCounts
            .getOrPut(ip) { AtomicInteger(0) }
            .incrementAndGet()

        val firstSeen = requestTimestamps.getOrPut(ip) { Instant.now() }
        val secondsElapsed = Instant.now().epochSecond - firstSeen.epochSecond

        // More than 100 requests in under 10 seconds = scanner
        if (count > 100 && secondsElapsed < 10) {
            triggerAlert("PORT_SCAN_DETECTED", ip,
                "IP sent $count requests in ${secondsElapsed}s")
            IPBlocker.recordFailedLogin(ip)
        }

        // Detect suspicious paths
        val suspiciousPaths = listOf(
            "/admin", "/wp-admin", "/phpmyadmin",
            "/.env", "/.git", "/config",
            "/backup", "/database", "/db",
            "/shell", "/cmd", "/eval"
        )
        suspiciousPaths.forEach { suspicious ->
            if (path.lowercase().contains(suspicious)) {
                triggerAlert("SUSPICIOUS_PATH", ip,
                    "Accessed suspicious path: $path")
            }
        }

        // Detect vulnerability scanners by user agent
        val scannerAgents = listOf(
            "nikto", "sqlmap", "nmap", "masscan",
            "zgrab", "dirbuster", "gobuster",
            "burpsuite", "metasploit"
        )
        userAgent?.let { ua ->
            scannerAgents.forEach { scanner ->
                if (ua.lowercase().contains(scanner)) {
                    triggerAlert("SCANNER_DETECTED", ip,
                        "Known scanner detected: $ua")
                    IPBlocker.recordFailedLogin(ip)
                }
            }
        }
    }

    fun getAlerts(): List<SecurityAlert> = alerts.takeLast(100)

    fun getAlertCount(): Int = alerts.size

    private fun triggerAlert(type: String, ip: String, details: String) {
        val alert = SecurityAlert(type, ip, details)
        alerts.add(alert)
        logger.error("SECURITY ALERT | $type | IP: $ip | $details")
        // TODO: Add email/Slack notification here
    }
}