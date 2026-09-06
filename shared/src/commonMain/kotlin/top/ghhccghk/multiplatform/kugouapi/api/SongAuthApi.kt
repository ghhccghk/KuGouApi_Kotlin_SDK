package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 歌曲授权 API
 * 提供歌曲授权验证功能。
 * 对齐 module/song_auth.js
 */
class SongAuthApi(private val executor: RequestExecutor) {

    /**
     * 获取歌曲授权
     *
     * @param authorization 授权令牌
     * @param albumAudioId 专辑音频ID
     * @param hash 歌曲哈希
     * @param moduleId 模块ID，默认 51
     */
    suspend fun getSongAuth(
        authorization: String,
        albumAudioId: Long = 0,
        hash: String = "",
        moduleId: Int = 51
    ): KuGouResponse {
        val params = mapOf(
            "authorization" to authorization,
            "module_id" to moduleId,
            "album_audio_id" to albumAudioId,
            "clientver" to 11561,
            "hash" to hash.lowercase()
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://trackercdngz.kugou.com",
                url = "/v1/authorization",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
