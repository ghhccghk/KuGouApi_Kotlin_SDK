package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.put

/**
 * 歌单相关 API
 */
class PlaylistApi(private val executor: RequestExecutor) {

    /**
     * 收藏/创建歌单
     */
    suspend fun addPlaylist(
        name: String,
        listCreateUserId: String,
        listCreateListId: String,
        type: Int = 0,
        source: Int = 1,
        isPri: Int = 0,
        listCreateGid: String = ""
    ): KuGouResponse {
        val clienttime = currentTimeMillis() / 1000
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        val dataMap = buildJsonObject {
            put("userid", userid)
            put("token", token)
            put("total_ver", 0)
            put("name", name)
            put("type", type)
            put("source", source)
            put("is_pri", if (type == 0) isPri else 0)
            put("list_create_userid", listCreateUserId)
            put("list_create_listid", listCreateListId)
            put("list_create_gid", listCreateGid)
            put("from_shupinmv", 0)
        }

        return executor.execute(
            KuGouRequest(
                url = "/cloudlist.service/v5/add_list",
                method = HttpMethod.POST,
                data = dataMap,
                params = if (type == 0) mapOf(
                    "last_time" to clienttime,
                    "last_area" to "gztx",
                    "userid" to userid,
                    "token" to token
                ) else emptyMap(),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 取消收藏/删除歌单
     */
    suspend fun deletePlaylist(listId: Long): KuGouResponse {
        val clienttime = currentTimeMillis() / 1000
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        val dataMap = buildJsonObject {
            put("listid", listId)
            put("total_ver", 0)
            put("type", 1)
        }
        val aesKeyBase = PlatformIdentity.generateRandomString(6).lowercase()
        val md5Key = Crypto.md5(aesKeyBase)
        val encryptKey = md5Key.substring(0, 16)
        val iv = md5Key.substring(16, 32)
        val encryptedData = Crypto.aesEncryptBase64(dataMap.toString(), encryptKey, iv)

        val rsaData = buildJsonObject {
            put("aes", aesKeyBase)
            put("uid", userid.toLongOrNull() ?: 0L)
            put("token", token)
        }.toString().encodeToByteArray()
        val p = Crypto.rsaEncryptPkcs1(rsaData, Crypto.activePublicRasKey(executor.config)).uppercase()

        val response = executor.execute(
            KuGouRequest(
                url = "/v2/delete_list",
                method = HttpMethod.POST,
                data = encryptedData,
                params = mapOf(
                    "clienttime" to clienttime,
                    "key" to RequestSigner(executor.config).signParamsKey(clienttime.toString()),
                    "last_area" to "gztx",
                    "clientver" to executor.config.activeClientVersion,
                    "appid" to executor.config.activeAppId,
                    "last_time" to clienttime,
                    "p" to p
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "cloudlist.service.kugou.com"),
                responseType = ResponseType.BYTES
            )
        )

        if (response.status == 200) {
            val bytesStr = response.body["bytes"]?.jsonPrimitive?.content ?: ""
            if (bytesStr.isNotEmpty()) {
                try {
                    val bytes = bytesStr.split(",").map { it.toByte() }.toByteArray()
                    val responseBase64 = Crypto.encodeBase64(bytes)
                    val decryptedJson = Crypto.aesDecryptBase64(responseBase64, encryptKey, iv)
                    return response.copy(body = Json.parseToJsonElement(decryptedJson) as JsonObject)
                } catch (_: Exception) {}
            }
        }
        return response
    }

    /**
     * 获取歌单详情
     */
    suspend fun getPlaylistDetail(ids: String): KuGouResponse {
        val collectionIds = ids.split(",").filter { it.isNotEmpty() }
        val data = buildJsonArray {
            collectionIds.forEach { id ->
                addJsonObject { put("global_collection_id", id) }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/v3/get_list_info",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("data", data)
                    put("userid", executor.cookieJar.getUserid())
                    put("token", executor.cookieJar.getToken())
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "pubsongs.kugou.com")
            )
        )
    }

    /**
     * 获取音效歌单
     */
    suspend fun getEffectPlaylists(page: Int = 1, pageSize: Int = 30): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/pubsongs/v1/get_sound_effect_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("page", page)
                    put("pagesize", pageSize)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取相似歌单
     */
    suspend fun getSimilarPlaylists(ids: String): KuGouResponse {
        val collectionIds = ids.split(",").filter { it.isNotEmpty() }
        val data = buildJsonArray {
            collectionIds.forEach { id ->
                addJsonObject { put("global_collection_id", id) }
            }
        }
        val dateNow = currentTimeMillis()

        return executor.execute(
            KuGouRequest(
                url = "/pubsongs/v1/kmr_get_similar_lists",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clientver", executor.config.activeClientVersion)
                    put("clienttime", dateNow)
                    put("key", RequestSigner(executor.config).signParamsKey(dateNow))
                    put("userid", executor.cookieJar.getUserid())
                    put("ugc", 1)
                    put("show_list", 1)
                    put("need_songs", 1)
                    put("data", data)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取歌单标签分类
     */
    suspend fun getPlaylistTags(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/pubsongs/v1/get_tags_by_type",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("tag_type", "collection")
                    put("tag_id", 0)
                    put("source", 3)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取歌单所有歌曲 (公开版)
     */
    suspend fun getPlaylistTracks(
        id: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/pubsongs/v2/get_other_list_file_nofilt",
                method = HttpMethod.GET,
                params = mapOf(
                    "area_code" to 1,
                    "begin_idx" to (page - 1) * pageSize,
                    "plat" to 1,
                    "type" to 1,
                    "mode" to 1,
                    "personal_switch" to 1,
                    "extend_fields" to "abtags,hot_cmt,popularization",
                    "pagesize" to pageSize,
                    "global_collection_id" to id
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取歌单所有歌曲 (新版/私有版)
     */
    suspend fun getPlaylistTracksNew(
        listId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v4/get_list_all_file",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("listid", listId)
                    put("userid", executor.cookieJar.getUserid())
                    put("area_code", 1)
                    put("show_relate_goods", 0)
                    put("pagesize", pageSize)
                    put("allplatform", 1)
                    put("show_cover", 1)
                    put("type", 0)
                    put("token", executor.cookieJar.getToken())
                    put("page", page)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "cloudlist.service.kugou.com")
            )
        )
    }

    /**
     * 向歌单添加歌曲
     * @param data 格式: "name|hash|album_id|mixsongid", 多个用逗号分隔
     */
    suspend fun addTracks(listId: String, data: String): KuGouResponse {
        val clienttime = currentTimeMillis() / 1000
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        val resource = buildJsonArray {
            data.split(",").forEach { s ->
                val parts = s.split("|")
                addJsonObject {
                    put("number", 1)
                    put("name", parts.getOrNull(0) ?: "")
                    put("hash", parts.getOrNull(1) ?: "")
                    put("size", 0)
                    put("sort", 0)
                    put("timelen", 0)
                    put("bitrate", 0)
                    put("album_id", parts.getOrNull(2)?.toLongOrNull() ?: 0L)
                    put("mixsongid", parts.getOrNull(3)?.toLongOrNull() ?: 0L)
                }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/cloudlist.service/v6/add_song",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("userid", userid)
                    put("token", token)
                    put("listid", listId)
                    put("list_ver", 0)
                    put("type", 0)
                    put("slow_upload", 1)
                    put("scene", "false;null")
                    put("data", resource)
                },
                params = mapOf(
                    "last_time" to clienttime,
                    "last_area" to "gztx",
                    "userid" to userid,
                    "token" to token
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 从歌单删除歌曲
     * @param fileIds 歌曲在歌单中的 ID，多个用逗号分隔
     */
    suspend fun removeTracks(listId: String, fileIds: String): KuGouResponse {
        val resource = buildJsonArray {
            fileIds.split(",").forEach { id ->
                addJsonObject { put("fileid", id.toLongOrNull() ?: 0L) }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/v4/delete_songs",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("listid", listId)
                    put("userid", executor.cookieJar.getUserid())
                    put("data", resource)
                    put("type", 0)
                    put("token", executor.cookieJar.getToken())
                    put("list_ver", 0)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "cloudlist.service.kugou.com")
            )
        )
    }

    suspend fun getThemePlayLists(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v2/getthemelist",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("platform", "android")
                    put("clientver", executor.config.activeClientVersion)
                    put("clienttime", currentTimeMillis())
                    put("area_code", 1)
                    put("module_id", 1)
                    put("userid",executor.cookieJar.getUserid())
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "everydayrec.service.kugou.com")
            )
        )

    }
}
