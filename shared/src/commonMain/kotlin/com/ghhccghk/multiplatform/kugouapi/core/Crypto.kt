package com.ghhccghk.multiplatform.kugouapi.core

import kotlin.io.encoding.Base64
import io.ktor.util.*

/**
 * Platform-specific cryptographic operations.
 */
expect object Crypto {
    /** Returns lowercase hex string of MD5 hash */
    fun md5(data: String): String

    /** AES-CBC encrypt with PKCS7 padding. Returns hex-encoded ciphertext. */
    fun aesEncrypt(plaintext: String, key: String, iv: String): String

    /** AES-CBC encrypt with PKCS7 padding. Returns base64-encoded ciphertext. */
    fun aesEncryptBase64(plaintext: String, key: String, iv: String): String

    /** AES-CBC decrypt with PKCS7 padding. Input is base64-encoded ciphertext. Returns UTF-8 plaintext. */
    fun aesDecryptBase64(ciphertextBase64: String, key: String, iv: String): String

    /** AES-CBC decrypt with PKCS7 padding. Input is hex-encoded ciphertext. Returns UTF-8 plaintext. */
    fun aesDecrypt(ciphertextHex: String, key: String, iv: String): String

    /** Raw RSA encryption (no padding). Returns hex-encoded ciphertext. */
    fun rsaEncrypt(data: ByteArray, publicKeyPem: String): String

    /** RSA encryption with PKCS1-V1_5 padding. Returns hex-encoded ciphertext. */
    fun rsaEncryptPkcs1(data: ByteArray, publicKeyPem: String): String

    /** Encode bytes to Base64 string */
    fun encodeBase64(data: ByteArray): String

    fun inflate(data: ByteArray): ByteArray
}

internal val Crypto.publicRasKey: String get() = "-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIAG7QOELSYoIJvTFJhMpe1s/gbjDJX51HBNnEl5HXqTW6lQ7LC8jr9fWZTwusknp+sVGzwd40MwP6U5yDE27M/X1+UR4tvOGOqp94TJtQ1EPnWGWXngpeIW5GxoQGao1rmYWAu6oi1z9XkChrsUdC6DJE5E221wf/4WLFxwAtRQIDAQAB\n-----END PUBLIC KEY-----"

fun Crypto.decodeBase64(base64String: String): ByteArray {
    return Base64.decode(base64String)
}

/**
 * AES 加密（自动生成密钥）—— 对齐 cryptoAesEncrypt(data)
 *
 * - 随机生成 16 位小写种子
 * - 派生 Key = MD5(种子).substring(0, 32)
 * - 派生 IV  = Key.substring(16)
 * - 返回 Pair(HEX密文, 种子字符串)
 */
fun Crypto.aesEncryptAuto(plaintext: String): Pair<String, String> {
    val tempKey = PlatformIdentity.generateRandomString(16).lowercase()
    val md5 = md5(tempKey)
    val key = md5.substring(0, 32)
    val iv = key.substring(16)
    val hex = aesEncrypt(plaintext, key, iv)
    return hex to tempKey
}

/**
 * AES 加密（指定密钥）—— 对齐 cryptoAesEncrypt(data, { key, iv })
 */
fun Crypto.aesEncryptWith(plaintext: String, key: String, iv: String): String {
    return aesEncrypt(plaintext, key, iv)
}

/**
 * AES 解密（使用种子派生密钥）—— 对齐 cryptoAesDecrypt(ciphertext, tempKey)
 *
 * - key = MD5(tempKey).substring(0, 32)
 * - iv  = key.substring(16)
 * - 解密 HEX 密文，返回原始字符串
 */
fun Crypto.aesDecryptWithSeed(ciphertextHex: String, tempKey: String): String {
    val md5 = md5(tempKey)
    val key = md5.substring(0, 32)
    val iv = key.substring(16)
    return aesDecrypt(ciphertextHex, key, iv)
}

/**
 * KRC 歌词解码 (全平台通用方案)
 */
fun Crypto.decodeLyrics(valData: ByteArray): String {
    if (valData.size < 4) return ""

    // 酷狗固定的 16 字节解密密钥
    val enKey = intArrayOf(64, 71, 97, 119, 94, 50, 116, 71, 81, 54, 49, 45, 206, 210, 110, 105)

    // 跳过前 4 个字节（文件头 "krc1"）
    val krcBytes = valData.copyOfRange(4, valData.size)
    val len = krcBytes.size

    // 解密循环
    for (index in 0 until len) {
        // 修复点：通过 and 0xFF 确保异或后的数据在 0~255 范围内，防止在 iOS Native 编译或运行时因符号位溢出报错
        val decryptedByte = (krcBytes[index].toInt() xor enKey[index % enKey.size]) and 0xFF
        krcBytes[index] = decryptedByte.toByte()
    }

    return try {
        // 核心修改：调用 Ktor 提供的全平台通用工具方法库进行解压
        // 这里的 gzip = false 表示使用标准的 Deflate 算法解压
        inflate(krcBytes).decodeToString()
    } catch (_: Exception) {
        ""
    }
}
