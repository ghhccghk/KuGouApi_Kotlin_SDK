package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 歌曲相关 API
 * 提供歌曲详情、相关歌曲、歌词下载、歌曲权限信息等功能。
 */
class SongApi(private val executor: RequestExecutor) {

    /**
     * 获取歌曲信息 (批量)
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
     * 获取音乐专辑歌手信息
     *
     * @param albumAudioId 专辑音乐 id (album_audio_id/MixSongID 均可以), 可以传多个，每个以逗号分开
     * @param fields 可以传 album_info authors.base base audio_info, authors.ip, extra, tags, tagmap 每个 field 以逗号分开
     */
    suspend fun getKrmAudio(albumAudioId: String, fields: String = ""): KuGouResponse {
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

    /**
     * 获取歌曲播放链接 (旧版 v5)
     * 对齐 module/song_url.js
     */
    suspend fun getSongUrl(
        hash: String,
        albumId: Long = 0,
        albumAudioId: Long = 0,
        quality: String = "128",
        freePart: Boolean = false,
        ppageId: String = ""
    ): KuGouResponse {
        val magicQualities = mapOf(
            "piano" to "magic_piano",
            "acappella" to "magic_acappella",
            "subwoofer" to "magic_subwoofer",
            "ancient" to "magic_ancient",
            "dj" to "magic_dj",
            "surnay" to "magic_surnay"
        )
        val finalQuality = magicQualities[quality] ?: quality

        val isLite = executor.config.isLite
        val pageId = if (isLite) 967177915L else 151369488L
        val defaultPPageId = if (isLite) "356753938,823673182,967485191" else "463467626,350369493,788954147"

        val params = mapOf(
            "album_id" to albumId,
            "area_code" to 1,
            "hash" to hash.lowercase(),
            "ssa_flag" to "is_fromtrack",
            "version" to 11430,
            "page_id" to pageId,
            "quality" to finalQuality,
            "album_audio_id" to albumAudioId,
            "behavior" to "play",
            "pid" to (if (isLite) 411 else 2),
            "cmd" to 26,
            "pidversion" to 3001,
            "IsFreePart" to (if (freePart) 1 else 0),
            "ppage_id" to (if (ppageId.isNotEmpty()) ppageId else defaultPPageId),
            "cdnBackup" to 1,
            "module" to "",
            "clientver" to 11430
        )

        return executor.execute(
            KuGouRequest(
                url = "/v5/url",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "trackercdn.kugou.com"),
                encryptKey = true,
                notSignature = true
            )
        )
    }

    /**
     * 获取歌曲播放链接 (新版 v6)
     * 对齐 module/song_url_new.js
     */
    suspend fun getSongUrlNew(
        hash: String,
        albumAudioId: Long,
        freePart: Boolean = false,
        vipToken: String = "",
        vipType: Int = 0
    ): KuGouResponse {
        val clientTime = currentTimeMillis()
        val userid = executor.cookieJar.getUserid()
        val mid = executor.cookieJar.getMid()
        val appId = executor.config.activeAppId
        val salt = executor.config.signKeySalt

        val key = Crypto.md5("${hash.lowercase()}${salt}${appId}${mid}${userid}")

        val data = buildJsonObject {
            put("area_code", "1")
            put("behavior", "play")
            put("qualities", buildJsonArray {
                listOf("128", "320", "flac", "high", "multitrack", "viper_atmos", "viper_tape", "viper_clear", "super").forEach { add(it) }
            })
            putJsonObject("resource") {
                put("album_audio_id", albumAudioId)
                put("collect_list_id", "3")
                put("collect_time", clientTime)
                put("hash", hash.lowercase())
                put("id", 0)
                put("page_id", 1)
                put("type", "audio")
            }
            put("token", executor.cookieJar.getToken())
            putJsonObject("tracker_param") {
                put("all_m", 1)
                put("auth", "")
                put("is_free_part", if (freePart) 1 else 0)
                put("key", key)
                put("module_id", 0)
                put("need_climax", 1)
                put("need_xcdn", 1)
                put("open_time", "")
                put("pid", "411")
                put("pidversion", "3001")
                put("priv_vip_type", "6")
                put("viptoken", vipToken)
            }
            put("userid", userid)
            put("vip", vipType)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://tracker.kugou.com",
                url = "/v6/priv_url",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取音频高潮部分
     * 对齐 module/song_climax.js
     */
    suspend fun getSongClimax(hash: String): KuGouResponse {
        val hashes = hash.split(",").filter { it.isNotEmpty() }
        val data = buildJsonArray {
            hashes.forEach { h ->
                addJsonObject {
                    put("hash", h)
                }
            }
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://expendablekmrcdn.kugou.com",
                url = "/v1/audio_climax/audio",
                method = HttpMethod.GET,
                params = mapOf("data" to data.toString()),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 歌曲成绩单 (排行榜信息)
     * 对齐 module/song_ranking.js
     */
    suspend fun getSongRanking(albumAudioId: Long): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/grow/v1/song_ranking/play_page/ranking_info",
                method = HttpMethod.GET,
                params = mapOf("album_audio_id" to albumAudioId),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 歌曲榜单过滤
     * 对齐 module/song_ranking_filter.js
     */
    suspend fun getSongRankingFilter(
        albumAudioId: Long,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/grow/v1/song_ranking/unlock/v2/ranking_filter",
                method = HttpMethod.GET,
                params = mapOf(
                    "album_audio_id" to albumAudioId,
                    "page" to page,
                    "pagesize" to pageSize
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
