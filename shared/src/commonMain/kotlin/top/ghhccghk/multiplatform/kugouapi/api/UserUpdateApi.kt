package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 用户资料修改 API
 * 提供修改个人资料、头像等功能。
 * 对齐 module/user_update.js, user_update_avatar.js
 */
class UserUpdateApi(private val executor: RequestExecutor) {

    /**
     * 修改个人资料
     *
     * @param nickname 昵称
     * @param sex 性别，0=女，1=男，2=保密
     * @param birthday 生日，格式 yyyy-MM-dd
     * @param photo 头像文件名
     * @param province 省份
     * @param city 城市
     * @param memo 备注
     * @param signature 个性签名
     * @param tags 标签
     */
    suspend fun updateUserInfo(
        nickname: String? = null,
        sex: Int? = null,
        birthday: String? = null,
        photo: String? = null,
        province: String? = null,
        city: String? = null,
        memo: String? = null,
        signature: String? = null,
        tags: String? = null
    ): KuGouResponse {
        val token = executor.cookieJar.getToken()
        val userid = executor.cookieJar.getUserid()
        val mid = executor.cookieJar.getMid()
        val clientTime = currentTimeMillis() / 1000

        val data = buildJsonObject {
            nickname?.let { put("nickname", it) }
            sex?.let { put("sex", it) }
            birthday?.let { put("birthday", it) }
            photo?.let { put("photo", it) }
            province?.let { put("province", it) }
            city?.let { put("city", it) }
            memo?.let { put("memo", it) }
            signature?.let { put("signature", it) }
            tags?.let { put("tags", it) }
        }

        if (data.isEmpty()) {
            return KuGouResponse(
                status = 400,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", "至少需要提供一个待修改字段")
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        }

        // RSA 加密
        val rsaInput = buildJsonObject {
            put("clienttime", clientTime)
            put("token", token)
        }.toString()
        val p = Crypto.rsaEncrypt(rsaInput.encodeToByteArray(), Crypto.activePublicRasKey(executor.config)).uppercase()

        val requestData = buildJsonObject {
            put("clienttime", clientTime)
            put("appid", executor.config.activeAppId)
            put("clientver", executor.config.activeClientVersion)
            put("mid", mid)
            put("uuid", "-")
            put("userid", userid)
            put("key", signParamsKey(clientTime))
            put("p", p)
            put("data", data)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://update.user.kugou.com",
                url = "/v1/update_userinfo",
                method = HttpMethod.POST,
                data = requestData,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true,
                notSignature = true
            )
        )
    }

    /**
     * 修改头像
     *
     * @param imgFile 图片数据，Base64 编码
     * @param filename 文件名，默认 avatar.jpg
     */
    suspend fun updateAvatar(
        imgFile: String,
        filename: String = "avatar.jpg"
    ): KuGouResponse {
        val token = executor.cookieJar.getToken()
        val userid = executor.cookieJar.getUserid()
        val mid = executor.cookieJar.getMid()
        val clientTime = currentTimeMillis() / 1000

        // RSA 加密
        val rsaInput = buildJsonObject {
            put("clienttime", clientTime)
            put("token", token)
        }.toString()
        val p = Crypto.rsaEncrypt(rsaInput.encodeToByteArray(), Crypto.activePublicRasKey(executor.config)).uppercase()

        val requestData = buildJsonObject {
            put("clienttime", clientTime)
            put("appid", executor.config.activeAppId)
            put("clientver", executor.config.activeClientVersion)
            put("mid", mid)
            put("uuid", "-")
            put("userid", userid)
            put("key", signParamsKey(clientTime))
            put("p", p)
            put("imgFile", imgFile)
            put("filename", filename)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = "http://update.user.kugou.com",
                url = "/v1/update_avatar",
                method = HttpMethod.POST,
                data = requestData,
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = true,
                notSignature = true
            )
        )
    }

    // 辅助函数：签名参数 key
    private fun signParamsKey(clientTime: Long): String {
        val str = "OIlwieks28dk2k092lksi2UIkp"
        return Crypto.md5("$clientTime$str${executor.config.activeAppId}")
    }
}
