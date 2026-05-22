package com.ghhccghk.multiplatform.kugouapi.shared.core

import com.ghhccghk.multiplatform.kugouapi.shared.KuGouConfig
import com.ghhccghk.multiplatform.kugouapi.shared.model.EncryptType

internal class RequestSigner(private val config: KuGouConfig) {

    /**
     * Android signature: MD5(salt + sortedKeyValuePairs + bodyData + salt)
     */
    fun signatureAndroidParams(params: Map<String, Any?>, data: String = ""): String {
        val paramsString = params.entries
            .sortedBy { it.key }
            .joinToString("") { (key, value) ->
                val v = if (value is Map<*, *>) {
                    kotlinx.serialization.json.Json.encodeToString(
                        kotlinx.serialization.serializer(), value
                    )
                } else {
                    value.toString()
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
        val paramsString = params.entries
            .sortedBy { it.key }
            .joinToString("") { (key, value) -> "$key=$value" }
        val salt = config.webSignatureSalt
        return Crypto.md5("$salt$paramsString$salt")
    }

    /**
     * Register signature: MD5("1014" + sortedValues + "1014")
     */
    fun signatureRegisterParams(params: Map<String, Any?>): String {
        val paramsString = params.values
            .sortedBy { it.toString() }
            .joinToString("") { it.toString() }
        return Crypto.md5("1014${paramsString}1014")
    }

    /**
     * signKey: MD5(hash + salt + appid + mid + userid)
     */
    fun signKey(hash: String, mid: String, userid: Any = 0, appid: Any? = null): String {
        val salt = config.signKeySalt
        val appId = appid ?: config.activeAppId
        return Crypto.md5("$hash$salt$appId$mid$userid")
    }

    /**
     * signParamsKey: MD5(appid + salt + clientver + data)
     */
    fun signParamsKey(data: Any): String {
        val salt = config.androidSignatureSalt
        return Crypto.md5("${config.activeAppId}$salt${config.activeClientVersion}$data")
    }

    /**
     * Compute signature based on encrypt type
     */
    fun computeSignature(
        encryptType: EncryptType,
        params: Map<String, Any?>,
        data: String = ""
    ): String {
        return when (encryptType) {
            EncryptType.ANDROID -> signatureAndroidParams(params, data)
            EncryptType.WEB -> signatureWebParams(params)
            EncryptType.REGISTER -> signatureRegisterParams(params)
        }
    }
}
