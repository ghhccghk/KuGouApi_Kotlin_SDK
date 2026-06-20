package top.ghhccghk.multiplatform.kugouapi.core

import platform.Foundation.*
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
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
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object Crypto {
    actual fun md5(data: String): String {
        return md5Pure(data.encodeToByteArray())
    }

    actual fun sha1(data: String): String {
        return sha1Pure(data.encodeToByteArray())
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

    actual suspend fun rsaEncrypt(data: ByteArray, publicKeyPem: String): String = withContext(Dispatchers.IO) {
        val pemContent = extractPem(publicKeyPem)
        val keyBytes = decodeBase64(pemContent)
        val (modulus, exponent) = parseRsaPublicKey(keyBytes)
        val message = IosBigInt.fromByteArray(data)
        val result = message.modPow(exponent, modulus)
        val keyLength = (modulus.bitLength() + 7) / 8
        val resultBytes = result.toByteArray(keyLength)
        resultBytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    actual suspend fun rsaEncryptPkcs1(data: ByteArray, publicKeyPem: String): String = withContext(Dispatchers.IO) {
        rsaEncrypt(data, publicKeyPem)
    }

    actual fun encodeBase64(data: ByteArray): String {
        val nsData = data.toNSData()
        return nsData.base64EncodedStringWithOptions(0UL)
    }

    actual fun decodeBase64(data: String): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        val nsData = NSData.create(base64EncodedString = data, options = NSDataBase64DecodingIgnoreUnknownCharacters)
            ?: return ByteArray(0)
        return nsData.toByteArray()
    }

    actual fun inflate(data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)

        val stream = nativeHeap.alloc<z_stream>()
        memset(stream.ptr, 0, sizeOf<z_stream>().convert())

        if (inflateInit(stream.ptr) != Z_OK) {
            nativeHeap.free(stream)
            throw RuntimeException("zlib inflateInit failed")
        }

        val pinnedInput = data.pin()
        stream.next_in = pinnedInput.addressOf(0).reinterpret()
        stream.avail_in = data.size.toUInt()

        val bufferSize = 16384
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

        val totalSize = outputChunks.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (chunk in outputChunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }

        return result
    }

    actual fun aesEncryptRaw(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val padded = pkcs7Pad(plaintext, 16)
        return aesCbcEncrypt(padded, key, iv)
    }

    actual suspend fun rsaEncryptOaep(data: ByteArray, spkiDerHex: String): ByteArray = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException("RSA-OAEP is not yet implemented on iOS")
    }

    // ============================================================
    //  Private helpers - PEM / Hex
    // ============================================================

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
            data[i / 2] = ((hex[i].digitToInt(16) shl 4) + hex[i + 1].digitToInt(16)).toByte()
        }
        return data
    }

    // ============================================================
    //  PKCS7 Padding / Unpadding
    // ============================================================

    private fun pkcs7Pad(data: ByteArray, blockSize: Int): ByteArray {
        val padding = blockSize - (data.size % blockSize)
        return data + ByteArray(padding) { padding.toByte() }
    }

    private fun pkcs7Unpad(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val padding = data.last().toInt() and 0xFF
        if (padding !in 1..16) return data
        if (data.size < padding) return data
        for (i in data.size - padding until data.size) {
            if ((data[i].toInt() and 0xFF) != padding) return data
        }
        return data.copyOf(data.size - padding)
    }

    // ============================================================
    //  SHA-1 pure Kotlin implementation
    // ============================================================

    private fun sha1Pure(input: ByteArray): String {
        val h0 = 0x67452301
        val h1 = 0xEFCDAB89.toInt()
        val h2 = 0x98BADCFE.toInt()
        val h3 = 0x10325476
        val h4 = 0xC3D2E1F0.toInt()

        val msgLen = input.size
        val bitLen = msgLen.toLong() * 8

        val paddedLen = ((msgLen + 9 + 63) / 64) * 64
        val padded = ByteArray(paddedLen)
        input.copyInto(padded)
        padded[msgLen] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[paddedLen - 1 - i] = ((bitLen shr (i * 8)) and 0xFF).toByte()
        }

        var a: Int; var b: Int; var c: Int; var d: Int; var e: Int
        var ah = h0; var bh = h1; var ch = h2; var dh = h3; var eh = h4

        val w = IntArray(80)

        for (chunk in 0 until paddedLen step 64) {
            for (i in 0 until 16) {
                val off = chunk + i * 4
                w[i] = ((padded[off].toInt() and 0xFF) shl 24) or
                        ((padded[off + 1].toInt() and 0xFF) shl 16) or
                        ((padded[off + 2].toInt() and 0xFF) shl 8) or
                        (padded[off + 3].toInt() and 0xFF)
            }
            for (i in 16 until 80) {
                val tmp = w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]
                w[i] = (tmp shl 1) or (tmp ushr 31)
            }

            a = ah; b = bh; c = ch; d = dh; e = eh

            for (i in 0 until 80) {
                var f: Int; val k: Int
                when {
                    i < 20 -> {
                        f = (b and c) or (b.inv() and d)
                        k = 0x5A827999
                    }
                    i < 40 -> {
                        f = b xor c xor d
                        k = 0x6ED9EBA1
                    }
                    i < 60 -> {
                        f = (b and c) or (b and d) or (c and d)
                        k = 0x8F1BBCDC.toInt()
                    }
                    else -> {
                        f = b xor c xor d
                        k = 0xCA62C1D6.toInt()
                    }
                }
                val temp = ((a shl 5) or (a ushr 27)) + f + e + k + w[i]
                e = d; d = c; c = (b shl 30) or (b ushr 2); b = a; a = temp
            }

            ah += a; bh += b; ch += c; dh += d; eh += e
        }

        val result = ByteArray(20)
        for (i in 0 until 4) {
            result[i] = ((ah shr (24 - i * 8)) and 0xFF).toByte()
            result[i + 4] = ((bh shr (24 - i * 8)) and 0xFF).toByte()
            result[i + 8] = ((ch shr (24 - i * 8)) and 0xFF).toByte()
            result[i + 12] = ((dh shr (24 - i * 8)) and 0xFF).toByte()
            result[i + 16] = ((eh shr (24 - i * 8)) and 0xFF).toByte()
        }
        return result.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    // ============================================================
    //  MD5 pure Kotlin implementation
    // ============================================================

    private fun md5Pure(input: ByteArray): String {
        val s = intArrayOf(
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
        )
        val k = intArrayOf(
            -680876936, -389564586, 606105819, -1044525330, -176418897, 1200080426,
            -1473231341, -45705983, 1770035416, -1958414417, -42063, -1990404162,
            1804603682, -40341101, -1502002290, 1236535329, -165796510, -1069501632,
            643717713, -373897302, -701558691, 38016083, -660478335, -405537848,
            568446438, -1019803690, -187363961, 1163531501, -1444681467, -51403784,
            1735328473, -1926607734, -378558, -2022574463, 1839030562, -35309556,
            -1530992060, 1272893353, -155497632, -1094730640, 681279174, -358537222,
            -722521979, 76029189, -640364487, -421815835, 530742520, -995338651,
            -198630844, 1126891415, -1416354905, -57434055, 1700485571, -1894986606,
            -1051523, -2054922799, 1873313359, -30611744, -1560198380, 1309151649,
            -145523070, -1120210379, 718787259, -343485551
        )

        val msgLen = input.size
        val bitLen = msgLen.toLong() * 8
        val paddedLen = ((msgLen + 9 + 63) / 64) * 64
        val padded = ByteArray(paddedLen)
        input.copyInto(padded)
        padded[msgLen] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[paddedLen - 1 - i] = ((bitLen shr (i * 8)) and 0xFF).toByte()
        }

        var a0 = 0x67452301
        var b0 = 0xEFCDAB89.toInt()
        var c0 = 0x98BADCFE.toInt()
        var d0 = 0x10325476

        for (chunk in 0 until paddedLen step 64) {
            val m = IntArray(16)
            for (i in 0 until 16) {
                val off = chunk + i * 4
                m[i] = ((padded[off].toInt() and 0xFF)) or
                        ((padded[off + 1].toInt() and 0xFF) shl 8) or
                        ((padded[off + 2].toInt() and 0xFF) shl 16) or
                        ((padded[off + 3].toInt() and 0xFF) shl 24)
            }

            var a = a0; var b = b0; var c = c0; var d = d0

            for (i in 0 until 64) {
                var f: Int; val g: Int
                when {
                    i < 16 -> {
                        f = (b and c) or (b.inv() and d)
                        g = i
                    }
                    i < 32 -> {
                        f = (d and b) or (d.inv() and c)
                        g = (5 * i + 1) % 16
                    }
                    i < 48 -> {
                        f = b xor c xor d
                        g = (3 * i + 5) % 16
                    }
                    else -> {
                        f = c xor (b or d.inv())
                        g = (7 * i) % 16
                    }
                }
                f += a + k[i] + m[g]
                a = d; d = c; c = b
                b += (f shl s[i]) or (f ushr (32 - s[i]))
            }

            a0 += a; b0 += b; c0 += c; d0 += d
        }

        fun Int.toLittleEndianBytes(): ByteArray {
            return byteArrayOf(
                (this and 0xFF).toByte(),
                ((this shr 8) and 0xFF).toByte(),
                ((this shr 16) and 0xFF).toByte(),
                ((this shr 24) and 0xFF).toByte()
            )
        }

        val result = a0.toLittleEndianBytes() + b0.toLittleEndianBytes() +
                c0.toLittleEndianBytes() + d0.toLittleEndianBytes()
        return result.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    // ============================================================
    //  AES-128-CBC (pure Kotlin)
    // ============================================================

    private val S_BOX = intArrayOf(
        0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
        0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
        0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
        0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
        0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
        0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
        0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
        0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
        0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
        0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
        0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
        0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
        0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
        0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
        0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
        0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
    )

    private val INV_S_BOX = intArrayOf(
        0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb,
        0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb,
        0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e,
        0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0xb2, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25,
        0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92,
        0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84,
        0x90, 0xd8, 0xab, 0x00, 0x8c, 0xbc, 0xd3, 0x0a, 0xf7, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x06,
        0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b,
        0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73,
        0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e,
        0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b,
        0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4,
        0x1f, 0xdd, 0xa8, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f,
        0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef,
        0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61,
        0x17, 0x2b, 0x04, 0x7e, 0xba, 0x77, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d
    )

    private val RCON = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36)

    private fun gmul(a: Int, b: Int): Int {
        var p = 0
        var aa = a
        var bb = b
        for (i in 0 until 8) {
            if ((bb and 1) != 0) p = p xor aa
            val hi = aa and 0x80
            aa = (aa shl 1) and 0xFF
            if (hi != 0) aa = aa xor 0x1b
            bb = bb shr 1
        }
        return p and 0xFF
    }

    private fun aesKeyExpansion(key: ByteArray): Array<IntArray> {
        val roundKeys = Array(44) { IntArray(4) }
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                roundKeys[i][j] = key[i * 4 + j].toInt() and 0xFF
            }
        }
        for (i in 4 until 44) {
            val temp = roundKeys[i - 1].copyOf()
            if (i % 4 == 0) {
                val t0 = temp[0]
                for (j in 0 until 3) temp[j] = S_BOX[temp[(j + 1) % 4]]
                temp[3] = S_BOX[t0]
                temp[0] = temp[0] xor RCON[i / 4 - 1]
            }
            for (j in 0 until 4) {
                roundKeys[i][j] = roundKeys[i - 4][j] xor temp[j]
            }
        }
        return roundKeys
    }

    private fun subBytes(state: Array<IntArray>) {
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                state[r][c] = S_BOX[state[r][c]]
            }
        }
    }

    private fun invSubBytes(state: Array<IntArray>) {
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                state[r][c] = INV_S_BOX[state[r][c]]
            }
        }
    }

    private fun shiftRows(state: Array<IntArray>) {
        var temp: Int
        temp = state[1][0]
        state[1][0] = state[1][1]; state[1][1] = state[1][2]; state[1][2] = state[1][3]; state[1][3] = temp
        temp = state[2][0]; state[2][0] = state[2][2]; state[2][2] = temp
        temp = state[2][1]; state[2][1] = state[2][3]; state[2][3] = temp
        temp = state[3][3]
        state[3][3] = state[3][2]; state[3][2] = state[3][1]; state[3][1] = state[3][0]; state[3][0] = temp
    }

    private fun invShiftRows(state: Array<IntArray>) {
        var temp: Int
        temp = state[1][3]
        state[1][3] = state[1][2]; state[1][2] = state[1][1]; state[1][1] = state[1][0]; state[1][0] = temp
        temp = state[2][0]; state[2][0] = state[2][2]; state[2][2] = temp
        temp = state[2][1]; state[2][1] = state[2][3]; state[2][3] = temp
        temp = state[3][0]
        state[3][0] = state[3][1]; state[3][1] = state[3][2]; state[3][2] = state[3][3]; state[3][3] = temp
    }

    private fun mixColumns(state: Array<IntArray>) {
        for (c in 0 until 4) {
            val s0 = state[0][c]; val s1 = state[1][c]
            val s2 = state[2][c]; val s3 = state[3][c]
            state[0][c] = gmul(s0, 2) xor gmul(s1, 3) xor s2 xor s3
            state[1][c] = s0 xor gmul(s1, 2) xor gmul(s2, 3) xor s3
            state[2][c] = s0 xor s1 xor gmul(s2, 2) xor gmul(s3, 3)
            state[3][c] = gmul(s0, 3) xor s1 xor s2 xor gmul(s3, 2)
        }
    }

    private fun invMixColumns(state: Array<IntArray>) {
        for (c in 0 until 4) {
            val s0 = state[0][c]; val s1 = state[1][c]
            val s2 = state[2][c]; val s3 = state[3][c]
            state[0][c] = gmul(s0, 14) xor gmul(s1, 11) xor gmul(s2, 13) xor gmul(s3, 9)
            state[1][c] = gmul(s0, 9) xor gmul(s1, 14) xor gmul(s2, 11) xor gmul(s3, 13)
            state[2][c] = gmul(s0, 13) xor gmul(s1, 9) xor gmul(s2, 14) xor gmul(s3, 11)
            state[3][c] = gmul(s0, 11) xor gmul(s1, 13) xor gmul(s2, 9) xor gmul(s3, 14)
        }
    }

    private fun addRoundKey(state: Array<IntArray>, roundKeys: Array<IntArray>, round: Int) {
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                state[r][c] = state[r][c] xor roundKeys[round * 4 + c][r]
            }
        }
    }

    private fun bytesToState(block: ByteArray): Array<IntArray> {
        val state = Array(4) { IntArray(4) }
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                state[r][c] = block[c * 4 + r].toInt() and 0xFF
            }
        }
        return state
    }

    private fun stateToBytes(state: Array<IntArray>): ByteArray {
        val out = ByteArray(16)
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                out[c * 4 + r] = state[r][c].toByte()
            }
        }
        return out
    }

    private fun aesEncryptBlock(block: ByteArray, roundKeys: Array<IntArray>): ByteArray {
        val state = bytesToState(block)
        addRoundKey(state, roundKeys, 0)
        for (round in 1 until 10) {
            subBytes(state)
            shiftRows(state)
            mixColumns(state)
            addRoundKey(state, roundKeys, round)
        }
        subBytes(state)
        shiftRows(state)
        addRoundKey(state, roundKeys, 10)
        return stateToBytes(state)
    }

    private fun aesDecryptBlock(block: ByteArray, roundKeys: Array<IntArray>): ByteArray {
        val state = bytesToState(block)
        addRoundKey(state, roundKeys, 10)
        for (round in 9 downTo 1) {
            invShiftRows(state)
            invSubBytes(state)
            addRoundKey(state, roundKeys, round)
            invMixColumns(state)
        }
        invShiftRows(state)
        invSubBytes(state)
        addRoundKey(state, roundKeys, 0)
        return stateToBytes(state)
    }

    private fun xorBlocks(a: ByteArray, b: ByteArray): ByteArray {
        return ByteArray(16) { (a[it].toInt() xor b[it].toInt()).toByte() }
    }

    private fun aesCbcEncrypt(input: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val roundKeys = aesKeyExpansion(key)
        val output = ByteArray(input.size)
        var prevBlock = iv
        for (i in input.indices step 16) {
            val block = input.copyOfRange(i, i + 16)
            val xored = xorBlocks(block, prevBlock)
            val encrypted = aesEncryptBlock(xored, roundKeys)
            encrypted.copyInto(output, i)
            prevBlock = encrypted
        }
        return output
    }

    private fun aesCbcDecrypt(input: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val roundKeys = aesKeyExpansion(key)
        val output = ByteArray(input.size)
        var prevBlock = iv
        for (i in input.indices step 16) {
            val block = input.copyOfRange(i, i + 16)
            val decrypted = aesDecryptBlock(block, roundKeys)
            val result = xorBlocks(decrypted, prevBlock)
            result.copyInto(output, i)
            prevBlock = block
        }
        return output
    }

    // ============================================================
    //  RSA helpers with pure Kotlin BigInt
    // ============================================================

    private fun parseRsaPublicKey(keyBytes: ByteArray): Pair<IosBigInt, IosBigInt> {
        var pos = 0

        fun readTag(): Int { return keyBytes[pos++].toInt() and 0xFF }
        fun readLength(): Int {
            var len = keyBytes[pos++].toInt() and 0xFF
            if (len > 127) {
                val numBytes = len and 0x7F
                len = 0
                for (i in 0 until numBytes) {
                    len = (len shl 8) or (keyBytes[pos++].toInt() and 0xFF)
                }
            }
            return len
        }

        readTag(); readLength()            // SEQUENCE (outer)
        readTag(); pos += readLength()     // SEQUENCE (algorithm identifier)
        readTag(); readLength(); pos++     // BIT STRING
        readTag(); readLength()            // SEQUENCE (RSA public key)

        readTag() // INTEGER (modulus)
        val modLen = readLength()
        val modulusBytes = keyBytes.copyOfRange(pos, pos + modLen)
        pos += modLen

        readTag() // INTEGER (exponent)
        val expLen = readLength()
        val exponentBytes = keyBytes.copyOfRange(pos, pos + expLen)

        return Pair(IosBigInt.fromByteArray(modulusBytes), IosBigInt.fromByteArray(exponentBytes))
    }
}

