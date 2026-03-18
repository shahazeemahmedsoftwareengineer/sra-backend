package com.sra.routes

import com.sra.domain.models.DecryptRequest
import com.sra.domain.models.EncryptRequest
import com.sra.domain.models.GenerateKeyRequest
import com.sra.repository.ActivityRepository
import com.sra.repository.ShieldRepository
import com.sra.repository.UsageRepository
import com.sra.security.InputValidator
import com.sra.service.ShieldService
import com.sra.utils.AES256GCM
import com.sra.utils.ApiResponse
import com.sra.utils.NotFoundException
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

fun Route.shieldRoutes(
    shieldService:      ShieldService,
    shieldRepository:   ShieldRepository,
    usageRepository:    UsageRepository,
    activityRepository: ActivityRepository
) {
    route("/api/v1/shield") {

        rateLimit(RateLimitName("shield")) {
            authenticate("auth-jwt") {

                // ── GENERATE KEY ──────────────────────────────────
                post("/keys/generate") {
                    val userId  = call.requireUserId()
                    val request = try { call.receive<GenerateKeyRequest>() }
                    catch (e: Exception) { GenerateKeyRequest(tier = "full") }
                    val tier = if (request.tier in setOf("fast","medium","full")) request.tier else "full"

                    val usage = usageRepository.getUsage(userId)
                    if (usage.warningLevel == "exceeded") {
                        activityRepository.log(userId, "KEY_GENERATED", "FAILED", "Plan limit reached")
                        call.respond(HttpStatusCode.PaymentRequired,
                            ApiResponse(false, "Monthly API call limit reached. Please upgrade your plan.", null))
                        return@post
                    }

                    val result = shieldService.generateKey(userId, tier)
                    usageRepository.incrementApiCalls(userId)
                    activityRepository.log(userId, "KEY_GENERATED", "SUCCESS", "Tier: $tier · ${result.verificationCode}")
                    logger.info("Key generated | userId=$userId | tier=$tier")
                    call.respond(HttpStatusCode.OK, ApiResponse.success(result, "Encryption key generated successfully"))
                }

                // ── ROTATE KEY ────────────────────────────────────
                post("/keys/rotate") {
                    val userId = call.requireUserId()
                    val result = shieldService.rotateKey(userId)
                    usageRepository.incrementApiCalls(userId)
                    activityRepository.log(userId, "KEY_ROTATED", "SUCCESS", "Full entropy tier")
                    logger.info("Key rotated | userId=$userId")
                    call.respond(HttpStatusCode.OK, ApiResponse.success(result, "Key rotated successfully"))
                }

                // ── AUDIT LOG ─────────────────────────────────────
                get("/keys/audit") {
                    val userId   = call.requireUserId()
                    val auditLog = shieldService.getAuditLog(userId)
                    call.respond(HttpStatusCode.OK, ApiResponse.success(auditLog, "Audit log retrieved"))
                }

                // ── ENCRYPT ───────────────────────────────────────
                post("/encrypt") {
                    val userId = call.requireUserId()

                    val usage = usageRepository.getUsage(userId)
                    if (usage.warningLevel == "exceeded") {
                        activityRepository.log(userId, "ENCRYPT", "FAILED", "Plan limit reached")
                        call.respond(HttpStatusCode.PaymentRequired,
                            ApiResponse(false, "Monthly API call limit reached. Please upgrade your plan.", null))
                        return@post
                    }

                    val request = call.receive<EncryptRequest>()
                    InputValidator.validate(request.plaintext, "plaintext")
                    InputValidator.validateLength(request.plaintext, "plaintext", 10000)

                    val activeKey = shieldRepository.getInternalActiveKey(userId)
                        ?: throw NotFoundException("No active key. Generate a key first.")

                    val encrypted = AES256GCM.encrypt(request.plaintext, activeKey.encryptionKey)
                    usageRepository.incrementApiCalls(userId)
                    activityRepository.log(userId, "ENCRYPT", "SUCCESS", "AES-256-GCM · ${request.plaintext.length} chars")

                    call.respond(HttpStatusCode.OK, ApiResponse(
                        success = true,
                        message = "Encrypted with AES-256-GCM",
                        data    = mapOf("encrypted" to encrypted, "algorithm" to "AES-256-GCM")
                    ))
                }

                // ── DECRYPT ───────────────────────────────────────
                post("/decrypt") {
                    val userId = call.requireUserId()

                    val usage = usageRepository.getUsage(userId)
                    if (usage.warningLevel == "exceeded") {
                        activityRepository.log(userId, "DECRYPT", "FAILED", "Plan limit reached")
                        call.respond(HttpStatusCode.PaymentRequired,
                            ApiResponse(false, "Monthly API call limit reached. Please upgrade your plan.", null))
                        return@post
                    }

                    val request   = call.receive<DecryptRequest>()
                    val activeKey = shieldRepository.getInternalActiveKey(userId)
                        ?: throw NotFoundException("No active key found.")

                    val decrypted = AES256GCM.decrypt(request.encrypted, activeKey.encryptionKey)
                    usageRepository.incrementApiCalls(userId)
                    activityRepository.log(userId, "DECRYPT", "SUCCESS", "AES-256-GCM")

                    call.respond(HttpStatusCode.OK, ApiResponse(
                        success = true,
                        message = "Data decrypted successfully",
                        data    = mapOf("plaintext" to decrypted)
                    ))
                }

                // ── USAGE ─────────────────────────────────────────
                get("/usage") {
                    val userId = call.requireUserId()
                    val usage  = usageRepository.getUsage(userId)
                    call.respond(HttpStatusCode.OK, ApiResponse.success(usage, "Usage retrieved"))
                }

                // ── ACTIVITY ──────────────────────────────────────
                get("/activity") {
                    val userId   = call.requireUserId()
                    val activity = activityRepository.getActivity(userId)
                    call.respond(HttpStatusCode.OK, ApiResponse.success(activity, "Activity retrieved"))
                }
            }
        }

        // ── PUBLIC ────────────────────────────────────────────────
        rateLimit(RateLimitName("public")) {
            get("/verify/{code}") {
                val code = call.parameters["code"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest,
                        ApiResponse.error("Verification code required"))
                val result = shieldService.verifyKey(code)
                if (!result.valid) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse.error("Verification code not found"))
                } else {
                    call.respond(HttpStatusCode.OK, ApiResponse.success(result, "Verification complete"))
                }
            }
        }
    }
}
