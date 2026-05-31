package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 长音频 (有声书等) 相关 API
 */
class LongAudioApi(private val executor: RequestExecutor) {

    /**
     * 获取长音频专辑下的音乐列表
     */
    suspend fun getAlbumAudios(
        albumId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v2/album_audios",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("album_id", albumId)
                    put("area_code", 1)
                    put("tagid", 0)
                    put("page", page)
                    put("pagesize", pageSize)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "openapi.kugou.com",
                    "KG-TID" to "78"
                )
            )
        )
    }

    /**
     * 获取长音频专辑详情
     */
    suspend fun getAlbumDetail(ids: String): KuGouResponse {
        val data = buildJsonArray {
            ids.split(",").filter { it.isNotEmpty() }.forEach { id ->
                addJsonObject { put("album_id", id) }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/openapi/v2/broadcast",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("data", data)
                    put("show_album_tag", 1)
                    put("fields", "album_name,album_id,category,authors,sizable_cover,intro,author_name,trans_param,album_tag,mix_intro,full_intro,is_publish")
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("KG-TID" to "78")
            )
        )
    }

    /**
     * 获取长音频每日推荐
     */
    suspend fun getDailyRecommend(page: Int = 1, pageSize: Int = 30): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v1/home_new/daily_recommend",
                method = HttpMethod.POST,
                params = mapOf(
                    "module_id" to 1,
                    "size" to pageSize,
                    "page" to page
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取长音频排行榜推荐
     */
    suspend fun getRankRecommend(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v1/home_new/rank_card_recommend",
                method = HttpMethod.GET,
                params = mapOf("platform" to "ios"),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取长音频 VIP 推荐
     */
    suspend fun getVipRecommend(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v1/home_new/vip_select_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject { put("album_playlist", buildJsonArray { }) },
                params = mapOf("position" to "2", "clientver" to 12329),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取长音频每周新书推荐
     */
    suspend fun getWeekRecommend(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v1/home_new/week_new_albums_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject { put("album_playlist", buildJsonArray { }) },
                params = mapOf("clientver" to 12329),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
