package com.ghhccghk.multiplatform.kugouapi.search

import com.ghhccghk.multiplatform.kugouapi.network.KuGouClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class SearchApi {

    suspend fun search(
        keyword: String,
        page: Int = 1,
        pageSize: Int = 30
    ): SearchResponse {

        return KuGouClient.client.get(
            "https://mobiles.kugou.com/api/v3/search/song"
        ) {

            parameter("keyword", keyword)

            parameter("page", page)

            parameter("pagesize", pageSize)
        }.body()
    }
}