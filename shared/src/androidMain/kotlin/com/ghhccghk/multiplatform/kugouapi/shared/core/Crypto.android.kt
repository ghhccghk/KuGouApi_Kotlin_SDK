package com.ghhccghk.multiplatform.kugouapi.shared.core

import android.util.Base64
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
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
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    actual fun aesDecryptBase64(ciphertextBase64: String, key: String, iv: String): String {
        val ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)
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
        val keySpec = getRsaPublicKeySpec(publicKeyPem)
        // Raw RSA: m^e mod n (no padding)
        val message = BigInteger(1, data)
        val encrypted = message.modPow(keySpec.publicExponent, keySpec.modulus)
        val keyLength = (keySpec.modulus.bitLength() + 7) / 8
        val result = encrypted.toByteArray()
        val padded = if (result.size < keyLength) {
            val arr = ByteArray(keyLength)
            System.arraycopy(result, 0, arr, keyLength - result.size, result.size)
            arr
        } else if (result.size > keyLength) {
            result.copyOfRange(result.size - keyLength, result.size)
        } else result
        return padded.joinToString("") { "%02x".format(it) }
    }

    actual fun rsaEncryptPkcs1(data: ByteArray, publicKeyPem: String): String {
        val pemContent = extractPemContent(publicKeyPem)
        val keyBytes = Base64.decode(pemContent, Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encrypted = cipher.doFinal(data)
        return encrypted.joinToString("") { "%02x".format(it) }
    }

    actual fun encodeBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    private fun getRsaPublicKeySpec(publicKeyPem: String): java.security.interfaces.RSAPublicKey {
        val pemContent = extractPemContent(publicKeyPem)
        val keyBytes = Base64.decode(pemContent, Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(X509EncodedKeySpec(keyBytes)) as java.security.interfaces.RSAPublicKey
    }

    private fun extractPemContent(pem: String): String {
        return pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    actual fun rsaEncryptRaw(data: ByteArray, publicKeyPem: String): String {
        val publicKey = getRsaPublicKeySpec(publicKeyPem)
        val keyLength = (publicKey.modulus.bitLength() + 7) / 8  // 1024-bit → 128 bytes

        // JS: padded.set(buffer) → 数据在头部，尾部补零
        val padded = if (data.size < keyLength) {
            ByteArray(keyLength).also { data.copyInto(it, destinationOffset = 0) }
        } else data

        val message = BigInteger(1, padded)
        val encrypted = message.modPow(publicKey.publicExponent, publicKey.modulus)
        return encrypted.toString(16).padStart(keyLength * 2, '0')  // 小写 hex，256 chars
    }
}
