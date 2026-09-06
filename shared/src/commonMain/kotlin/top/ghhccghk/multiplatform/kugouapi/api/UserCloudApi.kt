package top.ghhccghk.multiplatform.kugouapi.api

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 用户云盘 API
 * 提供云盘音乐列表、删除、上传、匹配等功能。
 * 对齐 module/user_cloud_*.js
 */
class UserCloudApi(private val executor: RequestExecutor) {

    /**
     * 删除云盘音乐文件
     *
     * @param fileIds 文件ID列表，多个用逗号分隔
     * @param albumAudioIds 对应的专辑音频ID列表，多个用逗号分隔
     */
    suspend fun deleteCloudFiles(
        fileIds: String,
        albumAudioIds: String = ""
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()
        val mid = executor.cookieJar.getMid()
        val clientTime = currentTimeMillis() / 1000

        val fileIdList = fileIds.split(",").filter { it.isNotEmpty() }
        val albumAudioIdList = albumAudioIds.split(",").filter { it.isNotEmpty() }

        val data = buildJsonArray {
            fileIdList.forEachIndexed { index, fileId ->
                addJsonObject {
                    put("kv_id", fileId.toLongOrNull() ?: 0L)
                    put("album_audio_id", albumAudioIdList.getOrNull(index)?.toLongOrNull() ?: 
                        albumAudioIdList.firstOrNull()?.toLongOrNull() ?: 0L)
                }
            }
        }

        val dataMap = buildJsonObject {
            put("data", data)
        }

        // AES 加密 (playlistAesEncrypt)
        val aesKeyBase = PlatformIdentity.generateRandomString(6).lowercase()
        val md5Key = Crypto.md5(aesKeyBase)
        val encryptKey = md5Key.substring(0, 16)
        val iv = md5Key.substring(16, 32)
        val encryptedData = Crypto.aesEncryptBase64(dataMap.toString(), encryptKey, iv)

        // RSA 加密 key
        val rsaInput = buildJsonObject {
            put("aes", aesKeyBase)
            put("uid", userid)
            put("token", token)
        }.toString()
        val rsaEncrypted = Crypto.rsaEncrypt(rsaInput.encodeToByteArray(), Crypto.activePublicRasKey(executor.config)).uppercase()

        val params = mapOf(
            "clienttime" to clientTime,
            "mid" to mid,
            "key" to signParamsKey(clientTime.toString(), executor.config.activeAppId),
            "clientver" to executor.config.activeClientVersion,
            "appid" to executor.config.activeAppId,
            "p" to rsaEncrypted
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://mcloudservice.kugou.com",
                url = "/v1/del_files",
                method = HttpMethod.POST,
                data = Crypto.decodeBase64(encryptedData),
                params = params,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true,
                notSignature = true,
                responseType = ResponseType.BYTES
            )
        )
    }

    /**
     * 匹配云盘音乐
     *
     * @param hash 文件哈希
     * @param appid 应用ID
     * @param clientver 客户端版本
     */
    suspend fun matchCloudMusic(
        hash: String,
        appid: Int = executor.config.activeAppId,
        clientver: Int = executor.config.activeClientVersion
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()
        val mid = executor.cookieJar.getMid()
        val clientTime = currentTimeMillis() / 1000

        val params = mapOf(
            "hash" to hash,
            "appid" to appid,
            "clientver" to clientver,
            "clienttime" to clientTime,
            "mid" to mid,
            "userid" to userid,
            "token" to token
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://mcloudservice.kugou.com",
                url = "/v1/match",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    // 辅助函数：签名参数 key
    private fun signParamsKey(clientTime: String, appId: Int): String {
        val str = "OIlwieks28dk2k092lksi2UIkp"
        return Crypto.md5("$clientTime$str$appId")
    }
    
    /**
     * 云盘上传前曲库匹配
     *
     * @param hashes 文件哈希列表，多个用逗号分隔
     * @param albumAudioIds 专辑音频ID列表，多个用逗号分隔
     * @param appid 应用ID
     * @param clientver 客户端版本
     */
    suspend fun matchCloudMusicByHash(
        hashes: String,
        albumAudioIds: String = "",
        appid: Int = executor.config.activeAppId,
        clientver: Int = executor.config.activeClientVersion
    ): KuGouResponse {
        val clientTime = currentTimeMillis() / 1000
        val mid = executor.cookieJar.getMid()
        val dfid = executor.cookieJar.getDfid()

        val hashList = hashes.split(",").filter { it.isNotEmpty() }
        val albumAudioIdList = albumAudioIds.split(",").filter { it.isNotEmpty() }

        val data = buildJsonArray {
            hashList.forEachIndexed { index, hash ->
                addJsonObject {
                    put("hash", hash.lowercase())
                    val albumAudioId = albumAudioIdList.getOrNull(index) ?: albumAudioIdList.firstOrNull()
                    albumAudioId?.let {
                        if (it.toLongOrNull() ?: 0L > 0) {
                            put("album_audio_id", it)
                        }
                    }
                }
            }
        }

        val requestData = buildJsonObject {
            put("appid", appid)
            put("clienttime", clientTime)
            put("clientver", clientver)
            put("data", data)
            put("dfid", dfid)
            put("key", signParamsKey(clientTime.toString(), appid))
            put("mid", mid)
            put("show_privilege", 0)
            put("show_author_alias", 0)
            put("show_rel_album_audio_info", 0)
            put("show_remarks", 0)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://kmr.service.kugou.com",
                url = "/v2/album_audio/audio",
                method = HttpMethod.POST,
                data = requestData,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true,
                notSignature = true,
                headers = mapOf(
                    "x-router" to "kmr.service.kugou.com",
                    "Content-Type" to "application/json"
                )
            )
        )
    }

    /**
     * 上传音乐文件到用户云盘
     * 对齐 module/user_cloud_upload.js
     *
     * 流程：获取授权 → 初始化分片上传 → 上传分片 → 完成上传 → 添加文件到云盘
     *
     * @param fileData 文件二进制数据
     * @param filename 文件名（默认使用文件MD5）
     * @param extendname 文件扩展名（默认 mp3）
     * @param authorName 作者名
     * @param trackName 曲目名
     * @param hashStd 标准哈希（可选，用于匹配）
     * @param audioId 音频ID（可选）
     * @param albumAudioId 专辑音频ID（可选）
     * @param bitrate 码率（默认 4）
     * @param timelen 时长（默认 0）
     * @param autoMatch 是否自动匹配（默认 true）
     * @param partSize 分片大小（默认 4MB）
     */
    suspend fun uploadCloudFile(
        fileData: ByteArray,
        filename: String = "",
        extendname: String = "mp3",
        authorName: String = "",
        trackName: String = "",
        hashStd: String = "",
        audioId: Long = 0,
        albumAudioId: Long = 0,
        bitrate: Int = 4,
        timelen: Int = 0,
        autoMatch: Boolean = true,
        partSize: Int = 4 * 1024 * 1024
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()
        val mid = executor.cookieJar.getMid()
        val dfid = executor.cookieJar.getDfid()
        val uuid = executor.cookieJar.getGuid()
        val requestAppid = executor.config.activeAppId
        val requestClientver = executor.config.activeClientVersion
        val clienttime = currentTimeMillis() / 1000

        if (fileData.isEmpty()) {
            return KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", "文件数据不能为空")
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        }

        // 计算文件 MD5
        val fileHash = if (filename.isNotEmpty()) filename.lowercase() else Crypto.md5(fileData.decodeToString()).lowercase()
        val actualExtendname = extendname.removePrefix(".")
        val bucket = "musicclound"

        // 尝试自动匹配
        var matchInfo: MatchInfo? = null
        if (autoMatch && (hashStd.isEmpty() || audioId == 0L)) {
            try {
                val matchRes = matchCloudMusic(fileHash, requestAppid, requestClientver)
                if (matchRes.body["status"]?.jsonPrimitive?.intOrNull == 1) {
                    matchInfo = extractMatchInfo(matchRes.body)
                }
            } catch (_: Exception) {}
        }

        val actualHashStd = hashStd.ifEmpty { matchInfo?.hashStd ?: fileHash }
        val actualAudioId = if (audioId != 0L) audioId else (matchInfo?.audioId ?: 0L)
        val actualAlbumAudioId = if (albumAudioId != 0L) albumAudioId else (matchInfo?.albumAudioId ?: 0L)
        val actualAuthorName = authorName.ifEmpty { matchInfo?.authorName ?: "" }
        val actualTrackName = trackName.ifEmpty { matchInfo?.audioName ?: fileHash }
        val name = "."

        // 签名函数
        fun signBssParams(params: MutableMap<String, Any?>): MutableMap<String, Any?> {
            val signer = RequestSigner(executor.config)
            params["signature"] = signer.signatureAndroidParams(params.mapKeys { it.key }, "")
            return params
        }

        val bssVerifyCode = Crypto.md5("")

        try {
            // ========== 步骤1 获取授权 ==========
            val authParams = signBssParams(mutableMapOf(
                "appid" to requestAppid,
                "clientver" to requestClientver,
                "clienttime" to clienttime,
                "token" to token,
                "userid" to userid,
                "mid" to mid,
                "dfid" to dfid,
                "uuid" to uuid,
                "bucket" to bucket,
                "bss_verify_code" to bssVerifyCode,
                "request_appid" to requestAppid
            ))

            // 使用 HttpClient 进行外部请求
            val httpClient = HttpClient()

            val authResp = httpClient.post("http://bss.kugou.com/v3/get_upload_auth") {
                authParams.forEach { (k, v) -> parameter(k, v) }
            }
            val authBody = authResp.bodyAsText().let {
                Json.parseToJsonElement(it) as? JsonObject ?: buildJsonObject { put("raw", it) }
            }

            if (authBody["status"]?.jsonPrimitive?.intOrNull != 1) {
                httpClient.close()
                return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "获取上传授权失败")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )
            }

            val authorization = authBody["data"]?.jsonObject?.get("authorization")?.jsonPrimitive?.content ?: ""
            val externalHost = authBody["data"]?.jsonObject?.get("external_host")?.jsonPrimitive?.content ?: ""

            // ========== 步骤2 初始化分片上传 ==========
            val initParams = signBssParams(mutableMapOf(
                "bucket" to bucket,
                "authorization" to authorization,
                "filename" to fileHash,
                "version" to requestClientver,
                "userid" to userid,
                "token" to token,
                "dfid" to dfid,
                "mid" to mid,
                "uuid" to uuid,
                "appid" to requestAppid,
                "clientver" to requestClientver,
                "clienttime" to (currentTimeMillis() / 1000)
            ))

            val initResp = httpClient.post("http:///v3/multipart/init") {
                initParams.forEach { (k, v) -> parameter(k, v) }
                header("Authorization", authorization)
            }
            val initBody = initResp.bodyAsText().let {
                Json.parseToJsonElement(it) as? JsonObject ?: buildJsonObject { put("raw", it) }
            }

            if (initBody["status"]?.jsonPrimitive?.intOrNull != 1) {
                httpClient.close()
                return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "初始化分片上传失败")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )
            }

            val uploadId = initBody["data"]?.jsonObject?.get("upload_id")?.jsonPrimitive?.content ?: ""
            var bssFileHash = fileHash

            // ========== 步骤3 上传分片 ==========
            val partCount = (fileData.size + partSize - 1) / partSize
            for (i in 0 until partCount) {
                val start = i * partSize
                val end = minOf((i + 1) * partSize, fileData.size)
                val part = fileData.sliceArray(start until end)

                val uploadParams = signBssParams(mutableMapOf(
                    "bucket" to bucket,
                    "authorization" to authorization,
                    "filename" to fileHash,
                    "partnumber" to (i + 1),
                    "upload_id" to uploadId,
                    "body_empty" to 1,
                    "version" to requestClientver,
                    "userid" to userid,
                    "token" to token,
                    "dfid" to dfid,
                    "mid" to mid,
                    "uuid" to uuid,
                    "appid" to requestAppid,
                    "clientver" to requestClientver,
                    "clienttime" to (currentTimeMillis() / 1000)
                ))

                val uploadResp = httpClient.post("http:///v3/multipart/upload") {
                    uploadParams.forEach { (k, v) -> parameter(k, v) }
                    header("Authorization", authorization)
                    header("Content-Type", "application/octet-stream")
                    setBody(part)
                }
                val uploadBody = uploadResp.bodyAsText().let {
                    Json.parseToJsonElement(it) as? JsonObject ?: buildJsonObject { put("raw", it) }
                }

                if (uploadBody["status"]?.jsonPrimitive?.intOrNull != 1) {
                    httpClient.close()
                    return KuGouResponse(
                        status = 502,
                        body = buildJsonObject {
                            put("status", 0)
                            put("msg", "分片上传失败: 第片")
                        },
                        cookies = emptyMap(),
                        headers = emptyMap()
                    )
                }
            }

            // ========== 步骤4 完成上传 ==========
            val completeParams = signBssParams(mutableMapOf(
                "bucket" to bucket,
                "authorization" to authorization,
                "filename" to fileHash,
                "partnumber" to partCount,
                "upload_id" to uploadId,
                "md5" to fileHash,
                "version" to requestClientver,
                "userid" to userid,
                "token" to token,
                "if_id3" to 1,
                "dfid" to dfid,
                "mid" to mid,
                "uuid" to uuid,
                "appid" to requestAppid,
                "clientver" to requestClientver,
                "clienttime" to (currentTimeMillis() / 1000)
            ))

            val completeResp = httpClient.post("http:///v3/multipart/complete") {
                completeParams.forEach { (k, v) -> parameter(k, v) }
                header("Authorization", authorization)
            }
            val completeBody = completeResp.bodyAsText().let {
                Json.parseToJsonElement(it) as? JsonObject ?: buildJsonObject { put("raw", it) }
            }

            if (completeBody["status"]?.jsonPrimitive?.intOrNull != 1) {
                httpClient.close()
                return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "完成上传失败")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )
            }

            bssFileHash = completeBody["data"]?.jsonObject?.get("x-bss-filename")?.jsonPrimitive?.content ?: bssFileHash

            // ========== 步骤5 添加文件到云盘 ==========
            val addData = buildJsonObject {
                putJsonArray("data") {
                    addJsonObject {
                        put("name", name)
                        put("ext", actualExtendname)
                        put("author_name", actualAuthorName)
                        put("hash", bssFileHash)
                        put("hash_std", actualHashStd)
                        put("audio_id", actualAudioId)
                        put("bitrate", bitrate)
                        put("album_audio_id", actualAlbumAudioId)
                        put("size", fileData.size)
                        put("timelen", timelen)
                    }
                }
                put("list_ver", 0)
            }

            // AES 加密
            val (aesEncrypted, aesKey) = Crypto.aesEncryptAuto(addData.toString())

            // RSA 加密
            val rsaInput = buildJsonObject {
                put("aes", aesKey)
                put("uid", userid)
                put("token", token)
            }.toString()
            val p = Crypto.rsaEncrypt(rsaInput.encodeToByteArray(), Crypto.activePublicRasKey(executor.config)).uppercase()

            val addParams = mapOf(
                "clienttime" to clienttime,
                "mid" to mid,
                "key" to RequestSigner(executor.config).signParamsKey(clienttime.toString()),
                "clientver" to requestClientver,
                "appid" to requestAppid,
                "p" to p
            )

            val addResp = httpClient.post("https://mcloudservice.kugou.com/v1/add_files") {
                addParams.forEach { (k, v) -> parameter(k, v) }
                setBody(Crypto.decodeBase64(aesEncrypted))
                contentType(ContentType.Application.OctetStream)
            }
            val addRespBytes = addResp.readRawBytes()

            httpClient.close()

            // 尝试解密响应
            val responseBody = try {
                val decrypted = Crypto.aesDecryptBase64(Crypto.encodeBase64(addRespBytes), aesKey.substring(0, 32), aesKey.substring(16, 32))
                Json.parseToJsonElement(decrypted) as? JsonObject ?: buildJsonObject { put("raw", decrypted) }
            } catch (_: Exception) {
                try {
                    Json.parseToJsonElement(addRespBytes.decodeToString()) as? JsonObject
                        ?: buildJsonObject { put("raw", addRespBytes.decodeToString()) }
                } catch (_: Exception) {
                    buildJsonObject { put("raw", addRespBytes.decodeToString()) }
                }
            }

            return KuGouResponse(
                status = 200,
                body = buildJsonObject {
                    responseBody.forEach { (k, v) -> put(k, v) }
                    putJsonObject("uploadInfo") {
                        put("authorization", authorization)
                        put("external_host", externalHost)
                        put("upload_id", uploadId)
                        put("hash", bssFileHash)
                        put("local_hash", fileHash)
                        put("hash_std", actualHashStd)
                        put("audio_id", actualAudioId)
                        put("album_audio_id", actualAlbumAudioId)
                        put("matched", matchInfo != null)
                        put("filesize", fileData.size)
                    }
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        } catch (e: Exception) {
            return KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", e.message ?: "上传失败")
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        }
    }

    // 匹配信息数据类
    private data class MatchInfo(
        val albumAudioId: Long = 0,
        val audioId: Long = 0,
        val hashStd: String = "",
        val authorName: String = "",
        val audioName: String = ""
    )

    // 从响应中提取匹配信息
    private fun extractMatchInfo(body: JsonObject): MatchInfo? {
        val match = body["match"]?.jsonObject
            ?: body["match_list"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: body["data"]?.jsonArray?.firstOrNull()?.jsonArray?.firstOrNull()?.jsonObject

        if (match == null) return null

        val audioInfo = match["audio_info"]?.jsonObject
        val albumAudioId = match["album_audio_id"]?.jsonPrimitive?.longOrNull ?: 0
        val audioId = audioInfo?.get("audio_id")?.jsonPrimitive?.longOrNull
            ?: match["audio_id"]?.jsonPrimitive?.longOrNull ?: 0
        val hashStd = audioInfo?.get("hash")?.jsonPrimitive?.contentOrNull
            ?: match["hash"]?.jsonPrimitive?.contentOrNull ?: ""

        if (albumAudioId == 0L && audioId == 0L && hashStd.isEmpty()) return null

        return MatchInfo(
            albumAudioId = albumAudioId,
            audioId = audioId,
            hashStd = hashStd,
            authorName = match["author_name"]?.jsonPrimitive?.contentOrNull ?: "",
            audioName = match["ori_audio_name"]?.jsonPrimitive?.contentOrNull
                ?: match["audio_name"]?.jsonPrimitive?.contentOrNull
                ?: match["songname"]?.jsonPrimitive?.contentOrNull ?: ""
        )
    }
}