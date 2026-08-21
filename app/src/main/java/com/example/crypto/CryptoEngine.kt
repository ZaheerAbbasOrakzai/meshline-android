package com.example.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {

    private const val PBKDF2_ITERATIONS = 250000
    private const val KEY_SIZE_BITS = 256
    private const val GCM_IV_SIZE_BYTES = 12
    private const val GCM_TAG_SIZE_BITS = 128
    private const val SALT_SIZE_BYTES = 16

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_SIZE_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun deriveMasterKey(passphrase: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun generateEcdhKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    fun encodePublicKey(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    fun decodePublicKey(encoded: String): PublicKey {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(bytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(spec)
    }

    fun encodePrivateKey(privateKey: PrivateKey): String {
        return Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
    }

    fun decodePrivateKey(encoded: String): PrivateKey {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(bytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePrivate(spec)
    }

    fun encryptWithKey(plainText: String, secretKey: SecretKey): String {
        val iv = ByteArray(GCM_IV_SIZE_BYTES)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_SIZE_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        return "$ivBase64.$cipherBase64"
    }

    fun decryptWithKey(encryptedPayload: String, secretKey: SecretKey): String {
        val parts = encryptedPayload.split(".")
        if (parts.size != 2) return encryptedPayload // fallback
        val iv = Base64.decode(parts[0], Base64.DEFAULT)
        val cipherText = Base64.decode(parts[1], Base64.DEFAULT)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_SIZE_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val decrypted = cipher.doFinal(cipherText)
        return String(decrypted, Charsets.UTF_8)
    }

    fun computeSharedSecret(myPrivateKey: PrivateKey, peerPublicKey: PublicKey): SecretKey {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()
        val md = MessageDigest.getInstance("SHA-256")
        val derivedKeyBytes = md.digest(sharedSecret)
        return SecretKeySpec(derivedKeyBytes, "AES")
    }

    fun encryptMessage(
        plainText: String,
        myPrivateKey: PrivateKey,
        peerPublicKey: PublicKey
    ): String {
        val sharedKey = computeSharedSecret(myPrivateKey, peerPublicKey)
        return encryptWithKey(plainText, sharedKey)
    }

    fun decryptMessage(
        encryptedPayload: String,
        myPrivateKey: PrivateKey,
        peerPublicKey: PublicKey
    ): String {
        val sharedKey = computeSharedSecret(myPrivateKey, peerPublicKey)
        return decryptWithKey(encryptedPayload, sharedKey)
    }

    fun calculateSafetyNumberFingerprint(publicKeyString: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(publicKeyString.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString(":") { "%02X".format(it) }
    }

    fun calculateHmac(data: ByteArray, keyBytes: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }
}
