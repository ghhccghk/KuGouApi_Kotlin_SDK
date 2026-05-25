package com.ghhccghk.multiplatform.kugouapi

import com.ghhccghk.multiplatform.kugouapi.core.getPlatform

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}