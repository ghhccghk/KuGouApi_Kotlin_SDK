package top.ghhccghk.multiplatform.kugouapi.core

import top.ghhccghk.multiplatform.kugouapi.KuGouConfig
import kotlinx.serialization.json.*

/**
 * 平台特定的加密操作。
 */
expect object Crypto {
    /** 返回 MD5 哈希的小写十六进制字符串 */
    fun md5(data: String): String

    /** 返回 SHA-1 哈希的小写十六进制字符串 */
    fun sha1(data: String): String

    /** 使用 PKCS7 填充的 AES-CBC 加密。返回十六进制编码的密文。 */
    fun aesEncrypt(plaintext: String, key: String, iv: String): String

    /** 使用 PKCS7 填充的 AES-CBC 加密。返回 Base64 编码的密文。 */
    fun aesEncryptBase64(plaintext: String, key: String, iv: String): String

    /** 使用 PKCS7 填充的 AES-CBC 解密。输入为 Base64 编码的密文。返回 UTF-8 明文。 */
    fun aesDecryptBase64(ciphertextBase64: String, key: String, iv: String): String

    /** 使用 PKCS7 填充 of AES-CBC 解密。输入为十六进制编码的密文。返回 UTF-8 明文。 */
    fun aesDecrypt(ciphertextHex: String, key: String, iv: String): String


    /** 原始 RSA 加密（无填充）。返回十六进制编码的密文。 */
    suspend fun rsaEncrypt(data: ByteArray, publicKeyPem: String): String

    /** 使用 PKCS1-V1_5 填充的 RSA 加密。返回十六进制编码的密文。 */
    suspend fun rsaEncryptPkcs1(data: ByteArray, publicKeyPem: String): String

    /** 将字节数组编码为 Base64 字符串 */
    fun encodeBase64(data: ByteArray): String

    /** 将 Base64 字符串解码为字节数组 */
    fun decodeBase64(data: String): ByteArray

    /** 解压缩 zlib 压缩的数据 */
    fun inflate(data: ByteArray): ByteArray

    /**
     * 原始 AES-CBC 加密（PKCS7 填充），输入输出均为 ByteArray。
     * 用于 fingerprint 模块等需要直接操作字节数组的场景。
     */
    fun aesEncryptRaw(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray

    /**
     * RSA-OAEP SHA-256 加密。
     * @param data 待加密的字节数组
     * @param spkiDerHex SPKI DER 格式公钥的十六进制字符串
     * @return 密文字节数组
     */
    suspend fun rsaEncryptOaep(data: ByteArray, spkiDerHex: String): ByteArray
}
fun Crypto.activePublicRasKey(config: KuGouConfig): String {
    return if (config.isLite) publicLiteRasKey else publicRasKey
}
internal val publicRasKey: String get() = "-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIAG7QOELSYoIJvTFJhMpe1s/gbjDJX51HBNnEl5HXqTW6lQ7LC8jr9fWZTwusknp+sVGzwd40MwP6U5yDE27M/X1+UR4tvOGOqp94TJtQ1EPnWGWXngpeIW5GxoQGao1rmYWAu6oi1z9XkChrsUdC6DJE5E221wf/4WLFxwAtRQIDAQAB\n-----END PUBLIC KEY-----"
internal val publicLiteRasKey: String get() = "-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDECi0Np2UR87scwrvTr72L6oO01rBbbBPriSDFPxr3Z5syug0O24QyQO8bg27+0+4kBzTBTBOZ/WWU0WryL1JSXRTXLgFVxtzIY41Pe7lPOgsfTCn5kZcvKhYKJesKnnJDNr5/abvTGf+rHG3YRwsCHcQ08/q6ifSioBszvb3QiwIDAQAB\n-----END PUBLIC KEY-----"

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
