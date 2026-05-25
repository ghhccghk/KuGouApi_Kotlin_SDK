package com.ghhccghk.multiplatform.kugouapi.core

actual object Crypto {
    actual fun md5(data: String): String {
        TODO("Not yet implemented")
    }

    actual fun aesEncrypt(
        plaintext: String,
        key: String,
        iv: String
    ): String {
        TODO("Not yet implemented")
    }

    actual fun aesEncryptBase64(
        plaintext: String,
        key: String,
        iv: String
    ): String {
        TODO("Not yet implemented")
    }

    actual fun aesDecryptBase64(
        ciphertextBase64: String,
        key: String,
        iv: String
    ): String {
        TODO("Not yet implemented")
    }

    actual fun aesDecrypt(
        ciphertextHex: String,
        key: String,
        iv: String
    ): String {
        TODO("Not yet implemented")
    }

    actual fun rsaEncrypt(data: ByteArray, publicKeyPem: String): String {
        TODO("Not yet implemented")
    }

    actual fun rsaEncryptPkcs1(data: ByteArray, publicKeyPem: String): String {
        TODO("Not yet implemented")
    }

    actual fun encodeBase64(data: ByteArray): String {
        TODO("Not yet implemented")
    }
}