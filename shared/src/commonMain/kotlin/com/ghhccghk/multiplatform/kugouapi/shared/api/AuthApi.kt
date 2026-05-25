package com.ghhccghk.multiplatform.kugouapi.shared.api

import com.ghhccghk.multiplatform.kugouapi.shared.KuGouConfig
import com.ghhccghk.multiplatform.kugouapi.shared.core.*
import com.ghhccghk.multiplatform.kugouapi.shared.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 认证与身份 API
 * 提供设备注册、密码登录、手机验证码登录、Token 刷新、二维码登录等功能。
 */
class AuthApi(private val executor: RequestExecutor) {

    // ============================================================
    //  设备注册
    //  对齐 module/register_dev.js
    // ============================================================

    /**
     * 注册设备以获取设备唯一标识 dfid。
     *
     * 该接口是访问酷狗大部分 API 的前提，返回的 dfid 会自动存储在 [CookieJar] 中供后续请求使用。
     * 逻辑完整对齐：https://github.com/MakcRe/KuGouMusicApi/blob/main/module/register_dev.js
     *
     * @param availableRamSize 可用内存大小 (字节)
     * @param availableRomSize 可用存储大小 (字节)
     * @param availableSDSize 可用 SD 卡大小 (字节)
     * @param basebandVer 基带版本
     * @param batteryLevel 电池电量 (0-100)
     * @param batteryStatus 电池状态 (如：2 为充电中，3 为放电中)
     * @param brand 手机品牌 (如：Redmi, Xiaomi)
     * @param buildSerial 编译流水号
     * @param device 设备代号 (如：marble)
     * @param manufacturer 制造商 (如：Xiaomi)
     * @param accelerometer 是否支持加速度计
     * @param accelerometerValue 加速度计当前值
     * @param gravity 是否支持重力感应
     * @param gravityValue 重力感应当前值
     * @param gyroscope 是否支持陀螺仪
     * @param gyroscopeValue 陀螺仪当前值
     * @param light 是否支持光感
     * @param lightValue 光感当前值
     * @param magnetic 是否支持磁感
     * @param magneticValue 磁感当前值
     * @param orientation 是否支持方向感应
     * @param orientationValue 方向感应当前值
     * @param pressure 是否支持压力感应
     * @param pressureValue 压力感应当前值
     * @param step_counter 是否支持步数统计
     * @param step_counterValue 步数统计当前值
     * @param temperature 是否支持温度感应
     * @param temperatureValue 温度感应当前值
     */
    suspend fun registerDev(
        availableRamSize: Long = 4983533568L,
        availableRomSize: Long = 48114719L,
        availableSDSize: Long = 48114717L,
        basebandVer: String = "",
        batteryLevel: Int = 100,
        batteryStatus: Int = 3,
        brand: String = "Redmi",
        buildSerial: String = "unknown",
        device: String = "marble",
        manufacturer: String = "Xiaomi",
        accelerometer: Boolean = false,
        accelerometerValue: String = "",
        gravity: Boolean = false,
        gravityValue: String = "",
        gyroscope: Boolean = false,
        gyroscopeValue: String = "",
        light: Boolean = false,
        lightValue: String = "",
        magnetic: Boolean = false,
        magneticValue: String = "",
        orientation: Boolean = false,
        orientationValue: String = "",
        pressure: Boolean = false,
        pressureValue: String = "",
        step_counter: Boolean = false,
        step_counterValue: String = "",
        temperature: Boolean = false,
        temperatureValue: String = "",
    ): KuGouResponse {
        val guid = executor.cookieJar.getGuid()
        val useridStr = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        // 1. 准备设备指纹数据
        val dataMap = buildJsonObject {
            put("availableRamSize", availableRamSize)
            put("availableRomSize", availableRomSize)
            put("availableSDSize", availableSDSize)
            put("basebandVer", basebandVer)
            put("batteryLevel", batteryLevel)
            put("batteryStatus", batteryStatus)
            put("brand", brand)
            put("buildSerial", buildSerial)
            put("device", device)
            put("imei", guid)
            put("imsi", "")
            put("manufacturer", manufacturer)
            put("uuid", guid)
            put("accelerometer", accelerometer)
            put("accelerometerValue", accelerometerValue)
            put("gravity", gravity)
            put("gravityValue", gravityValue)
            put("gyroscope", gyroscope)
            put("gyroscopeValue", gyroscopeValue)
            put("light", light)
            put("lightValue", lightValue)
            put("magnetic", magnetic)
            put("magneticValue", magneticValue)
            put("orientation", orientation)
            put("orientationValue", orientationValue)
            put("pressure", pressure)
            put("pressureValue", pressureValue)
            put("step_counter", step_counter)
            put("step_counterValue", step_counterValue)
            put("temperature", temperature)
            put("temperatureValue", temperatureValue)
        }

        // 2. AES 加密 (playlistAesEncrypt)
        val aesKeyBase = PlatformIdentity.generateRandomString(6).lowercase()
        val md5Key = Crypto.md5(aesKeyBase)
        val encryptKey = md5Key.substring(0, 16)
        val iv = md5Key.substring(16, 32)
        val encryptedData = Crypto.aesEncryptBase64(dataMap.toString(), encryptKey, iv)

        // 3. RSA 加密 (rsaEncrypt2)
        val rsaData = buildJsonObject {
            put("aes", aesKeyBase)
            put("uid", useridStr.toLongOrNull() ?: 0L)
            put("token", token)
        }.toString().encodeToByteArray()
        val p = Crypto.rsaEncryptPkcs1(rsaData, Crypto.publicRasKey)

        // 4. 发起网络请求
        val response = executor.execute(
            KuGouRequest(
                baseUrl = "https://userservice.kugou.com",
                url = "/risk/v2/r_register_dev",
                method = HttpMethod.POST,
                data = encryptedData,
                params = mapOf("part" to 1, "platid" to 1, "p" to p),
                encryptType = EncryptType.ANDROID,
                responseType = ResponseType.BYTES,
            )
        )

        // 5. 解析并解密响应
        if (response.status == 200) {
            val bytesStr = response.body["bytes"]?.jsonPrimitive?.content ?: ""
            if (bytesStr.isNotEmpty()) {
                try {
                    val bytes = bytesStr.split(",").map { it.toByte() }.toByteArray()
                    val responseBase64 = Crypto.encodeBase64(bytes)
                    val decryptedJson = Crypto.aesDecryptBase64(responseBase64, encryptKey, iv)
                    val body = Json.parseToJsonElement(decryptedJson) as JsonObject
                    val data = body["data"]?.jsonObject ?: buildJsonObject {}
                    val dfid = data["dfid"]?.jsonPrimitive?.content
                    if (dfid != null) {
                        executor.cookieJar["dfid"] = dfid
                    }
                    val mergedBody = buildJsonObject {
                        body.forEach { (k, v) -> if (k != "data") put(k, v) }
                        put("data", data)
                    }
                    return response.copy(body = mergedBody)
                } catch (e: Exception) {
                    return response.copy(body = buildJsonObject {
                        put("status", 0)
                        put("error_code", -1)
                        put("msg", "解密失败: ${e.message}")
                    })
                }
            }
        }

        return response
    }

