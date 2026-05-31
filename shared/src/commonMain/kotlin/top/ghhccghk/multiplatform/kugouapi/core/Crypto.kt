package top.ghhccghk.multiplatform.kugouapi.core

import top.ghhccghk.multiplatform.kugouapi.KuGouConfig
import kotlinx.serialization.json.*

/**
 * Platform-specific cryptographic operations.
 */
expect object Crypto {
    /** Returns lowercase hex string of MD5 hash */
    fun md5(data: String): String

    /** Returns lowercase hex string of SHA-1 hash */
    fun sha1(data: String): String

    /** AES-CBC encrypt with PKCS7 padding. Returns hex-encoded ciphertext. */
    fun aesEncrypt(plaintext: String, key: String, iv: String): String

    /** AES-CBC encrypt with PKCS7 padding. Returns base64-encoded ciphertext. */
    fun aesEncryptBase64(plaintext: String, key: String, iv: String): String

    /** AES-CBC decrypt with PKCS7 padding. Input is base64-encoded ciphertext. Returns UTF-8 plaintext. */
    fun aesDecryptBase64(ciphertextBase64: String, key: String, iv: String): String

    /** AES-CBC decrypt with PKCS7 padding. Input is hex-encoded ciphertext. Returns UTF-8 plaintext. */
    fun aesDecrypt(ciphertextHex: String, key: String, iv: String): String

    /** Raw RSA encryption (no padding). Returns hex-encoded ciphertext. */
    suspend fun rsaEncrypt(data: ByteArray, publicKeyPem: String): String

    /** RSA encryption with PKCS1-V1_5 padding. Returns hex-encoded ciphertext. */
    suspend fun rsaEncryptPkcs1(data: ByteArray, publicKeyPem: String): String

    /** Encode bytes to Base64 string */
    fun encodeBase64(data: ByteArray): String

    /** Decode Base64 string to bytes */
    fun decodeBase64(data: String): ByteArray

    /** Decompress zlib inflated data */
    fun inflate(data: ByteArray): ByteArray
}

internal val Crypto.publicRasKey: String get() = "-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIAG7QOELSYoIJvTFJhMpe1s/gbjDJX51HBNnEl5HXqTW6lQ7LC8jr9fWZTwusknp+sVGzwd40MwP6U5yDE27M/X1+UR4tvOGOqp94TJtQ1EPnWGWXngpeIW5GxoQGao1rmYWAu6oi1z9XkChrsUdC6DJE5E221wf/4WLFxwAtRQIDAQAB\n-----END PUBLIC KEY-----"

// ============================================================
//  AES 加解密便捷方法
// ============================================================

fun Crypto.aesEncryptAuto(plaintext: String): Pair<String, String> {
    val tempKey = PlatformIdentity.generateRandomString(16).lowercase()
    val md5Value = md5(tempKey)
    val key = md5Value.substring(0, 32)
    val iv = key.substring(16)
    val hex = aesEncrypt(plaintext, key, iv)
    return hex to tempKey
}

fun Crypto.aesEncryptWith(plaintext: String, key: String, iv: String): String {
    return aesEncrypt(plaintext, key, iv)
}

fun Crypto.aesDecryptWithSeed(ciphertextHex: String, tempKey: String): String {
    val md5Value = md5(tempKey)
    val key = md5Value.substring(0, 32)
    val iv = key.substring(16)
    return aesDecrypt(ciphertextHex, key, iv)
}

/**
 * KRC 歌词解码
 */
fun Crypto.decodeLyrics(valData: ByteArray): String {
    if (valData.size < 4) return ""

    val enKey = intArrayOf(64, 71, 97, 119, 94, 50, 116, 71, 81, 54, 49, 45, 206, 210, 110, 105)
    val krcBytes = valData.copyOfRange(4, valData.size)
    val len = krcBytes.size

    for (index in 0 until len) {
        krcBytes[index] = (krcBytes[index].toInt() xor enKey[index % enKey.size]).toByte()
    }

    return try {
        inflate(krcBytes).decodeToString()
    } catch (_: Exception) {
        ""
    }
}
