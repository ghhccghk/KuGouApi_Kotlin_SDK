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
    ): KuGouResponse {
        val version = if (type == SearchType.SONG) "v3" else "v1"
        return executor.execute(
            KuGouRequest(
                url = "/$version/search/${type.value}",
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
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v1/search/suggest",
                method = HttpMethod.GET,
                params = mapOf(
                    "keyword" to keywords,
                ),
                headers = mapOf(
                    "x-router" to "complexsearch.kugou.com",
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
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v1/search/lyric",
                method = HttpMethod.GET,
                params = mapOf(
                    "keyword" to keywords,
                    "page" to page,
                    "pagesize" to pageSize,
                    "platform" to "AndroidFilter",
                    "albumhide" to 0,
                    "iscorrection" to 1,
                    "nocollect" to 0,
                ),
                headers = mapOf(
                    "x-router" to "complexsearch.kugou.com",
                ),
            )
        )
    }

    /**
     * 混合综合搜索
     * 对齐 module/search_mixed.js
     */
    suspend fun searchMixed(keyword: String): KuGouResponse {
        val time = currentTimeMillis()
        val requestId = "${Crypto.md5("bdaa53d04e7475feb9024164a47032f9$time")}_0"
        
        return executor.execute(
            KuGouRequest(
                url = "/v3/search/mixed",
                method = HttpMethod.GET,
                params = mapOf(
                    "ab_tag" to 0,
                    "ability" to 511,
                    "albumhide" to 0,
                    "apiver" to 22,
                    "area_code" to 1,
                    "clientver" to 20125,
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
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "complexsearch.kugou.com",
                    "kg-clienttimems" to time.toString()
                )
            )
        )
    }
}
