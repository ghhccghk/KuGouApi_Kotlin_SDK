package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 图片相关 API
 */
class ImageApi(private val executor: RequestExecutor) {

    /**
     * 获取歌曲/专辑图片
     */
    suspend fun getImages(
        hash: String,
        albumId: String = "",
        albumAudioId: String = "",
        count: Int = 5
    ): KuGouResponse {
        val hashes = hash.split(",").filter { it.isNotEmpty() }
        val albumIds = albumId.split(",")
        val audioIds = albumAudioId.split(",")
        
        val data = buildJsonArray {
            hashes.forEachIndexed { index, h ->
                addJsonObject {
                    put("album_id", albumIds.getOrNull(index)?.toLongOrNull() ?: 0L)
                    put("hash", h)
                    put("album_audio_id", audioIds.getOrNull(index)?.toLongOrNull() ?: 0L)
                }
            }
        }

        val paramsMap = mutableMapOf<String, Any?>(
            "album_image_type" to "-3",
            "appid" to executor.config.activeAppId,
            "clientver" to executor.config.activeClientVersion,
            "author_image_type" to "3,4,5",
            "count" to count,
            "data" to data,
            "isCdn" to 1,
            "publish_time" to 1
        )

        val signer = RequestSigner(executor.config)
        val signature = signer.signatureAndroidParams(paramsMap)

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://expendablekmr.kugou.com",
                url = "/container/v2/image",
                method = HttpMethod.GET,
                params = paramsMap + ("signature" to signature),
                encryptType = EncryptType.ANDROID,
                notSignature = true,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 获取音频相关的作者图片
     */
    suspend fun getAudioImages(
        hash: String,
        audioId: String = "",
        albumAudioId: String = "",
        filename: String = "",
        count: Int = 5
    ): KuGouResponse {
        val hashes = hash.split(",").filter { it.isNotEmpty() }
        val audioIds = audioId.split(",")
        val albumAudioIds = albumAudioId.split(",")
        val filenames = filename.split(",")

        val data = buildJsonArray {
            hashes.forEachIndexed { index, h ->
                addJsonObject {
                    put("audio_id", audioIds.getOrNull(index)?.toLongOrNull() ?: 0L)
                    put("hash", h)
                    put("album_audio_id", albumAudioIds.getOrNull(index)?.toLongOrNull() ?: 0L)
                    put("filename", filenames.getOrNull(index) ?: "")
                }
            }
        }

        val paramsMap = mutableMapOf<String, Any?>(
            "appid" to executor.config.activeAppId,
            "clientver" to executor.config.activeClientVersion,
            "count" to count,
            "data" to data,
            "isCdn" to 1,
            "publish_time" to 1,
            "show_authors" to 1
        )

        val signer = RequestSigner(executor.config)
        val signature = signer.signatureAndroidParams(paramsMap)

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://expendablekmr.kugou.com",
                url = "/v2/author_image/audio",
                method = HttpMethod.GET,
                params = paramsMap + ("signature" to signature),
                encryptType = EncryptType.ANDROID,
                notSignature = true,
                clearDefaultParams = true
            )
        )
    }
}
