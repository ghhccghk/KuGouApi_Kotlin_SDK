package com.ghhccghk.multiplatform.kugouapi.shared.core

import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.RSAPublicKeySpec
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
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val ivBytes = iv.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            IvParameterSpec(ivBytes)
        )
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return encrypted.joinToString("") { "%02x".format(it) }
    }

    actual fun aesDecrypt(ciphertextHex: String, key: String, iv: String): String {
        val ciphertext = hexToBytes(ciphertextHex)
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        val ivBytes = iv.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            IvParameterSpec(ivBytes)
        )
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    actual fun rsaEncrypt(data: ByteArray, publicKeyPem: String): String {
        val pemContent = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val keyBytes = android.util.Base64.decode(pemContent, android.util.Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = keyFactory.generatePublic(
            java.security.spec.X509EncodedKeySpec(keyBytes)
        ) as java.security.interfaces.RSAPublicKey

        // Raw RSA: m^e mod n (no padding, matching Node.js implementation)
        val message = BigInteger(1, data)
        val modulus = keySpec.modulus
        val exponent = keySpec.publicExponent
        val encrypted = message.modPow(exponent, modulus)
        val keyLength = (modulus.bitLength() + 7) / 8
        val result = encrypted.toByteArray()
        // Pad to key length
        return if (result.size < keyLength) {
            val padded = ByteArray(keyLength)
            System.arraycopy(result, 0, padded, keyLength - result.size, result.size)
            padded.joinToString("") { "%02x".format(it) }
        } else {
            result.joinToString("") { "%02x".format(it) }
        }
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
}
