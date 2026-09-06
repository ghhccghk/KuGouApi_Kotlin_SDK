package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.put

/**
 * 歌单相关 API。提供歌单详情、歌单歌曲、歌单分类、创建/删除歌单、导入歌单等功能。对齐 module/playlist_* 系列。
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

    /**
     * 导入歌单 - 添加任务
     * 对齐 module/import_playlist.js (add_task)
     *
     * @param url 歌单链接（taskType=0时）
     * @param listId 歌单ID（taskType=1时）
     * @param listName 歌单名称
     * @param taskType 任务类型：0=链接，1=ID
     * @param source 来源
     */
    suspend fun importPlaylistAddTask(
        url: String = "",
        listId: Long = 0,
        listName: String = "",
        taskType: Int = 0,
        source: Int = 3
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()
        val taskSn = ""

        val data = buildJsonObject {
            put("userid", userid)
            put("token", token)
            put("source", source)
            put("task_type", taskType)
            if (taskType == 0) {
                put("url", url)
            } else {
                put("listid", listId)
                if (listName.isNotEmpty()) put("list_name", listName)
                put("task_sn", taskSn)
            }
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://gateway.kugou.com",
                url = "/assetservice/import/v1/add_task",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 导入歌单 - 提交图片验证码
     * 对齐 module/import_playlist.js (submit_img)
     *
     * @param imgBase64 图片Base64编码
     * @param taskSn 任务序列号
     */
    suspend fun importPlaylistSubmitImg(
        imgBase64: String,
        taskSn: String
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        // 移除 data:image 前缀
        val cleanBase64 = imgBase64.replace(Regex("^data:image/[^;]+;base64,"), "")

        val data = buildJsonObject {
            put("userid", userid)
            put("token", token)
            put("img_base64", cleanBase64)
            put("task_sn", taskSn)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://gateway.kugou.com",
                url = "/assetservice/import/v1/submit_img",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 导入歌单 - 获取任务数量
     * 对齐 module/import_playlist.js (task_count)
     *
     * @param classify 分类
     */
    suspend fun importPlaylistTaskCount(
        classify: Int = 1
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        val data = buildJsonObject {
            put("userid", userid)
            put("token", token)
            put("classify", classify)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://gateway.kugou.com",
                url = "/assetservice/import/v1/task_count",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 导入歌单 - 查询任务状态
     * 对齐 module/import_playlist.js (query_task_status)
     *
     * @param ids 任务ID列表
     */
    suspend fun importPlaylistQueryTaskStatus(
        ids: List<Long>
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        val data = buildJsonObject {
            put("userid", userid)
            put("token", token)
            putJsonArray("ids") {
                ids.forEach { add(it) }
            }
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://gateway.kugou.com",
                url = "/assetservice/import/v1/query_task_status",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 导入歌单 - 查询任务详情
     * 对齐 module/import_playlist.js (query_task)
     *
     * @param listId 歌单ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param showMissed 是否显示缺失歌曲
     */
    suspend fun importPlaylistQueryTask(
        listId: String,
        page: Int = 1,
        pageSize: Int = 30,
        showMissed: Boolean = true
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        val data = buildJsonObject {
            put("userid", userid)
            put("token", token)
            put("listid", listId)
            put("page", page.coerceAtLeast(1))
            put("pagesize", pageSize.coerceAtLeast(1))
            put("show_missed", if (showMissed) 1 else 0)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://gateway.kugou.com",
                url = "/pubsongs/v1/query_task",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }
}