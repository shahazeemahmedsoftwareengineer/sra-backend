package com.sra.security

import com.sra.utils.BadRequestException
import org.slf4j.LoggerFactory

object InputValidator {

    private val logger = LoggerFactory.getLogger(InputValidator::class.java)

    private val sqlInjectionPatterns = listOf(
        "' OR ", "' AND ", "1=1", "1 = 1",
        "UNION SELECT", "DROP TABLE", "DROP DATABASE",
        "INSERT INTO", "DELETE FROM", "UPDATE SET",
        "--", "/*", "*/", "xp_", "EXEC(",
        "EXECUTE(", "CAST(", "CONVERT(",
        "CHAR(", "NCHAR(", "VARCHAR("
    )

    private val xssPatterns = listOf(
        "<script>", "</script>", "javascript:",
        "onerror=", "onload=", "onclick=",
        "onmouseover=", "eval(", "alert(",
        "document.cookie", "window.location",
        "<iframe>", "<img src=", "data:text/html"
    )

    private val pathTraversalPatterns = listOf(
        "../", "..\\", "%2e%2e",
        "/etc/passwd", "/etc/shadow",
        "C:\\Windows", "cmd.exe",
        "/bin/bash", "/bin/sh"
    )

    // Master check — runs all validations
    fun validate(input: String, fieldName: String = "input"): String {
        if (input.isBlank()) return input

        val upper = input.uppercase()

        // Check SQL injection
        sqlInjectionPatterns.forEach { pattern ->
            if (upper.contains(pattern.uppercase())) {
                logger.warn("SQL injection attempt detected in $fieldName: $pattern")
                throw BadRequestException("Invalid characters in $fieldName")
            }
        }

        // Check XSS
        xssPatterns.forEach { pattern ->
            if (upper.contains(pattern.uppercase())) {
                logger.warn("XSS attempt detected in $fieldName: $pattern")
                throw BadRequestException("Invalid characters in $fieldName")
            }
        }

        // Check path traversal
        pathTraversalPatterns.forEach { pattern ->
            if (upper.contains(pattern.uppercase())) {
                logger.warn("Path traversal attempt in $fieldName: $pattern")
                throw BadRequestException("Invalid characters in $fieldName")
            }
        }

        return input
    }

    fun validateEmail(email: String): String {
        val trimmed = email.trim().lowercase()
        if (!trimmed.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            throw BadRequestException("Invalid email format")
        }
        return trimmed
    }

    fun validatePassword(password: String) {
        if (password.length < 8) {
            throw BadRequestException("Password must be at least 8 characters")
        }
        if (password.length > 128) {
            throw BadRequestException("Password too long")
        }
    }

    fun validateTier(tier: String): String {
        val valid = listOf("fast", "medium", "full")
        if (tier.lowercase() !in valid) {
            throw BadRequestException("Tier must be fast, medium or full")
        }
        return tier.lowercase()
    }

    fun validateLength(input: String, fieldName: String, maxLength: Int): String {
        if (input.length > maxLength) {
            throw BadRequestException("$fieldName too long (max $maxLength characters)")
        }
        return input
    }
}