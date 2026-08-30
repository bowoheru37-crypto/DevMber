package com.example.data.remote

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * High-grade cryptographic manager for Studio project encryption and payload validation.
 * Supports AES-256 (CBC/PKCS5) with cryptographically secure dynamic IVs,
 * 256-bit SHA-256 digests, and constant-time HMAC-SHA256 signatures.
 * Optimized for low-RAM mobile devices (Android 5.0+ / itel A70).
 */
object SecurityManager {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val DEFAULT_PASS = "StudioAIEngineSecureKey2026!#"
    private const val IV_SIZE_BYTES = 16
    private val secureRandom = SecureRandom()

    private fun getKey(passphrase: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(bytes, "AES")
    }

    /**
     * Encrypts plainText using AES-256-CBC with a freshly generated 16-byte dynamic IV.
     * Output format: Base64(IV + Ciphertext).
     */
    fun encrypt(plainText: String, keyPhrase: String = DEFAULT_PASS): String {
        return try {
            val key = getKey(keyPhrase)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_SIZE_BYTES).apply { secureRandom.nextBytes(this) }
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    /**
     * Decrypts Base64(IV + Ciphertext) with dynamic IV extraction,
     * including graceful backward compatibility for legacy zero-IV payloads.
     */
    fun decrypt(cipherText: String, keyPhrase: String = DEFAULT_PASS): String {
        return try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            val key = getKey(keyPhrase)
            val cipher = Cipher.getInstance(ALGORITHM)

            if (combined.size > IV_SIZE_BYTES) {
                val iv = ByteArray(IV_SIZE_BYTES)
                val cipherBytes = ByteArray(combined.size - IV_SIZE_BYTES)
                System.arraycopy(combined, 0, iv, 0, IV_SIZE_BYTES)
                System.arraycopy(combined, IV_SIZE_BYTES, cipherBytes, 0, cipherBytes.size)

                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                val original = cipher.doFinal(cipherBytes)
                String(original, Charsets.UTF_8)
            } else {
                // Fallback for legacy payloads
                val iv = ByteArray(IV_SIZE_BYTES) { 0 }
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                val original = cipher.doFinal(combined)
                String(original, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            // Attempt legacy fixed zero-IV decoding if combined format fails
            try {
                val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
                val key = getKey(keyPhrase)
                val cipher = Cipher.getInstance(ALGORITHM)
                val iv = ByteArray(IV_SIZE_BYTES) { 0 }
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                val original = cipher.doFinal(decoded)
                String(original, Charsets.UTF_8)
            } catch (fallbackEx: Exception) {
                cipherText
            }
        }
    }

    /**
     * Computes standard 256-bit SHA-256 checksum in full hex representation (64 chars).
     */
    fun computeSha256Checksum(content: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
            val hexString = StringBuilder(hash.size * 2)
            for (b in hash) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: Exception) {
            "0000000000000000000000000000000000000000000000000000000000000000"
        }
    }

    /**
     * Computes HMAC-SHA256 signature for data integrity and tamper proofing.
     */
    fun computeHmacSha256(content: String, keyString: String = DEFAULT_PASS): String {
        return try {
            val secretKey = SecretKeySpec(keyString.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKey)
            val hmacBytes = mac.doFinal(content.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Constant-time HMAC comparison to eliminate timing attacks.
     */
    fun verifyHmacSha256(content: String, expectedHmac: String, keyString: String = DEFAULT_PASS): Boolean {
        if (expectedHmac.isBlank()) return false
        val computed = computeHmacSha256(content, keyString)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            expectedHmac.toByteArray(Charsets.UTF_8)
        )
    }
}
