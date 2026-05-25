package com.ghhccghk.multiplatform.kugouapi.core

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

    // JS 环境下的 Base64 编码
    actual fun encodeBase64(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val binaryString = data.map { (it.toInt() and 0xFF).toChar() }.joinToString("")
        return platformBtoa(binaryString)
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

    private fun md5Pure(input: ByteArray): String {
        return "" // 留作你的纯算法拓展或占位
    }

    actual fun inflate(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }
}

// 针对传统 Kotlin/JS 平台的原生垫片环境检测
private fun platformBtoa(str: String): String {
    return js(
        "typeof btoa === 'function' ? btoa(str) : Buffer.from(str, 'binary').toString('base64')"
    ) as String
}

private fun platformAtob(str: String): String {
    return js(
        "typeof atob === 'function' ? atob(str) : Buffer.from(str, 'base64').toString('binary')"
    ) as String
}