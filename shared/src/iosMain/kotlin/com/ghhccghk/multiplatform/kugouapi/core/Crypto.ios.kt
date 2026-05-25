package com.ghhccghk.multiplatform.kugouapi.core

import platform.Foundation.*
import kotlinx.cinterop.*
import platform.posix.memset
import platform.zlib.Z_DATA_ERROR
import platform.zlib.Z_MEM_ERROR
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.Z_STREAM_ERROR
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit
import platform.zlib.z_stream

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
        // 修复：不使用 JVM 的 String.format
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
        val ciphertext = decodeBase64(ciphertextBase64) // 修复：调用修改后的方法
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
        val keyBytes = decodeBase64(pemContent) // 修复：调用修改后的方法
        val (modulus, exponent) = parseRsaPublicKey(keyBytes)
        val message = bytesToBigInteger(data)
        val result = modPow(message, exponent, modulus)
        val keyLength = (modulus.bitLength() + 7) / 8
        val resultBytes = bigIntegerToBytes(result, keyLength)
        // 修复：不使用 JVM 的 String.format
        return resultBytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    actual fun rsaEncryptPkcs1(data: ByteArray, publicKeyPem: String): String {
        return rsaEncrypt(data, publicKeyPem)
    }

    actual fun encodeBase64(data: ByteArray): String {
        val nsData = data.toNSData()
        return nsData.base64EncodedStringWithOptions(0UL) // 修复：iOS 选用 0UL
    }

    /**
     * 使用 iOS 原生 zlib (libz) 进行 Deflate 解压
     */
    @OptIn(ExperimentalForeignApi::class)
    actual fun inflate(data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)

        val stream = nativeHeap.alloc<z_stream>()
        memset(stream.ptr, 0, z_stream.size.toULong())

        // 初始化 zlib 分配器
        if (inflateInit(stream.ptr) != Z_OK) {
            nativeHeap.free(stream)
            throw RuntimeException("zlib inflateInit failed")
        }

        // 锁住输入数据指针
        val pinnedInput = data.pin()
        stream.next_in = pinnedInput.addressOf(0).reinterpret()
        stream.avail_in = data.size.toUInt()

        val bufferSize = 16384 // 16KB 缓冲区
        val outputChunks = mutableListOf<ByteArray>()
        val chunkBuffer = ByteArray(bufferSize)
        val pinnedOutput = chunkBuffer.pin()

        try {
            var status: Int
            do {
                stream.next_out = pinnedOutput.addressOf(0).reinterpret()
                stream.avail_out = bufferSize.toUInt()

                status = inflate(stream.ptr, Z_NO_FLUSH)

                if (status == Z_STREAM_ERROR || status == Z_DATA_ERROR || status == Z_MEM_ERROR) {
                    throw RuntimeException("zlib inflate failed with status: $status")
                }

                val bytesRead = bufferSize - stream.avail_out.toInt()
                if (bytesRead > 0) {
                    outputChunks.add(chunkBuffer.copyOf(bytesRead))
                }
            } while (status != Z_STREAM_END && stream.avail_in > 0U)

        } finally {
            inflateEnd(stream.ptr)
            pinnedInput.unpin()
            pinnedOutput.unpin()
            nativeHeap.free(stream)
        }

        // 合并解压后的所有数据块
        val totalSize = outputChunks.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (chunk in outputChunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }

        return result
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
        return "" // 保持你的 placeholder
    }
}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePin { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val byteArray = ByteArray(length)
    byteArray.usePin { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return byteArray
}