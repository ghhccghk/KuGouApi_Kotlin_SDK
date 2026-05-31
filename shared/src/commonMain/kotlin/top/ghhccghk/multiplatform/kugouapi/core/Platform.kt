package top.ghhccghk.multiplatform.kugouapi.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
/** Returns current time in epoch milliseconds. */
expect fun currentTimeMillis(): Long
