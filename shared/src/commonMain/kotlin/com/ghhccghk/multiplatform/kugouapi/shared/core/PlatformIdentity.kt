package com.ghhccghk.multiplatform.kugouapi.shared.core

import com.ghhccghk.multiplatform.kugouapi.shared.KuGouConfig

internal object PlatformIdentity {

    fun generateGuid(): String {
        fun segment(len: Int): String {
            val chars = "0123456789abcdef"
            return buildString(len) {
                repeat(len) { append(chars.random()) }
            }
        }
        return "${segment(8)}-${segment(4)}-${segment(4)}-${segment(4)}-${segment(12)}"
    }

    fun generateDev(length: Int = 10): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return buildString(length) {
            repeat(length) { append(chars.random()) }
        }
    }

    /**
     * 将 MD5 hex 转换为十进制大整数字符串
     * 与 Node.js 版本的 big-integer 实现等价
     */
    fun calculateMid(guid: String): String {
        val md5Hex = Crypto.md5(guid)
        var result = "0"
        for (ch in md5Hex) {
            // result = result * 16 + digit
            result = decimalMultiplyBySmall(result, 16)
            val digit = ch.toString().toInt(16)
            result = decimalAddSmall(result, digit)
        }
        return result
    }

    /** 十进制字符串 × 小整数 */
    private fun decimalMultiplyBySmall(num: String, multiplier: Int): String {
        if (num == "0") return "0"
        val sb = StringBuilder()
        var carry = 0
        for (i in num.length - 1 downTo 0) {
            val product = (num[i] - '0') * multiplier + carry
            sb.append(product % 10)
            carry = product / 10
        }
        while (carry > 0) {
            sb.append(carry % 10)
            carry /= 10
        }
        return sb.reverse().toString()
    }

    /** 十进制字符串 + 小整数 */
    private fun decimalAddSmall(num: String, addend: Int): String {
        val sb = StringBuilder()
        var carry = addend
        for (i in num.length - 1 downTo 0) {
            val sum = (num[i] - '0') + carry
            sb.append(sum % 10)
            carry = sum / 10
        }
        while (carry > 0) {
            sb.append(carry % 10)
            carry /= 10
        }
        return sb.reverse().toString()
    }

    fun initializeCookies(cookieJar: CookieJar, config: KuGouConfig) {
        // 强制重新生成，确保使用新的 calculateMid
        val guid = Crypto.md5(generateGuid())
        cookieJar["KUGOU_API_GUID"] = guid
        cookieJar["KUGOU_API_MID"] = calculateMid(guid)
        cookieJar["KUGOU_API_DEV"] = generateDev().uppercase()
        cookieJar["KUGOU_API_MAC"] = "02:00:00:00:00:00"
        cookieJar["dfid"] = "-"

        println("=== PlatformIdentity Init ===")
        println("GUID: ${cookieJar.getGuid()}")
        println("MID: ${cookieJar.getMid()}")
        println("DEV: ${cookieJar.getDev()}")
        println("=============================")
    }
}