    // ============================================================
    //  发送验证码
    //  对齐 module/captcha_sent.js
    // ============================================================

    /**
     * 发送手机验证码（用于 [loginByPhoneCode] 登录）。
     *
     * @param mobile 手机号
     */
    suspend fun sendCaptcha(
        mobile: String,
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "http://login.user.kugou.com",
                url = "/v7/send_mobile_code",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("businessid", 5)
                    put("mobile", mobile)
                    put("plat", 3)
                },
                encryptType = EncryptType.ANDROID,
                clearDefaultParams = false,
            )
        )
    }

    // ============================================================
    //  密码登录
    //  对齐 module/login.js
    // ============================================================

    /**
     * 账号密码登录。
     *
     * 加密流程：
     * 1. AES 加密 `{pwd, code, clienttime_ms}` → 密文 HEX + 随机种子
     * 2. RSA 加密 `{clienttime_ms, key: 种子}` → 签名 pk
     * 3. POST `/v9/login_by_pwd`
     *
     * ⚠️ 不会自动写入 CookieJar，请自行从响应 body.data 中提取 token/userid。
     *
     * @param username 用户名（酷狗号/手机号/邮箱）
     * @param password 密码
     */
    suspend fun loginByPassword(
        username: String,
        password: String,
    ): KuGouResponse {
        val dateNow = currentTimeMillis()

        // 1. AES 加密密码
        val encryptInput = buildJsonObject {
            put("pwd", password)
            put("code", "")
            put("clienttime_ms", dateNow)
        }.toString()
        val (encryptedParams, tempKey) = Crypto.aesEncryptAuto(encryptInput)

        // 2. RSA 加密密钥种子
        val rsaInput = buildJsonObject {
            put("clienttime_ms", dateNow)
            put("key", tempKey)
        }.toString()
        val pk = Crypto.rsaEncrypt(rsaInput.encodeToByteArray(), Crypto.publicRasKey).uppercase()

        // 3. 构建请求参数
        val dataMap = buildJsonObject {
            put("plat", 1)
            put("support_multi", 1)
            put("clienttime_ms", dateNow)
            put("t1", "562a6f12a6e803453647d16a08f5f0c2ff7eee692cba2ab74cc4c8ab47fc467561a7c6b586ce7dc46a63613b246737c03a1dc8f8d162d8ce1d2c71893d19f1d4b797685a4c6d3d81341cbde65e488c4829a9b4d42ef2df470eb102979fa5adcdd9b4eecfea8b909ff7599abeb49867640f10c3c70fc444effca9d15db44a9a6c907731e2bb0f22cd9b3536380169995693e5f0e2424e3378097d3813186e3fe96bbe7023808a0981b4e2b6135a76faac")
            put("t2", "31c4daf4cf480169ccea1cb7d4a209295865a9d2b788510301694db229b87807469ea0d41b4d4b9173c2151da7294aeebfc9738df154bbdf11a4e117bb5dff6a3af8ce5ce333e681c1f29a44038f27567d58992eb81283e080778ac77db1400fdf49b7cf7e26be2e5af4da7830cc3be4")
            put("t3", "MCwwLDAsMCwwLDAsMCwwLDA=")
            put("username", username)
            put("params", encryptedParams)
            put("pk", pk)
        }

        // 4. 发送请求
        return executor.execute(
            KuGouRequest(
                url = "/v9/login_by_pwd",
                method = HttpMethod.POST,
                data = dataMap,
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "login.user.kugou.com"),
            )
        )
    }

    // ============================================================
    //  手机验证码登录
    //  对齐 module/login_cellphone.js
    // ============================================================

    /**
     * 手机号 + 验证码登录。
     *
     * 加密流程与密码登录类似，但使用手机号和验证码作为凭据。
     *
     * ⚠️ 不会自动写入 CookieJar，请自行从响应 body.data 中提取 token/userid。
     *
     * @param mobile 手机号
     * @param code 验证码
     */
    suspend fun loginByPhoneCode(
        mobile: String,
        code: String,
    ): KuGouResponse {
        val dateNow = currentTimeMillis()

        // 1. AES 加密手机号和验证码
        val encryptInput = buildJsonObject {
            put("mobile", mobile)
            put("code", code)
        }.toString()
        val (encryptedParams, tempKey) = Crypto.aesEncryptAuto(encryptInput)

        // 手机号脱敏（前2位 + ***** + 最后1位）
        val maskedMobile = if (mobile.length >= 11) {
            "${mobile.substring(0, 2)}*****${mobile.substring(10, 11)}"
        } else {
            mobile
        }

        // 2. 构建请求参数
        val dataMap = buildJsonObject {
            put("plat", 1)
            put("support_multi", 1)
            put("t1", 0)
            put("t2", 0)
            put("clienttime_ms", dateNow)
            put("mobile", maskedMobile)
            put("key", RequestSigner(executor.config).signParamsKey(dateNow))
            put("params", encryptedParams)
            put("pk", Crypto.rsaEncrypt(
                buildJsonObject {
                    put("clienttime_ms", dateNow)
                    put("key", tempKey)
                }.toString().encodeToByteArray(),
                Crypto.publicRasKey
            ).uppercase())
            put("t3", "MCwwLDAsMCwwLDAsMCwwLDA=")
        }

        // 3. 发送请求
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://loginserviceretry.kugou.com",
                url = "/v7/login_by_verifycode",
                method = HttpMethod.POST,
                data = dataMap,
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "support-calm" to "1",
                    "User-Agent" to "Android16-1070-11440-130-0-LOGIN-wifi",
                ),
            )
        )
    }

    // ============================================================
    //  Token 刷新登录
    //  对齐 module/login_token.js
    // ============================================================

    /**
     * 使用已有 Token 刷新登录态。
     *
     * 加密流程：
     * 1. 使用固定 Key/IV AES 加密 `{clienttime, token}` → p3
     * 2. 随机 AES 加密空对象 → params + 种子
     * 3. RSA 加密 `{clienttime_ms, key: 种子}` → pk
     *
     * ⚠️ 不会自动写入 CookieJar，请自行从响应 body.data 中提取 token/userid。
     *
     * @param token 已有的登录 Token（为空时从 CookieJar 自动读取）
     * @param userid 用户 ID（为空时从 CookieJar 自动读取）
     */
    suspend fun loginByToken(
        token: String = "",
        userid: String = "",
    ): KuGouResponse {
        val dateNow = currentTimeMillis()
        val actualToken = token.ifEmpty { executor.cookieJar.getToken() }
        val actualUserid = userid.ifEmpty { executor.cookieJar.getUserid() }

        // 1. 使用固定 Key/IV 加密 clienttime + token → p3
        val fixedKey = "90b8382a1bb4ccdcf063102053fd75b8"
        val fixedIv = "f063102053fd75b8"
        val p3Input = buildJsonObject {
            put("clienttime", dateNow / 1000)
            put("token", actualToken)
        }.toString()
        val p3 = Crypto.aesEncryptWith(p3Input, fixedKey, fixedIv)

        // 2. 随机 AES 加密空对象 → params + 种子
        val (paramsHex, tempKey) = Crypto.aesEncryptAuto("{}")

        // 3. RSA 加密密钥种子
        val pk = Crypto.rsaEncrypt(
            buildJsonObject {
                put("clienttime_ms", dateNow)
                put("key", tempKey)
            }.toString().encodeToByteArray(),
            Crypto.publicRasKey
        )

        // 4. 构建请求参数
        val dataMap = buildJsonObject {
            put("dfid", executor.cookieJar.getDfid())
            put("p3", p3)
            put("plat", 1)
            put("t1", 0)
            put("t2", 0)
            put("t3", "MCwwLDAsMCwwLDAsMCwwLDA=")
            put("pk", pk)
            put("params", paramsHex)
            put("userid", actualUserid)
            put("clienttime_ms", dateNow)
        }

        // 5. 发送请求
        return executor.execute(
            KuGouRequest(
                baseUrl = "http://login.user.kugou.com",
                url = "/v5/login_by_token",
                method = HttpMethod.POST,
                data = dataMap,
                encryptType = EncryptType.ANDROID,
            )
        )
    }

    // ============================================================
    //  二维码登录
    //  对齐 module/login_qr_key.js / login_qr_check.js
    // ============================================================

    /**
     * 生成二维码登录的唯一 Key。
     *
     * 返回的 key 用于创建二维码图片和轮询扫码状态。
     *
     * @param type 应用类型，"standard" 或 "web"（影响 appid）
     */
    suspend fun createQrKey(
        type: String = "standard",
    ): KuGouResponse {
        val appid = if (type == "web") 1014 else 1001
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://login-user.kugou.com",
                url = "/v2/qrcode",
                method = HttpMethod.GET,
                params = mapOf(
                    "appid" to appid,
                    "type" to 1,
                    "plat" to 4,
                    "qrcode_txt" to "https://h5.kugou.com/apps/loginQRCode/html/index.html?appid=${executor.config.activeAppId}&",
                    "srcappid" to KuGouConfig.SRC_APP_ID,
                ),
                encryptType = EncryptType.WEB,
            )
        )
    }

    /**
     * 根据 key 生成二维码登录 URL（本地生成，不发送网络请求）。
     *
     * @param key 由 [createQrKey] 返回的 key
     * @return 二维码登录页面的完整 URL
     */
    fun createQrCodeUrl(key: String): String {
        return "https://h5.kugou.com/apps/loginQRCode/html/index.html?qrcode=$key"
    }

    /**
     * 轮询检查二维码扫码状态。
     *
     * 状态码：
     * - 0: 二维码已过期
     * - 1: 等待扫码
     * - 2: 已扫码，等待确认
     * - 4: 授权登录成功（此时响应中会包含 token 和 userid）
     *
     * ⚠️ 不会自动写入 CookieJar，请自行从响应 body.data 中提取 token/userid。
     *
     * @param key 由 [createQrKey] 返回的 key
     */
    suspend fun checkQrCode(
        key: String,
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://login-user.kugou.com",
                url = "/v2/get_userinfo_qrcode",
                method = HttpMethod.GET,
                params = mapOf(
                    "plat" to 4,
                    "appid" to executor.config.activeAppId,
                    "srcappid" to KuGouConfig.SRC_APP_ID,
                    "qrcode" to key,
                ),
                encryptType = EncryptType.WEB,
            )
        )
    }
}
