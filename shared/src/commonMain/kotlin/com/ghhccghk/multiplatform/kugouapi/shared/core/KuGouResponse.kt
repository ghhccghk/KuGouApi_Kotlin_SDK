package com.ghhccghk.multiplatform.kugouapi.shared.core

import kotlinx.serialization.json.JsonObject

data class KuGouResponse(
    val status: Int,
    val body: JsonObject,
    val cookies: Map<String, String>,
    val headers: Map<String, String>,
)
