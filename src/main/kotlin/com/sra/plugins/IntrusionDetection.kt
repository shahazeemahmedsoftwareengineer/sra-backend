package com.sra.plugins

import com.sra.security.IntrusionDetector
import io.ktor.server.application.*
import io.ktor.server.request.*

fun Application.configureIntrusionDetection() {
    intercept(ApplicationCallPipeline.Monitoring) {
        val ip = call.request.local.remoteAddress
        val path = call.request.path()
        val userAgent = call.request.headers["User-Agent"]
        IntrusionDetector.recordRequest(ip, path, userAgent)
    }
}