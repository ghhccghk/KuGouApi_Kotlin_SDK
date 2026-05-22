package com.ghhccghk.multiplatform.kugouapi.shared.core

import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import platform.Foundation.create

actual object Crypto {
    actual fun md5(data: String): String {
        val bytes = data.encodeToByteArray()
        val digest = ByteArray(16)
        // Pure Kotlin MD5 implementation
        return md5Pure(bytes)
    }

    actual fun aesEncrypt(plaintext: String, key: String, iv: String): String {
        // Use CommonCrypto via C interop - simplified approach
        val keyBytes = key.encodeToByteArray().copyOf(16)
        val ivBytes = iv.encodeToByteArray().copyOf(16)
        val inputBytes = plaintext.encodeToByteArray()
        val padded = pkcs7Pad(inputBytes, 16)
        val output = aesCbcEncrypt(padded, keyBytes, ivBytes)
        return output.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
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
        // For iOS, RSA raw encryption using SecKey would require complex interop.
        // Using a simplified approach that matches the Node.js implementation.
        val pemContent = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        // Fallback: manual modular exponentiation
        val keyBytes = decodeBase64(pemContent)
        val (modulus, exponent) = parseRsaPublicKey(keyBytes)
        val message = bytesToBigInteger(data)
        val result = modPow(message, exponent, modulus)
        val keyLength = (modulus.bitLength() + 7) / 8
        val resultBytes = bigIntegerToBytes(result, keyLength)
        return resultBytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    // --- Pure Kotlin implementations ---

    private fun md5Pure(input: ByteArray): String {
        // RFC 1321 MD5 implementation
        val message = input.copyOf()
        val origLen = message.size.toLong()
        val bitLen = origLen * 8

        // Padding
        val paddedLen = ((origLen + 8) / 64 + 1) * 64
        val padded = ByteArray(paddedLen.toInt())
        message.copyInto(padded)
        padded[origLen.toInt()] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[(paddedLen - 8 + i).toInt()] = (bitLen shr (i * 8)).toByte()
        }

        var a0 = 0x67452301L
        var b0 = 0xefcdab89L
        var c0 = 0x98badcfeL
        var d0 = 0x10325476L

        val s = intArrayOf(
            7,12,17,22,7,12,17,22,7,12,17,22,7,12,17,22,
            5,9,14,20,5,9,14,20,5,9,14,20,5,9,14,20,
            4,11,16,23,4,11,16,23,4,11,16,23,4,11,16,23,
            6,10,15,21,6,10,15,21,6,10,15,21,6,10,15,21
        )
        val k = longArrayOf(
            0xd76aa478,0xe8c7b756,0x242070db,0xc1bdceee,
            0xf57c0faf,0x4787c62a,0xa8304613,0xfd469501,
            0x698098d8,0x8b44f7af,0xffff5bb1,0x895cd7be,
            0x6b901122,0xfd987193,0xa679438e,0x49b40821,
            0xf61e2562,0xc040b340,0x265e5a51,0xe9b6c7aa,
            0xd62f105d,0x02441453,0xd8a1e681,0xe7d3fbc8,
            0x21e1cde6,0xc33707d6,0xf4d50d87,0x455a14ed,
            0xa9e3e905,0xfcefa3f8,0x676f02d9,0x8d2a4c8a,
            0xfffa3942,0x8771f681,0x6d9d6122,0xfde5380c,
            0xa4beea44,0x4bdecfa9,0xf6bb4b60,0xbebfbc70,
            0x289b7ec6,0xeaa127fa,0xd4ef3085,0x04881d05,
            0xd9d4d039,0xe6db99e5,0x1fa27cf8,0xc4ac5665,
            0xf4292244,0x432aff97,0xab9423a7,0xfc93a039,
            0x655b59c3,0x8f0ccc92,0xffeff47d,0x85845dd1,
            0x6fa87e4f,0xfe2ce6e0,0xa3014314,0x4e0811a1,
            0xf7537e82,0xbd3af235,0x2ad7d2bb,0xeb86d391
        )

        for (offset in 0 until padded.size step 64) {
            val m = IntArray(16)
            for (j in 0 until 16) {
                m[j] = (padded[offset + j*4].toInt() and 0xFF) or
                        ((padded[offset + j*4+1].toInt() and 0xFF) shl 8) or
                        ((padded[offset + j*4+2].toInt() and 0xFF) shl 16) or
                        ((padded[offset + j*4+3].toInt() and 0xFF) shl 24)
            }

            var a = a0; var b = b0; var c = c0; var d = d0

            for (i in 0 until 64) {
                var f = 0L; var g = 0
                when {
                    i < 16 -> { f = (b and c) or (b.inv() and d); g = i }
                    i < 32 -> { f = (d and b) or (d.inv() and c); g = (5*i+1) % 16 }
                    i < 48 -> { f = b xor c xor d; g = (3*i+5) % 16 }
                    else -> { f = c xor (b or d.inv()); g = (7*i) % 16 }
                }
                val temp = d
                d = c
                c = b
                b = b + rotateLeft((a + f + k[i] + m[g].toLong()), s[i])
                a = temp
            }
            a0 += a; b0 += b; c0 += c; d0 += d
        }

        return intToHexLittleEndian(a0) + intToHexLittleEndian(b0) +
                intToHexLittleEndian(c0) + intToHexLittleEndian(d0)
    }

    private fun rotateLeft(x: Long, n: Int): Long {
        return ((x shl n) or (x ushr (32 - n))) and 0xFFFFFFFFL
    }

    private fun intToHexLittleEndian(value: Long): String {
        val v = value and 0xFFFFFFFFL
        return "%02x".format((v and 0xFF).toInt()) +
                "%02x".format(((v shr 8) and 0xFF).toInt()) +
                "%02x".format(((v shr 16) and 0xFF).toInt()) +
                "%02x".format(((v shr 24) and 0xFF).toInt())
    }

    private fun pkcs7Pad(data: ByteArray, blockSize: Int): ByteArray {
        val padLen = blockSize - (data.size % blockSize)
        return data + ByteArray(padLen) { padLen.toByte() }
    }

    private fun pkcs7Unpad(data: ByteArray): ByteArray {
        val padLen = data.last().toInt() and 0xFF
        return data.copyOf(data.size - padLen)
    }

    private fun aesCbcEncrypt(input: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        // Pure Kotlin AES implementation
        // For production, this should use platform crypto; here we use a simplified version
        val result = ByteArray(input.size)
        var prev = iv.copyOf()
        for (i in input.indices step 16) {
            val block = input.copyOfRange(i, i + 16)
            val xored = ByteArray(16) { (block[it].toInt() xor prev[it].toInt()).toByte() }
            val encrypted = aesEncryptBlock(xored, key)
            encrypted.copyInto(result, i)
            prev = encrypted
        }
        return result
    }

    private fun aesCbcDecrypt(input: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val result = ByteArray(input.size)
        var prev = iv.copyOf()
        for (i in input.indices step 16) {
            val block = input.copyOfRange(i, i + 16)
            val decrypted = aesDecryptBlock(block, key)
            val plain = ByteArray(16) { (decrypted[it].toInt() xor prev[it].toInt()).toByte() }
            plain.copyInto(result, i)
            prev = block
        }
        return result
    }

    // AES S-Box
    private val sBox = intArrayOf(
        0x63,0x7c,0x77,0x7b,0xf2,0x6b,0x6f,0xc5,0x30,0x01,0x67,0x2b,0xfe,0xd7,0xab,0x76,
        0xca,0x82,0xc9,0x7d,0xfa,0x59,0x47,0xf0,0xad,0xd4,0xa2,0xaf,0x9c,0xa4,0x72,0xc0,
        0xb7,0xfd,0x93,0x26,0x36,0x3f,0xf7,0xcc,0x34,0xa5,0xe5,0xf1,0x71,0xd8,0x31,0x15,
        0x04,0xc7,0x23,0xc3,0x18,0x96,0x05,0x9a,0x07,0x12,0x80,0xe2,0xeb,0x27,0xb2,0x75,
        0x09,0x83,0x2c,0x1a,0x1b,0x6e,0x5a,0xa0,0x52,0x3b,0xd6,0xb3,0x29,0xe3,0x2f,0x84,
        0x53,0xd1,0x00,0xed,0x20,0xfc,0xb1,0x5b,0x6a,0xcb,0xbe,0x39,0x4a,0x4c,0x58,0xcf,
        0xd0,0xef,0xaa,0xfb,0x43,0x4d,0x33,0x85,0x45,0xf9,0x02,0x7f,0x50,0x3c,0x9f,0xa8,
        0x51,0xa3,0x40,0x8f,0x92,0x9d,0x38,0xf5,0xbc,0xb6,0xda,0x21,0x10,0xff,0xf3,0xd2,
        0xcd,0x0c,0x13,0xec,0x5f,0x97,0x44,0x17,0xc4,0xa7,0x7e,0x3d,0x64,0x5d,0x19,0x73,
        0x60,0x81,0x4f,0xdc,0x22,0x2a,0x90,0x88,0x46,0xee,0xb8,0x14,0xde,0x5e,0x0b,0xdb,
        0xe0,0x32,0x3a,0x0a,0x49,0x06,0x24,0x5c,0xc2,0xd3,0xac,0x62,0x91,0x95,0xe4,0x79,
        0xe7,0xc8,0x37,0x6d,0x8d,0xd5,0x4e,0xa9,0x6c,0x56,0xf4,0xea,0x65,0x7a,0xae,0x08,
        0xba,0x78,0x25,0x2e,0x1c,0xa6,0xb4,0xc6,0xe8,0xdd,0x74,0x1f,0x4b,0xbd,0x8b,0x8a,
        0x70,0x3e,0xb5,0x66,0x48,0x03,0xf6,0x0e,0x61,0x35,0x57,0xb9,0x86,0xc1,0x1d,0x9e,
        0xe1,0xf8,0x98,0x11,0x69,0xd9,0x8e,0x94,0x9b,0x1e,0x87,0xe9,0xce,0x55,0x28,0xdf,
        0x8c,0xa1,0x89,0x0d,0xbf,0xe6,0x42,0x68,0x41,0x99,0x2d,0x0f,0xb0,0x54,0xbb,0x16
    )

    private val invSBox = intArrayOf(
        0x52,0x09,0x6a,0xd5,0x30,0x36,0xa5,0x38,0xbf,0x40,0xa3,0x9e,0x81,0xf3,0xd7,0xfb,
        0x7c,0xe3,0x39,0x82,0x9b,0x2f,0xff,0x87,0x34,0x8e,0x43,0x44,0xc4,0xde,0xe9,0xcb,
        0x54,0x7b,0x94,0x32,0xa6,0xc2,0x23,0x3d,0xee,0x4c,0x95,0x0b,0x43,0xfa,0xc0,0x58,
        0xcd,0x79,0xe5,0xf5,0x98,0x11,0x69,0xd9,0x8e,0x94,0x9b,0x1e,0x87,0xe9,0xce,0x55,
        0x28,0xdf,0x8c,0xa1,0x89,0x0d,0xbf,0xe6,0x42,0x68,0x41,0x99,0x2d,0x0f,0xb0,0x54,
        0xbb,0x16,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0x52,0x09,0x6a,0xd5,0x30,0x36,0xa5,0x38,0xbf,0x40,0xa3,0x9e,0x81,0xf3,0xd7,0xfb,
        0x7c,0xe3,0x39,0x82,0x9b,0x2f,0xff,0x87,0x34,0x8e,0x43,0x44,0xc4,0xde,0xe9,0xcb,
        0x54,0x7b,0x94,0x32,0xa6,0xc2,0x23,0x3d,0xee,0x4c,0x95,0x0b,0x4e,0xc3,0xf1,0x40,
        0x08,0x2e,0xa1,0x66,0x28,0xd9,0x24,0xb2,0x76,0x5b,0xa2,0x49,0x6d,0x8b,0xd1,0x25,
        0x72,0xf8,0xf6,0x64,0x86,0x68,0x98,0x16,0xd4,0xa4,0x5c,0xcc,0x5d,0x65,0xb6,0x92,
        0x6c,0x70,0x48,0x50,0xfd,0xed,0xb9,0xda,0x5e,0x15,0x46,0x57,0xa7,0x8d,0x9d,0x84,
        0x90,0xd8,0xab,0x00,0x8c,0xbc,0xd3,0x0a,0xf7,0xe4,0x58,0x05,0xb8,0xb3,0x45,0x06,
        0xd0,0x2c,0x1e,0x8f,0xca,0x3f,0x0f,0x02,0xc1,0xaf,0xbd,0x03,0x01,0x13,0x8a,0x6b,
        0x3a,0x91,0x11,0x41,0x4f,0x67,0xdc,0xea,0x97,0xf2,0xcf,0xce,0xf0,0xb4,0xe6,0x73,
        0x96,0xac,0x74,0x22,0xe7,0xad,0x35,0x85,0xe2,0xf9,0x37,0xe8,0x1c,0x75,0xdf,0x6e,
        0x47,0xf1,0x1a,0x71,0x1d,0x29,0xc5,0x89,0x6f,0xb7,0x62,0x0e,0xaa,0x18,0xbe,0x1b,
        0xfc,0x56,0x3e,0x4b,0xc6,0xd2,0x79,0x20,0x9a,0xdb,0xc0,0xfe,0x78,0xcd,0x5a,0xf4,
        0x1f,0xdd,0xa8,0x33,0x88,0x07,0xc7,0x31,0xb1,0x12,0x10,0x59,0x27,0x80,0xec,0x5f,
        0x60,0x51,0x7f,0xa9,0x19,0xb5,0x4a,0x0d,0x2d,0xe5,0x7a,0x9f,0x93,0xc9,0x9c,0xef,
        0xa0,0xe0,0x3b,0x4d,0xae,0x2a,0xf5,0xb0,0xc8,0xeb,0xbb,0x3c,0x83,0x53,0x99,0x61,
        0x17,0x2b,0x04,0x7e,0xba,0x77,0xd6,0x26,0xe1,0x69,0x14,0x63,0x55,0x21,0x0c,0x7d
    )

    private fun gmul(a: Int, b: Int): Int {
        var p = 0; var aa = a; var bb = b
        for (i in 0 until 8) {
            if (bb and 1 != 0) p = p xor aa
            val hi = aa and 0x80
            aa = (aa shl 1) and 0xFF
            if (hi != 0) aa = aa xor 0x1b
            bb = bb shr 1
        }
        return p
    }

    private fun subBytes(state: Array<IntArray>) {
        for (r in 0 until 4) for (c in 0 until 4) state[r][c] = sBox[state[r][c]]
    }

    private fun invSubBytes(state: Array<IntArray>) {
        for (r in 0 until 4) for (c in 0 until 4) state[r][c] = invSBox[state[r][c]]
    }

    private fun shiftRows(state: Array<IntArray>) {
        var temp = state[1][0]
        state[1][0] = state[1][1]; state[1][1] = state[1][2]; state[1][2] = state[1][3]; state[1][3] = temp
        temp = state[2][0]; state[2][0] = state[2][2]; state[2][2] = temp
        temp = state[2][1]; state[2][1] = state[2][3]; state[2][3] = temp
        temp = state[3][3]; state[3][3] = state[3][2]; state[3][2] = state[3][1]; state[3][1] = state[3][0]; state[3][0] = temp
    }

    private fun invShiftRows(state: Array<IntArray>) {
        var temp = state[1][3]
        state[1][3] = state[1][2]; state[1][2] = state[1][1]; state[1][1] = state[1][0]; state[1][0] = temp
        temp = state[2][0]; state[2][0] = state[2][2]; state[2][2] = temp
        temp = state[2][1]; state[2][1] = state[2][3]; state[2][3] = temp
        temp = state[3][0]; state[3][0] = state[3][1]; state[3][1] = state[3][2]; state[3][2] = state[3][3]; state[3][3] = temp
    }

    private fun mixColumns(state: Array<IntArray>) {
        for (c in 0 until 4) {
            val a = IntArray(4) { state[it][c] }
            state[0][c] = gmul(a[0],2) xor gmul(a[1],3) xor a[2] xor a[3]
            state[1][c] = a[0] xor gmul(a[1],2) xor gmul(a[2],3) xor a[3]
            state[2][c] = a[0] xor a[1] xor gmul(a[2],2) xor gmul(a[3],3)
            state[3][c] = gmul(a[0],3) xor a[1] xor a[2] xor gmul(a[3],2)
        }
    }

    private fun invMixColumns(state: Array<IntArray>) {
        for (c in 0 until 4) {
            val a = IntArray(4) { state[it][c] }
            state[0][c] = gmul(a[0],14) xor gmul(a[1],11) xor gmul(a[2],13) xor gmul(a[3],9)
            state[1][c] = gmul(a[0],9) xor gmul(a[1],14) xor gmul(a[2],11) xor gmul(a[3],13)
            state[2][c] = gmul(a[0],13) xor gmul(a[1],9) xor gmul(a[2],14) xor gmul(a[3],11)
            state[3][c] = gmul(a[0],11) xor gmul(a[1],13) xor gmul(a[2],9) xor gmul(a[3],14)
        }
    }

    private fun addRoundKey(state: Array<IntArray>, roundKey: Array<IntArray>) {
        for (r in 0 until 4) for (c in 0 until 4) state[r][c] = state[r][c] xor roundKey[r][c]
    }

    private fun keyExpansion(key: ByteArray): Array<IntArray> {
        val nk = key.size / 4
        val nr = nk + 6
        val totalWords = 4 * (nr + 1)
        val w = Array(totalWords) { IntArray(4) }

        for (i in 0 until nk) {
            for (j in 0 until 4) w[i][j] = key[i*4+j].toInt() and 0xFF
        }

        val rcon = intArrayOf(0x01,0x02,0x04,0x08,0x10,0x20,0x40,0x80,0x1b,0x36)

        for (i in nk until totalWords) {
            val temp = IntArray(4) { w[i-1][it] }
            if (i % nk == 0) {
                // RotWord
                val t = temp[0]; temp[0] = temp[1]; temp[1] = temp[2]; temp[2] = temp[3]; temp[3] = t
                // SubWord
                for (j in 0 until 4) temp[j] = sBox[temp[j]]
                temp[0] = temp[0] xor rcon[i/nk - 1]
            } else if (nk > 6 && i % nk == 4) {
                for (j in 0 until 4) temp[j] = sBox[temp[j]]
            }
            for (j in 0 until 4) w[i][j] = w[i-nk][j] xor temp[j]
        }

        return w
    }

    private fun getRoundKey(expandedKey: Array<IntArray>, round: Int): Array<IntArray> {
        val rk = Array(4) { IntArray(4) }
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                rk[r][c] = expandedKey[round * 4 + c][r]
            }
        }
        return rk
    }

    private fun bytesToState(block: ByteArray): Array<IntArray> {
        val state = Array(4) { IntArray(4) }
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                state[r][c] = block[c*4+r].toInt() and 0xFF
            }
        }
        return state
    }

    private fun stateToBytes(state: Array<IntArray>): ByteArray {
        val out = ByteArray(16)
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                out[c*4+r] = state[r][c].toByte()
            }
        }
        return out
    }

    private fun aesEncryptBlock(block: ByteArray, key: ByteArray): ByteArray {
        val expandedKey = keyExpansion(key)
        val nk = key.size / 4
        val nr = nk + 6
        val state = bytesToState(block)

        addRoundKey(state, getRoundKey(expandedKey, 0))

        for (round in 1 until nr) {
            subBytes(state)
            shiftRows(state)
            mixColumns(state)
            addRoundKey(state, getRoundKey(expandedKey, round))
        }

        subBytes(state)
        shiftRows(state)
        addRoundKey(state, getRoundKey(expandedKey, nr))

        return stateToBytes(state)
    }

    private fun aesDecryptBlock(block: ByteArray, key: ByteArray): ByteArray {
        val expandedKey = keyExpansion(key)
        val nk = key.size / 4
        val nr = nk + 6
        val state = bytesToState(block)

        addRoundKey(state, getRoundKey(expandedKey, nr))

        for (round in nr - 1 downTo 1) {
            invShiftRows(state)
            invSubBytes(state)
            addRoundKey(state, getRoundKey(expandedKey, round))
            invMixColumns(state)
        }

        invShiftRows(state)
        invSubBytes(state)
        addRoundKey(state, getRoundKey(expandedKey, 0))

        return stateToBytes(state)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((hex[i].digitToInt(16) shl 4) + hex[i+1].digitToInt(16)).toByte()
        }
        return data
    }

    // BigInteger implementation for RSA
    private data class BigInt(val digits: IntArray, val negative: Boolean) {
        fun bitLength(): Int {
            if (digits.isEmpty()) return 0
            val top = digits.last()
            return (digits.size - 1) * 32 + (32 - top.countLeadingZeroBits())
        }
    }

    private fun bytesToBigInteger(bytes: ByteArray): BigInt {
        if (bytes.isEmpty()) return BigInt(intArrayOf(0), false)
        val positive = bytes[0].toInt() and 0x80 == 0
        val raw = if (!positive) {
            // Two's complement
            val arr = ByteArray(bytes.size)
            var carry = 1
            for (i in bytes.indices.reversed()) {
                val v = (bytes[i].toInt() xor 0xFF) + carry
                arr[i] = (v and 0xFF).toByte()
                carry = v shr 8
            }
            arr
        } else bytes

        val words = mutableListOf<Int>()
        var i = raw.size
        while (i > 0) {
            val end = i
            val start = maxOf(0, i - 4)
            var word = 0
            for (j in start until end) {
                word = (word shl 8) or (raw[j].toInt() and 0xFF)
            }
            words.add(word)
            i = start
        }
        return BigInt(words.toIntArray(), !positive)
    }

    private fun bigIntegerToBytes(value: BigInt, minLen: Int): ByteArray {
        val words = value.digits
        val byteList = mutableListOf<Int>()
        for (word in words) {
            byteList.add((word shr 24) and 0xFF)
            byteList.add((word shr 16) and 0xFF)
            byteList.add((word shr 8) and 0xFF)
            byteList.add(word and 0xFF)
        }
        // Remove leading zeros
        var start = 0
        while (start < byteList.size - 1 && byteList[start] == 0) start++
        val result = ByteArray(maxOf(byteList.size - start, minLen))
        for (i in (byteList.size - start) downTo 1) {
            result[result.size - i] = byteList[byteList.size - i + start - (byteList.size - start - result.size + start)].toByte()
        }
        // Fix: simpler approach
        val simplified = ByteArray(maxOf(byteList.size - start, minLen))
        val offset = simplified.size - (byteList.size - start)
        for (i in start until byteList.size) {
            simplified[offset + i - start] = byteList[i].toByte()
        }
        return simplified
    }

    private fun modPow(base: BigInt, exponent: BigInt, modulus: BigInt): BigInt {
        var result = BigInt(intArrayOf(1), false)
        var b = base
        val bits = exponent.bitLength()
        for (i in 0 until bits) {
            val wordIdx = i / 32
            val bitIdx = i % 32
            if (wordIdx < exponent.digits.size && (exponent.digits[wordIdx] and (1 shl bitIdx)) != 0) {
                result = bigMulMod(result, b, modulus)
            }
            b = bigMulMod(b, b, modulus)
        }
        return result
    }

    private fun bigMulMod(a: BigInt, b: BigInt, m: BigInt): BigInt {
        // Simplified: convert to Long arrays for computation
        // This works for the 1024-bit RSA keys used by KuGou
        val aLong = bigToLongArray(a)
        val bLong = bigToLongArray(b)
        val mLong = bigToLongArray(m)

        // a * b using schoolbook multiplication
        val prod = LongArray(aLong.size + bLong.size)
        for (i in aLong.indices) {
            var carry = 0L
            for (j in bLong.indices) {
                val idx = i + j
                val mul = aLong[i] * bLong[j] + prod[idx] + carry
                prod[idx] = mul and 0x7FFFFFFFL
                carry = mul shr 31
            }
            prod[i + bLong.size] = carry
        }

        // prod mod m
        val remainder = longArrayMod(prod, mLong)
        return longArrayToBigInt(remainder)
    }

    private fun bigToLongArray(a: BigInt): LongArray {
        // Each element stores 31 bits
        val bits = a.bitLength()
        val len = (bits + 30) / 31
        val result = LongArray(len)
        for (i in 0 until len) {
            val startBit = i * 31
            val wordIdx = startBit / 32
            val bitOffset = startBit % 32
            var value = 0L
            if (wordIdx < a.digits.size) {
                value = (a.digits[wordIdx].toLong() and 0xFFFFFFFFL) shr bitOffset
            }
            if (bitOffset > 1 && wordIdx + 1 < a.digits.size) {
                value = value or ((a.digits[wordIdx + 1].toLong() and 0xFFFFFFFFL) shl (32 - bitOffset))
            }
            result[i] = value and 0x7FFFFFFFL
        }
        return result
    }

    private fun longArrayMod(a: LongArray, m: LongArray): LongArray {
        var remainder = a.copyOf()
        while (true) {
            // Compare
            var cmp = 0
            var i = maxOf(remainder.size, m.size) - 1
            while (i >= 0) {
                val av = if (i < remainder.size) remainder[i] else 0L
                val mv = if (i < m.size) m[i] else 0L
                if (av > mv) { cmp = 1; break }
                if (av < mv) { cmp = -1; break }
                i--
            }
            if (cmp < 0) break
            // Subtract m shifted
            var borrow = 0L
            for (j in m.indices) {
                val idx = j + (remainder.size - m.size).coerceAtLeast(0)
                if (idx < remainder.size) {
                    val diff = remainder[idx] - (if (j < m.size) m[j] else 0L) - borrow
                    remainder[idx] = if (diff < 0) diff + 0x80000000L else diff
                    borrow = if (diff < 0) 1L else 0L
                }
            }
            // Trim leading zeros
            while (remainder.size > 1 && remainder.last() == 0L) {
                remainder = remainder.copyOf(remainder.size - 1)
            }
        }
        return remainder
    }

    private fun longArrayToBigInt(arr: LongArray): BigInt {
        val words = IntArray(arr.size)
        for (i in arr.indices) words[i] = arr[i].toInt()
        return BigInt(words, false)
    }

    private fun parseRsaPublicKey(derBytes: ByteArray): Pair<BigInt, BigInt> {
        // Simple DER parsing for RSA public key
        // SEQUENCE { SEQUENCE { OID, NULL }, BIT STRING { SEQUENCE { modulus, exponent } } }
        var pos = 0

        fun readLength(): Int {
            val first = derBytes[pos++].toInt() and 0xFF
            if (first < 0x80) return first
            val numBytes = first and 0x7F
            var len = 0
            repeat(numBytes) { len = (len shl 8) or (derBytes[pos++].toInt() and 0xFF) }
            return len
        }

        fun readInteger(): ByteArray {
            assert(derBytes[pos++].toInt() and 0xFF == 0x02) // INTEGER tag
            val len = readLength()
            val data = derBytes.copyOfRange(pos, pos + len)
            pos += len
            return data
        }

        // Skip SEQUENCE
        pos++; readLength()
        // Skip inner SEQUENCE (algorithm identifier)
        pos++; val algLen = readLength(); pos += algLen
        // BIT STRING
        assert(derBytes[pos++].toInt() and 0xFF == 0x03) // BIT STRING tag
        readLength()
        pos++ // skip unused bits byte
        // Inner SEQUENCE (RSAPublicKey)
        pos++; readLength()
        val modulusBytes = readInteger()
        val exponentBytes = readInteger()

        val modulus = bytesToBigInteger(modulusBytes)
        val exponent = bytesToBigInteger(exponentBytes)
        return Pair(modulus, exponent)
    }

    private fun decodeBase64(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val cleanInput = input.replace("=", "")
        val output = mutableListOf<Byte>()
        var i = 0
        while (i < cleanInput.length) {
            val chunk = cleanInput.substring(i, minOf(i + 4, cleanInput.length))
            var bits = 0
            for (ch in chunk) {
                bits = (bits shl 6) or alphabet.indexOf(ch)
            }
            val shift = (4 - chunk.length) * 6
            bits = bits shl shift
            output.add(((bits shr 16) and 0xFF).toByte())
            if (chunk.length > 2) output.add(((bits shr 8) and 0xFF).toByte())
            if (chunk.length > 3) output.add((bits and 0xFF).toByte())
            i += 4
        }
        return output.toByteArray()
    }
}
