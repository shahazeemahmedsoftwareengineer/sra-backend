package com.sra.security

import com.sra.utils.BadRequestException

object InputValidator {

    // ── SQL INJECTION PATTERNS ────────────────────────────────────
    private val SQL_INJECTION_PATTERNS = listOf(
        Regex("(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE|UNION|SCRIPT)\\s"),
        Regex("(?i)(OR|AND)\\s+[\\w'\"]+\\s*=\\s*[\\w'\"]+"),
        Regex("--"),           // SQL comment
        Regex(";\\s*\\w"),     // chained SQL statement
        Regex("(?i)xp_"),      // SQL Server stored procs
        Regex("(?i)WAITFOR\\s+DELAY"),
        Regex("(?i)BENCHMARK\\s*\\("),
        Regex("'\\s*(OR|AND)"),
        Regex("1\\s*=\\s*1"),  // classic tautology
        Regex("(?i)SLEEP\\s*\\(")
    )

    // ── XSS PATTERNS ──────────────────────────────────────────────
    private val XSS_PATTERNS = listOf(
        Regex("(?i)<script[^>]*>"),
        Regex("(?i)</script>"),
        Regex("(?i)javascript\\s*:"),
        Regex("(?i)on\\w+\\s*="),      // onclick=, onload=, onerror=
        Regex("(?i)<iframe"),
        Regex("(?i)<img[^>]+src\\s*="),
        Regex("(?i)eval\\s*\\("),
        Regex("(?i)document\\.cookie"),
        Regex("(?i)document\\.write"),
        Regex("(?i)<svg[^>]*onload"),
        Regex("(?i)alert\\s*\\("),
        Regex("(?i)\\bvbscript\\s*:"),
        Regex("(?i)<object"),
        Regex("(?i)<embed"),
        Regex("(?i)base64\\s*,")       // base64 encoded payloads
    )

    // ── PATH TRAVERSAL PATTERNS ───────────────────────────────────
    private val PATH_TRAVERSAL_PATTERNS = listOf(
        Regex("\\.\\./"),       // ../
        Regex("\\.\\.\\\\"),    // ..\
        Regex("%2e%2e%2f", RegexOption.IGNORE_CASE),  // URL encoded ../
        Regex("%252e%252e", RegexOption.IGNORE_CASE)  // double encoded
    )

    // ── MAIN VALIDATE ─────────────────────────────────────────────
    // Call this on any free-text input from user
    fun validate(input: String, fieldName: String) {
        if (input.isBlank()) {
            throw BadRequestException("$fieldName cannot be empty")
        }
        checkSqlInjection(input, fieldName)
        checkXss(input, fieldName)
        checkPathTraversal(input, fieldName)
    }

    // ── LENGTH CHECK ──────────────────────────────────────────────
    fun validateLength(input: String, fieldName: String, maxLength: Int) {
        if (input.length > maxLength) {
            throw BadRequestException(
                "$fieldName exceeds maximum length of $maxLength characters"
            )
        }
    }

    // ── EMAIL VALIDATE ────────────────────────────────────────────
    fun validateEmail(email: String) {
        if (email.isBlank()) {
            throw BadRequestException("Email cannot be empty")
        }
        validateLength(email, "email", 254) // RFC 5321 max

        // Basic structure check
        val emailRegex = Regex("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")
        if (!emailRegex.matches(email.trim())) {
            throw BadRequestException("Invalid email format")
        }

        // Still check for injection even in email
        checkSqlInjection(email, "email")
        checkXss(email, "email")
    }

    // ── PASSWORD VALIDATE ─────────────────────────────────────────
    fun validatePassword(password: String) {
        if (password.isBlank()) {
            throw BadRequestException("Password cannot be empty")
        }
        if (password.length < 8) {
            throw BadRequestException("Password must be at least 8 characters")
        }
        validateLength(password, "password", 128)

        if (!password.any { it.isUpperCase() }) {
            throw BadRequestException("Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isDigit() }) {
            throw BadRequestException("Password must contain at least one number")
        }
        // Note: no XSS/SQL check on password — it gets hashed immediately
        // and bcrypt handles any special chars safely
    }

    // ── VERIFICATION CODE VALIDATE ────────────────────────────────
    // Only allow SRA-K-XXXXXXXX format — no injection possible
    fun validateVerificationCode(code: String) {
        val codeRegex = Regex("^SRA-[A-Z]-[A-Z0-9]{8}$")
        if (!codeRegex.matches(code)) {
            throw BadRequestException("Invalid verification code format")
        }
    }

    // ── PRIVATE CHECKERS ──────────────────────────────────────────

    private fun checkSqlInjection(input: String, fieldName: String) {
        SQL_INJECTION_PATTERNS.forEach { pattern ->
            if (pattern.containsMatchIn(input)) {
                // Log attempt but don't reveal which pattern matched
                IntrusionDetector.recordAttempt(
                    type    = "SQL_INJECTION",
                    field   = fieldName,
                    details = "Blocked SQL injection attempt in $fieldName"
                )
                throw BadRequestException("Invalid characters in $fieldName")
            }
        }
    }

    private fun checkXss(input: String, fieldName: String) {
        XSS_PATTERNS.forEach { pattern ->
            if (pattern.containsMatchIn(input)) {
                IntrusionDetector.recordAttempt(
                    type    = "XSS_ATTEMPT",
                    field   = fieldName,
                    details = "Blocked XSS attempt in $fieldName"
                )
                throw BadRequestException("Invalid characters in $fieldName")
            }
        }
    }

    private fun checkPathTraversal(input: String, fieldName: String) {
        PATH_TRAVERSAL_PATTERNS.forEach { pattern ->
            if (pattern.containsMatchIn(input)) {
                IntrusionDetector.recordAttempt(
                    type    = "PATH_TRAVERSAL",
                    field   = fieldName,
                    details = "Blocked path traversal attempt in $fieldName"
                )
                throw BadRequestException("Invalid characters in $fieldName")
            }
        }
    }
}