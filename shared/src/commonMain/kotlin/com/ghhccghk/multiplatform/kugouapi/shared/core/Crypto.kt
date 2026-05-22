package com.ghhccghk.multiplatform.kugouapi.shared.core

/**
 * Platform-specific cryptographic operations.
 */
expect object Crypto {
    /** Returns lowercase hex string of MD5 hash */
    fun md5(data: String): String

    /** AES-CBC encrypt with PKCS7 padding. Returns hex-encoded ciphertext. */
    fun aesEncrypt(plaintext: String, key: String, iv: String): String

    /** AES-CBC decrypt with PKCS7 padding. Input is hex-encoded ciphertext. Returns UTF-8 plaintext. */
    fun aesDecrypt(ciphertextHex: String, key: String, iv: String): String

    /** Raw RSA encryption (no padding). Returns hex-encoded ciphertext. */
    fun rsaEncrypt(data: ByteArray, publicKeyPem: String): String
}
