package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 黑名单相关 API
 * 提供歌曲/歌手黑名单查询功能。
 * 对齐 module/blacklist_list.js
 */
class BlacklistApi(private val executor: RequestExecutor) {

    /**
     * 获取黑名单列表
     *
     * @param label 黑名单类型：song（歌曲）或 singer（歌手）
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 30，最大 500
     * @param moduleId 模块ID，默认 473（黑名单管理）
     */
    suspend fun getBlacklist(
        label: String = "song",
        page: Int = 1,
        pageSize: Int = 30,
        moduleId: Int = 473
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()
        val clientTime = currentTimeMillis() / 1000

        val source = if (label == "singer") 4 else 3
        val actualPageSize = pageSize.coerceAtMost(500)

        // RSA 加密
        val rsaInput = buildJsonObject {
            put("clienttime", clientTime)
            put("token", token)
        }.toString()
        val p = Crypto.rsaEncrypt(rsaInput.encodeToByteArray(), Crypto.activePublicRasKey(executor.config))

        val data = buildJsonObject {
            put("userid", userid)
            put("source", source)
            put("label", label)
            put("p", p)
            put("page", page)
            put("pagesize", actualPageSize)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://relationuser.kugou.com",
                url = "/v2/get_list_items",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID,
                headers = mapOf("KG-TID" to moduleId.toString())
            )
        )
    }

    /**
     * 获取歌曲黑名单
     */
    suspend fun getSongBlacklist(
        page: Int = 1,
        pageSize: Int = 30,
        moduleId: Int = 473
    ): KuGouResponse {
        return getBlacklist("song", page, pageSize, moduleId)
    }

    /**
     * 获取歌手黑名单
     */
    suspend fun getSingerBlacklist(
        page: Int = 1,
        pageSize: Int = 30,
        moduleId: Int = 473
    ): KuGouResponse {
        return getBlacklist("singer", page, pageSize, moduleId)
    }
}
