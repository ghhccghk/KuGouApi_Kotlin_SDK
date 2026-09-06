package top.ghhccghk.multiplatform.kugouapi.api

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
}
