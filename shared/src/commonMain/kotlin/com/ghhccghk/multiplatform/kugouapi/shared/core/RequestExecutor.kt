package com.ghhccghk.multiplatform.kugouapi.shared.core

import com.ghhccghk.multiplatform.kugouapi.shared.KuGouConfig
import com.ghhccghk.multiplatform.kugouapi.shared.model.EncryptType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class RequestExecutor internal constructor(
    internal val config: KuGouConfig,
    internal val cookieJar: CookieJar,
) {

    private val signer = RequestSigner(config)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
            connectTimeoutMillis = config.timeoutMs
        }
        expectSuccess = false
    }

    init {
        PlatformIdentity.initializeCookies(cookieJar, config)
    }

    private fun normalize(value: Any?): String = RequestSigner.normalize(value)

    suspend fun execute(request: KuGouRequest): KuGouResponse {
        val baseUrl = request.baseUrl ?: config.defaultBaseUrl
        val dfid = cookieJar.getDfid()
        val mid = cookieJar.getMid()
        val token = cookieJar.getToken()
        val userid = cookieJar.getUserid()
        val clienttime = currentTimeMillis() / 1000

        // 1. 构建 params
        val params = linkedMapOf<String, Any?>()
        if (!request.clearDefaultParams) {
            params["dfid"] = dfid
            params["mid"] = mid
            params["uuid"] = mid
            params["appid"] = config.activeAppId
            params["clientver"] = config.activeClientVersion
            params["clienttime"] = clienttime
            if (token.isNotEmpty()) params["token"] = token
            if (userid != "0") params["userid"] = userid
        }
        params.putAll(request.params)

        // 2. 签名计算
        if (!params.containsKey("signature") && !request.notSignature) {
            val dataStr = when (val data = request.data) {
                null -> ""
                is String -> data
                else -> json.encodeToString(JsonElement.serializer(), serializeToElement(data))
            }
            params["signature"] = signer.computeSignature(request.encryptType, params, dataStr)
        }

        // 3. 构建 Headers
        val headers = linkedMapOf(
            "User-Agent" to config.userAgent,
            "dfid" to dfid,
            "clienttime" to clienttime.toString(),
            "mid" to mid,
            "kg-rc" to "1",
            "kg-thash" to "5d816a0",
            "kg-rec" to "1",
            "kg-rf" to "B9EDA08A64250DEFFBCADDEE00F8F25F",
        )
        headers.putAll(request.headers)

        println("[Search URL] ${baseUrl + request.url}?${params.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" }}")

        return try {
            val sortedKeys = params.keys.sorted()
            
            val response = when (request.method) {
                HttpMethod.GET -> {
                    client.get(baseUrl + request.url) {
                        url {
                            for (key in sortedKeys) {
                                val value = params[key]
                                if (value != null) parameter(key, normalize(value))
                            }
                        }
                        headers.forEach { (k, v) -> header(k, v) }
                    }
                }
                else -> {
                    client.post(baseUrl + request.url) {
                        // 将 body 统一转为可序列化格式
                        val bodyData = when (val data = request.data) {
                            null -> null
                            is String -> {
                                // String body 不设 application/json
                                data
                            }
                            is JsonObject -> {
                                contentType(ContentType.Application.Json)
                                data.toString() // 转为 JSON 字符串
                            }
                            else -> {
                                contentType(ContentType.Application.Json)
                                data
                            }
                        }
                        headers.forEach { (k, v) -> header(k, v) }
                        bodyData?.let { setBody(it) }
                        url {
                            for (key in sortedKeys) {
                                val value = params[key]
                                if (value != null) parameter(key, normalize(value))
                            }
                        }
                    }
                }
            }

            // 使用 body<ByteArray>() 获取原始字节
            val responseBytes = response.body<ByteArray>()

            if (request.responseType == ResponseType.BYTES) {
                return KuGouResponse(
                    status = response.status.value,
                    body = buildJsonObject { 
                        put("bytes", responseBytes.joinToString(",") { it.toString() })
                    },
                    cookies = extractCookies(response),
                    headers = response.headers.entries().associate { it.key to it.value.joinToString("; ") }
                )
            }

            val responseText = responseBytes.decodeToString()
            val responseBody = try {
                json.parseToJsonElement(responseText) as? JsonObject
                    ?: buildJsonObject { put("raw", responseText) }
            } catch (_: Exception) {
                buildJsonObject { put("raw", responseText) }
            }

            KuGouResponse(
                status = response.status.value,
                body = responseBody,
                cookies = extractCookies(response),
                headers = response.headers.entries().associate { it.key to it.value.joinToString("; ") }
            )
        } catch (e: Exception) {
            KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", e.message ?: "Unknown error")
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        }
    }

    private fun extractCookies(response: HttpResponse): Map<String, String> {
        val responseCookies = response.headers.getAll("Set-Cookie") ?: emptyList()
        cookieJar.updateFromResponse(responseCookies)
        val parsedCookies = mutableMapOf<String, String>()
        responseCookies.forEach { cookie ->
            val parts = cookie.split(";")[0].split("=", limit = 2)
            if (parts.size == 2) parsedCookies[parts[0].trim()] = parts[1].trim()
        }
        return parsedCookies
    }

    private fun serializeToElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> buildJsonObject {
            value.forEach { (k, v) -> put(k.toString(), serializeToElement(v)) }
        }
        is Iterable<*> -> buildJsonArray {
            value.forEach { add(serializeToElement(it)) }
        }
        else -> JsonPrimitive(value.toString())
    }

    fun close() {
        client.close()
    }
}
