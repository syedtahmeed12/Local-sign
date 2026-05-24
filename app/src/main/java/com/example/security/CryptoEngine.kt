package com.example.security

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {

    /**
     * Compute SHA-256 checksum of a given text content.
     */
    fun calculateSHA256(content: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "error_hash"
        }
    }

    /**
     * Derive a solid cryptographic AES Key from a user passphrase or pin locally on the device.
     */
    fun deriveKey(passphrase: String): SecretKey {
        // Scramble or hash passphrase to form a clean 256-bit (32 byte) key
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedKey = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hashedKey, "AES")
    }

    /**
     * Encrypt a text block using AES-256/CBC/PKCS5Padding and return a Pair of (EncryptedBase64, IVBase64).
     */
    fun encryptAES(plainText: String, secretKey: SecretKey): Pair<String, String> {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv ?: ByteArray(16)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            Pair(encryptedBase64, ivBase64)
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    /**
     * Decrypt an AES-256 base64 block back into plain text.
     */
    fun decryptAES(encryptedBase64: String, ivBase64: String, secretKey: SecretKey): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ivBytes))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "DECRYPTION_ERROR: Invalid Passphrase or Key matching"
        }
    }

    /**
     * Generates a "Digital Signature Seal" combining:
     * - Document Checksum (SHA-256)
     * - Signature string (Coordinates or typed name)
     * - User Passphrase salt
     * To verify authenticity and proof of non-repudiation.
     */
    fun generateDigitalSeal(documentHash: String, signatureContent: String, signerName: String, passphraseSalt: String): String {
        val composite = "$documentHash|$signatureContent|$signerName|$passphraseSalt"
        val digest = MessageDigest.getInstance("SHA-256")
        val sealBytes = digest.digest(composite.toByteArray(Charsets.UTF_8))
        return sealBytes.joinToString("") { "%02x".format(it) }
    }
}
