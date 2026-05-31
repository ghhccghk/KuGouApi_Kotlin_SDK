package top.ghhccghk.multiplatform.kugouapi.core

import android.os.Build


class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
