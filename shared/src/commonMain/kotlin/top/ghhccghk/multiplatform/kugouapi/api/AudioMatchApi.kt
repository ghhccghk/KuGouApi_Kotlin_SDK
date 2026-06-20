package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import io.ktor.http.ContentType

/**
 * 听歌识曲 API
 *
 * 对齐: https://github.com/MakcRe/KuGouMusicApi/blob/main/module/audio_match.js
 */
class AudioMatchApi(private val executor: RequestExecutor) {

    /**
     * 听歌识曲
     *
     * 通过上传 PCM 音频二进制数据到酷狗指纹识别服务，识别音频中包含的歌曲信息。
     * 逻辑完整对齐：https://github.com/MakcRe/KuGouMusicApi/blob/main/module/audio_match.js
     *
     * @param audioData PCM 格式的音频二进制数据（16-bit samples）
     * @param areaCode 地区代码，默认 1
     * @param includeUnpublish 是否包含未发布歌曲，默认 1
     * @return [KuGouResponse] 包含识别结果的响应对象
     */
    suspend fun match(
        audioData: ByteArray,
        areaCode: Int = 1,
        includeUnpublish: Int = 1,
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://tracker.kugou.com",
                url = "/fingerprint.service/v1/music_trackid_mulit",
                method = HttpMethod.POST,
                data = audioData,
                params = mapOf(
                    "fpid" to currentTimeMillis(),
                    "area_code" to areaCode,
                    "include_unpublish" to includeUnpublish,
                    "useid" to (userid.toLongOrNull() ?: 0L),
                    "multi_result" to 1,
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "User-Agent" to "KuGou/11490 (Android)",
                ),
                contentType = ContentType.Application.OctetStream,
            )
        )
    }
}
