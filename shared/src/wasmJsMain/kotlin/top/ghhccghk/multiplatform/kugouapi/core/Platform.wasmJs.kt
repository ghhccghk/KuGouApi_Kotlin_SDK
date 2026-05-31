package top.ghhccghk.multiplatform.kugouapi.core

import kotlin.js.ExperimentalWasmJsInterop

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun currentTimeMillis(): Long = jsDateNow().toLong()
