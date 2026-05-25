package com.ghhccghk.multiplatform.kugouapi.api

import com.ghhccghk.multiplatform.kugouapi.core.*
import com.ghhccghk.multiplatform.kugouapi.model.AudioRelatedSort
import com.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 歌曲相关 API
 * 提供歌曲详情、相关歌曲、歌词下载、歌曲权限信息等功能。
 */
class SongApi(private val executor: RequestExecutor) {

    /**
     * 获取歌曲信息 (批量)
     * 对齐 module/audio.js
     *
     * @param hash 歌曲 Hash，多个用逗号分隔
     */
    suspend fun getAudioInfo(hash: String): KuGouResponse {
        val hashes = hash.split(",").filter { it.isNotEmpty() }
        val data = buildJsonArray {
            hashes.forEach { h ->
                addJsonObject {
                    put("hash", h)
                    put("audio_id", 0)
                }
            }
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://kmr.service.kugou.com",
                url = "/v1/audio/audio",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("data", data)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "kmr.service.kugou.com")
            )
        )
    }

    /**
     * 获取相关歌曲/详情
     * 对齐 module/audio_related.js
     *
     * @param albumAudioId 专辑歌曲 ID
     * @param showDetail 是否显示详情 (true 会调用 /v2/audio_related/total)
     */
    suspend fun getRelatedAudio(
        albumAudioId: Long,
        showDetail: Boolean = false,
        page: Int = 1,
        pageSize: Int = 30,
        sort: AudioRelatedSort = AudioRelatedSort.ALL
    ): KuGouResponse {
        val sortMap = mapOf(AudioRelatedSort.ALL to 1, AudioRelatedSort.HOT to 2, AudioRelatedSort.NEW to 3)
        
        val params = mutableMapOf<String, Any?>(
            "album_audio_id" to albumAudioId,
            "appid" to 1005,
            "area_code" to 1,
            "clientver" to 12329,
            "version" to 1
        )

        if (!showDetail) {
            params["page"] = page
            params["pagesize"] = pageSize
            params["show_input"] = 1
            params["show_type"] = 0
            params["sort"] = sortMap[sort] ?: 1
            params["type"] = 0
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://listkmrp3cdnretry.kugou.com",
                url = if (showDetail) "/v2/audio_related/total" else "/v3/album_audio/related",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 匹配伴奏
     * 对齐 module/audio_accompany_matching.js
     */
    suspend fun matchAccompany(
        hash: String,
        fileName: String = "",
        mixId: Long = 0
    ): KuGouResponse {
        val params = mapOf(
            "isteen" to 0,
            "mixId" to mixId,
            "usemkv" to 1,
            "platform" to 2,
            "fileName" to fileName,
            "hash" to hash,
            "version" to 12375,
            "appid" to executor.config.activeAppId
        )

        // 伴奏接口使用特殊的签名方式
        val str = "*s&iN#G70*"
        val paramsString = params.keys.sorted().joinToString("&") { key ->
            "$key=${params[key]}"
        }
        val sign = Crypto.md5("$paramsString$str").substring(8, 24)

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://nsongacsing.kugou.com",
                url = "/sing7/accompanywan/json/v2/cdn/optimal_matching_accompany_2_listen.do",
                method = HttpMethod.GET,
                params = params + ("sign" to sign),
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true,
                notSignature = true
            )
        )
    }

    /**
     * 获取 KTV 作品总数
     * 对齐 module/audio_ktv_total.js
     */
    suspend fun getKtvTotal(
        songId: Long,
        singerName: String,
        songHash: String
    ): KuGouResponse {
        val params = mapOf(
            "isteen" to 0,
            "songId" to songId,
            "usemkv" to 1,
            "platform" to 2,
            "singerName" to singerName,
            "songHash" to songHash,
            "version" to 12375,
            "appid" to executor.config.activeAppId
        )

        val str = "*s&iN#G70*"
        val paramsString = params.keys.sorted().joinToString("&") { key ->
            "$key=${params[key]}"
        }
        val sign = Crypto.md5("$paramsString$str").substring(8, 24)

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://acsing.service.kugou.com",
                url = "/sing7/listenguide/json/v2/cdn/listenguide/get_total_opus_num_v02.do",
                method = HttpMethod.GET,
                params = params + ("sign" to sign),
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true,
                notSignature = true
            )
        )
    }

    /**
     * 获取歌曲权限信息 (Lite 版)
     * 对齐 module/privilege_lite.js
     */
    suspend fun getPrivilegeLite(hash: String, albumId: String = ""): KuGouResponse {
        val hashes = hash.split(",").filter { it.isNotEmpty() }
        val albumIds = albumId.split(",")
        
        val resource = buildJsonArray {
            hashes.forEachIndexed { index, h ->
                addJsonObject {
                    put("type", "audio")
                    put("page_id", 0)
                    put("hash", h)
                    put("album_id", albumIds.getOrNull(index)?.toLongOrNull() ?: 0L)
                }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/v2/get_res_privilege/lite",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("area_code", 1)
                    put("behavior", "play")
                    put("clientver", executor.config.activeClientVersion)
                    put("need_hash_offset", 1)
                    put("relate", 1)
                    put("support_verify", 1)
                    put("resource", resource)
                    put("qualities", buildJsonArray {
                        listOf("128", "320", "flac", "high", "viper_atmos", "viper_tape", "viper_clear", "super", "multitrack").forEach { add(it) }
                    })
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "media.store.kugou.com")
            )
        )
    }

    /**
     * 根据 ID 获取详细元数据 (歌手/专辑/歌曲)
     * 对齐 module/krm_audio.js
     *
     * @param albumAudioId 多个用逗号分隔
     */
    suspend fun getKrmAudio(albumAudioId: String, fields: String = "base"): KuGouResponse {
        val ids = albumAudioId.split(",").filter { it.isNotEmpty() }
        val data = buildJsonArray {
            ids.forEach { id ->
                addJsonObject {
                    put("entity_id", id.toLongOrNull() ?: 0L)
                }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/kmr/v2/audio",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("data", data)
                    put("fields", fields)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "openapi.kugou.com",
                    "KG-TID" to "238"
                )
            )
        )
    }

    /**
     * 获取歌词
     * 对齐 module/lyric.js
     *
     * @param id 歌词 ID
     * @param accessKey 访问 Key
     * @param decode 是否自动解码 KRC 格式
     */
    suspend fun getLyric(
        id: String,
        accessKey: String,
        fmt: String = "krc",
        decode: Boolean = false
    ): KuGouResponse {
        val response = executor.execute(
            KuGouRequest(
                baseUrl = "https://lyrics.kugou.com",
                url = "/download",
                method = HttpMethod.GET,
                params = mapOf(
                    "ver" to 1,
                    "client" to "android",
                    "id" to id,
                    "accesskey" to accessKey,
                    "fmt" to fmt,
                    "charset" to "utf8"
                ),
                encryptType = EncryptType.ANDROID
            )
        )

        if (decode && response.status == 200) {
            val content = response.body["content"]?.jsonPrimitive?.content
            if (!content.isNullOrEmpty()) {
                val contentType = response.body["contenttype"]?.jsonPrimitive?.intOrNull ?: 0
                val decodedText = if (fmt == "lrc" || contentType != 0) {
                    Crypto.decodeBase64(content).decodeToString()
                } else {
                    Crypto.decodeLyrics(Crypto.decodeBase64(content))
                }
                return response.copy(body = buildJsonObject {
                    response.body.forEach { (k, v) -> put(k, v) }
                    put("decodeContent", decodedText)
                })
            }
        }

        return response
    }
}
