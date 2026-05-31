package top.ghhccghk.multiplatform.kugouapi.core

import top.ghhccghk.multiplatform.kugouapi.KuGouConfig
import kotlin.text.iterator

internal object PlatformIdentity {

    fun generateRandomString(len: Int = 16, chars: String = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"): String {
        return buildString(len) {
            repeat(len) { append(chars.random()) }
        }
    }

    fun generateGuid(): String {
        fun segment(len: Int): String {
            val chars = "0123456789abcdef"
            return buildString(len) {
                repeat(len) { append(chars.random()) }
            }
        }
        return "${segment(8)}-${segment(4)}-${segment(4)}-${segment(4)}-${segment(12)}"
    }

    /**
     * 将 MD5 hex 转换为十进制大整数字符串
     * 对齐 Node.js 版本的 BigInt(MD5(str))
     */
    fun calculateMid(guid: String): String {
        val md5Hex = Crypto.md5(guid)
        return hexToBigIntString(md5Hex)
    }

    private fun hexToBigIntString(hex: String): String {
        var result = "0"
        for (ch in hex) {
            result = decimalMultiplyBySmall(result, 16)
            val digit = ch.toString().toInt(16)
            result = decimalAddSmall(result, digit)
        }
        return result
    }

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
        if (cookieJar.getGuid().isEmpty()) {
            val rawGuid = generateGuid()
            val hashedGuid = Crypto.md5(rawGuid)
            cookieJar["KUGOU_API_GUID"] = hashedGuid
            cookieJar["KUGOU_API_MID"] = calculateMid(hashedGuid)
        }
        if (cookieJar.getDev().isEmpty()) {
            cookieJar["KUGOU_API_DEV"] = generateRandomString(10).uppercase()
        }
        if (cookieJar["KUGOU_API_MAC"].isNullOrEmpty()) {
            cookieJar["KUGOU_API_MAC"] = "02:00:00:00:00:00"
        }
        if (cookieJar.getDfid() == "-") {
            cookieJar["dfid"] = "-"
        }
    }
}
