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

    /**
     * 编辑黑名单（添加/移除）
     * 对齐 module/blacklist.js
     *
     * @param label 黑名单类型：song（歌曲）或 singer（歌手）
     * @param action 操作：add（添加）或 delete（移除）
     * @param items 黑名单项列表，每项包含 k（标识）和 v（JSON字符串）
     * @param moduleId 模块ID，默认 473（黑名单管理）
     */
    suspend fun editBlacklist(
        label: String = "song",
        action: String = "add",
        items: List<BlacklistItem>,
        moduleId: Int = 473
    ): KuGouResponse {
        val token = executor.cookieJar.getToken()
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val clientTime = currentTimeMillis() / 1000
        val source = if (label == "singer") 4 else 3

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
            putJsonArray("items") {
                items.forEach { item ->
                    addJsonObject {
                        put("k", item.k)
                        put("v", item.v)
                    }
                }
            }
            put("action", action)
            put("p", p)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://relationuser.kugou.com",
                url = "/v1/edit_list_items",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID,
                headers = mapOf("KG-TID" to moduleId.toString())
            )
        )
    }

    /**
     * 添加歌曲到黑名单
     *
     * @param hash 歌曲 FileHash（小写）
     * @param name 歌曲名称（格式："歌手 - 歌名"）
     * @param mixsongid 歌曲 MixSongID
     * @param moduleId 模块ID
     */
    suspend fun addSongToBlacklist(
        hash: String,
        name: String = "",
        mixsongid: String = "",
        moduleId: Int = 473
    ): KuGouResponse {
        val timestamp = (currentTimeMillis() / 1000).toString()
        val v = buildJsonObject {
            put("n", name)
            put("m", mixsongid)
            put("t", timestamp)
        }.toString()

        return editBlacklist(
            label = "song",
            action = "add",
            items = listOf(BlacklistItem(k = hash.lowercase(), v = v)),
            moduleId = moduleId
        )
    }

    /**
     * 从黑名单移除歌曲
     *
     * @param hash 歌曲 FileHash（小写）
     * @param name 歌曲名称
     * @param mixsongid 歌曲 MixSongID
     * @param moduleId 模块ID
     */
    suspend fun removeSongFromBlacklist(
        hash: String,
        name: String = "",
        mixsongid: String = "",
        moduleId: Int = 473
    ): KuGouResponse {
        val timestamp = (currentTimeMillis() / 1000).toString()
        val v = buildJsonObject {
            put("n", name)
            put("m", mixsongid)
            put("t", timestamp)
        }.toString()

        return editBlacklist(
            label = "song",
            action = "delete",
            items = listOf(BlacklistItem(k = hash.lowercase(), v = v)),
            moduleId = moduleId
        )
    }

    /**
     * 添加歌手到黑名单
     *
     * @param singerId 歌手 ID
     * @param name 歌手名称
     * @param moduleId 模块ID
     */
    suspend fun addSingerToBlacklist(
        singerId: String,
        name: String = "",
        moduleId: Int = 473
    ): KuGouResponse {
        val timestamp = (currentTimeMillis() / 1000).toString()
        val v = buildJsonObject {
            put("n", name)
            put("t", timestamp)
        }.toString()

        return editBlacklist(
            label = "singer",
            action = "add",
            items = listOf(BlacklistItem(k = singerId, v = v)),
            moduleId = moduleId
        )
    }

    /**
     * 从黑名单移除歌手
     *
     * @param singerId 歌手 ID
     * @param name 歌手名称
     * @param moduleId 模块ID
     */
    suspend fun removeSingerFromBlacklist(
        singerId: String,
        name: String = "",
        moduleId: Int = 473
    ): KuGouResponse {
        val timestamp = (currentTimeMillis() / 1000).toString()
        val v = buildJsonObject {
            put("n", name)
            put("t", timestamp)
        }.toString()

        return editBlacklist(
            label = "singer",
            action = "delete",
            items = listOf(BlacklistItem(k = singerId, v = v)),
            moduleId = moduleId
        )
    }
}

/**
 * 黑名单项数据类
 */
data class BlacklistItem(
    val k: String,
    val v: String
)