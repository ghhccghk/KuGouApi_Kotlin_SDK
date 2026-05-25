package com.ghhccghk.multiplatform.kugouapi.api

import com.ghhccghk.multiplatform.kugouapi.core.*
import com.ghhccghk.multiplatform.kugouapi.model.*
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
}
