package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 用户已购内容 API
 * 提供已购单曲、已购专辑等功能。
 * 对齐 module/user_purchased_songs.js, user_purchased_albums.js
 */
class UserPurchasedApi(private val executor: RequestExecutor) {

    /**
     * 获取已购单曲
     *
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 50
     */
    suspend fun getPurchasedSongs(
        page: Int = 1,
        pageSize: Int = 50
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        val data = buildJsonObject {
            put("appid", executor.config.activeAppId)
            put("userid", userid)
            put("token", token)
            put("page", page)
            put("pagesize", pageSize)
            put("clientver", executor.config.activeClientVersion.toString())
            put("deleted", 0)
            put("need_audio_info", 1)
            put("area_code", "1")
        }

        return executor.execute(
            KuGouRequest(
                url = "/openapi/copyright/v1/audio/get_goods",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取已购专辑
     *
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 50
     */
    suspend fun getPurchasedAlbums(
        page: Int = 1,
        pageSize: Int = 50
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        val data = buildJsonObject {
            put("appid", executor.config.activeAppId)
            put("userid", userid)
            put("token", token)
            put("page", page)
            put("pagesize", pageSize)
            put("clientver", executor.config.activeClientVersion.toString())
            put("deleted", 0)
            put("need_audio_info", 1)
            put("area_code", "1")
            put("type", 1) // 1 表示专辑
        }

        return executor.execute(
            KuGouRequest(
                url = "/openapi/copyright/v1/audio/get_goods",
                method = HttpMethod.POST,
                data = data,
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
