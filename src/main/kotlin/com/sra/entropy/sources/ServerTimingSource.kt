package com.sra.entropy.sources

import org.slf4j.LoggerFactory

class ServerTimingSource {

    private val logger = LoggerFactory.getLogger(ServerTimingSource::class.java)
    private val timings = mutableListOf<Long>()

    fun recordTiming() {
        synchronized(timings) {
            timings.add(System.nanoTime())
            if (timings.size > 1000) {
                timings.removeAt(0)
            }
        }
    }

    fun collect(): String {
        recordTiming()
        val gaps = synchronized(timings) {
            if (timings.size < 2) {
                listOf(System.nanoTime())
            } else {
                timings.zipWithNext { a, b -> b - a }.takeLast(20)
            }
        }
        val result = gaps.joinToString("-")
        logger.debug("Server timing entropy collected: ${gaps.size} gaps")
        return result
    }

    fun getSourceName() = "SERVER_TIMING"
}