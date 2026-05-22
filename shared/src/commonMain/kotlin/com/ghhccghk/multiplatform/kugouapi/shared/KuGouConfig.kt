package com.ghhccghk.multiplatform.kugouapi.shared

data class KuGouConfig(
    val appId: Int = 1005,
    val clientVersion: Int = 20489,
    val liteAppId: Int = 3116,
    val liteClientVersion: Int = 11440,
    val isLite: Boolean = false,
    val defaultBaseUrl: String = "https://gateway.kugou.com",
    val timeoutMs: Long = 30_000,
    val userAgent: String = "Android15-1070-11083-46-0-DiscoveryDRADProtocol-wifi",
) {
    internal val activeAppId: Int get() = if (isLite) liteAppId else appId
    internal val activeClientVersion: Int get() = if (isLite) liteClientVersion else clientVersion

    internal val androidSignatureSalt: String
        get() = if (isLite) "LnT6xpN3khm36zse0QzvmgTZ3waWdRSA" else "OIlwieks28dk2k092lksi2UIkp"

    internal val webSignatureSalt: String = "NVPh5oo715z5DIWAeQlhMDsWXXQV4hwt"

    internal val signKeySalt: String
        get() = if (isLite) "185672dd44712f60bb1736df5a377e82" else "57ae12eb6890223e355ccfcb74edf70d"

    companion object {
        val WX_APP_ID = "wx79f2c4418704b4f8"
        val WX_LITE_APP_ID = "wx72b795aca60ad321"
        val WX_SECRET = "4efcab88b700769e376e3f6087b8abc9"
        val WX_LITE_SECRET = "33e486041e5e25729a4e3d2da7502f9a"
        val SRC_APP_ID = 2919
        val API_VER = 20
    }
}
