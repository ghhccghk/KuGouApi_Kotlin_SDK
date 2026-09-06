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

    // ────────────────────────────────────────────────────────────────
    // 新增：主题音乐
    // ────────────────────────────────────────────────────────────────

    /**
     * 获取主题音乐推荐
     * 对齐 node module/theme_music.js
     *
     * @param ids 主题分类 ID（可选）
     */
    suspend fun getThemeMusic(ids: String = ""): KuGouResponse {
        val dataMap = buildJsonObject {
            put("platform", "android")
            put("clienttime", currentTimeMillis() / 1000)
            if (ids.isNotEmpty()) put("show_theme_category_ids", ids)
            put("userid", executor.cookieJar.getUserid())
            put("module_id", 508)
        }

        return executor.execute(
            KuGouRequest(
                url = "/everydayrec.service/v1/mul_theme_category_recommend",
                method = HttpMethod.POST,
                data = dataMap,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取主题音乐详情
     * 对齐 node module/theme_music_detail.js
     *
     * @param id 主题分类 ID
     */
    suspend fun getThemeMusicDetail(id: String): KuGouResponse {
        val dataMap = buildJsonObject {
            put("platform", "android")
            put("clienttime", currentTimeMillis() / 1000)
            put("theme_category_id", id)
            put("show_theme_category_id", 0)
            put("userid", executor.cookieJar.getUserid())
            put("module_id", 508)
        }

        return executor.execute(
            KuGouRequest(
                url = "/everydayrec.service/v1/theme_category_recommend",
                method = HttpMethod.POST,
                data = dataMap,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取主题歌单所有歌曲
     * 对齐 node module/theme_playlist_track.js
     *
     * @param themeId 主题 ID
     */
    suspend fun getThemePlaylistTrack(themeId: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v2/gettheme_songidlist",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("platform", "android")
                    put("clientver", executor.config.activeClientVersion)
                    put("clienttime", currentTimeMillis())
                    put("area_code", 1)
                    put("module_id", 1)
                    put("userid", executor.cookieJar.getUserid())
                    put("theme_id", themeId)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "everydayrec.service.kugou.com")
            )
        )
    }

    /**
     * AI 推荐歌曲
     * 对齐 module/ai_recommend_song.js
     *
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 30
     */
    suspend fun getAiRecommendSong(page: Int = 1, pageSize: Int = 30): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/concepts/v1/ai/recommend_song",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("apiver", 2)
                    put("source", 2)
                    put("pagesize", pageSize)
                    put("page", page)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }
}