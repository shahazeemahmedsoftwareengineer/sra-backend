package com.sra.routes

import com.sra.domain.models.DecryptRequest
import com.sra.domain.models.EncryptRequest
import com.sra.domain.models.GenerateKeyRequest
import com.sra.repository.ShieldRepository
import com.sra.repository.UsageRepository
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

// Create a reusable plugin to check API usage limits
private val CheckUsageLimit = createRouteScopedPlugin("CheckUsageLimit") {
    val usageRepository = UsageRepository() // Or get it via DI

    on(Call) { call ->
        val userId = call.requireUserId()
        val usage = usageRepository.getUsage(userId)
        if (usage.warningLevel == "exceeded") {
            call.respond(HttpStatusCode.PaymentRequired, ApiResponse(
                success = false,
                message = "Monthly API call limit reached. Please upgrade your plan."
            ))
            finish() // Stop processing the request
        }
    }
}

fun Route.shieldRoutes(
    shieldService:    ShieldService,
    shieldRepository: ShieldRepository,
    usageRepository:  UsageRepository
) {

    route("/api/v1/shield") {

        // ── PROTECTED ROUTES (require JWT) ────────────────────────
        rateLimit(RateLimitName("shield")) {
            authenticate("auth-jwt") {

                // Install the usage check plugin for all routes in this block
                install(CheckUsageLimit)

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

                    // Track usage
                    usageRepository.incrementApiCalls(userId)

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
                    usageRepository.incrementApiCalls(userId)
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

                // POST /api/v1/shield/encrypt
                post("/encrypt") {
                    val userId = call.requireUserId()
                    val request = call.receive<EncryptRequest>()
                    InputValidator.validate(request.plaintext, "plaintext")
                    InputValidator.validateLength(request.plaintext, "plaintext", 10000)

                    val activeKey = shieldRepository.getInternalActiveKey(userId)
                        ?: throw com.sra.utils.NotFoundException("No active key. Generate a key first.")

                    val encrypted = AES256GCM.encrypt(request.plaintext, activeKey.encryptionKey)

                    // Track usage
                    usageRepository.incrementApiCalls(userId)

                    call.respond(HttpStatusCode.OK, ApiResponse(
                        success = true,
                        message = "Encrypted with AES-256-GCM",
                        data = mapOf("encrypted" to encrypted, "algorithm" to "AES-256-GCM")
                    ))
                }

                // POST /api/v1/shield/decrypt
                post("/decrypt") {
                    val userId = call.requireUserId()
                    val request = call.receive<DecryptRequest>()
                    val activeKey = shieldRepository.getInternalActiveKey(userId)
                        ?: throw com.sra.utils.NotFoundException("No active key found.")

                    val decrypted = AES256GCM.decrypt(request.encrypted, activeKey.encryptionKey)

                    // Track usage
                    usageRepository.incrementApiCalls(userId)

                    call.respond(HttpStatusCode.OK, ApiResponse(
                        success = true,
                        message = "Data decrypted successfully",
                        data = mapOf("plaintext" to decrypted)
                    ))
                }

                // GET /api/v1/shield/usage
                get("/usage") {
                    val userId = call.requireUserId()
                    val usage  = usageRepository.getUsage(userId)
                    call.respond(HttpStatusCode.OK, ApiResponse(
                        success = true,
                        message = "Usage retrieved",
                        data    = usage
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