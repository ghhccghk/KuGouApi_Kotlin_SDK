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
 * Industrial-grade, high-concurrency, low-allocation request executor.
 *
 * Thread-safe: all per-request state is stack-local; the shared [HttpClient] is
 * Ktor's default thread-safe instance. No mutable global state is exposed.
 *
 * The shared [HttpClient] is created lazily in [Companion] and reused across all
 * [RequestExecutor] instances. Call [shutdown] at application exit to release it.
 */
class RequestExecutor internal constructor(
    internal val config: KuGouConfig,
    internal val cookieJar: CookieJar,
) {

    private val signer = RequestSigner(config)

    // ────────────────────────────────────────────────────────────────
    // Companion: global singleton HttpClient + JSON config
    // ────────────────────────────────────────────────────────────────

    companion object {

        /**
         * Shared [Json] instance used for serialization / deserialization.
         *
         * `explicitNulls = false` omits `null` values during encoding, reducing payload size.
         */
        internal val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }

        /**
         * Lazy, thread-safe singleton [HttpClient].
         *
         * [LazyThreadSafetyMode.SYNCHRONIZED] is available in Kotlin common
         * and guarantees exactly-once initialization across all platforms
         * (JVM, Native, JS, WASM) without using `synchronized` or any
         * JVM-specific API.
         *
         * Configuration:
         * - [ContentNegotiation] with the shared [json] instance
         * - [HttpTimeout] installed (per-request overrides via [HttpRequestBuilder.timeout])
         * - `expectSuccess = false` to allow manual status handling
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
            check(!_shutdownCalled) { "RequestExecutor.shutdown() has been called. Create a new RequestExecutor to continue." }
            return sharedClientLazy.value
        }

        /**
         * Releases the shared [HttpClient] and its underlying resources.
         *
         * Call this once at application exit. After calling [shutdown], subsequent
         * calls to [getSharedClient] will create a fresh instance.
         */
        fun shutdown() {
            _shutdownCalled = true
            if (sharedClientLazy.isInitialized()) {
                sharedClientLazy.value.close()
            }
        }

        // ── URL helper ──────────────────────────────────────────────

        /**
         * Joins a base URL with a path, correctly handling leading / trailing
         * and duplicate slashes.
         *
         * Examples:
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

        // ── Cookie header builder ───────────────────────────────────

        /**
         * Builds a `Cookie` header value from the given [CookieJar], matching
         * Node.js request.js behavior:
         *
         *     Cookie: dfid=xxx; KUGOU_API_MID=xxx; token=xxx; userid=xxx
         *
         * Empty / default-zero values are still included for consistency with
         * the reference implementation.
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

        // ── serializeToElement (allocation-aware) ───────────────────

        /**
         * Converts an arbitrary value to a [JsonElement] without unnecessary
         * intermediate allocations.
         *
         * Supported types:
         * - [JsonElement] → pass-through
         * - [String] / [Number] / [Boolean] → [JsonPrimitive]
         * - [Map] → [JsonObject] (recursive)
         * - [Iterable] → [JsonArray] (recursive)
         * - null → [JsonNull]
         * - anything else → `toString()` as [JsonPrimitive]
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
    // Instance: no-op close, lifecycle managed by Companion.shutdown()
    // ────────────────────────────────────────────────────────────────

    init {
        PlatformIdentity.initializeCookies(cookieJar, config)
    }

    /**
     * No-op. The shared [HttpClient] is managed by [Companion.shutdown].
     *
     * Keeping this method for API compatibility; it does **not** close the
     * underlying HTTP client.
     */
    fun close() {
        // intentionally empty — see shutdown()
    }

    // ────────────────────────────────────────────────────────────────
    // Core: execute
    // ────────────────────────────────────────────────────────────────

    private fun normalize(value: Any?): String = RequestSigner.normalize(value)

    /**
     * Executes the given [request] and returns a [KuGouResponse].
     *
     * All per-request objects are stack-local. No shared mutable state is touched
     * outside the thread-safe [CookieJar] reads and the shared [HttpClient].
     */
    suspend fun execute(request: KuGouRequest): KuGouResponse {
        val client = getSharedClient()
        val baseUrl = request.baseUrl ?: config.defaultBaseUrl
        val url = joinUrl(baseUrl, request.url)

        // Snapshot cookie values once (reads from a ConcurrentHashMap-backed jar)
        val dfid = cookieJar.getDfid()
        val mid = cookieJar.getMid()
        val token = cookieJar.getToken()
        val userid = cookieJar.getUserid()
        val clienttime = currentTimeMillis() / 1000

        // ── 1. Build params (no sorting — only signature internals sort) ──

        val params = linkedMapOf<String, Any?>()
        if (!request.clearDefaultParams) {
            params["dfid"] = dfid
            params["mid"] = mid
            params["uuid"] = "-"              // fixed, aligns with Node.js
            params["appid"] = config.activeAppId
            params["clientver"] = config.activeClientVersion
            params["clienttime"] = clienttime
            if (token.isNotEmpty()) params["token"] = token
            if (userid != "0") params["userid"] = userid
        }
        params.putAll(request.params)

        // ── 2. Compute data string (for signature) ──

        val dataStr: String = when (val data = request.data) {
            null -> ""
            is String -> data
            else -> json.encodeToString(JsonElement.serializer(), serializeToElement(data))
        }

        // ── 3. Signature ──

        if (!params.containsKey("signature") && !request.notSignature) {
            params["signature"] = signer.computeSignature(request.encryptType, params, dataStr)
        }

        // ── 4. encryptKey → add `key` param ──

        if (request.encryptKey && !params.containsKey("key")) {
            params["key"] = signer.signParamsKey(dataStr)
        }

        // ── 5. Build headers ──

        val headers = linkedMapOf<String, String>()
        headers["User-Agent"] = config.userAgent
        headers["Accept"] = "application/json, text/plain, */*"
        headers["dfid"] = dfid
        headers["clienttime"] = clienttime.toString()
        headers["mid"] = mid
        headers["kg-rc"] = "1"
        headers["kg-thash"] = "5d816a0"
        headers["kg-rec"] = "1"
        headers["kg-rf"] = "B9EDA08A64250DEFFBCADDEE00F8F25F"
        // Cookie header
        headers["Cookie"] = buildCookieHeader(cookieJar)
        // User-provided overrides / additions
        headers.putAll(request.headers)

        // ── 6. Execute HTTP request ──

        return try {
            val response = when (request.method) {
                HttpMethod.GET -> {
                    client.get(url) {
                        applyTimeout(config.timeoutMs)
                        params.forEach { (key, value) ->
                            if (value != null) parameter(key, normalize(value))
                        }
                        headers.forEach { (k, v) -> header(k, v) }
                    }
                }
                HttpMethod.POST -> {
                    client.post(url) {
                        applyTimeout(config.timeoutMs)
                        params.forEach { (key, value) ->
                            if (value != null) parameter(key, normalize(value))
                        }
                        headers.forEach { (k, v) -> header(k, v) }

                        val data = request.data
                        if (data != null) {
                            when (data) {
                                is String -> {
                                    // Raw string body — do not set application/json
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

            // ── 7. Parse response ──

            val responseHeaders = response.headers.entries().associate { (k, v) ->
                k to v.joinToString("; ")
            }
            val responseCookies = extractCookies(response)

            if (request.responseType == ResponseType.BYTES) {
                val responseBytes = response.body<ByteArray>()
                KuGouResponse(
                    status = response.status.value,
                    body = buildJsonObject {
                        put("bytes", responseBytes.joinToString(",") { it.toString() })
                    },
                    cookies = responseCookies,
                    headers = responseHeaders,
                )
            } else {
                // Default: bodyAsText() avoids ByteArray → String double allocation
                val responseText = response.bodyAsText()
                val responseBody = try {
                    json.parseToJsonElement(responseText) as? JsonObject
                        ?: buildJsonObject { put("raw", responseText) }
                } catch (_: Exception) {
                    buildJsonObject { put("raw", responseText) }
                }
                KuGouResponse(
                    status = response.status.value,
                    body = responseBody,
                    cookies = responseCookies,
                    headers = responseHeaders,
                )
            }
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

    // ────────────────────────────────────────────────────────────────
    // Internal helpers
    // ────────────────────────────────────────────────────────────────

    /**
     * Applies per-request timeout overrides to the [HttpRequestBuilder].
     *
     * All three Ktor timeout dimensions are set from the config value.
     */
    private fun HttpRequestBuilder.applyTimeout(timeoutMs: Long) {
        timeout {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
            socketTimeoutMillis = timeoutMs
        }
    }

    /**
     * Parses `Set-Cookie` headers, updates the [cookieJar], and returns a
     * flat map of cookie name → value for the response envelope.
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
