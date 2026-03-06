package com.sra.routes

import com.sra.domain.models.NewShieldKeyRequest
import com.sra.domain.models.UnshieldedDataRequest
import com.sra.service.ShieldService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.shieldRoutes(shieldService: ShieldService) {
    route("/shield") {
        post("/keys") {
            val request = call.receive<NewShieldKeyRequest>()
            val publicKey = shieldService.createNewKey(request.identity)
            call.respond(mapOf("publicKey" to publicKey))
        }

        post("/unshield") {
            val request = call.receive<UnshieldedDataRequest>()
            val plaintext = shieldService.unshield(
                request.shieldedData.keyId,
                request.shieldedData.ciphertext,
                request.shieldedData.signature,
                "user-identity-placeholder" // FIXME: Get identity from authenticated session
            )
            call.respond(mapOf("plaintext" to plaintext))
        }
    }
}