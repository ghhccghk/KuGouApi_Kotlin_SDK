package com.ghhccghk.multiplatform.kugouapi.api

import com.ghhccghk.multiplatform.kugouapi.core.*
import com.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 歌手相关 API
 */
class ArtistApi(private val executor: RequestExecutor) {

    /**
     * 获取歌手详情
     * 对齐 module/artist_detail.js
     */
    suspend fun getDetail(id: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/kmr/v3/author",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("author_id", id)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "openapi.kugou.com",
                    "kg-tid" to "36"
                )
            )
        )
    }

    /**
     * 获取歌手专辑
     * 对齐 module/artist_albums.js
     */
    suspend fun getAlbums(
        id: String,
        page: Int = 1,
        pageSize: Int = 30,
        sort: ArtistSort = ArtistSort.HOT
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/kmr/v1/author/albums",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("author_id", id)
                    put("pagesize", pageSize)
                    put("page", page)
                    put("sort", if (sort == ArtistSort.HOT) 3 else 1)
                    put("category", 1)
                    put("area_code", "all")
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "openapi.kugou.com",
                    "kg-tid" to "36"
                )
            )
        )
    }

    /**
     * 获取歌手单曲
     * 对齐 module/artist_audios.js
     */
    suspend fun getAudios(
        id: String,
        page: Int = 1,
        pageSize: Int = 30,
        sort: ArtistSort = ArtistSort.HOT
    ): KuGouResponse {
        val dateNow = currentTimeMillis() / 1000
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://openapi.kugou.com",
                url = "/kmr/v1/audio_group/author",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clientver", executor.config.activeClientVersion)
                    put("mid", executor.cookieJar.getMid())
                    put("clienttime", dateNow)
                    put("key", RequestSigner(executor.config).signParamsKey(dateNow))
                    put("author_id", id)
                    put("pagesize", pageSize)
                    put("page", page)
                    put("sort", if (sort == ArtistSort.HOT) 1 else 2)
                    put("area_code", "all")
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "openapi.kugou.com",
                    "kg-tid" to "220"
                )
            )
        )
    }

    /**
     * 获取歌手 MV/视频
     * 对齐 module/artist_videos.js
     */
    suspend fun getVideos(
        id: String,
        page: Int = 1,
        pageSize: Int = 30,
        tag: ArtistVideoTag = ArtistVideoTag.ALL
    ): KuGouResponse {
        val tagMap = mapOf(
            ArtistVideoTag.OFFICIAL to 18,
            ArtistVideoTag.LIVE to 20,
            ArtistVideoTag.FAN to 23,
            ArtistVideoTag.ARTIST to 42419,
            ArtistVideoTag.ALL to ""
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://openapicdn.kugou.com",
                url = "/kmr/v1/author/videos",
                method = HttpMethod.GET,
                params = mapOf(
                    "author_id" to id,
                    "is_fanmade" to "",
                    "tag_idx" to (tagMap[tag] ?: ""),
                    "pagesize" to pageSize,
                    "page" to page
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 歌手列表
     * 对齐 module/artist_lists.js
     */
    suspend fun getLists(
        musician: Int = 0,
        sexType: ArtistSexType = ArtistSexType.ALL,
        regionType: ArtistRegionType = ArtistRegionType.ALL,
        hotSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/ocean/v6/singer/list",
                method = HttpMethod.GET,
                params = mapOf(
                    "musician" to musician,
                    "sextype" to sexType.value,
                    "showtype" to 2,
                    "type" to regionType.value,
                    "hotsize" to hotSize
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 关注歌手
     * 对齐 module/artist_follow.js
     */
    suspend fun follow(id: String): KuGouResponse {
        val singerId = id.toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()
        val userId = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val clientTime = currentTimeMillis() / 1000

        // 1. AES 加密
        val encryptInput = buildJsonObject {
            put("singerid", singerId)
            put("token", token)
        }.toString()
        val (encryptedParams, tempKey) = Crypto.aesEncryptAuto(encryptInput)

        // 2. RSA 加密
        val rsaInput = buildJsonObject {
            put("clienttime", clientTime)
            put("key", tempKey)
        }.toString().encodeToByteArray()
        val p = Crypto.rsaEncryptPkcs1(rsaInput, Crypto.publicRasKey)

        return executor.execute(
            KuGouRequest(
                url = "/followservice/v3/follow_singer",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("plat", 0)
                    put("userid", userId)
                    put("singerid", singerId)
                    put("source", 7)
                    put("p", p)
                    put("params", encryptedParams)
                },
                params = mapOf("clienttime" to clientTime),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 取消关注歌手
     * 对齐 module/artist_unfollow.js
     */
    suspend fun unfollow(id: String): KuGouResponse {
        val singerId = id.toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()
        val userId = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val clientTime = currentTimeMillis() / 1000

        val encryptInput = buildJsonObject {
            put("singerid", singerId)
            put("token", token)
        }.toString()
        val (encryptedParams, tempKey) = Crypto.aesEncryptAuto(encryptInput)

        val rsaInput = buildJsonObject {
            put("clienttime", clientTime)
            put("key", tempKey)
        }.toString().encodeToByteArray()
        val p = Crypto.rsaEncryptPkcs1(rsaInput, Crypto.publicRasKey)

        return executor.execute(
            KuGouRequest(
                url = "/followservice/v3/unfollow_singer",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("plat", 0)
                    put("userid", userId)
                    put("singerid", singerId)
                    put("source", 7)
                    put("p", p)
                    put("params", encryptedParams)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取关注歌手新歌
     * 对齐 module/artist_follow_newsongs.js
     */
    suspend fun getFollowNewSongs(
        lastAlbumId: Long = 0,
        pageSize: Int = 30,
        sort: FollowNewSongsSort = FollowNewSongsSort.TIME
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/feed/v1/follow/newsong_album_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("last_album_id", lastAlbumId)
                },
                params = mapOf(
                    "last_album_id" to lastAlbumId,
                    "page_size" to pageSize,
                    "opt_sort" to sort.value
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 歌手荣誉详情
     * 对齐 module/artist_honour.js
     */
    suspend fun getHonour(id: String, page: Int = 1, pageSize: Int = 30): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "http://h5activity.kugou.com",
                url = "/v1/query_singer_honour_detail",
                method = HttpMethod.POST,
                params = mapOf(
                    "singer_id" to id,
                    "pagesize" to pageSize,
                    "page" to page
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
