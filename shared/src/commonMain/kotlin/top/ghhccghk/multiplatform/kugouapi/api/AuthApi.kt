package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.KuGouConfig
import top.ghhccghk.multiplatform.kugouapi.core.Crypto
import top.ghhccghk.multiplatform.kugouapi.core.HttpMethod
import top.ghhccghk.multiplatform.kugouapi.core.KuGouRequest
import top.ghhccghk.multiplatform.kugouapi.core.KuGouResponse
import top.ghhccghk.multiplatform.kugouapi.core.PlatformIdentity
import top.ghhccghk.multiplatform.kugouapi.core.RequestExecutor
import top.ghhccghk.multiplatform.kugouapi.core.RequestSigner
import top.ghhccghk.multiplatform.kugouapi.core.ResponseType
import top.ghhccghk.multiplatform.kugouapi.core.aesEncryptAuto
import top.ghhccghk.multiplatform.kugouapi.core.aesEncryptWith
import top.ghhccghk.multiplatform.kugouapi.core.aesDecryptWithSeed
import top.ghhccghk.multiplatform.kugouapi.core.currentTimeMillis
import top.ghhccghk.multiplatform.kugouapi.core.publicRasKey
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.statement.readRawBytes
import kotlinx.serialization.json.*
import kotlinx.serialization.json.put
import top.ghhccghk.multiplatform.kugouapi.core.activePublicRasKey
import top.ghhccghk.multiplatform.kugouapi.core.Fingerprint
import kotlin.random.Random

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
     * 该接口是访问酷狗大部分 API 的前提，返回的 dfid 会自动存储在 [top.ghhccghk.multiplatform.kugouapi.core.CookieJar] 中供后续请求使用。
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

        val p = Crypto.rsaEncryptPkcs1(rsaData, Crypto.activePublicRasKey(executor.config))

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
                    println("bytesStr:$bytesStr")
                    val decryptedJson = Crypto.aesDecryptBase64(bytesStr, encryptKey, iv)
                    val body = Json.parseToJsonElement(decryptedJson) as JsonObject
                    val data = body["data"]?.jsonObject ?: buildJsonObject {}
                    val dfid = data["dfid"]?.jsonPrimitive?.content
                    if (dfid != null) {
                        executor.cookieJar["dfid"] = dfid
                    }

                    // 合并本地身份信息到返回结果中，对齐格式
                    val mergedData = buildJsonObject {
                        data.forEach { (k, v) -> put(k, v) }
                        put("mid", executor.cookieJar.getMid())
                        put("guid", executor.cookieJar.getGuid())
                        put("serverDev", executor.cookieJar.getDev())
                        put("mac", executor.cookieJar["KUGOU_API_MAC"] ?: "02:00:00:00:00:00")
                    }

                    val mergedBody = buildJsonObject {
                        // 确保 status 和 error_code 存在
                        put("status", body["status"] ?: JsonPrimitive(1))
                        put("error_code", body["error_code"] ?: JsonPrimitive(0))
                        body.forEach { (k, v) ->
                            if (k != "data" && k != "status" && k != "error_code") put(k, v)
                        }
                        put("data", mergedData)
                    }

                    return response.copy(body = mergedBody)
                } catch (e: Exception) {
                    return response.copy(body = buildJsonObject {
                        put("status", 0)
                        put("error_code", -1)
                        put("msg", "Decrypt failed: ${ e.printStackTrace()}")
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
        val pk = Crypto.rsaEncrypt(rsaInput.encodeToByteArray(), Crypto.activePublicRasKey(executor.config)).uppercase()

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
                Crypto.activePublicRasKey(executor.config)
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
        val isLite = executor.config.isLite

        // 1. 使用 Key/IV 加密 clienttime + token → p3（isLite 时使用不同密钥）
        val fixedKey = if (isLite) "c24f74ca2820225badc01946dba4fdf7" else "90b8382a1bb4ccdcf063102053fd75b8"
        val fixedIv = if (isLite) "adc01946dba4fdf7" else "f063102053fd75b8"
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
            Crypto.activePublicRasKey(executor.config)
        )

        // 4. isLite 时计算 t1 / t2
        val t2 = if (isLite) {
            val guid = executor.cookieJar.getGuid()
            val mac = executor.cookieJar.getMac()
            val dev = executor.cookieJar.getDev()
            Crypto.aesEncryptWith(
                "$guid|0f607264fc6318a92b9e13c65db7cd3c|$mac|$dev|$dateNow",
                "fd14b35e3f81af3817a20ae7adae7020", "17a20ae7adae7020"
            )
        } else "0"

        val t1Plain = executor.cookieJar["t1"]?.let { "$it|$dateNow" } ?: "|$dateNow"
        val t1 = if (isLite) Crypto.aesEncryptWith(
            t1Plain, "5e4ef500e9597fe004bd09a46d8add98", "04bd09a46d8add98"
        ) else "0"

        // 5. 构建请求参数
        val dataMap = buildJsonObject {
            put("dfid", executor.cookieJar.getDfid())
            put("p3", p3)
            put("plat", 1)
            put("t1", t1)
            put("t2", t2)
            put("t3", "MCwwLDAsMCwwLDAsMCwwLDA=")
            put("pk", pk)
            put("params", paramsHex)
            put("userid", actualUserid)
            put("clienttime_ms", dateNow)
            if (isLite) {
                put("dev", executor.cookieJar.getDev())
            }
        }

        // 6. 发送请求并解密 secu_params
        val response = executor.execute(
            KuGouRequest(
                baseUrl = "http://login.user.kugou.com",
                url = "/v5/login_by_token",
                method = HttpMethod.POST,
                data = dataMap,
                encryptType = EncryptType.ANDROID,
            )
        )

        if (response.status == 200) {
            val body = response.body
            val status = body["status"]?.jsonPrimitive?.intOrNull
            if (status == 1) {
                val data = body["data"]?.jsonObject
                val secuParams = data?.get("secu_params")?.jsonPrimitive?.content
                println("KuGou secu_params raw=$secuParams")
                if (!secuParams.isNullOrEmpty()) {
                    try {
                        val decrypted = Crypto.aesDecryptWithSeed(secuParams, tempKey)
                        println("KuGou decrypted=$decrypted")
                        val tokenObj = try {
                            Json.parseToJsonElement(decrypted) as? JsonObject
                        } catch (e: Exception) {
                            println("KuGou tokenObj parse failed: $e")
                            null
                        }
                        val mergedData = buildJsonObject {
                            data.forEach { (k, v) -> put(k, v) }
                            if (tokenObj != null) {
                                tokenObj.forEach { (k, v) -> put(k, v) }
                            } else {
                                put("token", decrypted)
                            }
                        }
                        val mergedBody = buildJsonObject {
                            body.forEach { (k, v) ->
                                if (k == "data") put(k, mergedData) else put(k, v)
                            }
                        }
                        return response.copy(body = mergedBody)
                    } catch (e: Exception) {
                        println("KuGou merge failed: $e")
                    }
                }
            } else {
                println("KuGou login failed, status=$status, body=$body")
            }
        }
        return response
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
                    "srcappid" to KuGouConfig.Companion.SRC_APP_ID,
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
                    "srcappid" to KuGouConfig.Companion.SRC_APP_ID,
                    "qrcode" to key,
                ),
                encryptType = EncryptType.WEB,
            )
        )
    }

    /**
     * 获取已登录设备列表。
     * 对齐 module/login_device.js
     */
    suspend fun getLoginDevices(): KuGouResponse {
        val clientTimeMs = currentTimeMillis()
        val token = executor.cookieJar.getToken()
        val userId = executor.cookieJar.getUserid()

        val (encryptedParams, tempKey) = Crypto.aesEncryptAuto(buildJsonObject {
            put("token", token)
        }.toString())

        val pk = Crypto.rsaEncrypt(
            buildJsonObject {
                put("clienttime_ms", clientTimeMs)
                put("key", tempKey)
            }.toString().encodeToByteArray(),
            Crypto.activePublicRasKey(executor.config)
        ).uppercase()

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://userinfoservice.kugou.com",
                url = "/v2/get_dev",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("plat", 1)
                    put("userid", userId)
                    put("clienttime_ms", clientTimeMs)
                    put("pk", pk)
                    put("params", encryptedParams)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 登出/踢出选定设备。
     * 对齐 module/login_device_kick.js
     */
    suspend fun kickDevice(targetMid: String): KuGouResponse {
        val clientTimeMs = currentTimeMillis()
        val dateNow = currentTimeMillis()
        val token = executor.cookieJar.getToken()
        val userId = executor.cookieJar.getUserid()

        val (encryptedToken, _) = Crypto.aesEncryptAuto(buildJsonObject {
            put("token", token)
        }.toString())

        return executor.execute(
            KuGouRequest(
                url = "/loginservice/v1/dev_logout",
                method = HttpMethod.GET,
                params = mapOf(
                    "appid" to executor.config.activeAppId,
                    "clientver" to executor.config.activeClientVersion,
                    "clienttime" to clientTimeMs,
                    "mid" to PlatformIdentity.calculateMid(targetMid),
                    "uuid" to "-",
                    "dfid" to executor.cookieJar.getDfid(),
                    "plat" to 1,
                    "userid" to userId,
                    "token" to encryptedToken,
                    "t_mid" to executor.cookieJar.getGuid(),
                    "t" to dateNow,
                    "t_appid" to 3116,
                    "t_clientver" to 10597,
                    "srcappid" to KuGouConfig.Companion.SRC_APP_ID,
                    "signature" to RequestSigner(executor.config).signParamsKey(dateNow)
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("Host" to "gateway.kugou.com")
            )
        )
    }

    /**
     * 微信开放平台登录 (第一步: 获取二维码)。
     * 对齐 module/login_wx_create.js
     *
     * 流程:
     * 1. 调用微信接口获取 access_token
     * 2. 用 access_token 获取 sdk_ticket
     * 3. 计算签名并请求 qrconnect 获取二维码信息
     *
     */
    suspend fun createWxLogin(): KuGouResponse {
        val isLite = executor.config.isLite
        val appId = if (isLite) KuGouConfig.WX_LITE_APP_ID else KuGouConfig.WX_APP_ID
        val secret = if (isLite) KuGouConfig.WX_LITE_SECRET else KuGouConfig.WX_SECRET

        val client = HttpClient()
        try {
            // 1. 获取 access_token
            val tokenResp = client.get("https://api.weixin.qq.com/cgi-bin/token") {
                parameter("appid", appId)
                parameter("secret", secret)
                parameter("grant_type", "client_credential")
            }.body<JsonObject>()

            val accessToken = tokenResp["access_token"]?.jsonPrimitive?.content
                ?: return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "获取微信 access_token 失败")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )

            // 2. 获取 sdk_ticket
            val ticketResp = client.get("https://api.weixin.qq.com/cgi-bin/ticket/getticket") {
                parameter("access_token", accessToken)
                parameter("type", 2)
            }.body<JsonObject>()

            val ticket = ticketResp["ticket"]?.jsonPrimitive?.content
            val errcode = ticketResp["errcode"]?.jsonPrimitive?.intOrNull ?: -1
            if (errcode != 0 || ticket == null) {
                return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "获取微信 sdk_ticket 失败: $ticketResp")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )
            }

            // 3. 计算签名
            val timestamp = currentTimeMillis() / 1000
            val noncestr = Crypto.md5(PlatformIdentity.generateRandomString(16))
            val signatureParams = "appid=${appId}&noncestr=${noncestr}&sdk_ticket=${ticket}&timestamp=${timestamp}"
            val signature = Crypto.sha1(signatureParams)

            // 4. 请求 qrconnect 获取二维码
            val qrResp = client.get("https://open.weixin.qq.com/connect/sdk/qrconnect") {
                parameter("appid", appId)
                parameter("noncestr", noncestr)
                parameter("timestamp", timestamp)
                parameter("scope", "snsapi_userinfo")
                parameter("signature", signature)
            }.body<JsonObject>()

            val qrErrcode = qrResp["errcode"]?.jsonPrimitive?.intOrNull ?: -1
            if (qrErrcode != 0) {
                return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "微信 qrconnect 请求失败: $qrResp")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )
            }

            // 5. 补充 qrcodeurl 并返回
            val uuid = qrResp["uuid"]?.jsonPrimitive?.content ?: ""
            val qrcode = qrResp["qrcode"]?.jsonObject ?: buildJsonObject {}
            val qrcodeUrl = "https://open.weixin.qq.com/connect/confirm?uuid=$uuid"

            val mergedBody = buildJsonObject {
                qrResp.forEach { (k, v) ->
                    if (k == "qrcode") {
                        put(k, buildJsonObject {
                            qrcode.forEach { (qk, qv) -> put(qk, qv) }
                            put("qrcodeurl", qrcodeUrl)
                        })
                    } else {
                        put(k, v)
                    }
                }
                if (!qrResp.containsKey("qrcode")) {
                    put("qrcode", buildJsonObject { put("qrcodeurl", qrcodeUrl) })
                }
            }

            return KuGouResponse(status = 200, body = mergedBody, cookies = emptyMap(), headers = emptyMap())
        } catch (e: Exception) {
            return KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", "微信登录异常: ${e.message}")
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        } finally {
            client.close()
        }
    }

    /**
     * 微信登录状态检查。
     * 对齐 module/login_wx_check.js
     */
    suspend fun checkWxLogin(uuid: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://long.open.weixin.qq.com",
                url = "/connect/l/qrconnect",
                method = HttpMethod.GET,
                params = mapOf("f" to "json", "uuid" to uuid),
                encryptType = EncryptType.WEB
            )
        )
    }

    // ============================================================
    //  开放平台登录
    //  对齐 module/login_openplat.js
    // ============================================================

    /**
     * 开放平台登录 (微信 OAuth code 登录)。
     *
     * 流程:
     * 1. 使用微信授权 code 换取 access_token + openid
     * 2. AES 加密 access_token + RSA 加密密钥种子
     * 3. POST `/v6/login_by_openplat`
     * 4. 解密响应中的 secu_params 获取 token
     *
     * ⚠️ 不会自动写入 CookieJar，请自行从响应 body.data 中提取 token/userid。
     *
     * @param code 微信 OAuth 授权码
     */
    suspend fun loginByOpenPlat(
        code: String
    ): KuGouResponse {
        val isLite = executor.config.isLite
        val appId = if (isLite) KuGouConfig.WX_LITE_APP_ID else KuGouConfig.WX_APP_ID
        val secret = if (isLite) KuGouConfig.WX_LITE_SECRET else KuGouConfig.WX_SECRET

        val client = HttpClient()
        try {
            val tokenResp = client.get("https://api.weixin.qq.com/sns/oauth2/access_token") {
                parameter("appid", appId)
                parameter("secret", secret)
                parameter("code", code)
                parameter("grant_type", "authorization_code")
            }.body<JsonObject>()

            val accessToken = tokenResp["access_token"]?.jsonPrimitive?.content
            val openid = tokenResp["openid"]?.jsonPrimitive?.content

            if (accessToken.isNullOrEmpty() || openid.isNullOrEmpty()) {
                return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "获取微信 access_token 失败: $tokenResp")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )
            }

            val dateNow = currentTimeMillis()
            val (encryptedParams, tempKey) = Crypto.aesEncryptAuto(
                buildJsonObject { put("access_token", accessToken) }.toString()
            )
            val pk = Crypto.rsaEncrypt(
                buildJsonObject {
                    put("clienttime_ms", dateNow)
                    put("key", tempKey)
                }.toString().encodeToByteArray(),
                Crypto.activePublicRasKey(executor.config)
            ).uppercase()

            val dev = executor.cookieJar.getDev()
            val guid = executor.cookieJar.getGuid()
            val mac = executor.cookieJar.getMac()

            val t2Key = "fd14b35e3f81af3817a20ae7adae7020"
            val t2Iv = "17a20ae7adae7020"
            val t1Key = "5e4ef500e9597fe004bd09a46d8add98"
            val t1Iv = "04bd09a46d8add98"

            val t2Plain = "$guid|0f607264fc6318a92b9e13c65db7cd3c|$mac|$dev|$dateNow"
            val t2 = if (isLite) Crypto.aesEncryptWith(t2Plain, t2Key, t2Iv) else "0"
            val t1Plain = "|$dateNow"
            val t1 = if (isLite) Crypto.aesEncryptWith(t1Plain, t1Key, t1Iv) else "0"

            val dataMap = buildJsonObject {
                put("dev", dev)
                put("force_login", 1)
                put("partnerid", 36)
                put("clienttime_ms", dateNow)
                put("t1", t1)
                put("t2", t2)
                put("t3", "MCwwLDAsMCwwLDAsMCwwLDA=")
                put("openid", openid)
                put("params", encryptedParams)
                put("pk", pk)
            }

            val response = executor.execute(
                KuGouRequest(
                    url = "/v6/login_by_openplat",
                    method = HttpMethod.POST,
                    data = dataMap,
                    encryptType = EncryptType.ANDROID,
                    headers = mapOf("x-router" to "login.user.kugou.com")
                )
            )

            if (response.status == 200) {
                val secuParams = response.body["data"]
                    ?.jsonObject?.get("secu_params")
                    ?.jsonPrimitive?.content
                if (!secuParams.isNullOrEmpty()) {
                    try {
                        val decrypted = Crypto.aesDecryptWithSeed(secuParams, tempKey)
                        val tokenBody = Json.parseToJsonElement(decrypted) as? JsonObject
                        if (tokenBody != null) {
                            val merged = buildJsonObject {
                                response.body.forEach { (k, v) ->
                                    if (k == "data") {
                                        put(k, buildJsonObject {
                                            v.jsonObject.forEach { (dk, dv) -> put(dk, dv) }
                                            tokenBody.forEach { (tk, tv) -> put(tk, tv) }
                                        })
                                    } else {
                                        put(k, v)
                                    }
                                }
                            }
                            return response.copy(body = merged)
                        } else {
                            val merged = buildJsonObject {
                                response.body.forEach { (k, v) ->
                                    if (k == "data") {
                                        put(k, buildJsonObject {
                                            v.jsonObject.forEach { (dk, dv) -> put(dk, dv) }
                                            put("token", decrypted)
                                        })
                                    } else {
                                        put(k, v)
                                    }
                                }
                            }
                            return response.copy(body = merged)
                        }
                    } catch (_: Exception) {}
                }
            }

            return response
        } catch (e: Exception) {
            return KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", "开放平台登录异常: ${e.message}")
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        } finally {
            client.close()
        }
    }

    /**
     * 获取验证码数据
     * 对齐 node module/get_verify_info.js
     *
     * @param eventid 事件 ID（由 SSA 二次验证返回）
     */
    suspend fun getVerifyInfo(eventid: String): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L

        return executor.execute(
            KuGouRequest(
                url = "/verifyservice/v3/get_verify_info",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("eventid", eventid)
                    put("userid", userid)
                    put("platid", 2)
                    put("rtype", 1)
                    put("wasm", 1)
                    put("i", "")
                    put("sid", "")
                    put("edt", "")
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 验证用户信息（验证码提交）
     * 对齐 node module/verify_user_info.js
     *
     * @param eventid 事件 ID
     * @param verifycode 验证码
     * @param vType 验证类型：23 = SMS，32 = CAPTCHA
     */
    suspend fun verifyUserInfo(
        eventid: String,
        verifycode: String = "",
        vType: Int = 23
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val mid = executor.cookieJar.getMid()
        val dfid = executor.cookieJar.getDfid()
        val webglHash = executor.cookieJar.getWebGLHash()

        // 生成 sid/edt 指纹
        val simulate = Fingerprint.generateSimulate(mid, userid.toString(), dfid, webglHash)

        // AES 加密
        val (aesEncrypted, aesKey) = if (vType == 23) {
            Crypto.aesEncryptAuto(buildJsonObject { }.toString())
        } else {
            Crypto.aesEncryptAuto(buildJsonObject { put("code", verifycode) }.toString())
        }

        // RSA 加密 AES 密钥
        val pk = Crypto.rsaEncrypt(
            buildJsonObject { put("key", aesKey) }.toString().encodeToByteArray(),
            Crypto.activePublicRasKey(executor.config)
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = "https://verifyservice.kugou.com",
                url = "/v4/verify_user_info",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("eventid", eventid)
                    put("userid", userid)
                    put("platid", 2)
                    put("v_type", vType)
                    put("wasm", 1)
                    put("i", "")
                    put("sid", simulate.sid)
                    put("edt", simulate.edt)
                    if (vType == 23) {
                        put("verifycode", verifycode)
                    } else {
                        put("code", verifycode)
                    }
                    put("pk", pk)
                    put("params", aesEncrypted)
                },
                params = mapOf("clientver" to 11510),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 生成 sid/edt 指纹并调用 verify_user_info
     * 对齐 node module/sidedt.js
     *
     * @param eventid 事件 ID
     * @param vType 验证类型：23 = SMS，32 = CAPTCHA
     * @param verifycode 验证码
     */
    suspend fun generateSidEdt(
        eventid: String = "",
        vType: Int = 23,
        verifycode: String = ""
    ): KuGouResponse {
        return verifyUserInfo(eventid, verifycode, vType)
    }

    /**
     * 用户验证（获取 auth token）
     * 对齐 module/user_verify.js
     *
     * 注意：成功时会自动将 auth 写入 CookieJar
     */
    suspend fun userVerify(): KuGouResponse {
        val response = executor.execute(
            KuGouRequest(
                baseUrl = "http://trackercdngz.kugou.com",
                url = "/v1/user_verify",
                method = HttpMethod.GET,
                params = mapOf("module_id" to 51),
                encryptType = EncryptType.ANDROID
            )
        )

        // 如果成功，自动保存 auth 到 cookie
        val body = response.body
        if (body["status"]?.jsonPrimitive?.intOrNull == 1) {
            val data = body["data"]?.jsonObject
            val auth = data?.get("auth")?.jsonPrimitive?.contentOrNull
            if (!auth.isNullOrEmpty()) {
                executor.cookieJar.setAuth(auth)
            }
        }

        return response
    }

    // ============================================================
    //  QQ 扫码登录
    //  对齐 module/login_qq_qr_create.js, login_qq_qr_check.js, login_qq.js
    // ============================================================

    /**
     * QQ 扫码登录 - 生成二维码
     * 对齐 module/login_qq_qr_create.js
     *
     * 返回二维码图片 Base64、qrsig、ptqrtoken 等参数，
     * 需要传递给 [checkQqQrCode] 进行轮询检测。
     */
    suspend fun createQqQrCode(): KuGouResponse {
        val isLite = executor.config.isLite
        val clientId = if (isLite) KuGouConfig.QQ_LITE_APP_ID else KuGouConfig.QQ_APP_ID
        val time = currentTimeMillis() / 1000
        val apkSigMd5 = "fe4a24d80fcf253a00676a808f62c2c6"
        val sign = Crypto.md5("_")

        val client = HttpClient()
        try {
            // 1. 获取授权页面
            val authParams = buildString {
                append("cancel_display=1&sdkp=a&display=mobile&format=json")
                append("&sign=&sdkv=3.5.11.lite&response_type=token")
                append("&status_os=11&client_id=&switch=1")
                append("&status_version=30&show_download_ui=true")
                append("&pf=openmobile_android&scope=all&compat_v=1")
                append("&status_machine=MEIZU+18+Pro&style=qr&time=")
                append("&redirect_uri=auth://tauth.qq.com/")
            }

            val authResp = client.get("https://openmobile.qq.com/oauth2.0/m_authorize?") {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    append("Referer", "https://xui.ptlogin2.qq.com/")
                }
            }.bodyAsText()

            // 解析 xlogin URL
            val srcMatch = Regex("""src = "([^"]+)"""").find(authResp)
            if (srcMatch == null) {
                return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "m_authorize 响应异常，未找到 xlogin 地址")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )
            }

            val xloginUrl = srcMatch.groupValues[1]
                .replace(Regex("""\\x([0-9A-Fa-f]{2})""")) {
                    it.groupValues[1].toInt(16).toChar().toString()
                }

            // 2. 获取 pt_login_sig cookie
            val xloginResp = client.get(xloginUrl) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    append("Referer", "https://xui.ptlogin2.qq.com/")
                }
            }

            val ptLoginSig = xloginResp.headers.getAll("Set-Cookie")
                ?.firstOrNull { it.startsWith("pt_login_sig=") }
                ?.split(";")?.firstOrNull()?.substringAfter("=") ?: ""

            // 3. 获取二维码图片
            val t = Random.nextDouble()
            val qrResp =
                client.get("https://xui.ptlogin2.qq.com/ssl/ptqrshow?s=8&e=0&appid=716027609&type=0&t=&daid=381&pt_3rd_aid=") {
                    headers {
                        append(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        )
                        append("Referer", xloginUrl)
                    }
                }.readRawBytes()

            val qrsig = "" // 需要从响应 cookie 中提取
            // 获取 qrsig
            val qrResp2 = client.get("https://xui.ptlogin2.qq.com/ssl/ptqrshow?s=8&e=0&appid=716027609&type=0&t=&daid=381&pt_3rd_aid=") {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    append("Referer", xloginUrl)
                }
            }

            val actualQrsig = qrResp2.headers.getAll("Set-Cookie")
                ?.firstOrNull { it.startsWith("qrsig=") }
                ?.split(";")?.firstOrNull()?.substringAfter("=") ?: ""

            if (actualQrsig.isEmpty()) {
                return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "未获取到 qrsig")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )
            }

            // 计算 ptqrtoken
            val ptqrtoken = hash33(actualQrsig)

            return KuGouResponse(
                status = 200,
                body = buildJsonObject {
                    put("qrcode", Crypto.encodeBase64(qrResp))
                    put("qrsig", actualQrsig)
                    put("ptqrtoken", ptqrtoken)
                    put("pt_login_sig", ptLoginSig)
                    put("xlogin_url", xloginUrl)
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        } catch (e: Exception) {
            return KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", e.message ?: "未知错误")
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        } finally {
            client.close()
        }
    }

    /**
     * QQ 扫码登录 - 检测扫码状态
     * 对齐 module/login_qq_qr_check.js
     *
     * @param qrsig 由 [createQqQrCode] 返回
     * @param ptqrtoken 由 [createQqQrCode] 返回
     * @param ptLoginSig 由 [createQqQrCode] 返回
     * @param xloginUrl 由 [createQqQrCode] 返回
     * @param ptOpenloginData 由 [createQqQrCode] 返回
     */
    suspend fun checkQqQrCode(
        qrsig: String,
        ptqrtoken: Int,
        ptLoginSig: String,
        xloginUrl: String,
        ptOpenloginData: String
    ): KuGouResponse {
        val isLite = executor.config.isLite
        val clientId = if (isLite) KuGouConfig.QQ_LITE_APP_ID else KuGouConfig.QQ_APP_ID

        val client = HttpClient()
        try {
            val sUrl = "http://connect.qq.com"
            val u1 = encodeURIComponent(sUrl)
            val pollUrl = buildString {
                append("https://xui.ptlogin2.qq.com/ssl/ptqrlogin?")
                append("u1=&from_ui=1&type=1&ptlang=2052")
                append("&ptqrtoken=&daid=381&aid=716027609")
                append("&pt_3rd_aid=&pt_openlogin_data=")
                append("&device=2&ptopt=1&pt_uistyle=35&jsver=v1.36.0")
                append("&login_sig=&r=")
            }

            val pollResp = client.get(pollUrl) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    append("Referer", xloginUrl)
                    append("Cookie", "qrsig=; pt_login_sig=")
                }
            }.bodyAsText()

            // 解析 ptuiCB(...) 响应
            val match = Regex("""ptuiCB\((.+)\)""").find(pollResp)
            if (match == null) {
                return KuGouResponse(
                    status = 502,
                    body = buildJsonObject {
                        put("status", 0)
                        put("msg", "ptqrlogin 响应解析失败")
                    },
                    cookies = emptyMap(),
                    headers = emptyMap()
                )
            }

            val parts = Regex("""'([^']*)'""").findAll(match.groupValues[1])
                .map { it.groupValues[1] }.toList()

            val code = parts.getOrNull(0) ?: ""
            val url = parts.getOrNull(2) ?: ""
            val msg = parts.getOrNull(4) ?: ""

            return when (code) {
                "66" -> {
                    // 等待扫码
                    KuGouResponse(
                        status = 200,
                        body = buildJsonObject {
                            put("status", "wait")
                            put("qrsig", qrsig)
                            put("ptqrtoken", ptqrtoken)
                            put("msg", "等待扫码")
                        },
                        cookies = emptyMap(),
                        headers = emptyMap()
                    )
                }
                "65" -> {
                    // 二维码已失效
                    KuGouResponse(
                        status = 200,
                        body = buildJsonObject {
                            put("status", "expired")
                            put("qrsig", qrsig)
                            put("ptqrtoken", ptqrtoken)
                            put("msg", "二维码已失效，请重新生成")
                        },
                        cookies = emptyMap(),
                        headers = emptyMap()
                    )
                }
                "0" -> {
                    // 登录成功，提取 openid 和 access_token
                    var openid = Regex("""openid=([^&#]+)""").find(url)?.groupValues?.get(1) ?: ""
                    var accessToken = Regex("""access_token=([^&#]+)""").find(url)?.groupValues?.get(1) ?: ""

                    if (openid.isEmpty() || accessToken.isEmpty()) {
                        return KuGouResponse(
                            status = 502,
                            body = buildJsonObject {
                                put("status", 0)
                                put("msg", "登录成功但未获取到 openid/access_token")
                            },
                            cookies = emptyMap(),
                            headers = emptyMap()
                        )
                    }

                    // 调用酷狗登录接口
                    return loginByQq(openid, accessToken)
                }
                else -> {
                    KuGouResponse(
                        status = 200,
                        body = buildJsonObject {
                            put("status", code)
                            put("qrsig", qrsig)
                            put("ptqrtoken", ptqrtoken)
                            put("msg", msg)
                        },
                        cookies = emptyMap(),
                        headers = emptyMap()
                    )
                }
            }
        } catch (e: Exception) {
            return KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", e.message ?: "未知错误")
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        } finally {
            client.close()
        }
    }

    /**
     * QQ 授权登录
     * 对齐 module/login_qq.js
     *
     * 使用 QQ 的 openid 和 access_token 登录酷狗
     *
     * @param openid QQ openid
     * @param accessToken QQ access_token
     */
    suspend fun loginByQq(
        openid: String,
        accessToken: String
    ): KuGouResponse {
        if (openid.isEmpty() || accessToken.isEmpty()) {
            return KuGouResponse(
                status = 502,
                body = buildJsonObject {
                    put("status", 0)
                    put("msg", "缺少 openid 或 access_token")
                },
                cookies = emptyMap(),
                headers = emptyMap()
            )
        }

        val isLite = executor.config.isLite
        val clientId = if (isLite) KuGouConfig.QQ_LITE_APP_ID else KuGouConfig.QQ_APP_ID

        // AES 加密 access_token
        val (aesEncrypted, aesKey) = Crypto.aesEncryptAuto(buildJsonObject {
            put("access_token", accessToken)
        }.toString())

        val dateNow = currentTimeMillis()
        val guid = executor.cookieJar.getGuid()
        val mac = executor.cookieJar.getMac()
        val dev = executor.cookieJar.getDev()

        // RSA 加密 AES 密钥
        val pk = Crypto.rsaEncrypt(
            buildJsonObject {
                put("clienttime_ms", dateNow)
                put("key", aesKey)
            }.toString().encodeToByteArray(),
            Crypto.activePublicRasKey(executor.config)
        ).uppercase()

        // t2 加密
        val t2Key = "fd14b35e3f81af3817a20ae7adae7020"
        val t2Iv = "17a20ae7adae7020"
        val t2Plain = "|0f607264fc6318a92b9e13c65db7cd3c|||"
        val t2 = Crypto.aesEncryptBase64(t2Plain, t2Key, t2Iv)

        // t1 加密
        val t1Key = "5e4ef500e9597fe004bd09a46d8add98"
        val t1Iv = "04bd09a46d8add98"
        val t1Plain = "|"
        val t1 = Crypto.aesEncryptBase64(t1Plain, t1Key, t1Iv)

        val response = executor.execute(
            KuGouRequest(
                url = "/v6/login_by_openplat",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("dev", dev)
                    put("force_login", 1)
                    put("partnerid", 1)
                    put("clienttime_ms", dateNow)
                    put("t1", if (isLite) t1 else "0")
                    put("t2", if (isLite) t2 else "0")
                    put("t3", "MCwwLDAsMCwwLDAsMCwwLDA=")
                    put("third_appid", clientId)
                    put("openid", openid)
                    put("params", aesEncrypted)
                    put("pk", pk)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "login.user.kugou.com")
            )
        )

        // 解密响应中的 token
        if (response.body["status"]?.jsonPrimitive?.intOrNull == 1) {
            try {
                val secuParams = response.body["data"]?.jsonObject?.get("secu_params")?.jsonPrimitive?.content
                if (!secuParams.isNullOrEmpty()) {
                    val decrypted = Crypto.aesDecryptBase64(secuParams, aesKey.substring(0, 32), aesKey.substring(16, 32))
                    val tokenData = Json.parseToJsonElement(decrypted).jsonObject
                    val token = tokenData["token"]?.jsonPrimitive?.content ?: ""

                    if (token.isNotEmpty()) {
                        // 自动保存到 CookieJar
                        executor.cookieJar.setToken(token)
                        tokenData["userid"]?.jsonPrimitive?.longOrNull?.let { executor.cookieJar.setUserid(it) }
                        tokenData["vip_type"]?.jsonPrimitive?.intOrNull?.let { executor.cookieJar["vip_type"] = it.toString() }
                        tokenData["vip_token"]?.jsonPrimitive?.contentOrNull?.let { executor.cookieJar.setVipToken(it) }
                    }

                    // 合并解密数据到响应
                    val mergedBody = buildJsonObject {
                        response.body.forEach { (k, v) ->
                            if (k == "data") {
                                put(k, buildJsonObject {
                                    v.jsonObject.forEach { (dk, dv) -> put(dk, dv) }
                                    tokenData.forEach { (tk, tv) -> put(tk, tv) }
                                })
                            } else {
                                put(k, v)
                            }
                        }
                    }
                    return response.copy(body = mergedBody)
                }
            } catch (_: Exception) {}
        }

        return response
    }

    /**
     * 计算 QQ qrsig 哈希值
     */
    private fun hash33(str: String): Int {
        var e = 0
        for (i in str.indices) {
            e += (e shl 5) + str[i].code
        }
        return 2147483647.toInt() and e
    }

    fun encodeURIComponent(value: String): String {
        val bytes = value.encodeToByteArray()
        val result = StringBuilder(bytes.size)

        val hex = "0123456789ABCDEF"

        for (byte in bytes) {
            val b = byte.toInt() and 0xFF

            val isUnescaped =
                b in 'A'.code..'Z'.code ||
                        b in 'a'.code..'z'.code ||
                        b in '0'.code..'9'.code ||
                        b == '-'.code ||
                        b == '_'.code ||
                        b == '.'.code ||
                        b == '!'.code ||
                        b == '~'.code ||
                        b == '*'.code ||
                        b == '\''.code ||
                        b == '('.code ||
                        b == ')'.code

            if (isUnescaped) {
                result.append(b.toChar())
            } else {
                result.append('%')
                result.append(hex[b ushr 4])
                result.append(hex[b and 0x0F])
            }
        }

        return result.toString()
    }
}