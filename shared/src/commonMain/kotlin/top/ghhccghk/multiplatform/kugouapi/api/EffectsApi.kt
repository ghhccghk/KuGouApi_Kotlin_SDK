package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 音效相关 API
 * 提供耳机音效、明星音效、汽车音效等功能。
 * 对齐 module/effects_*.js
 */
class EffectsApi(private val executor: RequestExecutor) {

    /**
     * 获取耳机品牌列表
     *
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 30
     */
    suspend fun getEffectsBrand(
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val params = mapOf(
            "sort" to 1,
            "pagesize" to pageSize,
            "page" to page
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://mobilecdngz.kugou.com",
                url = "/api/v5/earphone/get_brand",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 获取耳机品牌音效详情
     *
     * @param brandId 品牌 ID
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 30
     */
    suspend fun getEffectsBrandDetail(
        brandId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val params = mapOf(
            "brand_id" to brandId,
            "page" to page,
            "pagesize" to pageSize
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://mobilecdngz.kugou.com",
                url = "/api/v5/earphone/get_brand_detail",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 获取通用耳机音效
     */
    suspend fun getEffectsMatch(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "http://mobilecdngz.kugou.com",
                url = "/api/v5/earphone/match",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 获取明星音效列表
     *
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 30
     */
    suspend fun getEffectsArtist(
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val params = mapOf(
            "page" to page,
            "pagesize" to pageSize
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://mobilecdngz.kugou.com",
                url = "/api/v5/earphone/get_artist",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 获取汽车品牌列表
     */
    suspend fun getEffectsCarBrand(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "http://mobilecdngz.kugou.com",
                url = "/api/v5/earphone/get_car_brand",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }

    /**
     * 获取汽车品牌音效列表
     *
     * @param relId 汽车 ID
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 30
     */
    suspend fun getEffectsCarBrandList(
        relId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val params = mapOf(
            "rel_id" to relId,
            "page" to page,
            "pagesize" to pageSize
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://mobilecdngz.kugou.com",
                url = "/api/v5/earphone/get_car_brand_list",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true
            )
        )
    }
}
