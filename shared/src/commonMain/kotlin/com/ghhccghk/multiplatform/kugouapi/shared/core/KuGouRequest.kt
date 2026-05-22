package com.ghhccghk.multiplatform.kugouapi.shared.core

import com.ghhccghk.multiplatform.kugouapi.shared.model.EncryptType

data class KuGouRequest(
    val url: String,
    val method: HttpMethod,
    val params: Map<String, Any?> = emptyMap(),
    val data: Any? = null,
    val headers: Map<String, String> = emptyMap(),
    val baseUrl: String? = null,
    val encryptType: EncryptType = EncryptType.ANDROID,
    val clearDefaultParams: Boolean = false,
    val encryptKey: Boolean = false,
    val notSignature: Boolean = false,
    val responseType: ResponseType = ResponseType.JSON,
)

enum class HttpMethod { GET, POST }

enum class ResponseType { JSON, TEXT, BYTES }
