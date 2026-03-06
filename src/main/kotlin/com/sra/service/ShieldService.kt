package com.sra.service

import com.sra.repository.ShieldRepository
import com.sra.utils.HashUtils
import com.sra.utils.NotFoundException
import org.slf4j.LoggerFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher

class ShieldService(private val shieldRepository: ShieldRepository) {

    private val logger = LoggerFactory.getLogger(ShieldService::class.java)

    fun createNewKey(identity: String): String {
        // This is a placeholder. In a real system, you'd use a secure key management system.
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.genKeyPair()

        val publicKey = java.util.Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val encryptedPrivateKey = encryptPrivateKey(keyPair.private, identity)

        shieldRepository.create(publicKey, encryptedPrivateKey)
        logger.info("New shield key created for identity hash: ${HashUtils.sha256(identity)}")
        return publicKey
    }

    fun unshield(keyId: String, ciphertext: String, signature: String, identity: String): String {
        val key = shieldRepository.findByPublicKey(keyId)
            ?: throw NotFoundException("Key not found")

        // Placeholder for signature verification
        if (!verifySignature(key.publicKey, ciphertext, signature)) {
            throw SecurityException("Invalid signature")
        }

        val privateKey = decryptPrivateKey(key.encryptedPrivateKey, identity)
        val decryptedBytes = Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
            init(Cipher.DECRYPT_MODE, privateKey)
            doFinal(java.util.Base64.getDecoder().decode(ciphertext))
        }
        return String(decryptedBytes)
    }

    private fun encryptPrivateKey(privateKey: PrivateKey, identity: String): String {
        // WARNING: This is a simplistic and insecure way to "encrypt" a private key.
        // A real implementation should use a proper KDF and authenticated encryption.
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = HashUtils.sha256(identity).toByteArray().sliceArray(0..15)
        // ... implementation details omitted for brevity ...
        return java.util.Base64.getEncoder().encodeToString(privateKey.encoded)
    }

    private fun decryptPrivateKey(encryptedKey: String, identity: String): PrivateKey {
        // WARNING: See encryption warning.
        val decodedKey = java.util.Base64.getDecoder().decode(encryptedKey)
        val kf = java.security.KeyFactory.getInstance("RSA")
        return kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(decodedKey))
    }

    private fun verifySignature(publicKey: String, data: String, signature: String): Boolean {
        // Placeholder for signature verification logic
        return true
    }
}