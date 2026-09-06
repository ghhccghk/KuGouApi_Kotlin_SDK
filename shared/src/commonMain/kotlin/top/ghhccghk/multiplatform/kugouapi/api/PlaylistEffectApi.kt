package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 歌单音效 API
 * 提供音效歌单、相似歌单、歌单标签等功能。
 * 对齐 module/playlist_effect.js, playlist_similar.js, playlist_tags.js
 */
class PlaylistEffectApi(private val executor: RequestExecutor) {

    /**
     * 获取音效歌单
     *
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 30
     */
    suspend fun getPlaylistEffect(
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val data = buildJsonObject {
            put("page", page)
            put("pagesize", pageSize)
        }

        return executor.execute(
            KuGouRequest(
                url = "/pubsongs/v1/get_sound_effect_list",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取相似歌单
     *
     * @param playlistId 歌单ID
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 30
     */
    suspend fun getSimilarPlaylist(
        playlistId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val params = mapOf(
            "playlist_id" to playlistId,
            "page" to page,
            "pagesize" to pageSize
        )

        return executor.execute(
            KuGouRequest(
                url = "/pubsongs/v1/get_similar_playlist",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取歌单标签
     */
    suspend fun getPlaylistTags(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/pubsongs/v1/get_playlist_tags",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
