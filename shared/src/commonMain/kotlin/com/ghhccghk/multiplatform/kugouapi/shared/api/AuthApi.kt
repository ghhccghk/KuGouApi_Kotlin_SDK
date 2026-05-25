package com.ghhccghk.multiplatform.kugouapi.shared.api

import com.ghhccghk.multiplatform.kugouapi.shared.core.*
import com.ghhccghk.multiplatform.kugouapi.shared.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 认证与身份 API
 * 提供设备注册、登录、身份验证相关功能。
 */
class AuthApi(private val executor: RequestExecutor) {

    private val json = Json { ignoreUnknownKeys = true }

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
        // 传感器信息
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

        // 1. 准备设备指纹数据 (原始明文 JSON)
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
            // 传感器信息
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

        // 2. AES 加密流程 (等价于 playlistAesEncrypt)
        // 使用 6 位随机字符作为密钥种子，派生出 MD5 的前 16 位为 Key，后 16 位为 IV
        val aesKeyBase = PlatformIdentity.generateRandomString(6).lowercase()
        val md5Key = Crypto.md5(aesKeyBase)
        val encryptKey = md5Key.substring(0, 16)
        val iv = md5Key.substring(16, 32)
        
        val dataStr = dataMap.toString()
        val encryptedData = Crypto.aesEncryptBase64(dataStr, encryptKey, iv)

        // 3. RSA 加密流程 (等价于 rsaEncrypt2)
        // 拼接包含 AES 种子密钥、UID 和 Token 的元数据进行加密传输
        val rsaData = buildJsonObject {
            put("aes", aesKeyBase)
            val uid = useridStr.toLongOrNull() ?: 0L
            put("uid", uid)
            put("token", token)
        }.toString().encodeToByteArray()
        
        val p = Crypto.rsaEncryptPkcs1(rsaData, Crypto.publicRasKey)

        // 4. 发起网络请求
        val response = executor.execute(
            KuGouRequest(
                baseUrl = "https://userservice.kugou.com",
                url = "/risk/v2/r_register_dev",
                method = HttpMethod.POST,
                data = encryptedData, // Body 为 AES 加密后的密文
                params = mapOf(
                    "part" to 1,
                    "platid" to 1,
                    "p" to p // 签名参数 P 为 RSA 加密后的密钥元数据
                ),
                encryptType = EncryptType.ANDROID,
                responseType = ResponseType.BYTES // 注册接口返回加密二进制流
            )
        )

        // 5. 解析并解密响应
        if (response.status == 200) {
            val bytesStr = response.body["bytes"]?.jsonPrimitive?.content ?: ""
            if (bytesStr.isNotEmpty()) {
                try {
                    // 将返回的字节流转回 Base64 并使用之前的 AES Key 解密
                    val bytes = bytesStr.split(",").map { it.toByte() }.toByteArray()
                    val responseBase64 = Crypto.encodeBase64(bytes)
                    val decryptedJson = Crypto.aesDecryptBase64(responseBase64, encryptKey, iv)
                    
                    val body = json.parseToJsonElement(decryptedJson) as JsonObject
                    val data = body["data"]?.jsonObject ?: buildJsonObject {}
                    val dfid = data["dfid"]?.jsonPrimitive?.content
                    
                    // 成功获取后，将 dfid 存入持久化 CookieJar
                    if (dfid != null) {
                        executor.cookieJar["dfid"] = dfid
                    }

                    // 合并本地生成的身份信息（guid, mid, dev 等）到结果中，方便调用方使用
                    val mergedData = buildJsonObject {
                        data.forEach { (k, v) -> put(k, v) }
                        put("mid", executor.cookieJar.getMid())
                        put("guid", executor.cookieJar.getGuid())
                        put("serverDev", executor.cookieJar.getDev())
                        put("mac", executor.cookieJar["KUGOU_API_MAC"] ?: "02:00:00:00:00:00")
                    }

                    val mergedBody = buildJsonObject {
                        // 确保响应结构包含通用的 status 和 error_code 字段
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
                        put("msg", "解密失败: ${e.message}")
                    })
                }
            }
        }

        return response
    }

    suspend fun loginByPassword(username: String, password: String): KuGouResponse {
        val dateNow = currentTimeMillis()

        // === cryptoAesEncrypt 等价实现 ===
        // randomString(16).toLowerCase() → 16位随机小写
        val tempKey = PlatformIdentity.generateRandomString(16).lowercase()
        val aesKey = Crypto.md5(tempKey).substring(0, 32)   // 完整32位MD5，不是16位！
        val iv = aesKey.substring(aesKey.length - 16)        // key 的最后16位

        // 加密 {pwd, code, clienttime_ms}，输出 HEX
        val encryptInput = buildJsonObject {
            put("pwd", password)
            put("code", "")
            put("clienttime_ms", dateNow)
        }.toString()
        val encryptedParams = Crypto.aesEncrypt(encryptInput, aesKey, iv)  // ← HEX 输出

        // === cryptoRSAEncrypt 等价实现（原始模幂，非PKCS1）===
        val rsaInput = buildJsonObject {
            put("clienttime_ms", dateNow)
            put("key", tempKey)
        }.toString()
        val pk = Crypto.rsaEncryptRaw(rsaInput.encodeToByteArray(), Crypto.publicRasKey).uppercase()

        // === 构建 dataMap（key 顺序必须和 JS 一致）===
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

        val response = executor.execute(
            KuGouRequest(
                url = "/v9/login_by_pwd",
                method = HttpMethod.POST,
                data = dataMap,
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "login.user.kugou.com"),
            )
        )

        return response
    }
}
