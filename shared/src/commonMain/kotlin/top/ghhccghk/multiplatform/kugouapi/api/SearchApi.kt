package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

class SearchApi(private val executor: RequestExecutor) {

    /**
     * 综合搜索
     *
     * @param keywords 搜索关键词
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 30
     * @param type 搜索类型: song, special, lyric, album, author, mv
     */
    suspend fun search(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
        type: SearchType = SearchType.SONG,
        iscorrection: Int = 1,
        privilegeFilter: Int = 0,
        areaCode: Int = 1,
        tag: String = "",
        category: String = "1",
        sortType: Int = 0,
        searchSong: Int = 0,
    ): KuGouResponse {
        val isLite = executor.config.isLite

        // 歌曲搜索：/v2/search/song（概念版 clientver=201）
        if (type == SearchType.SONG) {
            val params = linkedMapOf<String, Any?>(
                "keyword" to keywords,
                "page" to page,
                "pagesize" to pageSize,
                "platform" to "AndroidFilter",
                "iscorrection" to iscorrection,
                "privilegefilter" to privilegeFilter,
                "area_code" to areaCode,
                "dopicfull" to 1,
            )
            if (tag.isNotEmpty()) params["tag"] = tag
            if (isLite) params["clientver"] = 201

            return executor.execute(
                KuGouRequest(
                    url = "/v2/search/song",
                    method = HttpMethod.GET,
                    params = params,
                    encryptType = EncryptType.ANDROID,
                    headers = mapOf("x-router" to "complexsearch.kugou.com"),
                )
            )
        }

        // 专辑搜索：/v1/search/album
        if (type == SearchType.ALBUM) {
            val params = linkedMapOf<String, Any?>(
                "keyword" to keywords,
                "page" to page,
                "pagesize" to pageSize,
                "platform" to "AndroidFilter",
                "iscorrection" to iscorrection,
                "category" to category,
                "sorttype" to if (sortType == 1) 1 else 0,
                "searchsong" to if (searchSong == 1) 1 else 0,
            )
            if (tag.isNotEmpty()) params["tag"] = tag
            if (isLite) params["clientver"] = 201

            return executor.execute(
                KuGouRequest(
                    url = "/v1/search/album",
                    method = HttpMethod.GET,
                    params = params,
                    encryptType = EncryptType.ANDROID,
                    headers = mapOf("x-router" to "complexsearch.kugou.com"),
                )
            )
        }

        // 其余类型：/v1/search/{type}
        return executor.execute(
            KuGouRequest(
                url = "/v1/search/${type.value}",
                method = HttpMethod.GET,
                params = mapOf(
                    "albumhide" to 0,
                    "iscorrection" to 1,
                    "keyword" to keywords,
                    "nocollect" to 0,
                    "page" to page,
                    "pagesize" to pageSize,
                    "platform" to "AndroidFilter",
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "complexsearch.kugou.com",
                    "kg-tid" to "1"
                ),
            )
        )
    }

    /**
     * 获取搜索默认关键词
     */
    suspend fun searchDefault(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v1/search/default",
                method = HttpMethod.GET,
                headers = mapOf(
                    "x-router" to "complexsearch.kugou.com",
                ),
            )
        )
    }

    /**
     * 热搜列表
     */
    suspend fun searchHot(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/api/v3/search/hot_tab",
                method = HttpMethod.GET,
                params = mapOf(
                    "navid" to 1,
                    "plat" to 2,
                ),
                headers = mapOf(
                    "x-router" to "msearch.kugou.com",
                ),
            )
        )
    }

    /**
     * 搜索建议
     */
    suspend fun searchSuggest(
        keywords: String,
        albumTipCount: Long = 10,
        correctTipCount: Long = 10,
        mvTipCount: Long = 10,
        musicTipCount: Long = 10,
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v2/getSearchTip",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID,
                params = mapOf(
                    "keyword" to keywords,
                    "AlbumTipCount" to albumTipCount,
                    "CorrectTipCount" to correctTipCount ,
                    "MVTipCount" to mvTipCount,
                    "MusicTipCount" to musicTipCount ,
                    "radiotip" to 1
                ),
                headers = mapOf(
                    "x-router" to "searchtip.kugou.com",
                ),
            )
        )
    }

    /**
     * 综合搜索 (complex)
     */
    suspend fun searchComplex(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v2/search/complex",
                method = HttpMethod.GET,
                params = mapOf(
                    "keyword" to keywords,
                    "page" to page,
                    "pagesize" to pageSize,
                ),
                headers = mapOf(
                    "x-router" to "complexsearch.kugou.com",
                ),
            )
        )
    }

    /**
     * 歌词搜索
     */
    suspend fun searchLyric(
        keywords: String = "",
        hash: String = "",
        albumAudioId: Long = 0L,
        man: String = "no",
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://lyrics.kugou.com",
                url = "/v1/search",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID,
                params = mapOf(
                    "keyword" to keywords,
                    "album_audio_id" to albumAudioId,
                    "appid" to executor.config.activeAppId,
                    "clientver" to executor.config.activeClientVersion,
                    "duration" to 0,
                    "hash" to hash,
                    "man" to man,
                    "lrctxt" to 1,
                ),
                clearDefaultParams = true
            )
        )
    }

    /**
     * 混合综合搜索
     * 对齐 module/search_mixed.js
     */
    suspend fun searchMixed(
        keyword: String,
        areaCode: Int = 1,
        iscorrection: Int = 1,
        cursor: Int = 0,
        clientver: Int = 201,
        vipStatus: Int = 0,
    ): KuGouResponse {
        val time = currentTimeMillis()
        val isLite = executor.config.isLite

        // 概念版使用 AndroidFilter 参数；标准版保留 IOSFilter 参数
        val params: Map<String, Any?> = if (isLite) {
            mapOf(
                "keyword" to keyword,
                "platform" to "AndroidFilter",
                "tag" to "em",
                "area_code" to areaCode,
                "iscorrection" to iscorrection,
                "cursor" to cursor,
                "apiver" to 22,
                "osversion" to "15",
                "ability" to 1,
                "clientver" to clientver,
                "vip_status" to vipStatus,
            )
        } else {
            val requestId = "${Crypto.md5("bdaa53d04e7475feb9024164a47032f9$time")}_0"
            mapOf(
                "ab_tag" to 0,
                "ability" to 511,
                "albumhide" to 0,
                "apiver" to 22,
                "area_code" to 1,
                "clientver" to if (clientver == 201) 20125 else clientver,
                "cursor" to 0,
                "is_gpay" to 0,
                "iscorrection" to 1,
                "keyword" to keyword,
                "nocollect" to 0,
                "osversion" to 16.5,
                "platform" to "IOSFilter",
                "recver" to 2,
                "req_ai" to 1,
                "requestid" to requestId,
                "search_ability" to 3,
                "sec_aggre" to 1,
                "sec_aggre_bitmap" to 0,
                "style_type" to 3,
                "tag" to "em"
            )
        }

        return executor.execute(
            KuGouRequest(
                url = "/v3/search/mixed",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "complexsearch.kugou.com",
                    "kg-clienttimems" to time.toString()
                )
            )
        )
    }
}
