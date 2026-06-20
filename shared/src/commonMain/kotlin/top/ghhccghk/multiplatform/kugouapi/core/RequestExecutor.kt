package top.ghhccghk.multiplatform.kugouapi.core

import top.ghhccghk.multiplatform.kugouapi.KuGouConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import kotlin.concurrent.Volatile

/**
 * 工业级、高并发、低内存分配的请求执行器。
 *
 * 线程安全：所有请求状态均为栈本地（stack-local）；共享的 [HttpClient] 是
 * Ktor 默认的线程安全实例。不暴露任何可变的全局状态。
 *
 * 共享的 [HttpClient] 在 [Companion] 中延迟创建，并在所有 [RequestExecutor]
 * 实例之间复用。应用退出时请调用 [shutdown] 以释放资源。
 */
class RequestExecutor internal constructor(
    internal val config: KuGouConfig,
    internal val cookieJar: CookieJar,
) {

    private val signer = RequestSigner(config)

    // ────────────────────────────────────────────────────────────────
    // Companion: 全局单例 HttpClient + JSON 配置
    // ────────────────────────────────────────────────────────────────

    companion object {

        /**
         * 用于序列化/反序列化的共享 [Json] 实例。
         *
         * `explicitNulls = false` 在编码期间省略 `null` 值，减小负载大小。
         */
        internal val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }

        /**
         * 延迟初始化的线程安全单例 [HttpClient]。
         *
         * [LazyThreadSafetyMode.SYNCHRONIZED] 在 Kotlin Common 中可用，
         * 保证在所有平台（JVM, Native, JS, WASM）上仅初始化一次，
         * 且无需使用 `synchronized` 或任何 JVM 特有的 API。
         *
         * 配置：
         * - 使用共享的 [json] 实例安装 [ContentNegotiation]
         * - 安装 [HttpTimeout]（可通过 [HttpRequestBuilder.timeout] 进行单次请求覆盖）
         * - `expectSuccess = false` 以允许手动处理状态码
         */
        private val sharedClientLazy = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            HttpClient {
                install(ContentNegotiation) {
                    json(json)
                }
                install(HttpTimeout)
                expectSuccess = false
            }
        }

        @Volatile
        private var _shutdownCalled = false

        internal fun getSharedClient(): HttpClient {
            check(!_shutdownCalled) { "RequestExecutor.shutdown() 已被调用。请创建新的 RequestExecutor 以继续。" }
            return sharedClientLazy.value
        }

        /**
         * 释放共享的 [HttpClient] 及其底层资源。
         *
         * 应用退出时调用一次即可。调用 [shutdown] 后，后续对 [getSharedClient]
         * 的调用将抛出异常。
         */
        fun shutdown() {
            _shutdownCalled = true
            if (sharedClientLazy.isInitialized()) {
                sharedClientLazy.value.close()
            }
        }

        // ── URL 辅助函数 ──────────────────────────────────────────────

        /**
         * 将基础 URL 与路径连接，正确处理前导/尾随以及重复的斜杠。
         *
         * 示例：
         * - `joinUrl("https://host", "/path")` → `https://host/path`
         * - `joinUrl("https://host/", "/path")` → `https://host/path`
         * - `joinUrl("https://host", "path")`  → `https://host/path`
         * - `joinUrl("https://host/", "//path//")` → `https://host/path/`
         */
        internal fun joinUrl(base: String, path: String): String {
            val b = base.trimEnd('/')
            val p = path.trimStart('/')
            return "$b/$p"
        }

        // ── Cookie Header 构建器 ───────────────────────────────────

        /**
         * 从给定的 [CookieJar] 构建 `Cookie` Header 值，匹配 Node.js request.js 的行为：
         *
         *     Cookie: dfid=xxx; KUGOU_API_MID=xxx; token=xxx; userid=xxx
         *
         * 为了与参考实现保持一致，空值或默认零值仍会包含在内。
         */
        internal fun buildCookieHeader(jar: CookieJar): String = buildString {
            append("dfid=")
            append(jar.getDfid())
            append("; KUGOU_API_MID=")
            append(jar.getMid())
            val token = jar.getToken()
            if (token.isNotEmpty()) {
                append("; token=")
                append(token)
            }
            val userid = jar.getUserid()
            if (userid != "0") {
                append("; userid=")
                append(userid)
            }
        }

        // ── serializeToElement (分配感知) ───────────────────

        /**
         * 将任意值转换为 [JsonElement]，避免不必要的中间分配。
         *
         * 支持的类型：
         * - [JsonElement] → 直接透传
         * - [String] / [Number] / [Boolean] → [JsonPrimitive]
         * - [Map] → [JsonObject] (递归)
         * - [Iterable] → [JsonArray] (递归)
         * - null → [JsonNull]
         * - 其他任何类型 → 调用 `toString()` 后作为 [JsonPrimitive]
         */
        internal fun serializeToElement(value: Any?): JsonElement = when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> buildJsonObject {
                value.forEach { (k, v) ->
                    if (k != null) put(k.toString(), serializeToElement(v))
                }
            }
            is Iterable<*> -> buildJsonArray {
                value.forEach { add(serializeToElement(it)) }
            }
            else -> JsonPrimitive(value.toString())
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 实例：空操作 close，生命周期由 Companion.shutdown() 管理
    // ────────────────────────────────────────────────────────────────

    init {
        PlatformIdentity.initializeCookies(cookieJar, config)
    }

    /**
     * 空操作。共享的 [HttpClient] 由 [Companion.shutdown] 管理。
     *
     * 保留此方法是为了保持 API 兼容性；它**不会**关闭底层的 HTTP 客户端。
     */
    fun close() {
        // 故意留空 — 参见 shutdown()
    }

    // ────────────────────────────────────────────────────────────────
    // 核心：执行请求
    // ────────────────────────────────────────────────────────────────

    private fun normalize(value: Any?): String = RequestSigner.normalize(value)

    /**
     * 执行给定的 [request] 并返回 [KuGouResponse]。
     *
     * 所有单个请求的对象均为栈本地。除了对线程安全的 [CookieJar] 的读取和
     * 共享的 [HttpClient] 之外，不会触碰任何共享的可变状态。
     */
    suspend fun execute(request: KuGouRequest): KuGouResponse {
        val client = getSharedClient()
        val baseUrl = request.baseUrl ?: config.defaultBaseUrl
        val url = joinUrl(baseUrl, request.url)

        // 一次性快照 Cookie 值（从基于 ConcurrentHashMap 的 Jar 中读取）
        val dfid = cookieJar.getDfid()
        val mid = cookieJar.getMid()
        val token = cookieJar.getToken()
        val userid = cookieJar.getUserid()
        val clienttime = currentTimeMillis() / 1000

        // ── 1. 构建参数 (不进行排序 — 仅在签名内部进行排序) ──

        val params = linkedMapOf<String, Any?>()
        if (!request.clearDefaultParams) {
            params["dfid"] = dfid
            params["mid"] = mid
            params["uuid"] = "-"              // 固定值，与 Node.js 保持一致
            params["appid"] = config.activeAppId
            params["clientver"] = config.activeClientVersion
            params["clienttime"] = clienttime
            if (token.isNotEmpty()) params["token"] = token
            params["userid"] = userid
        }
        params.putAll(request.params)

        // ── 2. 计算数据字符串 (用于签名) ──

        val dataStr: String = when (val data = request.data) {
            null -> ""
            is String -> data
            is ByteArray -> ""  // 二进制数据不参与字符串签名
            else -> json.encodeToString(JsonElement.serializer(), serializeToElement(data))
        }

        // ── 3. encryptKey → 添加 `key` 参数 ──

        if (request.encryptKey && !request.params.containsKey("key")) {
            val hash = params["hash"]?.toString() ?: ""
            params["key"] = signer.signKey(
                hash = hash,
                mid = mid,
                userid = userid.toLongOrNull()?:0L,
                appid = config.activeAppId.toLong()
            )
        }

        // ── 4. 签名 ──

        if (!request.params.containsKey("signature") && !request.notSignature) {
            params["signature"] = signer.computeSignature(request.encryptType, params, dataStr)
        }


        // ── 5. openapicdn URL 特殊处理 ──

        var finalUrl = url
        val finalParams = linkedMapOf<String, Any?>()
        finalParams.putAll(params)

        if (baseUrl.contains("openapicdn")) {
            val queryString = finalParams.entries.joinToString("&") { (k, v) -> "$k=${normalize(v)}" }
            finalUrl = if (finalUrl.contains("?")) "$finalUrl&$queryString" else "$finalUrl?$queryString"
            finalParams.clear()
        }

        // ── 6. 构建 Header ──

        val headers = linkedMapOf<String, String>()
        headers["User-Agent"] = config.userAgent
        headers["dfid"] = dfid
        headers["clienttime"] = clienttime.toString()
        headers["mid"] = mid
        // 用户提供的覆盖/补充
        headers.putAll(request.headers)

        println("Final Param:")
        params.forEach {
            println("${it.key}=${it.value}")
        }
        println("Final Headers:")
        headers.forEach {
            println("${it.key}=${it.value}")
        }
        // ── 7. 执行 HTTP 请求 ──

        return try {
            val response = when (request.method) {
                HttpMethod.GET -> {
                    client.get(finalUrl) {
                        applyTimeout(config.timeoutMs)
                        finalParams.forEach { (key, value) ->
                            if (value != null) parameter(key, normalize(value))
                        }
                        headers.forEach { (k, v) -> header(k, v) }
                    }
                }
                HttpMethod.POST -> {
                    client.post(finalUrl) {
                        applyTimeout(config.timeoutMs)
                        finalParams.forEach { (key, value) ->
                            if (value != null) parameter(key, normalize(value))
                        }
                        headers.forEach { (k, v) -> header(k, v) }

                        val data = request.data
                        if (data != null) {
                            when (data) {
                                is ByteArray -> {
                                    contentType(request.contentType)
                                    setBody(data)
                                }
                                is String -> {
                                    // 原始字符串 Body — 不设置 application/json
                                    setBody(data)
                                }
                                is JsonObject -> {
                                    contentType(ContentType.Application.Json)
                                    setBody(data.toString())
                                }
                                else -> {
                                    contentType(ContentType.Application.Json)
                                    setBody(data)
                                }
                            }
                        }
                    }
                }
            }

            // ── 8. 解析响应 ──

            val responseHeaders = response.headers.entries().associate { (k, v) ->
                k to v.joinToString("; ")
            }
            val responseCookies = extractCookies(response)

            val (finalStatus, responseBody) = if (request.responseType == ResponseType.BYTES) {
                val responseBytes = response.body<ByteArray>()
                // 使用 Base64 传输字节以避免低效的逗号分隔字符串
                val body = buildJsonObject {
                    put("bytes", Crypto.encodeBase64(responseBytes))
                }
                response.status.value to body
            } else {
                // 默认：bodyAsText() 避免 ByteArray → String 的双重分配
                val responseText = response.bodyAsText()
                val parsedBody = try {
                    json.parseToJsonElement(responseText) as? JsonObject
                        ?: buildJsonObject { put("raw", responseText) }
                } catch (_: Exception) {
                    buildJsonObject { put("raw", responseText) }
                }

                // Node.js 状态码映射逻辑
                val businessStatus = parsedBody["status"]?.jsonPrimitive?.intOrNull
                val errorCode = parsedBody["error_code"]?.jsonPrimitive?.intOrNull
                val mappedStatus = if (businessStatus == 0 || (errorCode != null && errorCode != 0)) {
                    502
                } else {
                    response.status.value
                }

                mappedStatus to parsedBody
            }

            KuGouResponse(
                status = finalStatus,
                body = responseBody,
                cookies = responseCookies,
                headers = responseHeaders,
            )
        } catch (e: Exception) {
            KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", e.message ?: "未知错误")
                },
                cookies = emptyMap(),
                headers = emptyMap(),
            )
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 内部辅助函数
    // ────────────────────────────────────────────────────────────────

    /**
     * 将单次请求的超时覆盖应用到 [HttpRequestBuilder]。
     *
     * Ktor 的三个超时维度均从配置值设置。
     */
    private fun HttpRequestBuilder.applyTimeout(timeoutMs: Long) {
        timeout {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
            socketTimeoutMillis = timeoutMs
        }
    }

    /**
     * 解析 `Set-Cookie` Header，更新 [cookieJar]，并返回响应信封中的
     * Cookie 名称 → 值的扁平映射。
     */
    private fun extractCookies(response: HttpResponse): Map<String, String> {
        val responseCookies = response.headers.getAll("Set-Cookie") ?: emptyList()
        if (responseCookies.isNotEmpty()) {
            cookieJar.updateFromResponse(responseCookies)
        }
        if (responseCookies.isEmpty()) return emptyMap()
        val parsed = linkedMapOf<String, String>()
        for (cookie in responseCookies) {
            val pair = cookie.split(";")[0].split("=", limit = 2)
            if (pair.size == 2) {
                parsed[pair[0].trim()] = pair[1].trim()
            }
        }
        return parsed
    }
}
