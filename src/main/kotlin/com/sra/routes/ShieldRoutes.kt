package com.sra.routes

import com.sra.domain.models.DecryptRequest
import com.sra.domain.models.EncryptRequest
import com.sra.domain.models.GenerateKeyRequest
import com.sra.repository.ShieldRepository
import com.sra.security.InputValidator
import com.sra.service.ShieldService
import com.sra.utils.AES256GCM
import com.sra.utils.ApiResponse
import com.sra.utils.requireUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ShieldRoutes")

fun Route.shieldRoutes(shieldService: ShieldService, shieldRepository: ShieldRepository) {

    route("/api/v1/shield") {

        // ── PROTECTED ROUTES (require JWT) ────────────────────────
        rateLimit(RateLimitName("shield")) {
            authenticate("auth-jwt") {

                // POST /api/v1/shield/keys/generate
                post("/keys/generate") {
                    val userId = call.requireUserId()
                    val request = try {
                        call.receive<GenerateKeyRequest>()
                    } catch (e: Exception) {
                        GenerateKeyRequest(tier = "full")
                    }
                    val validTiers = setOf("fast", "medium", "full")
                    val tier = if (request.tier in validTiers) request.tier else "full"
                    logger.info("Key generation requested | userId=$userId | tier=$tier")
                    val result = shieldService.generateKey(userId, tier)
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse.success(result, "Encryption key generated successfully")
                    )
                }

                // POST /api/v1/shield/keys/rotate
                post("/keys/rotate") {
                    val userId = call.requireUserId()
                    logger.info("Key rotation requested | userId=$userId")
                    val result = shieldService.rotateKey(userId)
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse.success(result, "Key rotated successfully")
                    )
                }

                // GET /api/v1/shield/keys/audit
                get("/keys/audit") {
                    val userId = call.requireUserId()
                    val auditLog = shieldService.getAuditLog(userId)
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse.success(auditLog, "Audit log retrieved")
                    )
                }

                // Encrypt data using a Shield key
                post("/encrypt") {
                    val userId = call.requireUserId()
                    val request = call.receive<EncryptRequest>()

                    // Validate input
                    InputValidator.validate(request.plaintext, "plaintext")
                    InputValidator.validateLength(request.plaintext, "plaintext", 10000)

                    val activeKey = shieldRepository.getInternalActiveKey(userId)
                        ?: throw com.sra.utils.NotFoundException("No active key. Generate a key first.")

                    val encrypted = AES256GCM.encrypt(request.plaintext, activeKey.encryptionKey)
                    call.respond(HttpStatusCode.OK, ApiResponse(
                        success = true,
                        message = "Encrypted with AES-256-GCM",
                        data = mapOf("encrypted" to encrypted, "algorithm" to "AES-256-GCM")
                    ))
                }

                // Decrypt data using a Shield key
                post("/decrypt") {
                    val userId = call.requireUserId()
                    val request = call.receive<DecryptRequest>()

                    val activeKey = shieldRepository.getInternalActiveKey(userId)
                        ?: throw com.sra.utils.NotFoundException("No active key found.")

                    val decrypted = AES256GCM.decrypt(request.encrypted, activeKey.encryptionKey)

                    call.respond(HttpStatusCode.OK, ApiResponse(
                        success = true,
                        message = "Data decrypted successfully",
                        data = mapOf("plaintext" to decrypted)
                    ))
                }
            }
        }

        // ── PUBLIC ROUTES (no auth needed) ────────────────────────
        rateLimit(RateLimitName("public")) {
            get("/verify/{code}") {
                val code = call.parameters["code"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.error("Verification code required")
                    )
                val result = shieldService.verifyKey(code)
                if (!result.valid) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse.error("Verification code not found")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse.success(result, "Verification complete")
                    )
                }
            }
        }
    }
}