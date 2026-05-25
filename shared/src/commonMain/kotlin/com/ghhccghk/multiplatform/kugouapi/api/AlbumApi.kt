package com.ghhccghk.multiplatform.kugouapi.api

import com.ghhccghk.multiplatform.kugouapi.core.*
import com.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 专辑相关 API
 */
class AlbumApi(private val executor: RequestExecutor) {

    /**
     * 获取专辑元数据 (批量)
     * 对齐 module/album.js
     *
     * @param albumId 多个用逗号分隔
     */
    suspend fun getAlbumMetadata(albumId: String, fields: String = ""): KuGouResponse {
        val ids = albumId.split(",").filter { it.isNotEmpty() }
        val data = buildJsonArray {
            ids.forEach { id ->
                addJsonObject {
                    put("album_id", id)
                    put("album_name", "")
                    put("author_name", "")
                }
            }
        }

        val dateNow = currentTimeMillis()
        return executor.execute(
            KuGouRequest(
                baseUrl = "http://kmr.service.kugou.com",
                url = "/v1/album",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clienttime", dateNow)
                    put("clientver", executor.config.activeClientVersion)
                    put("data", data)
                    put("dfid", executor.cookieJar.getDfid())
                    put("fields", fields)
                    put("key", RequestSigner(executor.config).signParamsKey(dateNow))
                    put("mid", executor.cookieJar.getMid())
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "kmr.service.kugou.com"
                )
            )
        )
    }

    /**
     * 获取专辑详情
     * 对齐 module/album_detail.js
     */
    suspend fun getAlbumDetail(id: String, isBuy: Int = 0): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/kmr/v2/albums",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("data", buildJsonArray {
                        addJsonObject { put("album_id", id) }
                    })
                    put("is_buy", isBuy)
                    put("fields", "album_id,album_name,publish_date,sizable_cover,intro,language,is_publish,heat,type,quality,authors,exclusive,author_name,trans_param")
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "openapi.kugou.com",
                    "kg-tid" to "255"
                )
            )
        )
    }

    /**
     * 获取唱片店分类数据
     * 对齐 module/album_shop.js
     */
    suspend fun getAlbumShop(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/zhuanjidata/v3/album_shop_v2/get_classify_data",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取专辑下的音乐列表
     * 对齐 module/album_songs.js
     */
    suspend fun getAlbumSongs(
        id: String,
        page: Int = 1,
        pageSize: Int = 30,
        isBuy: String = ""
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v1/album_audio/lite",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("album_id", id)
                    put("is_buy", isBuy)
                    put("page", page)
                    put("pagesize", pageSize)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "openapi.kugou.com",
                    "kg-tid" to "255"
                )
            )
        )
    }
}
