package top.ghhccghk.multiplatform.kugouapi.core

import kotlin.js.Date

class JsPlatform : Platform {
    override val name: String = "JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun currentTimeMillis(): Long = Date.now().toLong()
