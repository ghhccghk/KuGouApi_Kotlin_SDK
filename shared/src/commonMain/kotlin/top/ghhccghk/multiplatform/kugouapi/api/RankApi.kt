package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 排行榜相关 API
 */
class RankApi(private val executor: RequestExecutor) {

    /**
     * 获取排行榜列表
     */
    suspend fun getList(withSong: Int = 1): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/ocean/v6/rank/list",
                method = HttpMethod.GET,
                params = mapOf(
                    "plat" to 2,
                    "withsong" to withSong,
                    "parentid" to 0
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取排行榜详情
     */
    suspend fun getInfo(
        rankId: String,
        rankCid: Int = 0,
        withAlbumImg: Int = 1,
        zone: String = ""
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/ocean/v6/rank/info",
                method = HttpMethod.GET,
                params = mapOf(
                    "rankid" to rankId,
                    "rank_cid" to rankCid,
                    "with_album_img" to withAlbumImg,
                    "zone" to zone
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取排行榜音乐列表
     */
    suspend fun getAudio(
        rankId: String,
        rankCid: Int = 0,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/openapi/kmr/v2/rank/audio",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("rank_id", rankId)
                    put("rank_cid", rankCid)
                    put("page", page)
                    put("pagesize", pageSize)
                    put("show_portrait_mv", 1)
                    put("show_type_total", 1)
                    put("filter_original_remarks", 1)
                    put("area_code", 1)
                    put("type", 1)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("kg-tid" to "369")
            )
        )
    }

    /**
     * 获取排行榜推荐列表
     */
    suspend fun getTop(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/mobileservice/api/v5/rank/rec_rank_list",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取排行榜往期列表
     */
    suspend fun getVol(rankId: String, rankCid: Int = 0): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/ocean/v6/rank/vol",
                method = HttpMethod.GET,
                params = mapOf(
                    "rankid" to rankId,
                    "rank_cid" to rankCid,
                    "ranktype" to 1,
                    "type" to 0,
                    "plat" to 2
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
