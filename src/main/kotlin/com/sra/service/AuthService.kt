package com.sra.service

import com.sra.config.SecurityConfig
import com.sra.domain.models.*
import com.sra.repository.UserRepository
import com.sra.utils.BadRequestException
import com.sra.utils.UnauthorizedException
import org.slf4j.LoggerFactory

class AuthService(private val userRepository: UserRepository) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun register(request: RegisterRequest): AuthResponse {
        validateRegisterRequest(request)

        val existing = userRepository.findByEmail(request.email)
        if (existing != null) {
            throw BadRequestException("Email already registered")
        }

        val user = userRepository.create(
            email = request.email.lowercase().trim(),
            password = request.password,
            name = request.name.trim()
        )

        val token = SecurityConfig.generateToken(user.id, user.email)
        logger.info("New user registered: ${user.email}")

        return AuthResponse(token = token, user = user)
    }

    fun login(request: LoginRequest): AuthResponse {
        val result = userRepository.findByEmail(request.email.lowercase().trim())
            ?: throw UnauthorizedException("Invalid email or password")

        val (user, hashedPassword, isActive) = result

        if (!isActive) throw UnauthorizedException("Account is disabled")

        val passwordValid = com.sra.utils.HashUtils.verifyPassword(request.password, hashedPassword)
        if (!passwordValid) throw UnauthorizedException("Invalid email or password")

        val token = SecurityConfig.generateToken(user.id, user.email)
        logger.info("User logged in: ${user.email}")

        // Always return fresh user from DB (includes latest plan)
        return AuthResponse(token = token, user = user)
    }

    fun generateApiKey(userId: Int): String {
        val apiKey = userRepository.generateApiKey(userId)
        logger.info("API key generated for user: $userId")
        return apiKey
    }

    // ── NEW: Update plan by email (called from Stripe webhook) ──
    fun updatePlanByEmail(email: String, plan: String): Boolean {
        val updated = userRepository.updatePlanByEmail(email.lowercase().trim(), plan)
        if (updated) {
            logger.info("Plan updated via webhook: $email → $plan")
        }
        return updated
    }

    // ── NEW: Get user by email (for plan sync) ──
    fun getUserByEmail(email: String): com.sra.domain.models.User? {
        return userRepository.findUserByEmail(email.lowercase().trim())
    }

    private fun validateRegisterRequest(request: RegisterRequest) {
        if (request.email.isBlank()) throw BadRequestException("Email is required")
        if (!request.email.contains("@")) throw BadRequestException("Invalid email format")
        if (request.password.length < 8) throw BadRequestException("Password must be at least 8 characters")
        if (request.name.isBlank()) throw BadRequestException("Name is required")
    }
}