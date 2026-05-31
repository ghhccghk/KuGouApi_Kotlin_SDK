package top.ghhccghk.multiplatform.kugouapi

import top.ghhccghk.multiplatform.kugouapi.core.getPlatform

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}