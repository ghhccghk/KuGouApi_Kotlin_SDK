package com.ghhccghk.multiplatform.kugouapi

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}