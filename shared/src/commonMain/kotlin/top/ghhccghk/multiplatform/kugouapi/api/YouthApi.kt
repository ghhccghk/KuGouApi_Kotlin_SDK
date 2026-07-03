package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * Youth 频道相关 API
 *
 * 对齐 Node.js `youth_*` 系列模块，提供 Youth 频道浏览、VIP 领取、
 * 动态查看、听歌记录等功能。
 */
class YouthApi(private val executor: RequestExecutor) {

    // ────────────────────────────────────────────────────────────────
    // 频道
    // ────────────────────────────────────────────────────────────────

    /**
     * Youth 频道全部列表
     * 对齐 node module/youth_channel_all.js
     */
    suspend fun getChannelAll(
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v2/channel/channel_all_list",
                method = HttpMethod.GET,
                params = mapOf(
                    "page" to page,
                    "pagesize" to pageSize,
                    "type" to 1
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * Youth 安利推荐
     * 对齐 node module/youth_channel_amway.js
     *
     * @param globalCollectionId 频道全局收藏 ID
     */
    suspend fun getChannelAmway(globalCollectionId: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/api/amway/v2/index",
                method = HttpMethod.GET,
                params = mapOf("global_collection_id" to globalCollectionId),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * Youth 频道详情
     * 对齐 node module/youth_channel_detail.js
     *
     * @param globalCollectionIds 频道全局收藏 ID，多个用逗号分隔
     */
    suspend fun getChannelDetail(globalCollectionIds: String): KuGouResponse {
        val ids = globalCollectionIds.split(",").filter { it.isNotEmpty() }
        val data = buildJsonArray {
            ids.forEach { id ->
                addJsonObject { put("global_collection_id", id) }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/youth/api/channel/v1/channel_list_by_id",
                method = HttpMethod.POST,
                data = buildJsonObject { put("data", data) },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 相似频道推荐
     * 对齐 node module/youth_channel_similar.js
     *
     * @param channelId 频道 ID
     */
    suspend fun getSimilarChannel(channelId: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v1/channel/get_friendly_channel",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("area_code", 1)
                    put("playlist_ver", 2)
                    put("vip_type", executor.cookieJar.getVipType())
                    put("platform", "ios")
                },
                params = mapOf("channel_id" to channelId),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * Youth 频道歌曲列表
     * 对齐 node module/youth_channel_song.js
     *
     * @param globalCollectionId 频道全局收藏 ID
     * @param page 页码
     * @param pageSize 每页数量
     */
    suspend fun getChannelSong(
        globalCollectionId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/api/channel/v1/channel_get_song_audit_passed",
                method = HttpMethod.GET,
                params = mapOf(
                    "global_collection_id" to globalCollectionId,
                    "pagesize" to pageSize,
                    "page" to page,
                    "is_filter" to 0
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * Youth 频道歌曲详情
     * 对齐 node module/youth_channel_song_detail.js
     *
     * @param globalCollectionId 频道全局收藏 ID
     * @param fileid 文件 ID
     */
    suspend fun getChannelSongDetail(
        globalCollectionId: String,
        fileid: String
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v2/post/get_song_detail",
                method = HttpMethod.GET,
                params = mapOf(
                    "global_collection_id" to globalCollectionId,
                    "fileid" to fileid
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * Youth 频道订阅/取消订阅
     * 对齐 node module/youth_channel_sub.js
     *
     * @param globalCollectionId 频道全局收藏 ID
     * @param subscribe true = 订阅，false = 取消订阅
     */
    suspend fun subscribeChannel(
        globalCollectionId: String,
        subscribe: Boolean = true
    ): KuGouResponse {
        val url = if (subscribe) {
            "/youth/v1/channel_subscribe"
        } else {
            "/youth/v1/channel_un_subscribe"
        }

        return executor.execute(
            KuGouRequest(
                url = url,
                method = if (subscribe) HttpMethod.POST else HttpMethod.DELETE,
                params = mapOf(
                    "global_collection_id" to globalCollectionId,
                    "source" to 1
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    // ────────────────────────────────────────────────────────────────
    // VIP 相关
    // ────────────────────────────────────────────────────────────────

    /**
     * 领取 VIP（听歌领一天）
     * 对齐 node module/youth_day_vip.js
     *
     * @param receiveDay 领取天数
     */
    suspend fun receiveVip(receiveDay: Int = 1): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v1/recharge/receive_vip_listen_song",
                method = HttpMethod.POST,
                params = mapOf(
                    "source_id" to 90139,
                    "receive_day" to receiveDay
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("content-type" to "application/x-www-form-urlencoded")
            )
        )
    }

    /**
     * 升级 VIP
     * 对齐 node module/youth_day_vip_upgrade.js
     */
    suspend fun upgradeVipReward(): KuGouResponse {
        val userid = executor.cookieJar.getUserid()

        return executor.execute(
            KuGouRequest(
                url = "/youth/v1/listen_song/upgrade_vip_reward",
                method = HttpMethod.POST,
                params = mapOf(
                    "kugouid" to (userid.toLongOrNull() ?: 0L),
                    "ad_type" to 1
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 领取 VIP（看广告）
     * 对齐 node module/youth_vip.js
     */
    suspend fun claimVip(): KuGouResponse {
        val time = currentTimeMillis()

        return executor.execute(
            KuGouRequest(
                url = "/youth/v1/ad/play_report",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("ad_id", 12307537187)
                    put("play_end", time)
                    put("play_start", time - 30000)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取联合 VIP 信息
     * 对齐 node module/youth_union_vip.js
     */
    suspend fun getUnionVip(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://kugouvip.kugou.com",
                url = "/v1/get_union_vip",
                method = HttpMethod.GET,
                params = mapOf(
                    "busi_type" to "concept",
                    "opt_product_types" to "dvip,qvip",
                    "product_type" to "svip"
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取月 VIP 记录
     * 对齐 node module/youth_month_vip_record.js
     */
    suspend fun getMonthVipRecord(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v1/activity/get_month_vip_record",
                method = HttpMethod.GET,
                params = mapOf("latest_limit" to 100),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    // ────────────────────────────────────────────────────────────────
    // 动态 & 听歌
    // ────────────────────────────────────────────────────────────────

    /**
     * 获取 Youth 动态
     * 对齐 node module/youth_dynamic.js
     */
    suspend fun getDynamic(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v3/user/get_dynamic",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取 Youth 最近动态
     * 对齐 node module/youth_dynamic_recent.js
     */
    suspend fun getRecentDynamic(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v3/user/recent_dynamic",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 上报听歌（听歌领取 VIP）
     * 对齐 node module/youth_listen_song.js
     *
     * @param mixsongid 歌曲 MixID
     */
    suspend fun reportListenSong(mixsongid: Long = 666075191): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v2/report/listen_song",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("mixsongid", mixsongid)
                },
                params = mapOf("clientver" to 10566),
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "user-agent" to "Android13-1070-10566-201-0-ReportPlaySongToServerProtocol-wifi",
                    "content-type" to "application/json; charset=utf-8"
                )
            )
        )
    }

    /**
     * 获取 Youth 用户公开歌曲
     * 对齐 node module/youth_user_song.js
     *
     * @param userid 用户 ID
     * @param type 类型
     * @param page 页码
     * @param pageSize 每页数量
     */
    suspend fun getUserSong(
        userid: String,
        type: Int = 0,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/youth/v1/get_user_song_public",
                method = HttpMethod.GET,
                params = mapOf(
                    "filter_video" to 0,
                    "type" to type,
                    "userid" to userid,
                    "pagesize" to pageSize,
                    "page" to page,
                    "is_filter" to 0
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
