package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * Top 推荐相关 API
 *
 * 对齐 Node.js `top_*` 系列模块，提供新歌速递、精选卡片、
 * 推荐专辑、精选歌单等功能。
 */
class TopApi(private val executor: RequestExecutor) {

    /**
     * 推荐新专辑
     * 对齐 node module/top_album.js
     *
     * @param page 页码
     * @param pageSize 每页数量
     */
    suspend fun getTopAlbum(
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/musicadservice/v1/mobile_newalbum_sp",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("apiver", 20)
                    put("token", executor.cookieJar.getToken())
                    put("page", page)
                    put("pagesize", pageSize)
                    put("withpriv", 1)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * IP 推荐/今日推荐
     * 对齐 node module/top_ip.js
     */
    suspend fun getTopIp(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "http://musicadservice.kugou.com",
                url = "/v1/daily_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    putJsonObject("tags") {}
                },
                params = mapOf(
                    "clientver" to 12349,
                    "area_code" to 1
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 精选歌单
     * 对齐 node module/top_playlist.js
     *
     * @param categoryId 分类 ID，0=推荐，11292=HI-RES
     * @param page 页码
     * @param pageSize 每页数量
     */
    suspend fun getTopPlaylist(
        categoryId: Int = 0,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val dateTime = currentTimeMillis()
        val signer = RequestSigner(executor.config)
        val userid = executor.cookieJar.getUserid()
        val mid = executor.cookieJar.getMid()

        val specialRecommend = buildJsonObject {
            put("withtag", 1)
            put("withsong", 1)
            put("sort", 1)
            put("ugc", 1)
            put("is_selected", 0)
            put("withrecommend", 1)
            put("area_code", 1)
            put("categoryid", categoryId)
        }

        return executor.execute(
            KuGouRequest(
                url = "/v2/special_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("mid", mid)
                    put("clientver", executor.config.activeClientVersion)
                    put("platform", "android")
                    put("clienttime", dateTime)
                    put("userid", userid)
                    put("module_id", 1)
                    put("page", page)
                    put("pagesize", pageSize)
                    put("key", signer.signParamsKey(dateTime.toString()))
                    put("special_recommend", specialRecommend)
                    put("req_multi", 1)
                    put("retrun_min", 5)
                    put("return_special_falg", 1)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "specialrec.service.kugou.com")
            )
        )
    }

    /**
     * 新歌速递
     * 对齐 node module/top_song.js
     *
     * @param rankId 排行榜 ID，21608=默认
     * @param page 页码
     * @param pageSize 每页数量
     */
    suspend fun getTopSong(
        rankId: Int = 21608,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/musicadservice/container/v1/newsong_publish",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("rank_id", rankId)
                    put("userid", executor.cookieJar.getUserid())
                    put("page", page)
                    put("pagesize", pageSize)
                    putJsonArray("tags") {}
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 热门好歌精选卡片推荐
     * 对齐 node module/top_card.js
     *
     * card_id 对应关系：
     * - 1: 精选好歌随心听 / 私人专属好歌
     * - 2: 经典怀旧金曲
     * - 3: 热门好歌精选
     * - 4: 小众宝藏佳作
     * - 6: VIP 专属推荐
     *
     * @param cardId 卡片 ID
     */
    suspend fun getTopCard(cardId: Int = 1): KuGouResponse {
        val dateTime = currentTimeMillis()
        val signer = RequestSigner(executor.config)
        val mid = executor.cookieJar.getMid()

        return executor.execute(
            KuGouRequest(
                url = "/singlecardrec.service/v1/single_card_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clientver", executor.config.activeClientVersion)
                    put("platform", "android")
                    put("clienttime", dateTime)
                    put("userid", executor.cookieJar.getUserid())
                    put("key", signer.signParamsKey(dateTime))
                    put("fakem", "60f7ebf1f812edbac3c63a7310001701760f")
                    put("area_code", 1)
                    put("mid", mid)
                    put("uuid", "-")
                    putJsonArray("client_playlist") {}
                    put("u_info", "a0c35cd40af564444b5584c2754dedec")
                },
                params = mapOf(
                    "card_id" to cardId,
                    "fakem" to "60f7ebf1f812edbac3c63a7310001701760f",
                    "area_code" to 1,
                    "platform" to "ios"
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 青年版热门好歌精选
     * 对齐 node module/top_card_youth.js
     *
     * card_id 对应关系：
     * - 3001: 私人专属好歌
     * - 3004: 小众宝藏佳作
     * - 3005: 潮流尝鲜
     * - 3006: VIP 专属推荐
     * - 3014: 喜欢这首歌的 TA 也喜欢
     * - 3101: 概念 er 新推
     * - 3102: 喜欢「欧美」的 TA 也喜欢
     *
     * @param cardId 卡片 ID
     * @param pageSize 每页数量
     */
    suspend fun getTopCardYouth(
        cardId: Int = 3005,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v1/song/single_card_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("tagid", "")
                    put("u_info", "")
                    put("source_mixsong", "")
                },
                params = mapOf(
                    "card_id" to cardId,
                    "area_code" to 1,
                    "platform" to "ios",
                    "module_id" to 1,
                    "ver" to "v2",
                    "pagesize" to pageSize,
                    "clientver" to 11490
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 曲风盲盒 · 随机心动
     * 对齐 node module/top_tag_card_youth.js
     */
    suspend fun getTopTagCardYouth(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v1/song/tag_card_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("tagid", "")
                    put("u_info", "")
                    put("source_mixsong", "")
                },
                params = mapOf(
                    "ver" to "v2",
                    "area_code" to 1,
                    "platform" to "ios",
                    "module_id" to 1,
                    "clientver" to 11490
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