// ============================================================
//  Pure Kotlin BigInteger for iOS (sufficient for RSA modPow)
//  Stores magnitude as little-endian IntArray of 32-bit words.
// ============================================================

@Suppress("NOTHING_TO_INLINE")
internal class IosBigInt private constructor(
    private val mag: IntArray, // little-endian 32-bit words
    private val sign: Int      // -1, 0, or 1
) {
    companion object {
        val ZERO = IosBigInt(IntArray(0), 0)

        fun fromByteArray(bytes: ByteArray): IosBigInt {
            // Big-endian byte array
            if (bytes.isEmpty()) return ZERO
            val positive = (bytes[0].toInt() and 0x80) == 0
            val magnitude = if (positive) bytes else twosComplement(bytes)

            // Strip leading zeros
            var start = 0
            while (start < magnitude.size && magnitude[start].toInt() == 0) start++

            if (start >= magnitude.size) return ZERO

            val trimmed = magnitude.copyOfRange(start, magnitude.size)
            val numWords = (trimmed.size + 3) / 4
            val words = IntArray(numWords)

            for (i in trimmed.indices) {
                val byteVal = trimmed[trimmed.size - 1 - i].toInt() and 0xFF
                words[i / 4] = words[i / 4] or (byteVal shl ((i % 4) * 8))
            }

            return IosBigInt(words, if (positive) 1 else -1)
        }

        private fun twosComplement(bytes: ByteArray): ByteArray {
            val result = ByteArray(bytes.size)
            var carry = 1
            for (i in bytes.indices.reversed()) {
                val v = (bytes[i].toInt() and 0xFF).inv() + carry
                result[i] = (v and 0xFF).toByte()
                carry = v shr 8
            }
            return result
        }
    }

    fun isZero(): Boolean = sign == 0

    fun bitLength(): Int {
        if (sign == 0) return 0
        val topWord = mag[mag.size - 1]
        return (mag.size - 1) * 32 + (32 - topWord.countLeadingZeroBits())
    }

    fun toByteArray(length: Int): ByteArray {
        val result = ByteArray(length)
        if (sign == 0) return result

        val numBits = bitLength()
        val numBytes = (numBits + 7) / 8

        for (i in 0 until numBytes) {
            val wordIdx = i / 4
            val byteIdx = i % 4
            val byteVal = if (wordIdx < mag.size) (mag[wordIdx] shr (byteIdx * 8)) and 0xFF else 0
            val resultIdx = length - 1 - i
            if (resultIdx in result.indices) {
                result[resultIdx] = (if (sign < 0) (0xFF - byteVal + (if (i == 0) 1 else 0)) and 0xFF else byteVal).toByte()
            }
        }

        if (sign < 0) {
            // Fill leading bytes with 0xFF
            for (i in 0 until (length - numBytes)) {
                result[i] = 0xFF.toByte()
            }
        }

        return result
    }

    operator fun compareTo(other: IosBigInt): Int {
        if (sign != other.sign) return sign.compareTo(other.sign)
        if (sign == 0) return 0

        val cmp = compareMagnitude(other)
        return if (sign > 0) cmp else -cmp
    }

    private fun compareMagnitude(other: IosBigInt): Int {
        val maxLen = maxOf(mag.size, other.mag.size)
        for (i in maxLen - 1 downTo 0) {
            val a = if (i < mag.size) mag[i].toLong() and 0xFFFFFFFFL else 0L
            val b = if (i < other.mag.size) other.mag[i].toLong() and 0xFFFFFFFFL else 0L
            if (a != b) return if (a > b) 1 else -1
        }
        return 0
    }

    private fun addMag(other: IosBigInt): IosBigInt {
        val maxLen = maxOf(mag.size, other.mag.size)
        val result = IntArray(maxLen + 1)
        var carry = 0L
        for (i in 0 until maxLen) {
            val sum = (if (i < mag.size) mag[i].toLong() and 0xFFFFFFFFL else 0L) +
                    (if (i < other.mag.size) other.mag[i].toLong() and 0xFFFFFFFFL else 0L) + carry
            result[i] = sum.toInt()
            carry = sum ushr 32
        }
        result[maxLen] = carry.toInt()
        return IosBigInt(result, 1)
    }

    private fun subMag(other: IosBigInt): IosBigInt {
        // Assumes |this| >= |other|
        val result = IntArray(mag.size)
        var borrow = 0L
        for (i in 0 until mag.size) {
            val diff = (mag[i].toLong() and 0xFFFFFFFFL) -
                    (if (i < other.mag.size) other.mag[i].toLong() and 0xFFFFFFFFL else 0L) - borrow
            result[i] = diff.toInt()
            borrow = if (diff < 0) 1L else 0L
        }
        return IosBigInt(trimLeadingZeros(result), 1)
    }

    operator fun plus(other: IosBigInt): IosBigInt {
        if (sign == 0) return other
        if (other.sign == 0) return this
        if (sign == other.sign) {
            return IosBigInt(addMag(other).mag, sign)
        }
        val cmp = compareMagnitude(other)
        return when {
            cmp == 0 -> ZERO
            cmp > 0 -> IosBigInt(subMag(other).mag, sign)
            else -> IosBigInt(other.subMag(this).mag, other.sign)
        }
    }

    operator fun minus(other: IosBigInt): IosBigInt {
        if (other.sign == 0) return this
        if (sign == 0) return IosBigInt(other.mag, -other.sign)
        if (sign != other.sign) {
            return IosBigInt(addMag(other).mag, sign)
        }
        val cmp = compareMagnitude(other)
        return when {
            cmp == 0 -> ZERO
            cmp > 0 -> IosBigInt(subMag(other).mag, sign)
            else -> IosBigInt(other.subMag(this).mag, -sign)
        }
    }

    operator fun times(other: IosBigInt): IosBigInt {
        if (sign == 0 || other.sign == 0) return ZERO
        val resultMag = multiplyMag(mag, other.mag)
        return IosBigInt(resultMag, sign * other.sign)
    }

    private fun multiplyMag(a: IntArray, b: IntArray): IntArray {
        val result = IntArray(a.size + b.size)
        for (i in a.indices) {
            var carry = 0L
            val aVal = a[i].toLong() and 0xFFFFFFFFL
            for (j in b.indices) {
                val prod = aVal * (b[j].toLong() and 0xFFFFFFFFL) +
                        (result[i + j].toLong() and 0xFFFFFFFFL) + carry
                result[i + j] = prod.toInt()
                carry = prod ushr 32
            }
            result[i + b.size] = carry.toInt()
        }
        return trimLeadingZeros(result)
    }

    fun divideAndRemainder(other: IosBigInt): Pair<IosBigInt, IosBigInt> {
        val cmp = compareMagnitude(other)
        if (cmp < 0) return Pair(ZERO, this)
        if (cmp == 0) return Pair(IosBigInt(intArrayOf(1), sign * other.sign), ZERO)

        // Long division in base 2^32
        val dividend = mag.copyOf()
        val divisor = other.mag
        val n = divisor.size
        val m = dividend.size - n

        val quotient = IntArray(m + 1)

        // Normalize: shift divisor so its top bit is set
        val shift = divisor[n - 1].countLeadingZeroBits()
        val normDivisor = if (shift > 0) shiftLeft(divisor, shift) else divisor.copyOf()
        val normDividend = if (shift > 0) shiftLeft(dividend, shift) else dividend.copyOf()

        for (j in m downTo 0) {
            // Estimate q
            val top2 = if (j + n < normDividend.size) {
                ((normDividend[j + n].toLong() and 0xFFFFFFFFL) shl 32) or
                        (if (j + n - 1 < normDividend.size) normDividend[j + n - 1].toLong() and 0xFFFFFFFFL else 0L)
            } else {
                if (j + n - 1 < normDividend.size) normDividend[j + n - 1].toLong() and 0xFFFFFFFFL else 0L
            }

            val topDiv = normDivisor[n - 1].toLong() and 0xFFFFFFFFL
            var qhat = if (n + j < normDividend.size) {
                (top2 / topDiv).coerceAtMost(0xFFFFFFFFL)
            } else {
                0L
            }

            // Refine qhat
            while (qhat > 0) {
                val prod = multiplySingle(normDivisor, qhat.toInt())
                val cmpProd = compareArrays(normDividend, j, prod)
                if (cmpProd >= 0) break
                qhat--
            }

            if (qhat > 0) {
                val prod = multiplySingle(normDivisor, qhat.toInt())
                subtractInPlace(normDividend, j, prod)
            }

            quotient[j] = qhat.toInt()
        }

        // Remainder = normDividend >> shift
        val remainderMag = if (shift > 0) shiftRight(normDividend, shift) else normDividend
        val remainder = IosBigInt(trimLeadingZeros(remainderMag), if (this.sign < 0) -1 else 1)
        val quotientResult = IosBigInt(trimLeadingZeros(quotient), sign * other.sign)

        return Pair(quotientResult, if (remainder.isZero()) ZERO else remainder)
    }

    fun modPow(exponent: IosBigInt, modulus: IosBigInt): IosBigInt {
        if (modulus.isZero()) throw ArithmeticException("Modulus is zero")
        if (modulus == IosBigInt(intArrayOf(1), 1)) return ZERO
        if (exponent.isZero()) return IosBigInt(intArrayOf(1), 1)

        var base = this % modulus
        if (base.sign < 0) base = base + modulus

        var result = IosBigInt(intArrayOf(1), 1)

        // Square-and-multiply
        for (i in 0 until exponent.mag.size) {
            val word = exponent.mag[i]
            for (bit in 0 until 32) {
                if (i == exponent.mag.size - 1 && word ushr bit == 0) break

                if ((word shr bit) and 1 == 1) {
                    result = (result * base) % modulus
                }
                base = (base * base) % modulus
            }
        }
        return result
    }

    operator fun rem(other: IosBigInt): IosBigInt {
        if (other.isZero()) throw ArithmeticException("Division by zero")
        if (isZero()) return ZERO
        val (_, remainder) = divideAndRemainder(other)
        return remainder
    }

    operator fun div(other: IosBigInt): IosBigInt {
        if (other.isZero()) throw ArithmeticException("Division by zero")
        val (quotient, _) = divideAndRemainder(other)
        return quotient
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IosBigInt) return false
        return sign == other.sign && mag.contentEquals(other.mag)
    }

    override fun hashCode(): Int = mag.contentHashCode() * 31 + sign

    // Helper functions
    private fun trimLeadingZeros(a: IntArray): IntArray {
        var len = a.size
        while (len > 0 && a[len - 1] == 0) len--
        return if (len == a.size) a else a.copyOf(len)
    }

    private fun shiftLeft(a: IntArray, bits: Int): IntArray {
        if (bits == 0) return a.copyOf()
        val wordShift = bits / 32
        val bitShift = bits % 32
        val result = IntArray(a.size + wordShift + 1)
        for (i in a.indices) {
            val v = a[i].toLong() and 0xFFFFFFFFL
            result[i + wordShift] = result[i + wordShift] or (v shl bitShift).toInt()
            if (bitShift > 0) {
                result[i + wordShift + 1] = result[i + wordShift + 1] or (v shr (32 - bitShift)).toInt()
            }
        }
        return result
    }

    private fun shiftRight(a: IntArray, bits: Int): IntArray {
        if (bits == 0) return a.copyOf()
        val wordShift = bits / 32
        val bitShift = bits % 32
        val result = IntArray(maxOf(a.size - wordShift, 0))
        for (i in result.indices) {
            val srcIdx = i + wordShift
            val v = a[srcIdx].toLong() and 0xFFFFFFFFL
            result[i] = (v shr bitShift).toInt()
            if (bitShift > 0 && srcIdx + 1 < a.size) {
                result[i] = result[i] or ((a[srcIdx + 1].toLong() and 0xFFFFFFFFL) shl (32 - bitShift)).toInt()
            }
        }
        return result
    }

    private fun multiplySingle(a: IntArray, b: Int): IntArray {
        val result = IntArray(a.size + 1)
        val bLong = b.toLong() and 0xFFFFFFFFL
        var carry = 0L
        for (i in a.indices) {
            val prod = (a[i].toLong() and 0xFFFFFFFFL) * bLong + carry
            result[i] = prod.toInt()
            carry = prod ushr 32
        }
        result[a.size] = carry.toInt()
        return result
    }

    // Compare a[offset..offset+len) with b[0..len), returns -1, 0, or 1
    private fun compareArrays(a: IntArray, offset: Int, b: IntArray): Int {
        val bLen = b.size
        for (i in bLen - 1 downTo 0) {
            val av = if (offset + i < a.size) a[offset + i].toLong() and 0xFFFFFFFFL else 0L
            val bv = b[i].toLong() and 0xFFFFFFFFL
            if (av != bv) return if (av > bv) 1 else -1
        }
        return 0
    }

    // a[offset..] -= b[0..]
    private fun subtractInPlace(a: IntArray, offset: Int, b: IntArray) {
        var borrow = 0L
        for (i in b.indices) {
            val idx = offset + i
            if (idx >= a.size) break
            val diff = (a[idx].toLong() and 0xFFFFFFFFL) - (b[i].toLong() and 0xFFFFFFFFL) - borrow
            a[idx] = diff.toInt()
            borrow = if (diff < 0) 1L else 0L
        }
        // Propagate borrow
        var idx = offset + b.size
        while (borrow != 0L && idx < a.size) {
            val diff = (a[idx].toLong() and 0xFFFFFFFFL) - borrow
            a[idx] = diff.toInt()
            borrow = if (diff < 0) 1L else 0L
            idx++
        }
    }
}

// ============================================================
//  NSData <-> ByteArray helpers
// ============================================================

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    val pinned = pin()
    try {
        return NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    } finally {
        pinned.unpin()
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val byteArray = ByteArray(length)
    val pinned = byteArray.pin()
    try {
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    } finally {
        pinned.unpin()
    }
    return byteArray
}
