package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 视频相关 API。提供 MV 播放、视频详情、视频搜索、视频 URL 获取等功能。对齐 module/video_* 和 kmr_audio_mv 等。
 */
class VideoApi(private val executor: RequestExecutor) {

    /**
     * 获取歌曲对应的直播 MV 信息
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

    // ────────────────────────────────────────────────────────────────
    // 新增：视频详情、特权、URL
    // ────────────────────────────────────────────────────────────────

    /**
     * 获取视频详情
     * 对齐 node module/video_detail.js
     *
     * @param ids 视频 ID，多个用逗号分隔
     */
    suspend fun getVideoDetail(ids: String): KuGouResponse {
        val idList = ids.split(",").filter { it.isNotEmpty() }
        val resource = buildJsonArray {
            idList.forEach { id ->
                addJsonObject { put("video_id", id) }
            }
        }

        val dateNow = currentTimeMillis() / 1000
        val mid = executor.cookieJar.getMid()
        val token = executor.cookieJar.getToken()
        val dfid = executor.cookieJar.getDfid()
        val uuid = Crypto.md5("${dfid}${mid}")

        return executor.execute(
            KuGouRequest(
                url = "/v1/video",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clientver", executor.config.activeClientVersion)
                    put("clienttime", dateNow)
                    put("mid", mid)
                    put("uuid", uuid)
                    put("dfid", dfid)
                    put("token", token)
                    put("key", RequestSigner(executor.config).signParamsKey(dateNow.toString()))
                    put("show_resolution", 1)
                    put("data", resource)
                },
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true,
                headers = mapOf("x-router" to "kmr.service.kugou.com")
            )
        )
    }

    /**
     * 获取视频特权信息
     * 对齐 node module/video_privilege.js
     *
     * @param hashes 视频 hash，多个用逗号分隔
     */
    suspend fun getVideoPrivilege(hashes: String): KuGouResponse {
        val hashList = hashes.split(",").filter { it.isNotEmpty() }
        val resource = buildJsonArray {
            hashList.forEach { h ->
                addJsonObject {
                    put("hash", h)
                    put("id", 0)
                    put("name", "")
                }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/v1/get_video_privilege",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("area_code", 1)
                    put("behavior", "play")
                    put("clientver", executor.config.activeClientVersion)
                    put("dfid", executor.cookieJar.getDfid())
                    put("mid", executor.cookieJar.getMid())
                    put("data", resource)
                    put("token", executor.cookieJar.getToken())
                    put("userid", executor.cookieJar.getUserid())
                    put("vip", executor.cookieJar.getVipType())
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "media.store.kugou.com")
            )
        )
    }

    /**
     * 获取视频播放 URL
     * 对齐 node module/video_url.js
     *
     * @param hash 视频 hash
     */
    suspend fun getVideoUrl(hash: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v2/interface/index",
                method = HttpMethod.GET,
                params = mapOf(
                    "backupdomain" to 1,
                    "cmd" to 123,
                    "ext" to "mp4",
                    "ismp3" to 0,
                    "hash" to hash,
                    "pid" to 1,
                    "type" to 1
                ),
                encryptType = EncryptType.ANDROID,
                encryptKey = true,
                headers = mapOf("x-router" to "trackermv.kugou.com")
            )
        )
    }
}
