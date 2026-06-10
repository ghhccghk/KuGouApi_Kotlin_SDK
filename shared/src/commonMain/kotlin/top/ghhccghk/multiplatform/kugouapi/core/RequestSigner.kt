package top.ghhccghk.multiplatform.kugouapi.core

import top.ghhccghk.multiplatform.kugouapi.KuGouConfig
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class RequestSigner(private val config: KuGouConfig) {

    companion object {
        fun normalize(value: Any?): String = when (value) {
            null -> ""
            is Boolean -> if (value) "1" else "0"
            else -> value.toString()
        }
    }

    /**
     * Android signature: MD5(salt + sortedKeyValuePairs + bodyData + salt)
     */
    fun signatureAndroidParams(params: Map<String, Any?>, data: String = ""): String {
        val paramsString = params.entries
            .sortedBy { it.key }
            .joinToString("") { (key, value) ->
                val v = if (value is Map<*, *>) {
                    Json.encodeToString(
                        serializer(), value
                    )
                } else {
                    normalize(value)
                }
                "$key=$v"
            }
        val salt = config.androidSignatureSalt
        return Crypto.md5("$salt$paramsString$data$salt")
    }

    /**
     * Web signature: MD5(salt + sortedKeyValuePairs + salt)
     */
    fun signatureWebParams(params: Map<String, Any?>): String {
        val salt = config.webSignatureSalt
        val sortedKeys = params.keys.sorted()
        val paramsString = buildString {
            for (key in sortedKeys) {
                append(key)
                append("=")
                append(normalize(params[key]))
            }
        }
        return Crypto.md5("$salt$paramsString$salt")
    }

    fun signatureRegisterParams(params: Map<String, Any?>): String {
        val paramsString = params.values
            .map { normalize(it) }
            .sorted()
            .joinToString("")
        return Crypto.md5("1014${paramsString}1014")
    }

    fun signKey(hash: String, mid: String, userid: Long = 0, appid: Long): String {
        val salt = config.signKeySalt
        println("$hash$salt$appid$mid$userid")
        return Crypto.md5("$hash$salt$appid$mid$userid")
    }

    /**
     * signParamsKey: MD5(appid + salt + clientver + data)
     * 对齐 Node.js helper.js 中的 signParamsKey
     */
    fun signParamsKey(data: Any): String {
        val salt = config.androidSignatureSalt
        return Crypto.md5("${config.activeAppId}$salt${config.activeClientVersion}$data")
    }

    fun computeSignature(
        encryptType: EncryptType,
        params: Map<String, Any?>,
        data: String = ""
    ): String = when (encryptType) {
        EncryptType.ANDROID -> signatureAndroidParams(params, data)
        EncryptType.WEB -> signatureWebParams(params)
        EncryptType.REGISTER -> signatureRegisterParams(params)
    }
}
