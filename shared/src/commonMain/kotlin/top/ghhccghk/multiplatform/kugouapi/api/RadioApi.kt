package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 电台/FM 相关 API
 */
class RadioApi(private val executor: RequestExecutor) {

    /**
     * 获取分类 FM 歌曲
     */
    suspend fun getFmClass(): KuGouResponse {
        val dateNow = currentTimeMillis()
        val userId = executor.cookieJar.getUserid()
        
        return executor.execute(
            KuGouRequest(
                url = "/v1/class_fm_song",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("kguid", userId)
                    put("clienttime", dateNow)
                    put("mid", executor.cookieJar.getMid())
                    put("platform", "android")
                    put("clientver", executor.config.activeClientVersion)
                    put("uid", userId)
                    put("get_tracker", 1)
                    put("key", RequestSigner(executor.config).signParamsKey(dateNow))
                    put("appid", executor.config.activeAppId)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "fm.service.kugou.com")
            )
        )
    }

    /**
     * 获取推荐 FM 列表
     */
    suspend fun getFmRecommend(): KuGouResponse {
        val dateNow = currentTimeMillis()
        
        return executor.execute(
            KuGouRequest(
                url = "/v1/rcmd_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clientver", executor.config.activeClientVersion)
                    put("clienttime", dateNow)
                    put("mid", executor.cookieJar.getMid())
                    put("key", RequestSigner(executor.config).signParamsKey(dateNow))
                    put("rcmdsongcount", 1)
                    put("level", 0)
                    put("area_code", 1)
                    put("get_tracker", 1)
                    put("uid", 0)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "fm.service.kugou.com")
            )
        )
    }

    /**
     * 获取 FM 歌曲列表
     */
    suspend fun getFmSongs(
        fmId: String,
        type: Int = 2,
        offset: Int = -1,
        size: Int = 20
    ): KuGouResponse {
        val dateNow = currentTimeMillis()
        val ids = fmId.split(",").filter { it.isNotEmpty() }
        
        val fmData = buildJsonArray {
            ids.forEach { id ->
                addJsonObject {
                    put("fmid", id)
                    put("fmtype", type)
                    put("offset", offset)
                    put("size", size)
                    put("singername", "")
                }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/v1/app_song_list_offset",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("area_code", 1)
                    put("clienttime", dateNow)
                    put("clientver", executor.config.activeClientVersion)
                    put("data", fmData)
                    put("get_tracker", 1)
                    put("key", RequestSigner(executor.config).signParamsKey(dateNow))
                    put("mid", executor.cookieJar.getMid())
                    put("uid", executor.cookieJar.getUserid())
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "fm.service.kugou.com",
                    "Content-Type" to "application/json"
                )
            )
        )
    }

    /**
     * 获取 FM 信息/图片
     */
    suspend fun getFmImage(fmId: String): KuGouResponse {
        val dateNow = currentTimeMillis()
        val ids = fmId.split(",").filter { it.isNotEmpty() }
        
        val fmData = buildJsonArray {
            ids.forEach { id ->
                addJsonObject {
                    put("fields", "imgUrl100,imgUrl50")
                    put("fmid", id)
                    put("fmtype", 2)
                }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/v1/fm_info",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clienttime", dateNow)
                    put("clientver", executor.config.activeClientVersion)
                    put("data", fmData)
                    put("dfid", executor.cookieJar.getDfid())
                    put("key", RequestSigner(executor.config).signParamsKey(dateNow))
                    put("mid", executor.cookieJar.getMid())
                    
                    val userId = executor.cookieJar.getUserid()
                    if (userId != "0") put("userid", userId)
                    val token = executor.cookieJar.getToken()
                    if (token.isNotEmpty()) put("token", token)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "fm.service.kugou.com",
                    "Content-Type" to "application/json"
                )
            )
        )
    }

    /**
     * 获取 PC 电台 Banner
     */
    suspend fun getPcDiantai(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://adservice.kugou.com",
                url = "/v3/pc_diantai",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("isvip", 0)
                    put("userid", executor.cookieJar.getUserid())
                    put("vipType", 0)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
