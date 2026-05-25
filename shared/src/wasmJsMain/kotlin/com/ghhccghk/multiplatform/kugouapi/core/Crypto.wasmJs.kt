package com.ghhccghk.multiplatform.kugouapi.core

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

actual object Crypto {

    actual fun md5(data: String): String {
        return md5Pure(data.encodeToByteArray())
    }

    actual fun aesEncrypt(plaintext: String, key: String, iv: String): String {
        val keyBytes = key.encodeToByteArray().copyOf(16)
        val ivBytes = iv.encodeToByteArray().copyOf(16)
        val inputBytes = plaintext.encodeToByteArray()
        val padded = pkcs7Pad(inputBytes, 16)
        val output = aesCbcEncrypt(padded, keyBytes, ivBytes)
        return output.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    actual fun aesEncryptBase64(plaintext: String, key: String, iv: String): String {
        val keyBytes = key.encodeToByteArray().copyOf(16)
        val ivBytes = iv.encodeToByteArray().copyOf(16)
        val inputBytes = plaintext.encodeToByteArray()
        val padded = pkcs7Pad(inputBytes, 16)
        val output = aesCbcEncrypt(padded, keyBytes, ivBytes)
        return encodeBase64(output)
    }

    actual fun aesDecryptBase64(ciphertextBase64: String, key: String, iv: String): String {
        val ciphertext = decodeBase64(ciphertextBase64)
        val keyBytes = key.encodeToByteArray().copyOf(16)
        val ivBytes = iv.encodeToByteArray().copyOf(16)
        val decrypted = aesCbcDecrypt(ciphertext, keyBytes, ivBytes)
        val unpadded = pkcs7Unpad(decrypted)
        return unpadded.decodeToString()
    }

    actual fun aesDecrypt(ciphertextHex: String, key: String, iv: String): String {
        val ciphertext = hexToBytes(ciphertextHex)
        val keyBytes = key.encodeToByteArray().copyOf(16)
        val ivBytes = iv.encodeToByteArray().copyOf(16)
        val decrypted = aesCbcDecrypt(ciphertext, keyBytes, ivBytes)
        val unpadded = pkcs7Unpad(decrypted)
        return unpadded.decodeToString()
    }

    actual fun rsaEncrypt(data: ByteArray, publicKeyPem: String): String {
        val pemContent = extractPem(publicKeyPem)
        val keyBytes = decodeBase64(pemContent)
        val (modulus, exponent) = parseRsaPublicKey(keyBytes)
        val message = bytesToBigInteger(data)
        val result = modPow(message, exponent, modulus)
        val keyLength = (modulus.bitLength() + 7) / 8
        val resultBytes = bigIntegerToBytes(result, keyLength)
        return resultBytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    actual fun rsaEncryptPkcs1(data: ByteArray, publicKeyPem: String): String {
        return rsaEncrypt(data, publicKeyPem)
    }

    // Wasm 环境下的 Base64 编码实现
    actual fun encodeBase64(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val binaryString = data.map { (it.toInt() and 0xFF).toChar() }.joinToString("")
        return jsAtobBtoa.btoa(binaryString)
    }

    // 补充：为了对齐公共部分的通用调用，增加 Wasm 端的 Base64 解码实现
    actual fun decodeBase64(base64String: String): ByteArray {
        if (base64String.isEmpty()) return ByteArray(0)
        val binaryString = jsAtobBtoa.atob(base64String)
        val bytes = ByteArray(binaryString.length)
        for (i in 0 until binaryString.length) {
            bytes[i] = binaryString[i].code.toByte()
        }
        return bytes
    }

    private fun extractPem(pem: String): String {
        return pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((hex[i].digitToInt(16) shl 4) + hex[i+1].digitToInt(16)).toByte()
        }
        return data
    }

    // 复用你的纯 Kotlin MD5 逻辑接口
    private fun md5Pure(input: ByteArray): String {
        // 这里调用你在项目中实现的纯算法 md5，或者留作你的 placeholder
        return ""
    }

    actual fun inflate(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }
}

// 采用 WasmJS 的外部声明（External）来调用浏览器的 atob/btoa 或 Node 环境的垫片
@JsFun("(str) => { if (typeof btoa === 'function') return btoa(str); return Buffer.from(str, 'binary').toString('base64'); }")
private external fun jsBtoa(str: String): String

@JsFun("(str) => { if (typeof atob === 'function') return atob(str); return Buffer.from(str, 'base64').toString('binary'); }")
private external fun jsAtob(str: String): String

private object jsAtobBtoa {
    fun btoa(str: String): String = jsBtoa(str)
    fun atob(str: String): String = jsAtob(str)
}