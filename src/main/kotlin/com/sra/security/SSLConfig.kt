package com.sra.security

import io.ktor.network.tls.certificates.*
import org.slf4j.LoggerFactory
import java.io.File

object SSLConfig {

    private val logger = LoggerFactory.getLogger(SSLConfig::class.java)

    fun generateCertificate() {
        val keystoreFile = File("keystore.jks")

        if (!keystoreFile.exists()) {
            logger.info("Generating SSL certificate...")
            val keystore = generateCertificate(
                file = keystoreFile,
                keyAlias = "sra-key",
                keyPassword = "sra-ssl-password",
                jksPassword = "sra-ssl-password"
            )
            logger.info("SSL certificate created at ${keystoreFile.absolutePath}")
        } else {
            logger.info("SSL certificate already exists")
        }
    }
}