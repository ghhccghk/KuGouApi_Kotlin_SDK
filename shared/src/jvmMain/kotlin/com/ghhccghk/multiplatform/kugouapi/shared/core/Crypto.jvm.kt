package com.ghhccghk.multiplatform.kugouapi.shared.core

import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object Crypto {
    actual fun md5(data: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    actual fun aesEncrypt(plaintext: String, key: String, iv: String): String {
        val bytes = aesOperation(plaintext.toByteArray(Charsets.UTF_8), key, iv, Cipher.ENCRYPT_MODE)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    actual fun aesEncryptBase64(plaintext: String, key: String, iv: String): String {
        val bytes = aesOperation(plaintext.toByteArray(Charsets.UTF_8), key, iv, Cipher.ENCRYPT_MODE)
        return Base64.getEncoder().encodeToString(bytes)
    }

    actual fun aesDecryptBase64(ciphertextBase64: String, key: String, iv: String): String {
        val ciphertext = Base64.getDecoder().decode(ciphertextBase64)
        val bytes = aesOperation(ciphertext, key, iv, Cipher.DECRYPT_MODE)
        return String(bytes, Charsets.UTF_8)
    }

    actual fun aesDecrypt(ciphertextHex: String, key: String, iv: String): String {
        val ciphertext = hexToBytes(ciphertextHex)
        val bytes = aesOperation(ciphertext, key, iv, Cipher.DECRYPT_MODE)
        return String(bytes, Charsets.UTF_8)
    }

    private fun aesOperation(data: ByteArray, key: String, iv: String, mode: Int): ByteArray {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val ivBytes = iv.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            mode,
            SecretKeySpec(keyBytes, "AES"),
            IvParameterSpec(ivBytes)
        )
        return cipher.doFinal(data)
    }

    actual fun rsaEncrypt(data: ByteArray, publicKeyPem: String): String {
        val publicKey = getRsaPublicKey(publicKeyPem)
        val message = BigInteger(1, data)
        val encrypted = message.modPow(publicKey.publicExponent, publicKey.modulus)
        val keyLength = (publicKey.modulus.bitLength() + 7) / 8
        val result = encrypted.toByteArray()
        val padded = if (result.size < keyLength) {
            val arr = ByteArray(keyLength)
            result.copyInto(arr, keyLength - result.size)
            arr
        } else if (result.size > keyLength) {
            result.copyOfRange(result.size - keyLength, result.size)
        } else result
        return padded.joinToString("") { "%02x".format(it) }
    }

    actual fun rsaEncryptPkcs1(data: ByteArray, publicKeyPem: String): String {
        val publicKey = getRsaPublicKey(publicKeyPem)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encrypted = cipher.doFinal(data)
        return encrypted.joinToString("") { "%02x".format(it) }
    }

    actual fun encodeBase64(data: ByteArray): String {
        return Base64.getEncoder().encodeToString(data)
    }

    private fun getRsaPublicKey(publicKeyPem: String): java.security.interfaces.RSAPublicKey {
        val pemContent = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val keyBytes = Base64.getDecoder().decode(pemContent)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(X509EncodedKeySpec(keyBytes)) as java.security.interfaces.RSAPublicKey
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((hex[i].digitToInt(16) shl 4) + hex[i + 1].digitToInt(16)).toByte()
        }
        return data
    }
}
