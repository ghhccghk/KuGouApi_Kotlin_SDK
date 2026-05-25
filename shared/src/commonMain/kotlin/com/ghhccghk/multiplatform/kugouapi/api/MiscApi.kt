package com.ghhccghk.multiplatform.kugouapi.api

import com.ghhccghk.multiplatform.kugouapi.core.*
import com.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 杂项 API (IP 专区、刷刷推荐、系统功能等)
 */
class MiscApi(private val executor: RequestExecutor) {

    /**
     * 刷刷推荐 (Feed)
     */
    suspend fun brush(
        mode: FmMode = FmMode.NORMAL,
        songPoolId: Int = 0
    ): KuGouResponse {
        val dateNow = currentTimeMillis()
        val userId = executor.cookieJar.getUserid()
        val vipType = executor.cookieJar.getVipType()

        val personalRecommend = buildJsonObject {
            put("userid", userId)
            put("appid", executor.config.activeAppId)
            put("playlist_ver", 2)
            put("clienttime", dateNow)
            put("mid", executor.cookieJar.getMid())
            put("new_sync_point", dateNow)
            put("module_id", 1)
            put("action", "login")
            put("vip_type", vipType)
            put("vip_flags", 3)
            put("recommend_source_locked", 0)
            put("song_pool_id", songPoolId)
            put("callerid", 0)
            put("m_type", 1)
            put("kguid", userId)
            put("platform", "ios")
            put("area_code", 1)
            put("fakem", "ca981cfc583a4c37f28d2d49000013c16a0a")
            put("clientver", 11850)
            put("mode", mode.value)
            put("active_swtich", "on")
            put("key", RequestSigner(executor.config).signParamsKey(dateNow))
        }

        val dataMap = buildJsonObject {
            put("behaviors", buildJsonArray { })
            put("abtest", buildJsonObject {
                put("abtest", buildJsonObject {
                    put("shuashua", buildJsonObject { put("commentcard", 2) })
                })
            })
            put("personal_recommend_params", personalRecommend)
        }

        return executor.execute(
            KuGouRequest(
                url = "/genesisapi/v1/newepoch_song_rec/feed",
                method = HttpMethod.POST,
                data = dataMap,
                params = mapOf(
                    "sort_type" to 1,
                    "platform" to "ios",
                    "page" to 1,
                    "content_ver" to 4,
                    "clientver" to 11850
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 根据 IP ID 获取相关数据
     */
    suspend fun getIpData(
        id: String,
        type: IpDataType = IpDataType.AUDIOS,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/openapi/v1/ip/${type.value}",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("is_publish", 1)
                    put("ip_id", id)
                    put("sort", 3)
                    put("page", page)
                    put("pagesize", pageSize)
                    put("query", 1)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取 IP 详情
     */
    suspend fun getIpDetail(ids: String): KuGouResponse {
        val data = buildJsonArray {
            ids.split(",").filter { it.isNotEmpty() }.forEach { id ->
                addJsonObject { put("ip_id", id) }
            }
        }
        return executor.execute(
            KuGouRequest(
                url = "/openapi/v1/ip",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("data", data)
                    put("is_publish", 1)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取 IP 歌单
     */
    suspend fun getIpPlaylist(id: String, page: Int = 1, pageSize: Int = 30): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/ocean/v6/pubsongs/list_info_for_ip",
                method = HttpMethod.POST,
                params = mapOf(
                    "ip" to id,
                    "page" to page,
                    "pagesize" to pageSize
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * IP 专区列表
     */
    suspend fun getIpZone(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v1/zone/index",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "yuekucategory.kugou.com")
            )
        )
    }

    /**
     * IP 专区主页 (今日推荐)
     */
    suspend fun getIpZoneHome(id: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v1/zone/home",
                method = HttpMethod.GET,
                params = mapOf("id" to id, "share" to 0),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "yuekucategory.kugou.com")
            )
        )
    }
}
