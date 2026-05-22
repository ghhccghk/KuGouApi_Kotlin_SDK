package com.ghhccghk.multiplatform.kugouapi

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform