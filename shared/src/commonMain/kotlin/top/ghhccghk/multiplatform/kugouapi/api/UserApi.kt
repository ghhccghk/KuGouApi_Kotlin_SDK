package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 用户相关 API (历史、收藏、信息等)
 */
class UserApi(private val executor: RequestExecutor) {

    /**
     * 获取最近播放的歌曲 (继续播放信息)
     */
    suspend fun getLatestSongsListen(pageSize: Int = 30): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                url = "/playque/devque/v1/get_latest_songs",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("area_code", "1")
                    put("sources", buildJsonArray {
                        listOf("pc", "mobile", "tv", "car").forEach { add(it) }
                    })
                    put("userid", userid)
                    put("ret_info", 1)
                    put("token", token)
                    put("pagesize", pageSize)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取歌曲被收藏的数量
     *
     * @param mixSongIds 多个用逗号分隔
     */
    suspend fun getFavoriteCount(mixSongIds: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/count/v1/audio/mget_collect",
                method = HttpMethod.GET,
                params = mapOf("mixsongids" to mixSongIds),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 上报听歌历史
     *
     * @param mxId 歌曲 MixID
     * @param time 播放时间戳 (秒)
     * @param pc 播放次数
     */
    suspend fun uploadPlayHistory(
        mxId: Long,
        time: Long = 0,
        pc: Int = 1
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()
        val timestamp = if (time == 0L) currentTimeMillis() / 1000 else time

        val song = buildJsonObject {
            put("mxid", mxId)
            put("op", 1)
            put("ot", timestamp)
            put("pc", pc)
        }

        return executor.execute(
            KuGouRequest(
                url = "/playhistory/v1/upload_songs",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("songs", buildJsonArray { add(song) })
                    put("token", token)
                    put("userid", userid)
                },
                params = mapOf("plat" to 3),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    suspend fun getUserPlaylist(
        pageSize: Int = 30,
        page: Int = 1
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                url = "/v7/get_all_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("token", token)
                    put("userid", userid)
                    put("total_ver",979)
                    put("type",2)
                    put("page",page)
                    put("pagesize",pageSize)
                },
                params = mapOf(
                    "plat" to 1,
                    "userid" to userid,
                    "token" to token),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "cloudlist.service.kugou.com")
            )
        )

    }

    suspend fun getUserDetail(): KuGouResponse {
        val dateNow = currentTimeMillis() / 1000
        val actualToken =  executor.cookieJar.getToken()
        val actualUserid = executor.cookieJar.getUserid()


        // RSA 加密密钥种子
        val pk = Crypto.rsaEncrypt(
            buildJsonObject {
                put("clienttime", dateNow)
                put("token", actualToken)
            }.toString().encodeToByteArray(),
            Crypto.activePublicRasKey(executor.config)
        )

        return executor.execute(
            KuGouRequest(
                url = "/v3/get_my_info",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("visit_time", dateNow)
                    put("userid", actualUserid)
                    put("usertype",1)
                    put("p",pk)
                },
                params = mapOf(
                    "plat" to 1
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "usercenter.kugou.com")
            )
        )
    }

    // ────────────────────────────────────────────────────────────────
    // 新增：云盘、关注、历史、VIP 等
    // ────────────────────────────────────────────────────────────────

    /**
     * 获取用户云盘歌曲列表
     * 对齐 node module/user_cloud.js
     *
     * 注意：此接口使用 AES+RSA 加密 + 自定义 baseUrl，响应为字节流需要解密。
     * 返回的 body 中 `raw` 字段为解密后的 JSON 字符串。
     */
    suspend fun getCloudList(
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()
        val mid = executor.cookieJar.getMid()
        val clienttime = currentTimeMillis() / 1000

        val dataMap = buildJsonObject {
            put("page", page)
            put("pagesize", pageSize)
            put("getkmr", 1)
        }

        // AES 加密请求数据
        val aesKey = Crypto.md5(currentTimeMillis().toString()).substring(0, 16)
        val aesIv = aesKey
        val aesEncrypted = Crypto.aesEncryptBase64(dataMap.toString(), aesKey, aesIv)

        // RSA 加密 AES 密钥
        val rsaSeed = buildJsonObject {
            put("aes", aesKey)
            put("uid", userid.toLongOrNull() ?: 0L)
            put("token", token)
        }.toString().encodeToByteArray()
        val p = Crypto.rsaEncrypt(rsaSeed, Crypto.activePublicRasKey(executor.config)).uppercase()

        val signer = RequestSigner(executor.config)

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://mcloudservice.kugou.com",
                url = "/v1/get_list",
                method = HttpMethod.POST,
                data = Crypto.decodeBase64(aesEncrypted),
                params = mapOf(
                    "clienttime" to clienttime,
                    "mid" to mid,
                    "key" to signer.signParamsKey(clienttime.toString()),
                    "clientver" to executor.config.activeClientVersion,
                    "appid" to executor.config.activeAppId,
                    "p" to p
                ),
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true,
                notSignature = true,
                responseType = ResponseType.BYTES
            )
        )
    }

    /**
     * 获取云盘音乐播放 URL
     * 对齐 node module/user_cloud_url.js
     */
    suspend fun getCloudUrl(
        hash: String,
        albumAudioId: Long = 0,
        audioId: Long = 0,
        name: String = ""
    ): KuGouResponse {
        val hashLower = hash.lowercase()
        val cloudKey = RequestSigner(executor.config).signKey(
            hash = hashLower,
            mid = "20026",
            userid = 0L,
            appid = 20026L
        )

        return executor.execute(
            KuGouRequest(
                url = "/bsstrackercdngz/v2/query_musicclound_url",
                method = HttpMethod.GET,
                params = mapOf(
                    "hash" to hashLower,
                    "ssa_flag" to "is_fromtrack",
                    "version" to "20102",
                    "ssl" to 0,
                    "album_audio_id" to albumAudioId,
                    "pid" to 20026,
                    "audio_id" to audioId,
                    "kv_id" to 2,
                    "key" to cloudKey,
                    "bucket" to "musicclound",
                    "name" to name,
                    "with_res_tag" to 0
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取用户关注的歌手列表
     * 对齐 node module/user_follow.js
     */
    suspend fun getFollowList(): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()
        val dateTime = currentTimeMillis() / 1000

        val pk = Crypto.rsaEncrypt(
            buildJsonObject {
                put("clienttime", dateTime)
                put("token", token)
            }.toString().encodeToByteArray(),
            Crypto.activePublicRasKey(executor.config)
        ).uppercase()

        return executor.execute(
            KuGouRequest(
                url = "/v4/follow_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("merge", 2)
                    put("need_iden_type", 1)
                    put("ext_params", "k_pic,jumptype,singerid,score")
                    put("userid", userid)
                    put("type", 0)
                    put("id_type", 0)
                    put("p", pk)
                },
                params = mapOf("plat" to 1),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "relationuser.kugou.com")
            )
        )
    }

    /**
     * 获取关注歌手的消息
     * 对齐 node module/user_follow_message.js
     *
     * @param id 歌手/用户 ID
     * @param pageSize 每页数量
     */
    suspend fun getFollowMessage(
        id: String,
        pageSize: Int = 30
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()

        return executor.execute(
            KuGouRequest(
                url = "/msg.mobile/v3/msgtag/history",
                method = HttpMethod.GET,
                params = mapOf(
                    "filter" to 1,
                    "maxid" to 0,
                    "pagesize" to pageSize,
                    "tag" to "chat:${userid}_${id}"
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取用户听歌历史排行
     * 对齐 node module/user_history.js
     */
    suspend fun getUserHistory(): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                url = "/playhistory/v1/get_songs",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("token", token)
                    put("userid", userid)
                    put("source_classify", "app")
                    put("to_subdivide_sr", 1)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取用户听歌排行 (listenservice)
     * 对齐 node module/user_listen.js
     *
     * @param type 列表类型，0 = 默认
     */
    suspend fun getUserListen(type: Int = 0): KuGouResponse {
        val token = executor.cookieJar.getToken()
        val userid = executor.cookieJar.getUserid()
        val clienttime = currentTimeMillis() / 1000

        val pk = Crypto.rsaEncrypt(
            buildJsonObject {
                put("clienttime", clienttime)
                put("token", token)
            }.toString().encodeToByteArray(),
            Crypto.activePublicRasKey(executor.config)
        ).uppercase()

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://listenservice.kugou.com",
                url = "/v2/get_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("t_userid", userid)
                    put("userid", userid)
                    put("list_type", type)
                    put("area_code", 1)
                    put("cover", 2)
                    put("p", pk)
                },
                params = mapOf(
                    "clienttime" to clienttime,
                    "plat" to 0
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取用户收藏的视频
     * 对齐 node module/user_video_collect.js
     */
    suspend fun getUserVideoCollect(
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                url = "/collectservice/v2/collect_list_mixvideo",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("userid", userid)
                    put("token", token)
                    put("page", page)
                    put("pagesize", pageSize)
                },
                params = mapOf("plat" to 1),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取用户喜欢的视频
     * 对齐 node module/user_video_love.js
     */
    suspend fun getUserVideoLove(
        pageSize: Int = 30
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()

        return executor.execute(
            KuGouRequest(
                url = "/m.comment.service/v1/get_user_like_video",
                method = HttpMethod.GET,
                params = mapOf(
                    "kugouid" to userid,
                    "pagesize" to pageSize,
                    "load_video_info" to 1,
                    "p" to 1,
                    "plat" to 1
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取用户 VIP 详情
     * 对齐 node module/user_vip_detail.js
     *
     * 注意：此方法与 YouthApi.getUnionVip() 调用相同端点，
     * 但参数更简化。如需完整信息请使用 YouthApi.getUnionVip()。
     */
    suspend fun getUserVipDetail(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://kugouvip.kugou.com",
                url = "/v1/get_union_vip",
                method = HttpMethod.GET,
                params = mapOf("busi_type" to "concept"),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
