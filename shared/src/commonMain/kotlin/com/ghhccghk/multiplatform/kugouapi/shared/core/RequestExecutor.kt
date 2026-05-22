package com.ghhccghk.multiplatform.kugouapi.shared.core

import com.ghhccghk.multiplatform.kugouapi.shared.KuGouConfig
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class RequestExecutor internal constructor(
    private val config: KuGouConfig,
    internal val cookieJar: CookieJar,
) {
    private val signer = RequestSigner(config)
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
            connectTimeoutMillis = config.timeoutMs
        }
    }

    init {
        PlatformIdentity.initializeCookies(cookieJar, config)
    }

    suspend fun execute(request: KuGouRequest): KuGouResponse {
        val baseUrl = request.baseUrl ?: config.defaultBaseUrl
        val dfid = cookieJar.getDfid()
        val mid = cookieJar.getMid()
        val token = cookieJar.getToken()
        val userid = cookieJar.getUserid()
        val clienttime = currentTimeMillis() / 1000

        // Build default params
        val defaultParams = mutableMapOf<String, Any?>(
            "dfid" to dfid,
            "mid" to mid,
            "uuid" to "-",
            "appid" to config.activeAppId,
            "clientver" to config.activeClientVersion,
            "clienttime" to clienttime,
        )
        if (token.isNotEmpty()) defaultParams["token"] = token
        if (userid != "0") defaultParams["userid"] = userid

        // Merge params
        val params = if (request.clearDefaultParams) {
            request.params.toMutableMap()
        } else {
            (defaultParams + request.params).toMutableMap()
        }

        // Add key if encryptKey
        if (request.encryptKey && params.containsKey("hash")) {
            params["key"] = signer.signKey(
                hash = params["hash"].toString(),
                mid = mid,
                userid = userid,
                appid = config.activeAppId
            )
        }

        // Compute signature
        if (!params.containsKey("signature") && !request.notSignature) {
            val dataStr = if (request.data != null) {
                if (request.data is String) request.data
                else Json.encodeToString(JsonElement.serializer(), serializeToElement(request.data))
            } else ""
            params["signature"] = signer.computeSignature(request.encryptType, params, dataStr)
        }

        // Build headers
        val headers = mutableMapOf(
            "User-Agent" to config.userAgent,
            "dfid" to dfid,
            "clienttime" to params["clienttime"].toString(),
            "mid" to mid,
            "kg-rc" to "1",
            "kg-thash" to "5d816a0",
            "kg-rec" to "1",
            "kg-rf" to "B9EDA08A64250DEFFBCADDEE00F8F25F",
        )
        headers.putAll(request.headers)

        // Debug logging
        println("=== KuGou Debug ===")
        println("URL: $baseUrl${request.url}")
        println("Params: $params")
        println("Headers: $headers")
        println("Mid: $mid")
        println("Dfid: $dfid")
        println("===================")

        return try {
            val url = "$baseUrl${request.url}"
            val fullUrl = buildString {
                append(url)
                append("?")
                params.forEach { (key, value) ->
                    if (value != null) {
                        append("$key=$value&")
                    }
                }
            }.removeSuffix("&")
            println("Full URL: $fullUrl")

            val response = if (request.method == HttpMethod.GET) {
                client.get(baseUrl + request.url) {
                    url {
                        params.forEach { (key, value) ->
                            if (value != null) parameter(key, value.toString())
                        }
                    }
                    headers.forEach { (key, value) -> header(key, value) }
                }
            } else {
                client.post("$baseUrl${request.url}") {
                    contentType(ContentType.Application.Json)
                    headers.forEach { (key, value) -> header(key, value) }
                    if (request.data != null) {
                        setBody(request.data)
                    }
                    url {
                        params.forEach { (key, value) ->
                            if (value != null) parameter(key, value.toString())
                        }
                    }
                }
            }

            // 酷狗API返回的Content-Type是text/plain，手动读取字符串再解析JSON
            val responseText = response.bodyAsText()
            val responseBody = try {
                Json.parseToJsonElement(responseText) as? JsonObject ?: buildJsonObject {
                    put("raw", responseText)
                }
            } catch (e: Exception) {
                buildJsonObject {
                    put("raw", responseText)
                }
            }
            val responseCookies = response.headers.getAll("Set-Cookie") ?: emptyList()
            val parsedCookies = mutableMapOf<String, String>()
            responseCookies.forEach { cookie ->
                val parts = cookie.split(";")[0].split("=", limit = 2)
                if (parts.size == 2) {
                    parsedCookies[parts[0].trim()] = parts[1].trim()
                }
            }

            // Update cookie jar
            cookieJar.updateFromResponse(responseCookies)

            val responseHeaders = mutableMapOf<String, String>()
            response.headers.forEach { name, values ->
                responseHeaders[name] = values.joinToString("; ")
            }

            val status = if (responseBody["status"]?.jsonPrimitive?.intOrNull == 0) {
                502
            } else {
                200
            }

            KuGouResponse(
                status = status,
                body = responseBody,
                cookies = parsedCookies,
                headers = responseHeaders,
            )
        } catch (e: Exception) {
            KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", e.message ?: "Unknown error")
                },
                cookies = emptyMap(),
                headers = emptyMap(),
            )
        }
    }

    private fun serializeToElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> buildJsonObject {
                value.forEach { (k, v) ->
                    put(k.toString(), serializeToElement(v))
                }
            }
            is List<*> -> buildJsonArray {
                value.forEach { add(serializeToElement(it)) }
            }
            else -> JsonPrimitive(value.toString())
        }
    }

    fun close() {
        client.close()
    }
}
