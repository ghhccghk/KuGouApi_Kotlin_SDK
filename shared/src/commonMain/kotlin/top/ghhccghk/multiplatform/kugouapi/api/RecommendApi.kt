package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 推荐相关 API
 */
class RecommendApi(private val executor: RequestExecutor) {

    /**
     * 获取每日歌曲推荐
     */
    suspend fun getDailyRecommend(platform: String = "ios"): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/everyday_song_recommend",
                method = HttpMethod.POST,
                params = mapOf("platform" to platform),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "everydayrec.service.kugou.com")
            )
        )
    }

    /**
     * 获取风格推荐
     */
    suspend fun getStyleRecommend(tagIds: String = "", platform: String = "ios"): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/everydayrec.service/everyday_style_recommend",
                method = HttpMethod.POST,
                params = mapOf("tagids" to tagIds),
                data = buildJsonObject { },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取每日历史推荐
     */
    suspend fun getHistory(
        mode: String = "list",
        platform: String = "ios",
        historyName: String = "",
        date: String = ""
    ): KuGouResponse {
        val params = mutableMapOf<String, Any?>(
            "mode" to mode,
            "platform" to platform
        )
        if (historyName.isNotEmpty()) params["history_name"] = historyName
        if (date.isNotEmpty()) params["date"] = date

        return executor.execute(
            KuGouRequest(
                url = "/everyday/api/v1/get_history",
                method = HttpMethod.POST,
                params = params,
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "everydayrec.service.kugou.com")
            )
        )
    }

    /**
     * 发现好友推荐
     */
    suspend fun getFriendRecommend(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://acsing.service.kugou.com",
                url = "/sing7/relation/json/v3/friend_rec_by_using_song_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("list", buildJsonArray {
                        addJsonObject {
                            put("user_id", 853927886)
                            put("mixsong_ids", buildJsonArray {
                                listOf(290083753, 251724346, 571554587, 250126644, 208831644).forEach { add(it) }
                            })
                        }
                    })
                },
                params = mapOf(
                    "channel" to 130,
                    "isteen" to 0,
                    "platform" to 2,
                    "usemkv" to 1
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("pid" to "126556797")
            )
        )
    }

    /**
     * 每日推荐歌曲 (Alternative)
     */
    suspend fun getRecommendSongs(platform: String = "android"): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/everyday_song_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("platform", platform)
                    put("userid", executor.cookieJar.getUserid())
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "everydayrec.service.kugou.com")
            )
        )
    }

    /**
     * 私人 FM 推荐
     */
    suspend fun getPersonalFm(
        action: FmAction = FmAction.PLAY,
        mode: FmMode = FmMode.NORMAL,
        songPoolId: Int = 0,
        remainSongCount: Int = 0,
        isOverplay: Boolean = false,
        hash: String = "",
        songId: String = "",
        playtime: String = "",
        platform: String = "ios"
    ): KuGouResponse {
        val dateNow = currentTimeMillis()
        val data = buildJsonObject {
            put("appid", executor.config.activeAppId)
            put("clienttime", dateNow)
            put("mid", executor.cookieJar.getMid())
            put("action", action.value)
            put("recommend_source_locked", 0)
            put("song_pool_id", songPoolId)
            put("callerid", 0)
            put("m_type", 1)
            put("platform", platform)
            put("area_code", 1)
            put("remain_songcnt", remainSongCount)
            put("clientver", executor.config.activeClientVersion)
            put("is_overplay", if (isOverplay) 1 else 0)
            put("mode", mode.value)
            put("fakem", "ca981cfc583a4c37f28d2d49000013c16a0a")
            put("key", RequestSigner(executor.config).signParamsKey(dateNow))
            
            val userId = executor.cookieJar.getUserid()
            if (userId != "0") {
                put("userid", userId)
                put("kguid", userId)
            }
            val token = executor.cookieJar.getToken()
            if (token.isNotEmpty()) put("token", token)
            val vipType = executor.cookieJar.getVipType()
            if (vipType != "0") put("vip_type", vipType)
            
            if (hash.isNotEmpty()) put("hash", hash)
            if (songId.isNotEmpty()) put("songid", songId)
            if (playtime.isNotEmpty()) put("playtime", playtime)
        }

        return executor.execute(
            KuGouRequest(
                url = "/v2/personal_recommend",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "persnfm.service.kugou.com")
            )
        )
    }

    /**
     * AI 智能推荐
     */
    suspend fun getAiRecommend(albumAudioIds: String): KuGouResponse {
        val dateNow = currentTimeMillis()
        val ids = albumAudioIds.split(",").filter { it.isNotEmpty() }
        val recommendSource = buildJsonArray {
            ids.forEach { id ->
                addJsonObject { put("ID", id.toLongOrNull() ?: 0L) }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/recommend",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("platform", "ios")
                    put("clientver", executor.config.activeClientVersion)
                    put("clienttime", dateNow)
                    put("userid", executor.cookieJar.getUserid())
                    put("client_playlist", buildJsonArray { })
                    put("source_type", 2)
                    put("playlist_ver", 2)
                    put("area_code", 1)
                    put("appid", executor.config.activeAppId)
                    put("key", RequestSigner(executor.config).signParamsKey(dateNow.toString()))
                    put("mid", executor.cookieJar.getMid())
                    put("recommend_source", recommendSource)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "songlistairec.kugou.com"),
                clearDefaultParams = true
            )
        )
    }
}
