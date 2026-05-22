package com.ghhccghk.multiplatform.kugouapi.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object KuGouClient {

    val client = HttpClient {

        install(ContentNegotiation) {

            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }

        defaultRequest {

            header(
                "User-Agent",
                Headers.USER_AGENT
            )

            header(
                "Referer",
                "https://www.kugou.com/"
            )
        }
    }
}