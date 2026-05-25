package com.ghhccghk.multiplatform.kugouapi

import com.ghhccghk.multiplatform.kugouapi.api.AuthApi
import com.ghhccghk.multiplatform.kugouapi.api.SearchApi
import com.ghhccghk.multiplatform.kugouapi.core.CookieJar
import com.ghhccghk.multiplatform.kugouapi.core.RequestExecutor

/**
 * KuGou Music API Kotlin Multiplatform SDK
 *
 * Usage:
 * ```
 * val client = KuGouClient()
 * val result = client.search.search("周杰伦")
 * println(result.body)
 * ```
 */
class KuGouClient(
    val config: KuGouConfig = KuGouConfig(),
    val cookieJar : CookieJar = CookieJar(config)

) {

    private val executor =
        RequestExecutor(config, cookieJar)

    val auth = AuthApi(executor)
    val search = SearchApi(executor)
}