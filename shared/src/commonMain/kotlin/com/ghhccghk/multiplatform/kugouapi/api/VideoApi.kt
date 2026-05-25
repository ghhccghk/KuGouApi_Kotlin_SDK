package com.ghhccghk.multiplatform.kugouapi.api

import com.ghhccghk.multiplatform.kugouapi.core.*
import com.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 视频/MV 相关 API
 */
class VideoApi(private val executor: RequestExecutor) {

    /**
     * 获取歌曲对应的 MV 信息
     *
     * @param albumAudioId 多个用逗号分隔
     */
    suspend fun getKmrAudioMv(albumAudioId: String, fields: String = ""): KuGouResponse {
        val ids = albumAudioId.split(",").filter { it.isNotEmpty() }
        val resource = buildJsonArray {
            ids.forEach { id ->
                addJsonObject { put("album_audio_id", id) }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/kmr/v1/audio/mv",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("data", resource)
                    put("fields", fields)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "openapi.kugou.com",
                    "KG-TID" to "38"
                )
            )
        )
    }
}
