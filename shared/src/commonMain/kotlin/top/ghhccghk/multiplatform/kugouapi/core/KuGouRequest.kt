package top.ghhccghk.multiplatform.kugouapi.core

import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import io.ktor.http.ContentType

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
    val contentType: ContentType = ContentType.Application.Json,
)

enum class HttpMethod { GET, POST }

enum class ResponseType { JSON, TEXT, BYTES }
