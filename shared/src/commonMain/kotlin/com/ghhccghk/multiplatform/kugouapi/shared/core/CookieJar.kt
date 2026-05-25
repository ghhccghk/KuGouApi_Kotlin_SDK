package com.ghhccghk.multiplatform.kugouapi.shared.core

import com.ghhccghk.multiplatform.kugouapi.shared.KuGouConfig

class CookieJar(private val config: KuGouConfig) {
    private val _cookies = mutableMapOf<String, String>()

    val cookies: Map<String, String> get() = _cookies.toMap()

    init {
        _cookies["KUGOU_API_PLATFORM"] = if (config.isLite) "lite" else "standard"
    }

    operator fun get(key: String): String? = _cookies[key]

    operator fun set(key: String, value: String) {
        _cookies[key] = value
    }

    fun getAll(): Map<String, String> = _cookies.toMap()

    fun updateFromResponse(cookies: List<String>) {
        for (cookie in cookies) {
            val parts = cookie.split(";")[0].split("=", limit = 2)
            if (parts.size == 2) {
                _cookies[parts[0].trim()] = parts[1].trim()
            }
        }
    }

    fun getToken(): String = _cookies["token"] ?: ""
        fun getUserid(): String = _cookies["userid"] ?: "0"
    fun getDfid(): String = _cookies["dfid"] ?: "-"
    fun getMid(): String = _cookies["KUGOU_API_MID"] ?: ""
    fun getGuid(): String = _cookies["KUGOU_API_GUID"] ?: ""
    fun getDev(): String = _cookies["KUGOU_API_DEV"] ?: ""
    fun getVipType(): String = _cookies["vip_type"] ?: "0"
    fun getVipToken(): String = _cookies["vip_token"] ?: ""

    fun isLoggedIn(): Boolean = getToken().isNotEmpty() && getUserid() != "0"

    fun dump() {
        println("=== CookieJar Contents ===")
        _cookies.forEach { (k, v) ->
            println("  $k = $v")
        }
        println("==========================")
    }
}
