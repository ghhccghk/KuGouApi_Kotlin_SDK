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
}
