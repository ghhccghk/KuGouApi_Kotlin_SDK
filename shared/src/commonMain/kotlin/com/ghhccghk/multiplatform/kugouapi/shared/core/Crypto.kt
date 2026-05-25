package com.ghhccghk.multiplatform.kugouapi.shared.core

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

    fun rsaEncryptRaw(data: ByteArray, publicKeyPem: String): String
}

internal val Crypto.publicRasKey: String get() = "-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIAG7QOELSYoIJvTFJhMpe1s/gbjDJX51HBNnEl5HXqTW6lQ7LC8jr9fWZTwusknp+sVGzwd40MwP6U5yDE27M/X1+UR4tvOGOqp94TJtQ1EPnWGWXngpeIW5GxoQGao1rmYWAu6oi1z9XkChrsUdC6DJE5E221wf/4WLFxwAtRQIDAQAB\n-----END PUBLIC KEY-----"
