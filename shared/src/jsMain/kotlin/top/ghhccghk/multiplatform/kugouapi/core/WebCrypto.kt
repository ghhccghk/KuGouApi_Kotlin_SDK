@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
package top.ghhccghk.multiplatform.kugouapi.core

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array

external object crypto {
    val subtle: SubtleCrypto
}

external interface CryptoKey

external interface AlgorithmIdentifier

external interface RsaHashedImportParams : AlgorithmIdentifier {
    var name: String
    var hash: String
}

external interface RsaOaepParams : AlgorithmIdentifier {
    var name: String
}

external interface SubtleCrypto {
    fun importKey(
        format: String,
        keyData: ArrayBuffer,
        algorithm: AlgorithmIdentifier,
        extractable: Boolean,
        keyUsages: Array<String>
    ): Promise<CryptoKey>

    fun encrypt(
        algorithm: AlgorithmIdentifier,
        key: CryptoKey,
        data: dynamic
    ): Promise<ArrayBuffer>
}

external class Promise<T> {
    fun <R> then(onFulfilled: (T) -> R): Promise<R>
}