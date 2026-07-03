package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 乐库相关 API
 *
 * 对齐 Node.js `yueku_*` 系列模块，提供乐库 Banner、FM、推荐等功能。
 */
class YuekuApi(private val executor: RequestExecutor) {

    /**
     * 获取乐库 Banner
     * 对齐 node module/yueku_banner.js
     */
    suspend fun getBanner(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/ads.gateway/v3/listen_banner",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("plat", 0)
                    put("channel", 201)
                    put("operator", 7)
                    put("networktype", 2)
                    put("userid", executor.cookieJar.getUserid())
                    put("vip_type", 0)
                    put("m_type", 0)
                    putJsonArray("tags") {}
                    put("apiver", 5)
                    put("ability", 2)
                    put("mode", "normal")
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取乐库 FM
     * 对齐 node module/yueku_fm.js
     */
    suspend fun getFm(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v1/time_fm_info",
                method = HttpMethod.GET,
                params = mapOf(
                    "operator" to 7,
                    "plat" to 0,
                    "type" to 11,
                    "area_code" to 1,
                    "req_multi" to 1
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "fm.service.kugou.com")
            )
        )
    }

    /**
     * 获取乐库主页推荐内容
     * 对齐 node module/yueku.js
     */
    suspend fun getRecommend(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v1/yueku/recommend_v2",
                method = HttpMethod.GET,
                params = mapOf(
                    "operator" to 7,
                    "plat" to 0,
                    "type" to 11,
                    "area_code" to 1,
                    "req_multi" to 1
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "service.mobile.kugou.com")
            )
        )
    }
}
