package com.ghhccghk.multiplatform.kugouapi.search

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(

    val status: Int,

    val data: SearchData
)

@Serializable
data class SearchData(

    val info: List<SongInfo>
)

@Serializable
data class SongInfo(

    val hash: String,

    val songname: String
)