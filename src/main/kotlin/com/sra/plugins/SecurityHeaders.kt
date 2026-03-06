package com.sra.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.configureSecurityHeaders() {
    install(DefaultHeaders) {
        header("X-Frame-Options", "DENY")
        header("X-Content-Type-Options", "nosniff")
        header("X-XSS-Protection", "1; mode=block")
        header("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
        header("Referrer-Policy", "no-referrer")
        header("Server", "SRA-Shield")
        header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
        header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
    }
}